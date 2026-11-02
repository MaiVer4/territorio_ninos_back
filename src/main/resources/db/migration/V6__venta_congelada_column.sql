-- V6__venta_congelada_column.sql
-- Asegurar columna congelada con valor por defecto TRUE en tabla ventas (CA-03)

DO $$
BEGIN
    IF to_regclass('public.ventas') IS NOT NULL THEN
        ALTER TABLE ventas ADD COLUMN IF NOT EXISTS congelada BOOLEAN NOT NULL DEFAULT TRUE;
        ALTER TABLE ventas ALTER COLUMN congelada SET DEFAULT TRUE;
    END IF;
END $$;
