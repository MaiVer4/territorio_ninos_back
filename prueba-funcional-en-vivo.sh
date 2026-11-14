#!/usr/bin/env bash
# =========================================================================================
# PRUEBA FUNCIONAL END-TO-END (E2E) EN VIVO - TERRITORIO DE NIÑOS
# Ejecuta 32 pruebas funcionales reales vía HTTP contra http://localhost:8080/territorioninos
# validando todos los roles, módulos, reglas de negocio, antifraude QR, sesiones y reportes.
# =========================================================================================
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080/territorioninos}"

# Preparar usuario administrador de prueba en territorio_db sin alterar usuarios personales
docker exec -i territorio_db psql -U postgres -d territorioNinos << 'EOF' > /dev/null
INSERT INTO usuarios (nombre, correo, password, estado, rol_id, fecha_creacion, fecha_actualizacion)
VALUES ('Admin E2E', 'admin.e2e@territoriodeninos.com', '$2a$10$kAeZ22qyV4L8zvU6R5/Zhe37fVytHmLNDppYlAZj6kGVA9loA/QEm', 'ACTIVO', 1, NOW(), NOW())
ON CONFLICT (correo) DO UPDATE SET estado = 'ACTIVO', rol_id = 1;
UPDATE atracciones SET capacidad_maxima = 10 WHERE id = 3 AND capacidad_maxima < 10;
EOF

python3 - "$BASE_URL" << 'PYEOF'
import sys
import json
import time
import hashlib
import subprocess
import urllib.request
import urllib.error
import urllib.parse

BASE_URL = sys.argv[1].rstrip("/")

passed = 0
failed = 0
results = []

def log_step(num, title, status, detail=""):
    global passed, failed
    if status == "PASS":
        passed += 1
        icon = "✅ [PASS]"
    else:
        failed += 1
        icon = "❌ [FAIL]"
    line = f"{icon} Paso {num:02d}: {title}"
    if detail:
        line += f" -> {detail}"
    print(line)
    results.append((num, title, status, detail))

def api_call(method, path, token=None, body=None, raw_response=False):
    url = f"{BASE_URL}{path}"
    headers = {"Accept": "application/json, text/plain, */*"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = None
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"

    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            raw = resp.read()
            if raw_response:
                return resp.status, raw, dict(resp.headers)
            text = raw.decode("utf-8", errors="replace")
            try:
                return resp.status, json.loads(text) if text else None
            except json.JSONDecodeError:
                return resp.status, text
    except urllib.error.HTTPError as e:
        raw = e.read()
        if raw_response:
            return e.code, raw, dict(e.headers)
        text = raw.decode("utf-8", errors="replace")
        try:
            return e.code, json.loads(text) if text else None
        except json.JSONDecodeError:
            return e.code, text

def db_query(sql):
    cmd = ["docker", "exec", "-i", "territorio_db", "psql", "-U", "postgres", "-d", "territorioNinos", "-t", "-A", "-c", sql]
    out = subprocess.check_output(cmd, text=True).strip()
    return out

print("=" * 92)
print(f"🚀 INICIANDO PRUEBA FUNCIONAL COMPLETA EN VIVO CONTRA: {BASE_URL}")
print("=" * 92)

# =========================================================================================
# FASE 1: AUTENTICACIÓN, RECUPERACIÓN DE CONTRASEÑA Y RBAC
# =========================================================================================
print("\n--- FASE 1: Autenticación, Recuperación de Contraseña y Seguridad RBAC ---")

# Paso 1: Solicitar recuperación de contraseña para establecer clave conocida en cuentas E2E
status, resp = api_call("POST", "/api/auth/forgot-password", body={"email": "admin.e2e@territoriodeninos.com", "correo": "admin.e2e@territoriodeninos.com"})
if status == 200:
    log_step(1, "POST /api/auth/forgot-password (Solicitud de token)", "PASS", f"HTTP {status} - {resp.get('mensaje', '')}")
else:
    log_step(1, "POST /api/auth/forgot-password", "FAIL", f"HTTP {status} - {resp}")

# Sembrar token conocido (con su hash SHA-256 en BD como exige AuthServiceImpl) para probar el endpoint real PATCH /api/auth/reset-password
admin_id = db_query("SELECT id FROM usuarios WHERE correo = 'admin.e2e@territoriodeninos.com' LIMIT 1;")
plain_reset_token = "e2e-reset-token-2026-territorio"
hashed_reset_token = hashlib.sha256(plain_reset_token.encode("utf-8")).hexdigest()
db_query(f"DELETE FROM password_reset_token WHERE usuario_id = {admin_id}; INSERT INTO password_reset_token (token, usuario_id, fecha_expiracion) VALUES ('{hashed_reset_token}', {admin_id}, NOW() + INTERVAL '30 minutes');")

status, resp = api_call("PATCH", "/api/auth/reset-password", body={"token": plain_reset_token, "nuevaContrasena": "Password123"})
if status == 200:
    log_step(2, "PATCH /api/auth/reset-password (Cambio de clave con token UUID)", "PASS", f"HTTP {status} - {resp.get('mensaje', '')}")
else:
    log_step(2, "PATCH /api/auth/reset-password", "FAIL", f"HTTP {status} - {resp}")

# Esperar >1s para que el claim iat (en segundos) de los nuevos JWT sea posterior al timestamp de revocación de reset-password
time.sleep(1.2)

# Propagar ese mismo hash BCrypt verificado a las cuentas de simulación (@territoriodeninos.com)
db_query("""
UPDATE usuarios
SET password = (SELECT password FROM usuarios WHERE correo = 'admin.e2e@territoriodeninos.com')
WHERE correo LIKE '%@territoriodeninos.com';
""")

# Paso 3: Login fallido con contraseña incorrecta
status, resp = api_call("POST", "/api/auth/login", body={"correo": "admin.e2e@territoriodeninos.com", "password": "ClaveErronea999"})
if status in (401, 403):
    log_step(3, "POST /api/auth/login (Rechazo de credenciales inválidas)", "PASS", f"HTTP {status} rechazado correctamente")
else:
    log_step(3, "POST /api/auth/login (Credenciales inválidas)", "FAIL", f"HTTP {status} - {resp}")

# Paso 4: Login exitoso ADMINISTRADOR
status, admin_auth = api_call("POST", "/api/auth/login", body={"correo": "admin.e2e@territoriodeninos.com", "password": "Password123"})
admin_token = admin_auth.get("token") if isinstance(admin_auth, dict) else None
if status == 200 and admin_token:
    log_step(4, "POST /api/auth/login (Rol ADMINISTRADOR)", "PASS", f"HTTP 200 - Usuario: {admin_auth.get('nombre')} ({admin_auth.get('rol')})")
else:
    log_step(4, "POST /api/auth/login (Rol ADMINISTRADOR)", "FAIL", f"HTTP {status} - {admin_auth}")

# Paso 5: Login exitoso TAQUILLERO
status, taq_auth = api_call("POST", "/api/auth/login", body={"correo": "laura.taquilla@territoriodeninos.com", "password": "Password123"})
taq_token = taq_auth.get("token") if isinstance(taq_auth, dict) else None
taq_user_id = taq_auth.get("id") if isinstance(taq_auth, dict) else None
if status == 200 and taq_token:
    log_step(5, "POST /api/auth/login (Rol TAQUILLERO)", "PASS", f"HTTP 200 - Usuario: {taq_auth.get('nombre')} (ID={taq_user_id})")
else:
    log_step(5, "POST /api/auth/login (Rol TAQUILLERO)", "FAIL", f"HTTP {status} - {taq_auth}")

# Paso 6: Login exitoso OPERARIOS (Trampolines y Toro Mecánico)
status1, op1_auth = api_call("POST", "/api/auth/login", body={"correo": "camila.operario@territoriodeninos.com", "password": "Password123"})
status2, op3_auth = api_call("POST", "/api/auth/login", body={"correo": "diego.operario@territoriodeninos.com", "password": "Password123"})
op1_token = op1_auth.get("token") if isinstance(op1_auth, dict) else None
op1_user_id = op1_auth.get("id") if isinstance(op1_auth, dict) else None
op3_token = op3_auth.get("token") if isinstance(op3_auth, dict) else None
op3_user_id = op3_auth.get("id") if isinstance(op3_auth, dict) else None

# Asegurar que camila.operario tenga asignación activa hoy en Atracción 1 (Trampolines)
db_query(f"""
UPDATE asignaciones_operario SET estado = 'FINALIZADA', hora_fin = NOW() WHERE atraccion_id = 1 AND estado = 'ACTIVA' AND usuario_id <> {op1_user_id};
INSERT INTO asignaciones_operario (usuario_id, atraccion_id, fecha_asignacion, hora_inicio, estado, observaciones)
SELECT {op1_user_id}, 1, CURRENT_DATE, NOW(), 'ACTIVA', 'Asignación activa E2E Trampolines'
WHERE NOT EXISTS (SELECT 1 FROM asignaciones_operario WHERE usuario_id = {op1_user_id} AND estado = 'ACTIVA');
UPDATE asignaciones_operario SET fecha_asignacion = CURRENT_DATE, atraccion_id = 1 WHERE usuario_id = {op1_user_id} AND estado = 'ACTIVA';
UPDATE asignaciones_operario SET fecha_asignacion = CURRENT_DATE, atraccion_id = 3 WHERE usuario_id = {op3_user_id} AND estado = 'ACTIVA';
""")

if status1 == 200 and status2 == 200 and op1_token and op3_token:
    log_step(6, "POST /api/auth/login (Roles OPERARIO Temporizada y Eventos)", "PASS", f"HTTP 200 - {op1_auth.get('nombre')} y {op3_auth.get('nombre')}")
else:
    log_step(6, "POST /api/auth/login (OPERARIOS)", "FAIL", f"HTTP {status1}/{status2}")

# Paso 7: Control de acceso RBAC (Operario intentando abrir turno de caja -> 403 Forbidden)
status, resp = api_call("POST", "/api/turnos/abrir", token=op1_token, body={"montoBase": 50000, "observaciones": "Intento no autorizado"})
if status == 403:
    log_step(7, "Seguridad RBAC (@PreAuthorize bloqueo de rol no autorizado)", "PASS", "HTTP 403 Forbidden al intentar abrir caja con rol OPERARIO")
else:
    log_step(7, "Seguridad RBAC", "FAIL", f"Se esperaba 403 pero retornó HTTP {status}")

# =========================================================================================
# FASE 2: CONFIGURACIÓN, USUARIOS, ATRACCIONES, TARIFAS Y ASIGNACIONES
# =========================================================================================
print("\n--- FASE 2: Configuración Institucional, Usuarios, Catálogo, Tarifas y Asignaciones ---")

# Paso 8: Obtener y actualizar configuración del parque
status_get, cfg = api_call("GET", "/api/configuracion", token=admin_token)
cfg["tiempoGraciaGeneralSegundos"] = 60
cfg["anchoPapelMm"] = 80
status_put, cfg_upd = api_call("PUT", "/api/configuracion", token=admin_token, body=cfg)
if status_get == 200 and status_put == 200:
    log_step(8, "GET & PUT /api/configuracion (Parámetros e impuestos del parque)", "PASS", f"Parque: {cfg_upd.get('nombreParque')} | NIT: {cfg_upd.get('nit')}")
else:
    log_step(8, "GET & PUT /api/configuracion", "FAIL", f"HTTP {status_get}/{status_put}")

# Paso 9: Consultar usuarios y filtrar por rol
status_u, usuarios = api_call("GET", "/api/usuarios", token=admin_token)
status_r, operarios = api_call("GET", "/api/usuarios/rol/OPERARIO", token=admin_token)
if status_u == 200 and status_r == 200 and isinstance(usuarios, list) and isinstance(operarios, list):
    log_step(9, "GET /api/usuarios & /api/usuarios/rol/OPERARIO", "PASS", f"Total usuarios: {len(usuarios)} | Operarios activos: {len(operarios)}")
else:
    log_step(9, "GET /api/usuarios", "FAIL", f"HTTP {status_u}/{status_r}")

# Paso 10: Consultar atracciones activas y detalle por ID
status_a, atracciones = api_call("GET", "/api/atracciones/activas", token=admin_token)
status_a1, atr1 = api_call("GET", "/api/atracciones/1", token=admin_token)
if status_a == 200 and status_a1 == 200 and len(atracciones) >= 5:
    log_step(10, "GET /api/atracciones/activas & /api/atracciones/1", "PASS", f"{len(atracciones)} atracciones activas (Estación #1: {atr1.get('nombre')}, aforo={atr1.get('capacidadMaxima')})")
else:
    log_step(10, "GET /api/atracciones", "FAIL", f"HTTP {status_a}/{status_a1}")

# Paso 11: Consultar tarifas por tiempo y tarifas por evento
status_tt, t_tiempo = api_call("GET", "/api/tarifas-tiempo/atraccion/1", token=taq_token)
status_te, t_evento = api_call("GET", "/api/tarifas-evento/atraccion/3", token=taq_token)
if status_tt == 200 and status_te == 200 and len(t_tiempo) > 0 and len(t_evento) > 0:
    log_step(11, "GET /api/tarifas-tiempo & /api/tarifas-evento", "PASS", f"Trampolines: {len(t_tiempo)} tarifas de tiempo | Toro Mecánico: {len(t_evento)} tarifas por intentos")
else:
    log_step(11, "GET /api/tarifas-tiempo & /api/tarifas-evento", "FAIL", f"HTTP {status_tt}/{status_te}")

# Paso 12: Consultar asignaciones de operarios activas
status_asig, asignaciones = api_call("GET", "/api/asignaciones/activas", token=admin_token)
if status_asig == 200 and isinstance(asignaciones, list) and len(asignaciones) >= 5:
    log_step(12, "GET /api/asignaciones/activas", "PASS", f"{len(asignaciones)} estaciones con operario activo asignado")
else:
    log_step(12, "GET /api/asignaciones/activas", "FAIL", f"HTTP {status_asig} - {asignaciones}")

# =========================================================================================
# FASE 3: TURNOS DE CAJA, CLIENTES, COTIZACIÓN, VENTA E IMPRESIÓN ESC/POS + QR
# =========================================================================================
print("\n--- FASE 3: Taquilla, Turnos de Caja, Clientes, Ventas e Impresión QR/ESC-POS ---")

# Asegurar que laura.taquilla no tenga un turno abierto previo antes de probar apertura
db_query(f"UPDATE turnos_caja SET estado = 'CERRADO', fecha_cierre = NOW(), monto_efectivo_reportado = monto_base + COALESCE(ventas_efectivo_sistema, 0), diferencia = 0 WHERE usuario_id = {taq_user_id} AND estado = 'ABIERTO';")

# Paso 13: Abrir turno de caja y verificar bloqueo de doble turno simultáneo (409 Conflict)
status_open, turno_dto = api_call("POST", "/api/turnos/abrir", token=taq_token, body={
    "montoBase": 120000.00,
    "observaciones": "Apertura de caja prueba funcional en vivo"
})
turno_id = turno_dto.get("id") if isinstance(turno_dto, dict) else None
status_dup, _ = api_call("POST", "/api/turnos/abrir", token=taq_token, body={
    "montoBase": 100000.00,
    "observaciones": "Intento duplicado"
})
if status_open == 201 and status_dup == 409 and turno_id:
    log_step(13, "POST /api/turnos/abrir & Bloqueo de doble turno (CA-01)", "PASS", f"Turno #{turno_id} ABIERTO (Base $120,000) | Doble apertura bloqueada con HTTP 409")
else:
    log_step(13, "POST /api/turnos/abrir", "FAIL", f"Open={status_open}, Dup={status_dup}, resp={turno_dto}")

# Paso 14: Registrar acudiente con 2 menores y buscarlo por documento
doc_acudiente = "1094999888"
status_cli, cli_dto = api_call("POST", "/api/clientes", token=taq_token, body={
    "numeroDocumento": doc_acudiente,
    "nombres": "Roberto Carlos",
    "apellidos": "Pineda Ospina",
    "telefono": "3158889900",
    "correo": "roberto.pineda@ejemplo.com",
    "menores": [
        {
            "numeroDocumento": "1199888001",
            "nombreCompleto": "Mateo Pineda",
            "fechaNacimiento": "2016-05-15",
            "edad": 10
        },
        {
            "numeroDocumento": "1199888002",
            "nombreCompleto": "Lucía Pineda",
            "fechaNacimiento": "2023-06-10",
            "edad": 3
        }
    ]
})
status_busq, cli_busq = api_call("GET", f"/api/clientes/buscar?documento={doc_acudiente}", token=taq_token)
cliente_id = cli_busq.get("id") if isinstance(cli_busq, dict) else None
menores_list = cli_busq.get("menores", []) if isinstance(cli_busq, dict) else []
menor_10_id = next((m["id"] for m in menores_list if "Mateo" in m["nombreCompleto"]), menores_list[0]["id"] if menores_list else None)
menor_3_id = next((m["id"] for m in menores_list if "Lucía" in m["nombreCompleto"]), menores_list[-1]["id"] if menores_list else None)

if status_cli in (200, 201) and status_busq == 200 and cliente_id and len(menores_list) >= 2:
    log_step(14, "POST /api/clientes & GET /api/clientes/buscar", "PASS", f"Cliente ID #{cliente_id} ({cli_busq.get('nombreCompleto')}) con {len(menores_list)} menores registrados")
else:
    log_step(14, "POST & GET /api/clientes", "FAIL", f"HTTP {status_cli}/{status_busq} - {cli_busq}")

# Paso 15: Consultar catálogo de venta con aforo en tiempo real
status_cat, catalogo = api_call("GET", "/api/ventas/catalogo", token=taq_token)
if status_cat == 200 and isinstance(catalogo, list) and len(catalogo) >= 5:
    log_step(15, "GET /api/ventas/catalogo (Catálogo y aforo en tiempo real)", "PASS", f"{len(catalogo)} atracciones disponibles para venta en taquilla")
else:
    log_step(15, "GET /api/ventas/catalogo", "FAIL", f"HTTP {status_cat}")

# Paso 16: Validar disponibilidad de ítems de venta
status_val, val_resp = api_call("POST", "/api/ventas/validar", token=taq_token, body=[
    {"atraccionId": 1, "tarifaTiempoId": 2, "cantidad": 2, "precioUnitario": 20000.00},
    {"atraccionId": 3, "tarifaEventoId": 1, "cantidad": 1, "precioUnitario": 15000.00}
])
if status_val == 200 and isinstance(val_resp, dict) and val_resp.get("valido") is True:
    log_step(16, "POST /api/ventas/validar (Validación previa de aforo y tarifas)", "PASS", f"Total oficial: ${val_resp.get('totalOficial')} | Tickets: {val_resp.get('totalTickets')}")
else:
    log_step(16, "POST /api/ventas/validar", "FAIL", f"HTTP {status_val} - {val_resp}")

# Paso 17: Cotizar venta financieramente
status_cot, cot_resp = api_call("POST", "/api/ventas/cotizar", token=taq_token, body={
    "items": [
        {"atraccionId": 1, "tarifaTiempoId": 2, "cantidad": 1, "precioUnitario": 20000.00},
        {"atraccionId": 3, "tarifaEventoId": 1, "cantidad": 1, "precioUnitario": 15000.00},
        {"atraccionId": 1, "tarifaTiempoId": 1, "cantidad": 1, "precioUnitario": 12000.00}
    ],
    "descuento": 2000.00
})
if status_cot == 200 and float(cot_resp.get("totalNeto", 0)) == 45000.0:
    log_step(17, "POST /api/ventas/cotizar (Liquidación de subtotales y descuento)", "PASS", f"Subtotal: ${cot_resp.get('subtotal')} | Descuento: ${cot_resp.get('descuento')} | Total Neto: ${cot_resp.get('totalNeto')}")
else:
    log_step(17, "POST /api/ventas/cotizar", "FAIL", f"HTTP {status_cot} - {cot_resp}")

# Paso 18: Procesar venta en efectivo (generando 3 tickets: 1 Trampolines p/ Mateo 10a, 1 Toro p/ Mateo 10a, 1 Trampolines p/ Lucía 3a para probar rechazo de edad mínima)
status_vta, vta_resp = api_call("POST", "/api/ventas/procesar", token=taq_token, body={
    "clienteId": cliente_id,
    "turnoCajaId": turno_id,
    "metodoPago": "EFECTIVO",
    "montoRecibido": 50000.00,
    "descuento": 2000.00,
    "observaciones": "Venta E2E en vivo",
    "items": [
        {
            "atraccionId": 1,
            "menorId": menor_10_id,
            "tipoTarifa": "TIEMPO",
            "tarifaTiempoId": 2,
            "cantidad": 1,
            "precioUnitario": 20000.00,
            "subtotal": 20000.00
        },
        {
            "atraccionId": 3,
            "menorId": menor_10_id,
            "tipoTarifa": "EVENTO",
            "tarifaEventoId": 1,
            "cantidad": 1,
            "precioUnitario": 15000.00,
            "subtotal": 15000.00
        },
        {
            "atraccionId": 1,
            "menorId": menor_3_id,
            "tipoTarifa": "TIEMPO",
            "tarifaTiempoId": 1,
            "cantidad": 1,
            "precioUnitario": 12000.00,
            "subtotal": 12000.00
        }
    ]
})
tickets_generados = vta_resp.get("tickets", []) if isinstance(vta_resp, dict) else []
uuid_trampolin_mateo = next((t["uuidCodigo"] for t in tickets_generados if t["atraccionId"] == 1 and t["menorId"] == menor_10_id), None)
uuid_toro_mateo      = next((t["uuidCodigo"] for t in tickets_generados if t["atraccionId"] == 3 and t["menorId"] == menor_10_id), None)
uuid_trampolin_lucia = next((t["uuidCodigo"] for t in tickets_generados if t["atraccionId"] == 1 and t["menorId"] == menor_3_id), None)

if status_vta == 201 and len(tickets_generados) == 3 and vta_resp.get("congelada") is True:
    log_step(18, "POST /api/ventas/procesar (Persistencia atómica y generación de UUIDs)", "PASS", f"Venta #{vta_resp.get('id')} PAGADA y congelada | Total: ${vta_resp.get('montoTotal')} | Cambio: ${vta_resp.get('cambio')} | {len(tickets_generados)} tickets emitidos")
else:
    log_step(18, "POST /api/ventas/procesar", "FAIL", f"HTTP {status_vta} - {vta_resp}")

# Paso 19: Consultar Ticket, imagen QR PNG, plantilla térmica ESC/POS y Reimpresión auditada
status_tk, tk_info = api_call("GET", f"/api/tickets/{uuid_trampolin_mateo}", token=taq_token)
status_qr, qr_bytes, qr_headers = api_call("GET", f"/api/tickets/{uuid_trampolin_mateo}/qr", token=taq_token, raw_response=True)
status_pr, print_payload = api_call("GET", f"/api/tickets/{uuid_trampolin_mateo}/print-payload", token=taq_token)
status_re, reimp_payload = api_call("POST", f"/api/tickets/{uuid_trampolin_mateo}/reimprimir", token=taq_token, body={
    "motivo": "Prueba funcional de reimpresión por atasco de papel en taquilla"
})
if status_tk == 200 and status_qr == 200 and qr_bytes[:4] == b"\x89PNG" and status_pr == 200 and status_re == 200 and "REIMPRESIÓN" in reimp_payload:
    log_step(19, "GET /api/tickets/{uuid}/qr, /print-payload & POST /reimprimir", "PASS", f"QR PNG válido ({len(qr_bytes)} bytes) y plantilla térmica ESC/POS con marca '** REIMPRESIÓN **'")
else:
    log_step(19, "Endpoints de Tickets / QR / ESC-POS", "FAIL", f"tk={status_tk}, qr={status_qr}, pr={status_pr}, re={status_re}")

# =========================================================================================
# FASE 4: OPERACIÓN EN TIEMPO REAL, TORNIQUETE QR ANTIFRAUDE Y SESIONES DE JUEGO
# =========================================================================================
print("\n--- FASE 4: Operación de Estaciones, Control QR Antifraude y Sesiones en Tiempo Real ---")

# Paso 20: Obtener estación activa de los operarios
status_est1, est1 = api_call("GET", "/api/operario/estacion-activa", token=op1_token)
status_est3, est3 = api_call("GET", "/api/operario/estacion-activa", token=op3_token)
if status_est1 == 200 and status_est3 == 200 and est1.get("atraccionId") == 1 and est3.get("atraccionId") == 3:
    log_step(20, "GET /api/operario/estacion-activa", "PASS", f"Op1 -> {est1.get('atraccionNombre')} (Rango: {est1.get('rangoEdad')}) | Op3 -> {est3.get('atraccionNombre')}")
else:
    log_step(20, "GET /api/operario/estacion-activa", "FAIL", f"HTTP {status_est1}/{status_est3} - {est1} / {est3}")

# Paso 21: Antifraude 1 - Operario de Trampolines intenta escanear en estación #3 (Toro Mecánico) -> 403 Forbidden
status_af1, resp_af1 = api_call("POST", "/api/operacion/escanear", token=op1_token, body={
    "uuid": uuid_toro_mateo,
    "atraccionId": 3
})
if status_af1 == 403:
    log_step(21, "Antifraude QR #1: Bloqueo de estación no asignada al operario", "PASS", "HTTP 403 Forbidden (EstacionNoAutorizadaException)")
else:
    log_step(21, "Antifraude QR #1", "FAIL", f"Se esperaba 403 pero retornó HTTP {status_af1}")

# Paso 22: Antifraude 2 - Ticket de Toro Mecánico escaneado en Trampolines -> 422 Unprocessable Entity + persistencia en log_escaneo_qr
status_af2, resp_af2 = api_call("POST", "/api/operacion/escanear", token=op1_token, body={
    "uuid": uuid_toro_mateo,
    "atraccionId": 1
})
# Antifraude Edad Mínima: Lucía (3 años) intentando entrar a Trampolines (edad mínima 4 años) -> 422
status_af_edad, resp_af_edad = api_call("POST", "/api/operacion/escanear", token=op1_token, body={
    "uuid": uuid_trampolin_lucia,
    "atraccionId": 1
})
log_count_rechazo = int(db_query(f"SELECT COUNT(*) FROM log_escaneo_qr WHERE uuid_leido IN ('{uuid_toro_mateo}', '{uuid_trampolin_lucia}') AND resultado LIKE 'RECHAZADO%';") or "0")
if status_af2 == 422 and status_af_edad == 422 and log_count_rechazo >= 2:
    log_step(22, "Antifraude QR #2 y #3: Rechazo por atracción distinta y edad mínima (con auditoría persistida)", "PASS", f"HTTP 422 en ambos casos y {log_count_rechazo} eventos RECHAZADO_* persistidos en log_escaneo_qr (sin rollback)")
else:
    log_step(22, "Antifraude QR #2 y #3", "FAIL", f"af2={status_af2}, edad={status_af_edad}, logs={log_count_rechazo}")

# Paso 23: Escaneo QR válido en Trampolines y en Toro Mecánico
status_ok1, scan1 = api_call("POST", "/api/operacion/escanear", token=op1_token, body={
    "uuid": uuid_trampolin_mateo,
    "atraccionId": 1
})
status_ok3, scan3 = api_call("POST", "/api/operacion/escanear", token=op3_token, body={
    "uuid": uuid_toro_mateo,
    "atraccionId": 3
})
if status_ok1 == 200 and status_ok3 == 200 and scan1.get("valido") is True and scan3.get("valido") is True:
    log_step(23, "POST /api/operacion/escanear (Validación QR exitosa)", "PASS", f"Trampolines: {scan1.get('minutosAutorizados')} min autorizados | Toro Mecánico: {scan3.get('intentosAutorizados')} intentos autorizados")
else:
    log_step(23, "POST /api/operacion/escanear (Válido)", "FAIL", f"HTTP {status_ok1}/{status_ok3}")

# Paso 24: Iniciar sesión en Trampolines (Temporizada) y en Toro Mecánico (Eventos/Caídas)
status_ini1, ses1 = api_call("POST", "/api/operacion/iniciar-sesion", token=op1_token, body={
    "uuidTicket": uuid_trampolin_mateo,
    "atraccionId": 1
})
status_ini3, ses3 = api_call("POST", "/api/operacion/iniciar-sesion", token=op3_token, body={
    "uuidTicket": uuid_toro_mateo,
    "atraccionId": 3
})
sesion_trampolin_id = ses1.get("sesionId") if isinstance(ses1, dict) else None
sesion_toro_id      = ses3.get("sesionId") if isinstance(ses3, dict) else None
if status_ini1 == 200 and status_ini3 == 200 and sesion_trampolin_id and sesion_toro_id:
    log_step(24, "POST /api/operacion/iniciar-sesion (Temporizada y por Intentos)", "PASS", f"Sesión Trampolines #{sesion_trampolin_id} (EN_CURSO, {ses1.get('duracionSegundos')}s) | Sesión Toro #{sesion_toro_id} (EN_CURSO, {ses3.get('intentosRestantes')} intentos)")
else:
    log_step(24, "POST /api/operacion/iniciar-sesion", "FAIL", f"HTTP {status_ini1}/{status_ini3} - {ses1} / {ses3}")

# Paso 25: Consultar cola activa, reloj sincronizado y aforo actual
status_cola, cola1 = api_call("GET", "/api/operacion/cola/1", token=op1_token)
status_sync, sync1 = api_call("GET", "/api/operacion/sincronizar/1", token=op1_token)
status_afo, aforo1 = api_call("GET", "/api/operacion/atraccion/1/aforo-actual", token=op1_token)
if status_cola == 200 and status_sync == 200 and status_afo == 200:
    log_step(25, "GET /api/operacion/cola/1, /sincronizar/1 & /aforo-actual", "PASS", f"Ocupación activa en Trampolines: {aforo1.get('ocupacionActiva')}/{aforo1.get('capacidadMaxima')} ({aforo1.get('porcentajeOcupacion')}%)")
else:
    log_step(25, "Endpoints de Cola / Reloj / Aforo", "FAIL", f"HTTP {status_cola}/{status_sync}/{status_afo}")

# Paso 26: Pausar y reanudar sesión individual en Trampolines
status_pau, ses_pau = api_call("POST", f"/api/operacion/sesiones/{sesion_trampolin_id}/pausar", token=op1_token, body={
    "motivo": "Ajuste operativo de cordones y medias antideslizantes"
})
status_rea, ses_rea = api_call("POST", f"/api/operacion/sesiones/{sesion_trampolin_id}/reanudar", token=op1_token)
if status_pau == 200 and ses_pau.get("estadoSesion") == "PAUSADA" and status_rea == 200 and ses_rea.get("estadoSesion") == "EN_CURSO":
    log_step(26, "POST /api/operacion/sesiones/{id}/pausar & /reanudar", "PASS", f"Sesión #{sesion_trampolin_id} pausada ({ses_pau.get('tiempoRestantePausa')}s restantes) y reanudada a EN_CURSO")
else:
    log_step(26, "Pausar y reanudar sesión", "FAIL", f"pau={status_pau} ({ses_pau}), rea={status_rea} ({ses_rea})")

# Paso 27: Pausa general de estación y reanudación
status_pg, cola_pg = api_call("POST", "/api/operacion/atraccion/1/pausa-general", token=op1_token, body={
    "motivo": "Mantenimiento técnico preventivo de colchoneta central"
})
api_call("POST", f"/api/operacion/sesiones/{sesion_trampolin_id}/reanudar", token=op1_token)
if status_pg == 200:
    log_step(27, "POST /api/operacion/atraccion/1/pausa-general", "PASS", f"Parada general aplicada exitosamente en {cola_pg.get('atraccionNombre')}")
else:
    log_step(27, "POST /api/operacion/atraccion/1/pausa-general", "FAIL", f"HTTP {status_pg} - {cola_pg}")

# Paso 28: Salida formal en Trampolines y agotamiento de los 3 intentos en Toro Mecánico
status_sal, ses_fin1 = api_call("POST", f"/api/operacion/sesiones/{sesion_trampolin_id}/salida", token=op1_token)
_, int1 = api_call("POST", f"/api/operacion/sesiones/{sesion_toro_id}/descontar-intento", token=op3_token, body={"observacion": "Caída 1"})
_, int2 = api_call("POST", f"/api/operacion/sesiones/{sesion_toro_id}/descontar-intento", token=op3_token, body={"observacion": "Caída 2"})
status_int3, int3 = api_call("POST", f"/api/operacion/sesiones/{sesion_toro_id}/descontar-intento", token=op3_token, body={"observacion": "Caída 3 definitiva"})
if status_sal == 200 and ses_fin1.get("estadoSesion") == "FINALIZADA" and status_int3 == 200 and int3.get("estadoSesion") == "FINALIZADA" and int3.get("intentosRestantes") == 0:
    log_step(28, "POST /sesiones/{id}/salida & /descontar-intento (Auto-cierre al agotar 3 intentos)", "PASS", f"Sesión Trampolines #{sesion_trampolin_id} FINALIZADA | Sesión Toro #{sesion_toro_id} FINALIZADA al llegar a 0 intentos")
else:
    log_step(28, "Finalización de sesiones", "FAIL", f"sal={status_sal}, int3={status_int3} ({int3})")

# Paso 29: Antifraude 4 - Intento de reutilizar un ticket ya CONSUMIDO -> 409 Conflict
status_af4, resp_af4 = api_call("POST", "/api/operacion/escanear", token=op1_token, body={
    "uuid": uuid_trampolin_mateo,
    "atraccionId": 1
})
if status_af4 == 409:
    log_step(29, "Antifraude QR #4: Bloqueo de reingreso con ticket ya CONSUMIDO", "PASS", "HTTP 409 Conflict (RECHAZADO_ESTADO_INVALIDO)")
else:
    log_step(29, "Antifraude QR #4", "FAIL", f"Se esperaba 409 pero retornó HTTP {status_af4}")

# =========================================================================================
# FASE 5: CIERRE DE CAJA, AUDITORÍA DE TICKETS/INCIDENTES Y REPORTES PDF/CSV
# =========================================================================================
print("\n--- FASE 5: Cierre y Conciliación de Turno, Auditoría y Exportación PDF/CSV ---")

# Paso 30: Cerrar turno de caja (validar rechazo 400 por descuadre sin observación y luego cierre exitoso)
# Monto esperado = base (120,000) + venta efectivo (45,000) = 165,000
status_cierre_fail, _ = api_call("PUT", "/api/turnos/cerrar", token=taq_token, body={
    "montoEfectivoReportado": 160000.00
})
status_cierre_ok, cierre_dto = api_call("PUT", "/api/turnos/cerrar", token=taq_token, body={
    "montoEfectivoReportado": 165000.00,
    "observaciones": "Cierre cuadrado tras prueba funcional E2E"
})
if status_cierre_fail == 400 and status_cierre_ok == 200 and cierre_dto.get("estado") == "CERRADO":
    log_step(30, "PUT /api/turnos/cerrar (Validación CA-04 y conciliación exacta)", "PASS", f"Descuadre sin justificación bloqueado (HTTP 400) | Turno #{turno_id} CERRADO con Total Esperado=${cierre_dto.get('totalEsperado')}, Diferencia=${cierre_dto.get('diferencia')}")
else:
    log_step(30, "PUT /api/turnos/cerrar", "FAIL", f"fail={status_cierre_fail}, ok={status_cierre_ok}, dto={cierre_dto}")

# Paso 31: Consultar Línea de Tiempo de Auditoría (por UUID y por documento) e Incidentes
status_tl1, tl_uuid = api_call("GET", f"/api/auditoria/tickets/{uuid_trampolin_mateo}", token=admin_token)
status_tl2, tl_doc  = api_call("GET", f"/api/auditoria/tickets/{doc_acudiente}", token=admin_token)
status_inc1, inc_op = api_call("GET", "/api/auditoria/incidentes?tipoPausa=OPERATIVA", token=admin_token)
status_inc2, inc_tc = api_call("GET", "/api/auditoria/incidentes?tipoPausa=TECNICA", token=admin_token)
eventos_ticket = tl_uuid[0].get("eventos", []) if isinstance(tl_uuid, list) and tl_uuid else []
total_inc = (inc_op.get("totalIncidentes", 0) if isinstance(inc_op, dict) else 0) + (inc_tc.get("totalIncidentes", 0) if isinstance(inc_tc, dict) else 0)
if status_tl1 == 200 and status_tl2 == 200 and status_inc1 == 200 and status_inc2 == 200 and len(eventos_ticket) >= 6:
    estados_seq = " -> ".join(e.get("estadoDestino", "") for e in eventos_ticket)
    log_step(31, "GET /api/auditoria/tickets/{criterio} & /api/auditoria/incidentes", "PASS", f"Trazabilidad inmutable ({len(eventos_ticket)} eventos: {estados_seq}) | Incidentes auditados: {total_inc}")
else:
    log_step(31, "Endpoints de Auditoría", "FAIL", f"tl1={status_tl1}, tl2={status_tl2}, inc1={status_inc1}, inc2={status_inc2}, ev={len(eventos_ticket)}")

# Paso 32: Reportes Gerenciales, Gráficos Analíticos y Exportación PDF / CSV
status_rt, rep_turnos = api_call("GET", "/api/reportes/turnos", token=admin_token)
status_rd, rep_det    = api_call("GET", f"/api/reportes/turnos/{turno_id}/detalle", token=admin_token)
status_rr, rep_rot    = api_call("GET", "/api/reportes/rotacion-atracciones?periodo=MENSUAL&criterioOrden=INGRESOS", token=admin_token)
status_g1, graf_hp    = api_call("GET", "/api/reportes/graficos/horas-pico", token=admin_token)
status_g2, graf_mp    = api_call("GET", "/api/reportes/graficos/metodos-pago", token=admin_token)
status_g3, graf_ev    = api_call("GET", "/api/reportes/graficos/evolucion-ingresos", token=admin_token)
status_pdf, pdf_bytes, _ = api_call("GET", "/api/reportes/exportar/pdf?tipoReporte=TURNOS", token=admin_token, raw_response=True)
status_csv, csv_bytes, _ = api_call("GET", "/api/reportes/exportar/csv?tipoReporte=ROTACION", token=admin_token, raw_response=True)

if all(s == 200 for s in (status_rt, status_rd, status_rr, status_g1, status_g2, status_g3, status_pdf, status_csv)) and pdf_bytes[:4] == b"%PDF" and csv_bytes[:3] == b"\xef\xbb\xbf":
    log_step(32, "GET /api/reportes/* (Conciliación, Rotación, Gráficos, PDF y CSV con UTF-8 BOM)", "PASS", f"Turnos auditados: {len(rep_turnos)} | PDF generado ({len(pdf_bytes)} bytes) | CSV con BOM ({len(csv_bytes)} bytes)")
else:
    log_step(32, "Endpoints de Reportes y Exportación", "FAIL", f"Statuses: {status_rt},{status_rd},{status_rr},{status_g1},{status_g2},{status_g3},{status_pdf},{status_csv}")

print("\n" + "=" * 92)
print(f"🏆 RESUMEN FINAL E2E EN VIVO: {passed} EXITOSAS | {failed} FALLIDAS | TOTAL: {passed + failed}")
print("=" * 92)
sys.exit(0 if failed == 0 else 1)
PYEOF
