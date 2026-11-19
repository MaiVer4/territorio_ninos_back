-- V11__rol_coordinador_y_reportes_atraccion.sql
-- Nuevo rol COORDINADOR, estado FUERA_DE_SERVICIO para atracciones y tabla de reportes de novedades (daños / suplementos)

-- 1. Actualizar posibles constraints CHECK generados por Hibernate en roles.nombre y atracciones.estado
DO $$
DECLARE
    rec RECORD;
BEGIN
    IF to_regclass('public.roles') IS NOT NULL THEN
        FOR rec IN
            SELECT conname FROM pg_constraint
            WHERE conrelid = 'public.roles'::regclass AND contype = 'c'
        LOOP
            EXECUTE format('ALTER TABLE public.roles DROP CONSTRAINT IF EXISTS %I', rec.conname);
        END LOOP;

        INSERT INTO public.roles (nombre, descripcion, fecha_creacion)
        VALUES ('COORDINADOR', 'Coordinador operativo: activa/desactiva atracciones, reporta novedades y apoya operación en estaciones', NOW())
        ON CONFLICT (nombre) DO NOTHING;
    END IF;

    IF to_regclass('public.atracciones') IS NOT NULL THEN
        FOR rec IN
            SELECT conname FROM pg_constraint
            WHERE conrelid = 'public.atracciones'::regclass AND contype = 'c'
              AND pg_get_constraintdef(oid) ILIKE '%estado%'
        LOOP
            EXECUTE format('ALTER TABLE public.atracciones DROP CONSTRAINT IF EXISTS %I', rec.conname);
        END LOOP;
    END IF;
END $$;

-- 2. Tabla de reportes de atracciones (daños, falta de suplementos, mantenimiento preventivo)
CREATE TABLE IF NOT EXISTS reportes_atraccion (
    id BIGSERIAL PRIMARY KEY,
    atraccion_id BIGINT NOT NULL REFERENCES atracciones(id),
    usuario_reporta_id BIGINT NOT NULL REFERENCES usuarios(id),
    tipo_reporte VARCHAR(40) NOT NULL,
    severidad VARCHAR(20) NOT NULL DEFAULT 'MEDIA',
    descripcion VARCHAR(1000) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTO',
    atraccion_desactivada BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_reporte TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_resolucion TIMESTAMP,
    usuario_resuelve_id BIGINT REFERENCES usuarios(id),
    observaciones_resolucion VARCHAR(1000)
);

CREATE INDEX IF NOT EXISTS idx_reporte_atraccion_atraccion ON reportes_atraccion(atraccion_id);
CREATE INDEX IF NOT EXISTS idx_reporte_atraccion_estado ON reportes_atraccion(estado);
CREATE INDEX IF NOT EXISTS idx_reporte_atraccion_fecha ON reportes_atraccion(fecha_reporte DESC);
