-- V8__operacion_fase1_y_base.sql
-- Fase 1, 2 y 3: Operación de atracciones, trazabilidad QR antifraude, sesiones de juego y aforo en tiempo real

-- 1. Ampliación de la tabla atracciones con restricciones opcionales de edad (HU-24.02, HU-26.05)
DO $$
BEGIN
    IF to_regclass('public.atracciones') IS NOT NULL THEN
        ALTER TABLE atracciones ADD COLUMN IF NOT EXISTS edad_minima INTEGER;
        ALTER TABLE atracciones ADD COLUMN IF NOT EXISTS edad_maxima INTEGER;
    END IF;
END $$;

-- 2. Ampliación de la tabla tickets con fecha de consumo (HU-32.03)
DO $$
BEGIN
    IF to_regclass('public.tickets') IS NOT NULL THEN
        ALTER TABLE tickets ADD COLUMN IF NOT EXISTS fecha_consumo TIMESTAMP;
    END IF;
END $$;

-- 3. Tabla de auditoría y trazabilidad antifraude de escaneos QR (HU-25.03)
CREATE TABLE IF NOT EXISTS log_escaneo_qr (
    id BIGSERIAL PRIMARY KEY,
    uuid_leido UUID NOT NULL,
    ticket_encontrado BOOLEAN NOT NULL DEFAULT FALSE,
    atraccion_id BIGINT NOT NULL REFERENCES atracciones(id),
    operario_id BIGINT NOT NULL REFERENCES usuarios(id),
    direccion_ip VARCHAR(64),
    timestamp_cliente TIMESTAMP,
    fecha_escaneo TIMESTAMP NOT NULL DEFAULT NOW(),
    resultado VARCHAR(40) NOT NULL,
    detalle VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_log_escaneo_uuid ON log_escaneo_qr(uuid_leido);
CREATE INDEX IF NOT EXISTS idx_log_escaneo_operario ON log_escaneo_qr(operario_id);
CREATE INDEX IF NOT EXISTS idx_log_escaneo_atraccion ON log_escaneo_qr(atraccion_id);
CREATE INDEX IF NOT EXISTS idx_log_escaneo_fecha ON log_escaneo_qr(fecha_escaneo DESC);

-- 4. Tabla de sesiones de juego para aforo en tiempo real y temporización (HU-27.01, HU-29.01, HU-32.01, HU-33.01)
CREATE TABLE IF NOT EXISTS sesiones_juego (
    id BIGSERIAL PRIMARY KEY,
    ticket_id UUID NOT NULL UNIQUE REFERENCES tickets(uuid_codigo),
    atraccion_id BIGINT NOT NULL REFERENCES atracciones(id),
    menor_id BIGINT REFERENCES menores(id),
    operario_id BIGINT NOT NULL REFERENCES usuarios(id),
    hora_inicio TIMESTAMP NOT NULL,
    hora_fin TIMESTAMP,
    duracion_segundos INTEGER NOT NULL DEFAULT 0,
    estado_sesion VARCHAR(30) NOT NULL DEFAULT 'EN_CURSO',
    tiempo_restante_pausa INTEGER,
    fecha_pausa TIMESTAMP,
    fecha_salida_real TIMESTAMP,
    segundos_exceso INTEGER NOT NULL DEFAULT 0,
    intentos_totales INTEGER,
    intentos_restantes INTEGER,
    alerta_pre_expiracion_enviada BOOLEAN NOT NULL DEFAULT FALSE,
    alerta_expiracion_enviada BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_sesion_atraccion_estado ON sesiones_juego(atraccion_id, estado_sesion);
CREATE INDEX IF NOT EXISTS idx_sesion_hora_fin ON sesiones_juego(hora_fin ASC);
CREATE INDEX IF NOT EXISTS idx_sesion_ticket ON sesiones_juego(ticket_id);

-- 5. Tabla de auditoría de incidentes y pausas (HU-29.02)
CREATE TABLE IF NOT EXISTS log_incidente_pausa (
    id BIGSERIAL PRIMARY KEY,
    sesion_juego_id BIGINT NOT NULL REFERENCES sesiones_juego(id),
    operario_id BIGINT NOT NULL REFERENCES usuarios(id),
    motivo VARCHAR(500) NOT NULL,
    es_pausa_general BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_inicio_pausa TIMESTAMP NOT NULL,
    fecha_fin_pausa TIMESTAMP,
    duracion_segundos INTEGER
);

CREATE INDEX IF NOT EXISTS idx_log_pausa_sesion ON log_incidente_pausa(sesion_juego_id);
CREATE INDEX IF NOT EXISTS idx_log_pausa_operario ON log_incidente_pausa(operario_id);
