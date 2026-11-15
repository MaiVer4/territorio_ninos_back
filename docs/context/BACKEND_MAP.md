# Mapa de Arquitectura y Servicios del Backend
> Fuente de verdad: `src/main/java/com/software/territoriodeninos/**`, `src/main/resources/application.properties`, `pom.xml`, `Dockerfile`, `start-app.sh`. Última sincronización: 2026-11-14 (commit 0c25453).

## 1. Stack, Convenciones y Reglas de Arquitectura
- **Stack:** Java 21, Spring Boot `4.0.6`, Spring Security + JWT (`jjwt`), Spring Data JPA (Hibernate), Flyway, WebSocket STOMP, ZXing (QR), OpenPDF / CSV, Lombok `1.18.38`.
- **Patrón de Controladores (`*Api` + `*Controller`):**
  - Las anotaciones de OpenAPI/Swagger (`@Tag`, `@Operation`, `@ApiResponses`) viven en la interfaz `*Api.java`.
  - La clase `*Controller.java` implementa `*Api`, define `@RestController`, `@RequestMapping`, `@PreAuthorize` y delega 100% en la interfaz `*Service`.
- **Patrón de Servicios (`*Service` + `*ServiceImpl`):**
  - Inyección por constructor mediante `@RequiredArgsConstructor` (campos `private final`).
  - Transaccionalidad: `@Transactional(readOnly = true)` para consultas, `@Transactional` para escritura. Excepciones como `TicketInvalidoOperacionException` en `OperacionServiceImpl.escanearTicket` usan `noRollbackFor = TicketInvalidoOperacionException.class` para persistir el `LogEscaneoQr` incluso cuando el ticket es rechazado.
- **Mapeo DTOs:** Se usan `record` de Java en casi todos los DTOs, con constructores secundarios, métodos estáticos `fromEntity(...)`, `@JsonAlias` (entrada) y `@JsonProperty` (salida) para reconciliar nombres con `territorio-front`. Solo `UsuarioMapper` usa clase dedicada.

---

## 2. Mapa de Paquetes (`com.software.territoriodeninos`)
| Paquete | Propósito |
| :--- | :--- |
| `config/` | `SecurityConfig` (JWT stateless, CORS), `WebSocketConfig` (`/ws-operacion`), `SwaggerConfig`, `PostgresConstraintInitializer` (índices únicos parciales). |
| `controller/` | 14 pares `*Api.java` + `*Controller.java` bajo `/api/...`. |
| `dto/` | 45 clases/records de transferencia de datos y eventos WebSocket. |
| `entity/` | 19 entidades JPA mapeadas a PostgreSQL. |
| `exception/` | `GlobalExceptionHandler` + 11 excepciones de dominio (`ReglaNegocioException`, `RecursoNoEncontradoException`, `ConflictoException`, `TurnoConflictoException`, `TicketInvalidoOperacionException`, etc.). |
| `mapper/` | `UsuarioMapper`. |
| `repository/` | 19 interfaces `JpaRepository` con queries JPQL y nativas para reportes, aforo y búsqueda flexible de tickets. |
| `security/` | `JwtAuthenticationFilter`, `JwtUtil`, `AuthenticatedUserProvider`, `LoginAttemptService` (rate limit login), `TokenBlacklistService` (revocación JWT en memoria). |
| `service/` | 19 pares `*Service` + `*ServiceImpl`, más `OperacionWebSocketPublisher` y `SesionJuegoMonitorScheduler`. |
| `util/` | 12 Enums de dominio + `FechaUtils` (parseo flexible de rangos), `HashUtil` (SHA-256), `TicketPrintFormatter` (tirilla térmica 58/80mm). |

---

## 3. Catálogo de Servicios (`service/`)
| Servicio (`*ServiceImpl`) | Métodos Públicos Clave | Responsabilidad (1 línea) | Dependencias Clave |
| :--- | :--- | :--- | :--- |
| `AuthServiceImpl` | `login`, `generarTokenRecuperacion`, `cambiarContrasena`, `cerrarSesion`, `purgarTokensExpirados` | Autenticación JWT, bloqueo por intentos fallidos, reset de password con token SHA-256 y logout con blacklist. | `UsuarioRepository`, `JwtUtil`, `LoginAttemptService`, `TokenBlacklistService`, `EmailService` |
| `UsuarioServiceImpl` | `registrar`, `obtenerTodos`, `obtenerPorId`, `actualizar`, `cambiarPassword`, `inactivar`, `activar`, `obtenerPorRol`, `eliminar` | CRUD de colaboradores, validación de complejidad de clave y revocación de sesión al inactivar o cambiar clave. | `UsuarioRepository`, `RolRepository`, `PasswordEncoder`, `UsuarioMapper` |
| `ClienteServiceImpl` | `buscarPorDocumento`, `registrarCliente`, `actualizarCliente` | Registro y búsqueda de adultos responsables y sincronización de su lista de `Menor`. | `ClienteRepository`, `MenorRepository` |
| `AtraccionServiceImpl` | `crear`, `obtenerTodas`, `obtenerActivas`, `obtenerPorId`, `actualizar`, `cambiarEstado`, `eliminar`, `obtenerCatalogoVenta`, `invalidarCacheCatalogo` | Gestión de atracciones y armado del catálogo POS en tiempo real (tarifas + ocupación activa + operario asignado). | `AtraccionRepository`, `AsignacionOperarioRepository`, `TarifaTiempoRepository`, `TarifaEventoRepository` |
| `TarifaTiempoServiceImpl` / `TarifaEventoServiceImpl` | `crear`, `obtenerPorAtraccion`, `obtenerPorId`, `actualizar`, `eliminar` | CRUD de bloques de tiempo (`TEMPORIZADA`) y paquetes de intentos (`EVENTOS_CAIDAS`). | `TarifaTiempoRepository` / `TarifaEventoRepository`, `AtraccionRepository` |
| `AsignacionOperarioServiceImpl` | `asignar`, `reasignar`, `finalizar`, `obtenerActivas`, `obtenerActivaPorOperario`, `obtenerActivasPorAtraccion`, `obtenerHistorialPorOperario` | Vincula un operario a una atracción garantizando máximo 1 asignación `ACTIVA` por operario. | `AsignacionOperarioRepository`, `UsuarioRepository`, `AtraccionRepository` |
| `TurnoCajaServiceImpl` | `abrirTurno`, `obtenerTurnoActivo`, `cerrarTurno`, `registrarArqueo`, `obtenerHistorialTurnos` | Ciclo de caja con **cierre ciego** (`obtenerTurnoActivo` oculta `ventasEfectivoSistema`), arqueo y conciliación de efectivo. | `TurnoCajaRepository`, `UsuarioRepository`, `AuthenticatedUserProvider` |
| `AforoValidatorServiceImpl` | `validarCapacidadAtraccion`, `validarConsistenciaTarifa`, `validarItemsVenta` | Verifica cupos disponibles en tiempo real y que el `precioUnitario` coincida con la tarifa vigente. | `AtraccionRepository`, `TarifaTiempoRepository`, `TarifaEventoRepository` |
| `VentaCalculoServiceImpl` | `cotizarVenta` | Calcula subtotales por línea, descuento, base gravable e impuestos según `ConfiguracionParque`. | `ConfiguracionParqueRepository`, `AforoValidatorService` |
| `VentaServiceImpl` | `procesarVenta`, `obtenerVentaPorId`, `obtenerVentasPorTurno`, `listarVentasPorTurno` | Ejecuta venta transaccional asociada al turno abierto, resuelve tarifas activas de respaldo, emite `Ticket`s con UUID único y registra `AuditoriaTicket`. | `VentaRepository`, `TicketRepository`, `TurnoCajaRepository`, `TarifaTiempoRepository`, `TarifaEventoRepository`, `UUIDGeneratorService`, `AuditoriaOperacionService` |
| `TicketServiceImpl` | `buscarTickets`, `obtenerTicketPorUuid` | Consulta de boletas por UUID completo, prefijo corto (8 chars), documento de cliente/menor o nombre. | `TicketRepository`, `TarifaTiempoRepository` |
| `QrCodeServiceImpl` | `generarQrPng`, `generarQrBase64`, `generarQrTicketPng`, `generarQrTicketBase64` | Generación de imágenes QR (PNG/Base64) mediante ZXing a partir del `uuidCodigo`. | `TicketRepository` |
| `TicketPrintServiceImpl` | `generarPayloadImpresion`, `reimprimirTicket` | Construye tirilla de texto plano (58/80mm) y audita reimpresiones (`LogReimpresion` + `AuditoriaTicket`). | `TicketRepository`, `ConfiguracionParqueService`, `TicketPrintFormatter`, `LogReimpresionRepository` |
| `OperacionServiceImpl` | `obtenerEstacionActiva`, `escanearTicket`, `iniciarSesion`, `obtenerCola`, `pausarSesion`, `reanudarSesion`, `pausaGeneral`, `registrarSalida`, `descontarIntento`, `sincronizarReloj`, `obtenerAforoActual` | Motor operativo de atracciones: validación QR antifraude, control de sesiones, pausas, caídas, aforo y emisión WS. | `TicketRepository`, `SesionJuegoRepository`, `LogEscaneoQrRepository`, `LogIncidentePausaRepository`, `OperacionWebSocketPublisher`, `AuditoriaOperacionService` |
| `SesionJuegoMonitorScheduler` | `monitorearSesionesPreExpiracion` (`@Scheduled(fixedRate = 5000)`) | Revisa cada 5s sesiones `EN_CURSO` para disparar alertas WS de pre-expiración (`<= 120s`) y tiempo expirado (`<= 0s`). | `SesionJuegoRepository`, `OperacionWebSocketPublisher`, `OperacionService` |
| `ConciliacionTurnoServiceImpl` | `obtenerReporteTurnos`, `obtenerDetalleTurno`, `obtenerRotacionAtracciones` | Consolida cierres de caja (`CUADRADO`/`SOBRANTE`/`FALTANTE`), desglose de ventas/boletas y métricas de rotación. | `TurnoCajaRepository`, `VentaRepository`, `SesionJuegoRepository`, `TicketRepository` |
| `EstadisticasReporteServiceImpl` | `obtenerHorasPico`, `obtenerDistribucionMetodosPago`, `obtenerEvolucionIngresosUltimos7Dias` | Agrega series estadísticas para gráficos gerenciales. | `SesionJuegoRepository`, `VentaRepository` |
| `ExportacionReporteServiceImpl` | `exportarPdf`, `exportarCsv` | Genera archivos descargables PDF y CSV UTF-8 con BOM para reportes de turnos, rotación e incidentes. | `ConciliacionTurnoService`, `AuditoriaOperacionService`, `ConfiguracionParqueService` |
| `AuditoriaOperacionServiceImpl` | `registrarTransicionTicket`, `consultarTimelinePorCriterio`, `consultarIncidentes` | Bitácora inmutable de cambios de estado de boletas (`auditoria_tickets`) y consulta de pausas/incidentes. | `AuditoriaTicketRepository`, `LogIncidentePausaRepository`, `TicketRepository` |

---

## 4. Flujos de Negocio Críticos (Paso a Paso)
1. **Apertura, Arqueo y Cierre Ciego de Caja (`TurnoCajaServiceImpl`):**
   - `abrirTurno`: Resuelve cajero con `AuthenticatedUserProvider` -> verifica que no tenga turno `ABIERTO` -> crea `TurnoCaja(estado=ABIERTO)`.
   - `obtenerTurnoActivo`: Devuelve `TurnoCajaResponseDTO.fromEntityCiego(...)` con `ventasEfectivoSistema = null` para que el taquillero no vea cuánto efectivo espera el sistema antes de declarar su conteo físico.
   - `registrarArqueo` / `cerrarTurno`: Suma ventas con `metodoPago == EFECTIVO` del turno -> calcula `totalEsperado = montoBase + ventasEfectivo` -> calcula `diferencia = montoEfectivoReportado - totalEsperado` -> en `cerrarTurno` cambia estado a `CERRADO`.
2. **Procesamiento de Venta y Emisión de Boletas (`VentaServiceImpl.procesarVenta`):**
   - Verifica turno `ABIERTO` del cajero -> valida aforo y precios (`AforoValidatorService`) -> calcula impuestos (`VentaCalculoService`) -> guarda `Venta` (`congelada=true`) y `DetalleVenta` -> por cada unidad en `cantidad`, genera un `Ticket` con `UUIDGeneratorService.generarUuidUnico()` en estado `PAGADO` -> registra hito inicial en `AuditoriaOperacionService.registrarTransicionTicket` -> invalida caché de catálogo.
3. **Escaneo QR e Inicio de Sesión en Atracción (`OperacionServiceImpl`):**
   - `escanearTicket`: Limpia el string recibido (`resolverTicketFlexible`: soporta UUID completo o prefijo de 8 hex con o sin `TDN-`). Verifica que el ticket exista, esté `PAGADO`, corresponda a la atracción, cumpla rango de edad del menor y haya aforo disponible. Registra **siempre** el intento en `LogEscaneoQr` y retorna `ValidacionTicketResponseDTO`.
   - `iniciarSesion`: Cambia `Ticket` a `EN_USO` (`fechaConsumo = now()`), crea `SesionJuego` (`EN_CURSO`, calcula `horaFin` según `duracionMinutos` o `intentosRestantes` según `TarifaEvento`), registra `AuditoriaTicket` y publica evento STOMP `NUEVO_INGRESO`.

---

## 5. Variables de Entorno, Comandos y Tests
- **Variables de entorno (`application.properties`):**
  - `DB_URL` (`jdbc:postgresql://localhost:5432/territorioNinos`), `DB_USERNAME` (`postgres`), `DB_PASSWORD` (`postgres`), `DDL_AUTO` (`update`), `FLYWAY_ENABLED` (`true`).
  - `JWT_SECRET` (**obligatorio** en ejecución, >= 32 bytes para HMAC-SHA), `JWT_EXPIRATION_HOURS` (`24`), `FRONTEND_URL` (`http://localhost:4200`).
  - `MAIL_HOST` (`localhost`), `MAIL_PORT` (`2525`), `MAIL_USERNAME` (`test`), `MAIL_PASSWORD` (`test`).
- **Ejecución y Tests:**
  - Compilar y correr tests unitarios/integración (usa H2): `./mvnw test` (o `./mvnw test -Dtest=NombreTest`).
  - Levantar aplicación local: `./start-app.sh` o `./mvnw spring-boot:run`.
  - Scripts E2E en vivo: `./prueba-funcional-en-vivo.sh` y `./simular-operacion.sh`.

---

## 6. Guía Rápida: "¿Dónde tocar para...?"
| Tarea | Archivos exactos a consultar/editar |
| :--- | :--- |
| Agregar/modificar un campo en una tabla | `entity/<Entidad>.java` + nueva migración `src/main/resources/db/migration/V10__*.sql` + actualizar `docs/context/DATABASE_SCHEMA.md` |
| Cambiar reglas de acceso o roles de un endpoint | `config/SecurityConfig.java` y `@PreAuthorize` en `controller/<Modulo>Controller.java` |
| Modificar cálculo de IVA, subtotal o cotización POS | `service/VentaCalculoServiceImpl.java` y `service/VentaServiceImpl.java` |
| Ajustar validación de QR, pausas, caídas o aforo | `service/OperacionServiceImpl.java` y `service/SesionJuegoMonitorScheduler.java` |
| Cambiar formato de tirilla impresa (58mm/80mm) | `util/TicketPrintFormatter.java` y `service/TicketPrintServiceImpl.java` |
| Ajustar reportes financieros, PDF/CSV o auditoría | `service/ConciliacionTurnoServiceImpl.java`, `ExportacionReporteServiceImpl.java`, `AuditoriaOperacionServiceImpl.java` |
