-- V10__ticket_hora_ingreso_estimada.sql
-- Sobreventa con aforo lleno: hora estimada de ingreso informada al acudiente (NULL = ingreso inmediato / AHORA)
DO $$
BEGIN
    IF to_regclass('public.tickets') IS NOT NULL THEN
        ALTER TABLE tickets ADD COLUMN IF NOT EXISTS hora_ingreso_estimada TIMESTAMP;
    END IF;
END $$;
