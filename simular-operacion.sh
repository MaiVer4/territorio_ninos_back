#!/bin/bash
# =========================================================================================
# Script ejecutable para simular un día de apogeo medio-bajo, un día de mucho apogeo
# y operación en vivo del parque "Territorio de Niños".
#
# Uso:
#   ./simular-operacion.sh                  # Ejecuta en territorioNinos y produccion
#   ./simular-operacion.sh territorioNinos  # Ejecuta solo en territorioNinos
#   ./simular-operacion.sh produccion       # Ejecuta solo en produccion
# =========================================================================================
set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
# El contenedor Docker territorio_db expone PostgreSQL en el puerto 5454 para localhost:8080 / localhost:4200
if ss -tln | grep -q ":5454 "; then
    DEFAULT_PORT="5454"
else
    DEFAULT_PORT="5432"
fi
DB_PORT="${DB_PORT:-$DEFAULT_PORT}"
DB_USER="${DB_USERNAME:-postgres}"
export PGPASSWORD="${DB_PASSWORD:-postgres}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="${SCRIPT_DIR}/scripts/simulacion_operacion_parque.sql"

TARGET="${1:-territorioNinos}"

ejecutar_psql() {
    local db_name="$1"
    shift
    if docker ps --format '{{.Names}}' 2>/dev/null | grep -qx "territorio_db"; then
        docker exec -i territorio_db psql -U "$DB_USER" -d "$db_name" "$@"
    else
        psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$db_name" "$@"
    fi
}

ejecutar_en_bd() {
    local db_name="$1"
    echo "================================================================================"
    if docker ps --format '{{.Names}}' 2>/dev/null | grep -qx "territorio_db"; then
        echo ">>> Cargando simulación en contenedor Docker [territorio_db] -> BD: ${db_name}"
    else
        echo ">>> Cargando simulación en PostgreSQL local (${DB_HOST}:${DB_PORT}) -> BD: ${db_name}"
    fi
    echo "================================================================================"
    ejecutar_psql "$db_name" -v ON_ERROR_STOP=1 < "$SQL_FILE" > /dev/null

    echo ""
    echo "📊 1. COMPARATIVA POR JORNADA EN [${db_name}]:"
    ejecutar_psql "$db_name" -c "
    WITH dias AS (
        SELECT
            DATE(fecha_venta) AS fecha,
            COUNT(*) AS total_ventas,
            SUM(monto_total) AS facturacion_total
        FROM ventas
        GROUP BY DATE(fecha_venta)
    )
    SELECT
        d.fecha,
        CASE
            WHEN d.fecha = (NOW() AT TIME ZONE 'America/Bogota')::DATE - 2 THEN '1. Apogeo Medio-Bajo (Entre semana)'
            WHEN d.fecha = (NOW() AT TIME ZONE 'America/Bogota')::DATE - 1 THEN '2. Mucho Apogeo (Fin de semana / Pico)'
            ELSE '3. Operación en Vivo (Hoy)'
        END AS escenario,
        d.total_ventas,
        (SELECT COUNT(*) FROM tickets t JOIN detalles_venta dv ON dv.id = t.detalle_venta_id JOIN ventas v2 ON v2.id = dv.venta_id WHERE DATE(v2.fecha_venta) = d.fecha) AS tickets_emitidos,
        TO_CHAR(d.facturacion_total, 'FM\$ 999,999,999.00') AS facturacion,
        (SELECT COUNT(*) FROM sesiones_juego s WHERE DATE(s.hora_inicio) = d.fecha) AS sesiones_juego,
        (SELECT COUNT(*) FROM log_incidente_pausa p WHERE DATE(p.fecha_inicio_pausa) = d.fecha) AS incidentes_pausas,
        (SELECT COUNT(*) FROM log_escaneo_qr q WHERE DATE(q.fecha_escaneo) = d.fecha AND q.resultado LIKE 'RECHAZADO%') AS rechazos_qr
    FROM dias d
    ORDER BY d.fecha;
    "

    echo "📊 2. ROTACIÓN E INGRESOS POR ATRACCIÓN EN [${db_name}]:"
    ejecutar_psql "$db_name" -c "
    SELECT
        a.id,
        a.nombre AS atraccion,
        a.tipo,
        a.capacidad_maxima AS aforo_max,
        COUNT(DISTINCT s.id) FILTER (WHERE DATE(s.hora_inicio) = (NOW() AT TIME ZONE 'America/Bogota')::DATE - 2) AS usos_dia_bajo,
        COUNT(DISTINCT s.id) FILTER (WHERE DATE(s.hora_inicio) = (NOW() AT TIME ZONE 'America/Bogota')::DATE - 1) AS usos_dia_alto,
        TO_CHAR(COALESCE((SELECT SUM(dv.subtotal) FROM detalles_venta dv WHERE dv.atraccion_id = a.id), 0), 'FM\$ 999,999,999.00') AS ingresos_brutos_acumulados
    FROM atracciones a
    LEFT JOIN sesiones_juego s ON s.atraccion_id = a.id
    GROUP BY a.id, a.nombre, a.tipo, a.capacidad_maxima
    ORDER BY a.id;
    "

    echo "📊 3. CONCILIACIÓN DE TURNOS DE CAJA EN [${db_name}]:"
    ejecutar_psql "$db_name" -c "
    SELECT
        t.id AS turno_id,
        u.nombre AS cajero,
        TO_CHAR(t.fecha_apertura, 'YYYY-MM-DD HH24:MI') AS apertura,
        t.estado,
        TO_CHAR(t.monto_base, 'FM\$ 999,999.00') AS base,
        TO_CHAR(t.ventas_efectivo_sistema, 'FM\$ 999,999,999.00') AS ventas_efec,
        TO_CHAR(COALESCE(t.diferencia, 0), 'FM\$ 999,999.00') AS diferencia,
        CASE
            WHEN t.estado = 'ABIERTO' THEN 'EN CURSO'
            WHEN t.diferencia = 0 THEN 'CUADRADO'
            WHEN t.diferencia > 0 THEN 'SOBRANTE'
            ELSE 'FALTANTE'
        END AS conciliacion
    FROM turnos_caja t
    JOIN usuarios u ON u.id = t.usuario_id
    ORDER BY t.fecha_apertura;
    "
}

if [ "$TARGET" = "ambas" ]; then
    ejecutar_en_bd "territorioNinos"
    ejecutar_en_bd "produccion"
    pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "produccion" -f "${SCRIPT_DIR}/produccion.sql"
    echo "✅ Respaldo produccion.sql actualizado con los datos de simulación."
elif [ "$TARGET" = "produccion" ]; then
    ejecutar_en_bd "produccion"
    pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "produccion" -f "${SCRIPT_DIR}/produccion.sql"
    echo "✅ Respaldo produccion.sql actualizado con los datos de simulación."
else
    ejecutar_en_bd "$TARGET"
fi
