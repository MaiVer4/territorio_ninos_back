-- V9__reportes_auditoria_y_estadisticas.sql
-- Fase 2: Trazabilidad inmutable de ciclo de vida de tickets e índice compuesto de incidentes/pausas

-- 1. Ampliación de log_incidente_pausa con atraccion_id y tipo_pausa (HU-41.01, HU-41.02, HU-41.03)
DO $$
BEGIN
    IF to_regclass('public.log_incidente_pausa') IS NOT NULL THEN
        ALTER TABLE log_incidente_pausa ADD COLUMN IF NOT EXISTS atraccion_id BIGINT REFERENCES atracciones(id);
        ALTER TABLE log_incidente_pausa ADD COLUMN IF NOT EXISTS tipo_pausa VARCHAR(50) DEFAULT 'OPERATIVA';
    END IF;
END $$;

-- Actualizar atraccion_id histórico a partir de sesion_juego
UPDATE log_incidente_pausa l
SET atraccion_id = s.atraccion_id
FROM sesiones_juego s
WHERE l.sesion_juego_id = s.id AND l.atraccion_id IS NULL;

-- 2. Índice compuesto en log_incidente_pausa para optimización de consultas por rango y atracción (HU-41.01)
CREATE INDEX IF NOT EXISTS idx_log_pausa_fecha_atraccion ON log_incidente_pausa(fecha_inicio_pausa, atraccion_id);

-- 3. Tabla de auditoría inmutable de ciclo de vida de tickets (HU-38.01, HU-38.02)
CREATE TABLE IF NOT EXISTS auditoria_tickets (
    id BIGSERIAL PRIMARY KEY,
    ticket_uuid UUID NOT NULL,
    estado_origen VARCHAR(30),
    estado_destino VARCHAR(30) NOT NULL,
    motivo VARCHAR(500),
    fecha_evento TIMESTAMP NOT NULL DEFAULT NOW(),
    usuario_id BIGINT REFERENCES usuarios(id),
    dispositivo_info VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_auditoria_ticket_uuid ON auditoria_tickets(ticket_uuid, fecha_evento ASC);
CREATE INDEX IF NOT EXISTS idx_auditoria_ticket_fecha ON auditoria_tickets(fecha_evento DESC);
CREATE INDEX IF NOT EXISTS idx_auditoria_ticket_usuario ON auditoria_tickets(usuario_id);
