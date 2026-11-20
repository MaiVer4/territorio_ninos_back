# Mapa de Arquitectura y Componentes del Frontend (`territorio-front`)
> Fuente de verdad: `/home/vera/Downloads/territorio-front/src/app/**`, `package.json`, `angular.json`. Última sincronización: 2026-11-20 (commit 6609998).

## 1. Stack, Convenciones y Reglas de Desarrollo
- **Stack:** Angular `21.2.0` (100% Standalone Components), Signals (`signal`, `computed`, `effect`), RxJS (`HttpClient`), Tailwind CSS `3.4.19`, Angular Material Icons (`material-symbols-outlined`), STOMP/WebSocket, Vitest `4.0.8`.
- **Convenciones obligatorias para agentes:**
  - **Sin NgModules:** Todo componente es `standalone: true` (por defecto en Angular 21) y se carga con `loadComponent` en `app.routes.ts`.
  - **Inyección de dependencias:** Preferir `inject(Servicio)` en lugar de constructores largos.
  - **Estado reactivo:** Usar Angular Signals expuestos en los servicios (`readonly estado = this._estado.asReadonly()`) y `computed()` para estado derivado.
  - **URL Base de API:** Configurada en `src/environments/environment.development.ts` (`http://localhost:8080/territorioninos/api`) y `environment.ts` (`/territorioninos/api` vía reverse proxy Nginx).
  - **Tests:** Se ejecutan con **Vitest** (`npm test` / `npx vitest run`), usando `vi.fn()` para mocks y `TestBed` de Angular.

---

## 2. Mapa de Rutas, Guards y Roles (`src/app/app.routes.ts`)
| Ruta | Componente | Guard | Roles Permitidos (`data.roles`) |
| :--- | :--- | :--- | :--- |
| `/login` | `LoginComponent` | — | Público |
| `/forgot-password` | `ForgotPasswordComponent` | — | Público |
| `/reset-password` | `ResetPasswordComponent` | — | Público |
| `/unauthorized` | `UnauthorizedComponent` | — | Público |
| `/taquilla/apertura` | `AperturaComponent` | `authGuard` | `TAQUILLERO`, `ADMINISTRADOR` |
| `/taquilla/pos` | `TaquillaPosComponent` | `authGuard` (+ verificación de caja activa) | `TAQUILLERO`, `ADMINISTRADOR` |
| `/taquilla/consulta` | `ConsultaTicketsComponent` | `authGuard` | `TAQUILLERO`, `ADMINISTRADOR` |
| `/operario/seleccion` | `OperarioSeleccionComponent` | `authGuard` | `OPERARIO`, `COORDINADOR`, `ADMINISTRADOR` |
| `/coordinador` | `CoordinadorPanelComponent` | `authGuard` | `COORDINADOR`, `ADMINISTRADOR` |
| `/admin` (Layout padre) | `AdminLayoutComponent` | `authGuard` (`canActivate` + `canActivateChild`) | `ADMINISTRADOR` |
| `/admin/dashboard` | `AdminDashboardComponent` | Hereda `authGuard` | `ADMINISTRADOR` |
| `/admin/atracciones` | `AtraccionesComponent` | Hereda `authGuard` | `ADMINISTRADOR` |
| `/admin/asignaciones` | `AsignacionesComponent` | Hereda `authGuard` | `ADMINISTRADOR` |
| `/admin/colaboradores` (alias `/admin/usuarios`) | `ColaboradoresComponent` | Hereda `authGuard` | `ADMINISTRADOR` |
| `/admin/reportes` | `ReportesComponent` | Hereda `authGuard` | `ADMINISTRADOR` |
| `/admin/turnos` | `TurnosComponent` | Hereda `authGuard` | `ADMINISTRADOR` |
| `/admin/auditoria` | `AuditoriaComponent` (Pestañas: Expediente, Incidentes, Logs, Novedades y Daños) | Hereda `authGuard` | `ADMINISTRADOR` |
| `/admin/novedades` | *(Redirección funcional a `/admin/auditoria?tab=novedades`)* | Hereda `authGuard` | `ADMINISTRADOR` |
| `/admin/configuracion` | `ConfiguracionComponent` | Hereda `authGuard` | `ADMINISTRADOR` |

### Comportamiento de Guards e Interceptors (`src/app/core/`)
- **`authGuard` (`core/guards/auth.guard.ts`):** Verifica `authService.isAuthenticated()`. Si no está autenticado, redirige a `/login?returnUrl=<url>`. Si está autenticado pero su rol no está en `data.roles` (buscando recursivamente en rutas padre), redirige a `/unauthorized`.
- **`cajaAbiertaGuard` (`core/guards/caja-abierta.guard.ts`):** Llama a `cajaService.verificarCajaAbierta()`. Si retorna `false` o error, muestra `notificationService.warn(...)` y redirige a `/taquilla/apertura`.
- **`jwtInterceptor` (`core/interceptors/jwt.interceptor.ts`):** Lee `localStorage.getItem('tdn_jwt_token')` (que guarda un JSON `{ token, tipoToken, id, nombre, correo, rol }`) e inyecta `Authorization: Bearer <token>` **únicamente** en URLs internas (`/api` o `environment.apiUrl`), excluyendo `/api/auth/login`, `/forgot-password` y `/reset-password`.
- **`errorInterceptor` (`core/interceptors/error.interceptor.ts`):**
  - En `401` (fuera de `/auth/login` y `/auth/logout`): ejecuta `authService.logout(true)` para limpiar sesión sin bucle circular.
  - En `403` y `400..499`: extrae `error.error?.detalles || error.error?.mensaje || error.error?.message` y dispara toast con `NotificationService.error(...)`.

---

## 3. Catálogo de Servicios (`src/app/core/services/`)
| Servicio | Endpoints Backend que Consume | Signals / Estado Expuesto | Responsabilidad Principal |
| :--- | :--- | :--- | :--- |
| `AuthService` | `POST /auth/login`, `POST /auth/logout`, `POST /auth/forgot-password`, `PATCH /auth/reset-password` | `currentUser`, `currentUserName`, `currentUserInitials` | Gestión de sesión en `localStorage` (`tdn_jwt_token`), decodificación de rol y redirección post-login. |
| `CajaService` | `GET /turnos/activo`, `POST /turnos/abrir`, `POST /turnos/cerrar`, `POST /turnos/{id}/arqueo`, `GET /turnos` | `turnoActual` | Apertura de turno, consulta de turno activo (cierre ciego), arqueo por denominaciones y cierre de caja. |
| `ClienteService` | `GET /clientes/documento/{doc}`, `POST /clientes`, `PUT /clientes/{id}` | — | Búsqueda de adulto responsable por documento y registro/actualización junto con sus menores. |
| `AtraccionService` | `GET /atracciones`, `GET /atracciones/activas`, `GET /ventas/catalogo`, `GET /atracciones/{id}`, `POST /atracciones`, `PUT /atracciones/{id}`, `DELETE /atracciones/{id}` | — | CRUD de atracciones, consulta de atracciones activas y carga del catálogo de venta POS con tarifas y cupos en tiempo real. |
| `TarifaService` | `GET/POST/PUT/DELETE /tarifas-tiempo...` y `/tarifas-evento...` | — | Gestión de bloques de tiempo (`TarifaBloque`) y paquetes de intentos (`TarifaEvento`) por atracción. |
| `VentaCalculoService` | *(Cálculo local)* | — | Cálculo de subtotales, IVA (`INCLUIDO`/`ADICIONAL`), cambio en efectivo y armado de `ResumenLiquidacionPOS`. |
| `ColaEmisionService` | `POST /ventas/procesar` (vía sincronización) | `ordenes`, `ordenReciente`, `boletasGeneradas`, `historialTicketsTurno`, `ordenesPendientes` | Orquesta la emisión de boletas tras el pago en POS, reconciliando los UUIDs generados por el backend con las boletas impresas. |
| `PrintService` | *(Usa `ConfiguracionService` + `window.print()`)* | `imprimiendo`, `ordenEnImpresion`, `boletasEnImpresion`, `anchoPapelMm`, `configParque` | Renderiza `TicketPrintTemplateComponent` en formato térmico de `58mm` u `80mm` y lanza diálogo de impresión. |
| `TicketConsultaService` | `GET /tickets/buscar?criterio=`, `GET /tickets/{uuid}` | — | Búsqueda de boletas en taquilla y cálculo de estado visual (`PAGADO`, `EN_USO`, `CONSUMIDO`) y tiempo restante. |
| `TicketReimpresionService` | `POST /tickets/{uuid}/reimprimir` | `historialReimpresiones`, `ultimaReimpresion`, `procesandoReimpresion` | Solicita reimpresión justificada (`ATASCO_PAPEL`, `FALTA_PAPEL`, `BOLETA_ILEGIBLE`), marca boleta como `REIMPRESIÓN` y soporta cola offline. |
| `AsignacionService` | `GET /asignaciones/activas`, `POST /asignaciones`, `DELETE /asignaciones/{id}` | — | Asignación y liberación de operarios en estaciones de trabajo. |
| `OperacionService` | `GET /operario/estacion-activa`, `POST /operacion/vincular`, `POST /operacion/desvincular`, `POST /operacion/autorizar-sobreaforo`, `POST /operacion/escanear`, `POST /operacion/iniciar-sesion`, `GET /operacion/cola/{id}`, `POST /operacion/sesiones/{id}/pausar|reanudar|salida|descontar-intento`, `POST /operacion/atraccion/{id}/pausa-general` | `estacionActiva`, `colaActiva`, `resultadoValidacionActual`, `paradaEmergenciaActiva`, `ocupacionActual`, `capacidadMaxima`, `colaOrdenadaPorUrgencia` | Motor de la vista de operario: vinculación asíncrona con refresco en `409`, autorización de sobreaforo en backend, validación QR fail-closed (`ERROR_SERVIDOR`/`SIN_CONEXION` en 5xx/offline), guarda anti-race en `sincronizarColaDesdeServidor`, reconciliación de `sesionId`, cronómetros y caídas. |
| `OfflineQueueService` | *(Persistencia en `localStorage`)* | `accionesPendientes`, `accionesDescartadas`, `totalPendientes`, `hayAccionesPendientes` | Encola acciones operativas cuando se pierde conexión y las reintenta al reconectar. |
| `WebsocketService` | STOMP `/territorioninos/ws-operacion` (`/topic/atraccion/{id}`, `/topic/operacion/dashboard`, `/topic/admin/dashboard`) | `connected`, `estadoConexion`, `serverTimeOffsetMs`, `ultimoMensajeRecibido`, `occupancy$` | Sincronización en tiempo real de colas y aforo para dashboard/POS con reconexión backoff/online en `dashboardAdminActivo` y desconexión automática vía `effect()` cuando `authService.currentUser()` pasa a `null`. |
| `ReporteService` | `GET /reportes/turnos`, `GET /reportes/turnos/{id}/detalle`, `GET /reportes/rotacion-atracciones`, `GET /reportes/graficos`, `GET /reportes/exportar/csv|pdf` | `descargandoArchivo`, `formatoDescargaActivo` | Reportes de conciliación de caja, rotación de atracciones, series estadísticas reales de ventas/afluencia por hora y descarga de blobs PDF/CSV. |
| `AuditoriaService` | `GET /auditoria/tickets/{criterio}`, `GET /auditoria/incidentes` | — | Consulta de línea de tiempo inmutable de un ticket y bitácora de incidentes/pausas (> 5 min resaltados). |
| `ConfiguracionService` | `GET /configuracion`, `PUT /configuracion` | — | Lectura y actualización de datos fiscales del parque, ancho de papel y tiempo de gracia. |
| `UsuarioService` | `GET/POST/PUT/DELETE /usuarios` | — | Gestión de colaboradores (`ADMINISTRADOR`, `TAQUILLERO`, `OPERARIO`). |
| `NotificationService` / `SoundNotificationService` / `PosAudioService` | *(UI / Web Audio API)* | `sonidoHabilitado`, `vibracionHabilitada` | Toasts (`ToastNotificationComponent`), sonidos de confirmación de pago POS, beep de escaneo QR y alarma sonora/vibración de tiempo agotado. |

---

## 4. Mapa de Componentes por Feature (`src/app/features/` y `shared/`)
- **`features/auth/pages/`:** `login`, `forgot-password`, `reset-password`, `unauthorized`.
- **`features/taquilla/`:**
  - `pages/apertura/apertura.component.ts`: Pantalla de apertura de turno de caja antes de entrar al POS.
  - `pages/pos/pos.component.ts` (`TaquillaPosComponent`): Pantalla principal de taquilla. Coordina catálogo, carrito (depurando ítems si una atracción pasa a `FUERA_DE_SERVICIO` vía WS), registro de cliente/menores, cobro, impresión y cierre de caja.
  - `components/`:
    - `catalogo-atracciones`: Tarjetas de atracciones con tarifas de tiempo/evento, cupos en vivo, validación de operario asignado y sobreventa con hora de ingreso estimada (`AHORA` o diferida).
    - `pos-cart`: Carrito de compras con asignación de menor por boleta y cálculo en vivo (permite venta con aforo completo).
    - `cliente-form` y `menores-chips`: Búsqueda por cédula, alta rápida de adulto y selección de menores.
    - `confirmar-pago-modal`: Selección de medio de pago (`EFECTIVO`, `TARJETA_DEBITO`, `TARJETA_CREDITO`, `TRANSFERENCIA`), cálculo de vueltas y disparo de venta.
    - `apertura-caja-modal` y `arqueo-caja-modal`: Conteo físico por denominación de billetes/monedas y cierre ciego.
    - `consulta-tickets` y `reimpresion-modal`: Búsqueda de boletas emitidas y reimpresión con motivo obligatorio.
- **`features/operario/`:**
  - `pages/seleccion/seleccion.component.ts` (`OperarioSeleccionComponent`): Contenedor operativo que muestra la estación asignada, el escáner QR y la cola activa; desvincula la estación confirmada al cerrar sesión.
  - `components/`: `seleccion-atraccion` (espera confirmación del backend antes de emitir `estacionConfirmada`), `scanner-qr` (cámara en vivo con fallback tolerante + fallback de captura con foto nativa móvil para entornos HTTP/insecure context + entrada manual + autorización de sobreaforo contra backend con señal `autorizandoSupervisor`), `resultado-validacion`, `cola-usuarios` (tarjetas ordenadas por urgencia), `cronometro` (temporizador regresivo con semáforo y pausas), `modo-evento` (contador de caídas/intentos y cronómetro de ronda).
- **`features/admin/`:**
  - `layout/admin-layout`: Sidebar de navegación administrativa, header con usuario actual y `<router-outlet>`.
  - `pages/`: `dashboard` (KPIs y aforo en vivo vía WS), `atracciones` (+ `atraccion-form` unificado con gestión directa de tarifas y modalidades libres, `tarifas-modal`), `asignaciones`, `colaboradores` (+ `colaborador-form`), `turnos` (historial y conciliación), `reportes` (tablas, gráficos y exportación CSV/PDF), `auditoria` (+ `components/ticket-timeline`, integrado con `ReportesAtraccionPanelComponent` en pestaña 'Novedades y Daños' con badge dinámico evitando carga duplicada con `?tab=novedades`, y guarda anti-doble-clic `actualizandoId`/manejo de errores/`reportesVista` `computed` en el panel y exportación CSV), `configuracion` (+ `ticket-preview`).
- **`shared/components/`:**
  - `qr-viewer`: Renderizador de código QR con validación óptica.
  - `ticket-print-template`: Plantilla térmica oculta en pantalla y visible en `@media print` (soporta 58mm y 80mm, imprime hora/texto de ingreso).
  - `toast-notification`: Componente visual de notificaciones.

---

## 5. Guía Rápida: "¿Dónde tocar para...?"
| Tarea en Frontend | Archivos exactos a consultar/editar |
| :--- | :--- |
| Cambiar el flujo de cobro, carrito o emisión de tickets en POS | `features/taquilla/pages/pos/pos.component.ts`, `core/services/cola-emision.service.ts`, `features/taquilla/components/confirmar-pago-modal/` |
| Modificar apertura, conteo de billetes o cierre ciego de caja | `core/services/caja.service.ts`, `features/taquilla/components/arqueo-caja-modal/arqueo-caja-modal.component.ts` |
| Ajustar diseño de tirilla impresa o vista previa de ticket | `shared/components/ticket-print-template/ticket-print-template.component.ts`, `core/services/print.service.ts`, `features/admin/pages/configuracion/ticket-preview.component.ts` |
| Tocar escaneo QR, cronómetro de juego o contador de caídas | `core/services/operacion.service.ts`, `features/operario/components/scanner-qr/`, `cronometro/`, `modo-evento/` |
| Ajustar reportes financieros, gráficas o auditoría | `core/services/reporte.service.ts`, `core/services/auditoria.service.ts`, `features/admin/pages/reportes/`, `features/admin/pages/auditoria/` |
| Ejecutar pruebas unitarias | `npm test` (o `npx vitest run src/app/ruta/archivo.spec.ts`) |
