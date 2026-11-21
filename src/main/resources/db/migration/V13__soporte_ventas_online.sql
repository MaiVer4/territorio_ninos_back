-- V13__soporte_ventas_online.sql
-- Habilita ventas por canal ONLINE (sin turno físico de caja obligatorio y con código de orden)

DO $$
BEGIN
    IF to_regclass('public.ventas') IS NOT NULL THEN
        -- 1. Permitir turno_caja_id nulo para ventas originadas en línea
        ALTER TABLE ventas ALTER COLUMN turno_caja_id DROP NOT NULL;

        -- 2. Columna canal_venta
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_name = 'ventas' AND column_name = 'canal_venta'
        ) THEN
            ALTER TABLE ventas ADD COLUMN canal_venta VARCHAR(20) NOT NULL DEFAULT 'TAQUILLA';
        END IF;

        -- 3. Columna codigo_orden único para trazabilidad de compras web
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_name = 'ventas' AND column_name = 'codigo_orden'
        ) THEN
            ALTER TABLE ventas ADD COLUMN codigo_orden VARCHAR(40);
            CREATE UNIQUE INDEX IF NOT EXISTS uq_idx_venta_codigo_orden ON ventas (codigo_orden) WHERE codigo_orden IS NOT NULL;
        END IF;

        CREATE INDEX IF NOT EXISTS idx_venta_canal ON ventas (canal_venta, fecha_venta DESC);
    END IF;
END $$;
