-- Endurecimiento de integridad.
-- Idempotente y segura en BD nueva (las tablas pueden no existir aún: las crea Hibernate).

DO $$
BEGIN
    -- 1. Ampliar pie legal del ticket (el fallback copia politicas_seguridad, 2000 chars).
    IF to_regclass('public.configuracion_parque') IS NOT NULL THEN
        ALTER TABLE configuracion_parque ALTER COLUMN pie_ticket_legal TYPE VARCHAR(2000);
    END IF;

    -- 2. Un operario solo puede tener una asignación ACTIVA (evita condición de carrera).
    IF to_regclass('public.asignaciones_operario') IS NOT NULL THEN
        CREATE UNIQUE INDEX IF NOT EXISTS ux_asignacion_operario_activa
            ON asignaciones_operario (usuario_id)
            WHERE estado = 'ACTIVA';
    END IF;
END $$;
