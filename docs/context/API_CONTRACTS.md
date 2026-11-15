# Contratos de API REST, Seguridad y WebSocket
> Fuente de verdad: `src/main/java/com/software/territoriodeninos/controller/*Controller.java`, `*Api.java`, `dto/*.java`, `config/SecurityConfig.java`, `config/WebSocketConfig.java`, `exception/GlobalExceptionHandler.java`. Última sincronización: 2026-11-15 (commit c2aca55).

## 1. Configuración Global, Seguridad y Errores
- **Context Path del Servidor:** `/territorioninos` (`server.port=8080`). Todas las rutas de controladores empiezan con `/api/...`, por lo que la URL completa en local es `http://localhost:8080/territorioninos/api/...`.
- **Autenticación JWT (`JwtAuthenticationFilter` + `JwtUtil`):**
  - Header: `Authorization: Bearer <token>`.
  - Claims incluidos: `sub` (correo), `id` (Long), `nombre`, `rol` (`ADMINISTRADOR` | `TAQUILLERO` | `OPERARIO`).
  - Expiración: `${JWT_EXPIRATION_HOURS:24}` horas. Secret: `${JWT_SECRET}`.
  - Revocación en memoria: `TokenBlacklistService` invalida tokens al hacer `/api/auth/logout`, cambiar contraseña o inactivar usuario.
  - Protección fuerza bruta: `LoginAttemptService` bloquea temporalmente tras intentos fallidos (HTTP 429 `CuentaBloqueadaException`).
- **Rutas públicas (`permitAll` en `SecurityConfig`):**
  - `/api/auth/**`, `/ws-operacion/**`, `/error`, `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`.
- **CORS:** Permite orígenes `http://localhost:*`, `http://127.0.0.1:*`, redes privadas (`192.168.*.*`, `10.*.*`, `172.16-31.*.*`) y `${FRONTEND_URL:http://localhost:4200}`.
- **Estructura Estándar de Error (`GlobalExceptionHandler`):**
  - `ErrorResponse`: `{ "status": int, "mensaje": String, "detalles": String, "timestamp": LocalDateTime }`
  - `ValidationErrorResponse` (para `@Valid` / `MethodArgumentNotValidException` -> HTTP 400): `{ "status": 400, "message": "Errores de validación", "errors": { "<campo>": "<mensaje>" } }`
  - **Mapeo Excepción → HTTP Status:**
    - `400 Bad Request`: `ReglaNegocioException`, `IllegalArgumentException`, `ConstraintViolationException`, `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`, `MissingServletRequestParameterException`, `DateTimeParseException`, `TokenInvalidoException`.
    - `401 Unauthorized`: `CredencialesInvalidasException`, token JWT inválido/expirado/revocado.
    - `403 Forbidden`: `AccessDeniedException` (`@PreAuthorize`).
    - `404 Not Found`: `RecursoNoEncontradoException`.
    - `409 Conflict`: `ConflictoException`, `TurnoConflictoException`, `CorreoDuplicadoException`, `DataIntegrityViolationException`, `TicketInvalidoOperacionException` (por defecto 409 o el `HttpStatus` que indique la excepción).
    - `429 Too Many Requests`: `CuentaBloqueadaException`.
- **WebSocket / STOMP (`WebSocketConfig` + `OperacionWebSocketPublisher`):**
  - Endpoint STOMP (con y sin SockJS): `/territorioninos/ws-operacion`.
  - Broker prefixes: `/topic`, `/queue`. App destination prefix: `/app`.
  - Tópicos emitidos por `OperacionWebSocketPublisher`:
    - `/topic/atraccion/{atraccionId}` y `/topic/operacion/dashboard`
  - Payload: `OperacionWebSocketEvent { tipo, atraccionId, sesionId, mensaje, sesion: SesionJuegoResponseDTO, colaActualizada: ColaAtraccionResponseDTO, timestamp }`
  - Valores de `tipo`: `NUEVO_INGRESO`, `PAUSA`, `REANUDACION`, `PAUSA_GENERAL`, `ALERTA_PRE_EXPIRACION`, `COLA_ACTUALIZADA`, `TIEMPO_EXPIRADO`, `SESION_FINALIZADA`.

---

## 2. Catálogo de Endpoints por Módulo

> **Abreviaturas de Roles:** `ADMIN` = `ADMINISTRADOR`, `TAQ` = `TAQUILLERO`, `OPE` = `OPERARIO`, `TODOS` = `ADMINISTRADOR, TAQUILLERO, OPERARIO`, `PÚBLICO` = sin token.

### 2.1 Autenticación (`AuthController` — `/api/auth`)
| Método | Ruta | Rol | Request | Response | Notas / Errores |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | PÚBLICO | `LoginRequestDTO` | `200 AuthResponseDTO` | `401` credenciales/inactivo, `429` bloqueado. |
| `POST` | `/api/auth/forgot-password` | PÚBLICO | `ForgotPasswordRequest` | `200 Map("mensaje")` | Siempre retorna 200 (no filtra existencia de correo). |
| `PATCH` | `/api/auth/reset-password` | PÚBLICO | `ResetPasswordRequest` | `200 Map("mensaje")` | `400` si token inválido/expirado. |
| `POST` | `/api/auth/logout` | PÚBLICO | Header `Authorization` | `200 Map("mensaje")` | Revoca el JWT en `TokenBlacklistService`. |

### 2.2 Usuarios / Colaboradores (`UsuarioController` — `/api/usuarios`)
| Método | Ruta | Rol | Request | Response | Notas / Errores |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/usuarios` | `ADMIN` | `UsuarioRegistroDTO` | `201 UsuarioResponseDTO` | `409 CorreoDuplicadoException`. |
| `GET` | `/api/usuarios` | `ADMIN, TAQ` | — | `200 List<UsuarioResponseDTO>` | |
| `GET` | `/api/usuarios/{id}` | `TODOS` | Path `id` | `200 UsuarioResponseDTO` | `404` si no existe. |
| `PUT` | `/api/usuarios/{id}` | `ADMIN, TAQ` | `UsuarioUpdateDTO` | `200 UsuarioResponseDTO` | |
| `PATCH` | `/api/usuarios/{id}/password` | `TODOS` | `ChangePasswordRequest` | `200 Map("mensaje")` | Valida `passwordActual` y revoca tokens previos. |
| `PATCH` | `/api/usuarios/{id}/inactivar` | `ADMIN` | Path `id` | `200 UsuarioResponseDTO` | Pasa a `INACTIVO` y bloquea token en blacklist. |
| `PATCH` | `/api/usuarios/{id}/activar` | `ADMIN` | Path `id` | `200 UsuarioResponseDTO` | Pasa a `ACTIVO`. |
| `GET` | `/api/usuarios/rol/{rol}` | `ADMIN, TAQ` | Path `rol` | `200 List<UsuarioResponseDTO>` | Filtra por `ADMINISTRADOR`, `TAQUILLERO`, `OPERARIO`. |
| `DELETE` | `/api/usuarios/{id}` | `ADMIN` | Path `id` | `204 No Content` | |

### 2.3 Clientes y Menores (`ClienteController` — `/api/clientes`)
| Método | Ruta | Rol | Request | Response | Notas / Errores |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/clientes/buscar` | `TAQ, ADMIN` | Query `?documento=` o `?doc=` | `200 ClienteResponseDTO` | `404` si no existe. Incluye lista `menores`. |
| `GET` | `/api/clientes/documento/{documento}` | `TAQ, ADMIN` | Path `documento` | `200 ClienteResponseDTO` | Alias path de búsqueda por documento. |
| `POST` | `/api/clientes` | `TAQ, ADMIN` | `ClienteRequestDTO` | `201 ClienteResponseDTO` | Crea o sincroniza menores asociados. |
| `PUT` | `/api/clientes/{id}` | `TAQ, ADMIN` | `ClienteRequestDTO` | `200 ClienteResponseDTO` | Actualiza datos del adulto y su lista de menores. |

### 2.4 Configuración del Parque (`ConfiguracionParqueController` — `/api/configuracion`)
| Método | Ruta | Rol | Request | Response | Notas |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/configuracion` | `TODOS` | — | `200 ConfiguracionParqueDTO` | Crea config por defecto si la tabla está vacía. |
| `PUT` | `/api/configuracion` | `ADMIN` | `ConfiguracionParqueDTO` | `200 ConfiguracionParqueDTO` | Actualiza NIT, razón social, IVA, papel (`58`/`80`), gracia. |

### 2.5 Atracciones y Tarifas (`AtraccionController`, `TarifaTiempoController`, `TarifaEventoController`)
| Método | Ruta (incluye alias) | Rol | Request | Response | Notas |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/atracciones` | `ADMIN` | `AtraccionRequestDTO` | `201 AtraccionResponseDTO` | `409` si nombre duplicado. |
| `GET` | `/api/atracciones` | `ADMIN, TAQ` | — | `200 List<AtraccionResponseDTO>` | Todas las atracciones. |
| `GET` | `/api/atracciones/activas` | `TODOS` | — | `200 List<AtraccionResponseDTO>` | Solo `estado == ACTIVO`. |
| `GET` | `/api/atracciones/{id}` | `TODOS` | Path `id` | `200 AtraccionResponseDTO` | |
| `PUT` | `/api/atracciones/{id}` | `ADMIN` | `AtraccionRequestDTO` | `200 AtraccionResponseDTO` | |
| `PATCH` | `/api/atracciones/{id}/estado` | `ADMIN` | Query `?estado=` | `200 AtraccionResponseDTO` | `ACTIVO`, `INACTIVO`, `MANTENIMIENTO`. |
| `DELETE` | `/api/atracciones/{id}` | `ADMIN` | Path `id` | `204 No Content` | Bloquea si tiene ventas/sesiones históricas. |
| `POST` | `/api/tarifas-tiempo` (o `/api/rates`) | `ADMIN` | `TarifaTiempoRequestDTO` | `201 TarifaTiempoResponseDTO` | `409` si `(atraccionId, duracionMinutos)` ya existe. |
| `GET` | `/api/tarifas-tiempo/atraccion/{atraccionId}` (o `/attraction/{atraccionId}`) | `TODOS` | Path `atraccionId` | `200 List<TarifaTiempoResponseDTO>` | |
| `GET` | `/api/tarifas-tiempo/{id}` | `TODOS` | Path `id` | `200 TarifaTiempoResponseDTO` | |
| `PUT` | `/api/tarifas-tiempo/{id}` | `ADMIN` | `TarifaTiempoRequestDTO` | `200 TarifaTiempoResponseDTO` | |
| `DELETE` | `/api/tarifas-tiempo/{id}` | `ADMIN` | Path `id` | `204 No Content` | |
| `POST` | `/api/tarifas-evento` (o `/api/event-rates`) | `ADMIN` | `TarifaEventoRequestDTO` | `201 TarifaEventoResponseDTO` | `409` si `(atraccionId, intentos)` ya existe. |
| `GET` | `/api/tarifas-evento/atraccion/{atraccionId}` (o `/attraction/{atraccionId}`) | `TODOS` | Path `atraccionId` | `200 List<TarifaEventoResponseDTO>` | |
| `GET` | `/api/tarifas-evento/{id}` | `TODOS` | Path `id` | `200 TarifaEventoResponseDTO` | |
| `PUT` | `/api/tarifas-evento/{id}` | `ADMIN` | `TarifaEventoRequestDTO` | `200 TarifaEventoResponseDTO` | |
| `DELETE` | `/api/tarifas-evento/{id}` | `ADMIN` | Path `id` | `204 No Content` | |

### 2.6 Asignaciones de Operarios (`AsignacionOperarioController` — `/api/asignaciones` y `/api/assignments`)
| Método | Ruta | Rol | Request | Response | Notas |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/asignaciones` | `ADMIN` | `AsignacionRequestDTO` | `201 AsignacionResponseDTO` | Si `forzarReasignacion=true`, cierra la activa previa; si no, lanza `409`. |
| `POST` | `/api/asignaciones/reasignar` | `ADMIN` | `ReasignacionRequestDTO` | `200 AsignacionResponseDTO` | Cierra asignación actual (`REASIGNADA`) y crea nueva `ACTIVA`. |
| `PATCH` | `/api/asignaciones/{id}/finalizar` | `ADMIN` | Path `id` | `200 AsignacionResponseDTO` | Marca `FINALIZADA` con `horaFin=now()`. |
| `DELETE` | `/api/asignaciones/{id}` | `ADMIN` | Path `id` | `200 AsignacionResponseDTO` | Alias de `finalizar(id)`. |
| `GET` | `/api/asignaciones/activas` | `ADMIN, TAQ, OPERARIO` | — | `200 List<AsignacionResponseDTO>` | |
| `GET` | `/api/asignaciones/operario/{usuarioId}/activa` (o `/operator/{usuarioId}/current`) | `TODOS` | Path `usuarioId` | `200 AsignacionResponseDTO` | `404` si el operario no tiene asignación activa. |
| `GET` | `/api/asignaciones/atraccion/{atraccionId}/activas` | `TODOS` | Path `atraccionId` | `200 List<AsignacionResponseDTO>` | |
| `GET` | `/api/asignaciones/operario/{usuarioId}/historial` | `ADMIN` | Path `usuarioId` | `200 List<AsignacionResponseDTO>` | |

### 2.7 Turnos de Caja (`TurnoCajaController` — `/api/turnos`)
| Método | Ruta | Rol | Request | Response | Notas |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/turnos/abrir` | `TAQ, ADMIN` | `AperturaCajaDTO` | `201 TurnoCajaResponseDTO` | `409 TurnoConflictoException` si el usuario ya tiene turno `ABIERTO`. |
| `GET` | `/api/turnos/activo` | `TAQ, ADMIN` | — | `200 TurnoCajaResponseDTO` o `204` | **Cierre ciego:** retorna `fromEntityCiego` (`ventasEfectivoSistema=null`, `totalEsperado=null`). |
| `PUT` / `POST` | `/api/turnos/cerrar` | `TAQ, ADMIN` | `CierreCajaDTO` | `200 TurnoCajaResponseDTO` | Calcula ventas en efectivo del turno, diferencia y cierra turno. |
| `POST` | `/api/turnos/{id}/arqueo` | `TAQ, ADMIN` | `ArqueoCajaDTO` (opc.) | `200 Map<String,Object>` | Calcula y devuelve `idTurno`, `montoEsperado`, `totalFisico`, `diferencia`, `estadoBalance`. |
| `GET` | `/api/turnos` | `ADMIN, TAQ` | Query `?fecha=YYYY-MM-DD` (opc.) | `200 List<TurnoCajaResponseDTO>` | Historial de turnos. |

### 2.8 Ventas y Punto de Venta (`VentaController` — `/api/ventas`)
| Método | Ruta | Rol | Request | Response | Notas |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/ventas/catalogo` | `TAQ, ADMIN` | — | `200 List<CatalogoAtraccionVentaDTO>` | Incluye tarifas activas, ocupación en vivo, `tieneCupo` y `tieneOperarioAsignado`. Cache 5s. |
| `POST` | `/api/ventas/validar` | `TAQ, ADMIN` | `List<ItemVentaValidacionDTO>` | `200 ResultadoValidacionVentaDTO` | Valida aforo disponible y consistencia de precios contra BD. |
| `POST` | `/api/ventas/cotizar` | `TAQ, ADMIN` | `CotizacionRequestDTO` | `200 CotizacionResponseDTO` | Calcula subtotal, descuento, base gravable, IVA y `totalNeto`. |
| `POST` | `/api/ventas` o `/api/ventas/procesar` | `TAQ, ADMIN` | `VentaProcesarRequestDTO` | `201 VentaResponseDTO` | Requiere turno `ABIERTO`. Persiste `Venta`, `DetalleVenta`, genera `Ticket`s con UUID único y registra `AuditoriaTicket`. |
| `GET` | `/api/ventas/{id}` | `TAQ, ADMIN` | Path `id` | `200 VentaResponseDTO` | |
| `GET` | `/api/ventas/turno/{turnoId}` | `TAQ, ADMIN` | Path `turnoId` | `200 List<VentaResponseDTO>` | |

### 2.9 Tickets, QR y Reimpresión (`TicketController` — `/api/tickets`)
| Método | Ruta | Rol | Request | Response | Notas |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/tickets/buscar` | `TODOS` | Query `?criterio=` | `200 List<TicketConsultaDTO>` | Busca por UUID, prefijo corto (8 hex), documento cliente/menor o nombre. |
| `GET` | `/api/tickets/{uuid}` | `TODOS` | Path `uuid` | `200 TicketConsultaDTO` | |
| `GET` | `/api/tickets/{uuid}/qr` | `TODOS` | Path `uuid` | `200 image/png` (`byte[]`) | Imagen PNG del código QR generada con ZXing. |
| `GET` | `/api/tickets/{uuid}/print-payload` | `TODOS` | Path `uuid` | `200 text/plain;charset=UTF-8` | Tirilla térmica formateada (58mm/80mm). |
| `POST` | `/api/tickets/{uuid}/reimprimir` | `TAQ, ADMIN` | `ReimpresionRequestDTO` | `200 text/plain;charset=UTF-8` | Solo tickets `PAGADO`. Registra `LogReimpresion` y `AuditoriaTicket`. |

### 2.10 Operación de Atracciones (`OperacionController` — `/api/operacion` y `OperarioEstacionController` — `/api/operario`)
| Método | Ruta | Rol | Request | Response | Notas |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/operario/estacion-activa` | `OPE, ADMIN` | — | `200 EstacionTrabajoDTO` | Obtiene la estación asignada al operario autenticado. |
| `POST` | `/api/operacion/escanear` | `OPE, ADMIN` | `EscaneoQrRequestDTO` | `200 ValidacionTicketResponseDTO` | Acepta UUID completo o prefijo corto (ej. `TDN-XXXXXXXX`). Registra siempre en `log_escaneo_qr`. Lanza `TicketInvalidoOperacionException` (`409`/`404`/`400`) si falla validación. |
| `POST` | `/api/operacion/iniciar-sesion` | `OPE, ADMIN` | `InicioSesionRequestDTO` | `200 SesionJuegoResponseDTO` | Pasa ticket a `EN_USO`, crea `SesionJuego`, publica WS `NUEVO_INGRESO`. |
| `GET` | `/api/operacion/cola/{atraccionId}` | `TODOS` | Path `atraccionId` | `200 ColaAtraccionResponseDTO` | Sesiones activas (`EN_CURSO`, `PAUSADA`) y cupos libres. |
| `POST` | `/api/operacion/sesiones/{id}/pausar` | `OPE, ADMIN` | `PausaRequestDTO` | `200 SesionJuegoResponseDTO` | Congela `tiempoRestantePausa`, crea `LogIncidentePausa`, publica WS `PAUSA`. |
| `POST` | `/api/operacion/sesiones/{id}/reanudar` | `OPE, ADMIN` | Path `id` | `200 SesionJuegoResponseDTO` | Recalcula `horaFin`, cierra `LogIncidentePausa`, publica WS `REANUDACION`. |
| `POST` | `/api/operacion/atraccion/{id}/pausa-general` | `OPE, ADMIN` | `PausaGeneralRequestDTO` | `200 ColaAtraccionResponseDTO` | Pausa todas las sesiones en curso de la atracción (`esPausaGeneral=true`). |
| `POST` | `/api/operacion/sesiones/{id}/salida` | `OPE, ADMIN` | Path `id` | `200 SesionJuegoResponseDTO` | Pasa sesión a `FINALIZADA` y ticket a `CONSUMIDO`, calcula `segundosExceso`, publica WS `SESION_FINALIZADA`. |
| `POST` | `/api/operacion/sesiones/{id}/descontar-intento` | `OPE, ADMIN` | `RegistroIntentoEventoDTO` (opc.) | `200 SesionJuegoResponseDTO` | Para `EVENTOS_CAIDAS`: descuenta 1 intento; si llega a 0, finaliza sesión automáticamente. |
| `GET` | `/api/operacion/sincronizar/{atraccionId}` | `TODOS` | Path `atraccionId` | `200 SincronizacionRelojResponseDTO` | Devuelve `serverEpochMillis`, `serverTimestamp` y `colaActualizada`. |
| `GET` | `/api/operacion/atraccion/{id}/aforo-actual` | `TODOS` | Path `id` | `200 AforoActualDTO` | Ocupación activa, capacidad máxima y porcentaje. |

### 2.11 Reportes y Auditoría (`ReporteController` — `/api/reportes`, `AuditoriaController` — `/api/auditoria`)
| Método | Ruta | Rol | Request (Query / Path) | Response |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/reportes/turnos` | `ADMIN` | `?fechaInicio=&fechaFin=&usuarioId=` | `200 List<ReporteTurnoDTO>` |
| `GET` | `/api/reportes/turnos/{id}/detalle` | `ADMIN` | Path `id` | `200 DetalleTurnoAuditadoDTO` |
| `GET` | `/api/reportes/rotacion-atracciones` | `ADMIN` | `?fechaInicio=&fechaFin=&periodo=MENSUAL&criterioOrden=USOS` | `200 List<RotacionAtraccionDTO>` |
| `GET` | `/api/reportes/exportar/pdf` | `ADMIN` | `?tipoReporte=TURNOS&fechaInicio=&fechaFin=&usuarioId=&atraccionId=` | `200 application/pdf` (`byte[]`) |
| `GET` | `/api/reportes/exportar/csv` | `ADMIN` | `?tipoReporte=TURNOS&fechaInicio=&fechaFin=&usuarioId=&atraccionId=` | `200 text/csv;charset=UTF-8` (`byte[]`) |
| `GET` | `/api/reportes/graficos` | `ADMIN` | `?periodo=HOY&fechaInicio=&fechaFin=` | `200 ReporteGraficosDTO` |
| `GET` | `/api/reportes/graficos/horas-pico` | `ADMIN` | `?fechaInicio=&fechaFin=` | `200 GraficoTendenciaDTO` |
| `GET` | `/api/reportes/graficos/metodos-pago` | `ADMIN` | `?fechaInicio=&fechaFin=` | `200 GraficoTendenciaDTO` |
| `GET` | `/api/reportes/graficos/evolucion-ingresos` | `ADMIN` | — | `200 GraficoTendenciaDTO` |
| `GET` | `/api/auditoria/tickets/{criterio}` | `ADMIN` | Path `criterio` (UUID, prefijo o doc) | `200 List<TimelineTicketDTO>` |
| `GET` | `/api/auditoria/incidentes` | `ADMIN` | `?fechaInicio=&fechaFin=&atraccionId=&tipoPausa=` | `200 ResumenIncidentesAuditoriaDTO` |

---

## 3. Diccionario Compacto de DTOs y Alias JSON (`dto/*.java`)

> Los alias (`@JsonAlias` en entrada y `@JsonProperty` adicionales en salida) existen para compatibilidad directa con `territorio-front`. **No eliminarlos.**

- **Autenticación y Usuarios:**
  - `LoginRequestDTO`: `{ correo: String (@NotBlank @Email), password: String (@NotBlank) }`
  - `AuthResponseDTO`: `{ token: String, tipoToken: String, id: Long, nombre: String, correo: String, rol: String }`
  - `ForgotPasswordRequest`: `{ correo: String (@JsonAlias("email")) }`
  - `ResetPasswordRequest`: `{ token: String, nuevaContrasena: String (regex >=8 chars, mayúscula, minúscula, dígito) }`
  - `ChangePasswordRequest`: `{ passwordActual: String, nuevaContrasena: String }`
  - `UsuarioRegistroDTO`: `{ nombre: String, correo: String, password: String (@JsonAlias("contrasena","contraseña","clave")), nombreRol: String (@JsonAlias("rol","role")) }`
  - `UsuarioUpdateDTO`: `{ nombre?: String, correo?: String, estado?: String, rol?: String (@JsonAlias("nombreRol","role")) }`
  - `UsuarioResponseDTO`: `{ id: Long, nombre: String, correo: String, estado: String, rol: String, fechaCreacion: LocalDateTime, fechaActualizacion: LocalDateTime }`
- **Clientes y Menores:**
  - `ClienteRequestDTO`: `{ numeroDocumento (@JsonAlias("documento","numero_documento","doc","identificacion")), nombres (@JsonAlias("nombre")), apellidos (@JsonAlias("apellido")), nombreCompleto? (se divide automáticamente si nombres/apellidos vienen vacíos), telefono (@JsonAlias("tel","celular","movil")), correo (@JsonAlias("email","correoElectronico")), menores: List<MenorDTO> (@JsonAlias("hijos","ninos","acompaniantes")) }`
  - `ClienteResponseDTO`: `{ id, idCliente (alias), numeroDocumento, documento (alias), nombres, apellidos, nombreCompleto (calculado), telefono, correo, email (alias), fechaRegistro, menores: List<MenorDTO> }`
  - `MenorDTO`: `{ id (@JsonAlias("idMenor","id_menor")), idMenor (alias), numeroDocumento (@JsonAlias("documento","doc")), nombreCompleto (@JsonAlias("nombre")), fechaNacimiento (@JsonAlias("fechaNac")), edad (1..17) }`
- **Atracciones, Tarifas y Asignaciones:**
  - `AtraccionRequestDTO` / `AtraccionResponseDTO`: `{ id, nombre, descripcion, tarifaMinuto, estado, tipo (TipoAtraccion), capacidadMaxima, duracionMinutosBase, toleranciaMinutos, caidasLimite, edadMinima, edadMaxima, fechaCreacion }`
  - `CatalogoAtraccionVentaDTO`: `{ id, nombre, descripcion, tipo, estado, tarifaMinuto, duracionMinutosBase, toleranciaMinutos, caidasLimite, capacidadMaxima, ocupacionActual, ninosActivos (alias), aforoDisponible, tieneCupo (calculado), tieneOperarioAsignado, tarifasTiempo, tarifasEvento }`
  - `TarifaTiempoRequestDTO` / `ResponseDTO`: `{ id, atraccionId, nombreAtraccion, duracionMinutos (@JsonAlias("minutos")), precio, activo (@JsonAlias("vigente")), fechaCreacion }`
  - `TarifaEventoRequestDTO` / `ResponseDTO`: `{ id, atraccionId, nombreAtraccion, intentos, precio, activo (@JsonAlias("vigente")), fechaCreacion }`
  - `AsignacionRequestDTO`: `{ usuarioId (@JsonAlias("operarioId")), atraccionId, observaciones?, forzarReasignacion? }`
  - `AsignacionResponseDTO`: `{ id, usuarioId, operarioId (alias), nombreOperario, operarioNombre (alias), correoOperario, operarioCorreo (alias), atraccionId, nombreAtraccion, atraccionNombre (alias), fechaAsignacion, horaInicio, horaFin, estado, activo (boolean calculado), observaciones, tipoAtraccion, capacidadMaxima, duracionMinutosBase, toleranciaMinutos, caidasLimite, edadMinima, edadMaxima }`
- **Caja, Ventas y Tickets:**
  - `AperturaCajaDTO`: `{ montoBase (@JsonAlias("montoInicial","monto_base","base")), observaciones? }`
  - `CierreCajaDTO`: `{ montoEfectivoReportado (@JsonAlias("montoFinal","totalEfectivoReal","monto_efectivo_reportado")), observaciones? }`
  - `ArqueoCajaDTO`: `{ montoEfectivoRecontado (@JsonAlias("monto","montoRecontado","montoFisico","totalFisico")), observaciones (@JsonAlias("justificacion","notas")) }`
  - `TurnoCajaResponseDTO`: `{ id, idTurno (alias), usuarioId, idUsuario (alias), cajeroNombre, nombreCajero (alias), montoBase, montoInicial (alias), fechaApertura, fechaCierre, ventasEfectivoSistema, totalRecaudado (alias), totalVentas (alias), totalEsperado (montoBase + ventasEfectivoSistema), montoEfectivoReportado, montoFinal (alias), diferencia, estado, observaciones }`
  - `ItemVentaValidacionDTO`: `{ atraccionId (@JsonAlias("idAtraccion")), tarifaTiempoId?, tarifaEventoId?, cantidad, precioUnitario (@JsonAlias("precio","valorUnitario")), subtotal?, duracionMinutos?, intentos? }`
  - `CotizacionRequestDTO` / `ResponseDTO`: `{ items (@JsonAlias("lineas","detalles")), subtotales, subtotal, descuento, baseGravable, porcentajeImpuesto, impuestos, totalNeto, total (alias) }`
  - `VentaProcesarRequestDTO`: `{ clienteId (@JsonAlias("idCliente"), 0 para Cliente de Mostrador), turnoCajaId? (@JsonAlias("idTurnoCaja")), metodoPago (@JsonAlias("formaPago","medioPago")), montoRecibido (@JsonAlias("efectivoRecibido","valorRecibido")), descuento?, observaciones?, items: List<DetalleVentaRequestDTO> (@JsonAlias("detalles","lineas")), clienteDocumento?, clienteNombre? }`
  - `DetalleVentaRequestDTO`: `{ atraccionId, menorId?, tipoTarifa, tarifaTiempoId?, tarifaEventoId?, cantidad, precioUnitario, subtotal?, uuid? (@JsonAlias("uuidCodigo","codigoQr")), codigoBoleta?, menorNombre?, menorEdad? }`
  - `VentaResponseDTO`: `{ id, ventaId (alias), clienteId, clienteDocumento, clienteNombreCompleto, clienteNombre (alias), turnoCajaId, subtotal, descuento, impuestos, montoTotal, montoRecibido, cambio, metodoPago, estadoVenta, fechaVenta, congelada, observaciones, detalles: List<DetalleVentaResponseDTO>, tickets: List<TicketResponseDTO> }`
  - `TicketResponseDTO`: `{ uuidCodigo, id (alias), uuid (alias), codigoQr (alias), codigoBoleta (alias TDN-{atrId}-{uuidPrefix}-001), detalleVentaId, atraccionId, atraccionNombre, menorId, menorNombre, tipoTarifa, estadoTicket, fechaEmision }`
  - `TicketConsultaDTO`: `{ uuidCodigo, id (alias), uuid (alias), estadoTicket, estadoActual (alias), minutosContratados, fechaEmision, horaEmision (alias), menorId, menorNombre, menorDocumento, atraccionId, atraccionNombre, tipoTarifa, ventaId, detalleVentaId, clienteId, clienteDocumento, clienteNombre }`
- **Operación, Reportes y Auditoría:**
  - `EscaneoQrRequestDTO`: `{ uuid: String (@JsonAlias("codigo","uuidTicket","codigoBoleta","codigoQr")), atraccionId: Long, timestamp?: LocalDateTime }`
  - `ValidacionTicketResponseDTO`: `{ valido, mensaje, uuidTicket, estadoActual, atraccionId, atraccionNombre, tipoAtraccion, menorId, menorNombre, menorEdad, acudienteNombre, acudienteDocumento, acudienteTelefono, tipoTarifa, minutosAutorizados, intentosAutorizados, duracionMinutosBase, toleranciaMinutos, caidasLimite, fechaEmision }`
  - `InicioSesionRequestDTO`: `{ uuidTicket: UUID (@JsonAlias("ticketUuid","ticket_uuid")), atraccionId: Long, timestamp?: LocalDateTime }`
  - `SesionJuegoResponseDTO`: `{ sesionId, ticketUuid, atraccionId, atraccionNombre, menorId, menorNombre, menorEdad, acudienteNombre, acudienteTelefono, acudienteDocumento, operarioNombre, operarioCorreo, horaInicio, horaFin, duracionSegundos, segundosRestantes, tiempoRestantePausa, fechaPausa, estadoSesion, segundosExceso, intentosTotales, intentosRestantes, alertaPreExpiracion, tiempoExpirado }`
  - `RotacionAtraccionDTO`: `{ atraccionId, nombreAtraccion, nombre (alias), tipoAtraccion, tipo (alias), capacidadMaxima, cantidadTotalUsos, volumenVisitas (alias), ingresosBrutos, tiempoPromedioMinutos, porcentajeOcupacion, ocupacionPromedioPorcentaje (alias), horaMayorAfluencia }`
