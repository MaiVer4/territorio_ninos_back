-- V4__ventas_and_impuestos.sql
-- Migración idempotente para asegurar columnas de impuestos en configuracion_parque
-- y estructura de persistencia transaccional de ventas y detalles de venta.

DO $$
BEGIN
    -- 1. Asegurar columnas de impuestos en configuracion_parque si la tabla ya existe
    IF to_regclass('public.configuracion_parque') IS NOT NULL THEN
        ALTER TABLE configuracion_parque
            ADD COLUMN IF NOT EXISTS aplica_impuesto BOOLEAN NOT NULL DEFAULT TRUE;
        ALTER TABLE configuracion_parque
            ADD COLUMN IF NOT EXISTS porcentaje_impuesto NUMERIC(5,2) NOT NULL DEFAULT 0.00;
    END IF;

    -- 2. Crear tabla ventas si las tablas padre existen
    IF to_regclass('public.clientes') IS NOT NULL AND to_regclass('public.turnos_caja') IS NOT NULL THEN
        CREATE TABLE IF NOT EXISTS ventas (
            id BIGSERIAL PRIMARY KEY,
            cliente_id BIGINT NOT NULL REFERENCES clientes(id),
            turno_caja_id BIGINT NOT NULL REFERENCES turnos_caja(id),
            monto_total NUMERIC(12,2) NOT NULL,
            subtotal NUMERIC(12,2),
            descuento NUMERIC(12,2) DEFAULT 0.00,
            impuestos NUMERIC(12,2) DEFAULT 0.00,
            monto_recibido NUMERIC(12,2),
            cambio NUMERIC(12,2) DEFAULT 0.00,
            metodo_pago VARCHAR(30) NOT NULL,
            estado_venta VARCHAR(20) NOT NULL DEFAULT 'COMPLETADA',
            fecha_venta TIMESTAMP NOT NULL DEFAULT NOW(),
            observaciones VARCHAR(500)
        );

        CREATE INDEX IF NOT EXISTS idx_venta_turno_caja ON ventas (turno_caja_id, fecha_venta DESC);
        CREATE INDEX IF NOT EXISTS idx_venta_cliente ON ventas (cliente_id);
        CREATE INDEX IF NOT EXISTS idx_venta_fecha ON ventas (fecha_venta DESC);
    END IF;

    -- 3. Crear tabla detalles_venta si ventas y atracciones existen
    IF to_regclass('public.ventas') IS NOT NULL AND to_regclass('public.atracciones') IS NOT NULL THEN
        CREATE TABLE IF NOT EXISTS detalles_venta (
            id BIGSERIAL PRIMARY KEY,
            venta_id BIGINT NOT NULL REFERENCES ventas(id) ON DELETE CASCADE,
            atraccion_id BIGINT NOT NULL REFERENCES atracciones(id),
            menor_id BIGINT REFERENCES menores(id),
            tipo_tarifa VARCHAR(30) NOT NULL,
            tarifa_tiempo_id BIGINT,
            tarifa_evento_id BIGINT,
            cantidad INTEGER NOT NULL DEFAULT 1,
            precio_unitario NUMERIC(12,2) NOT NULL,
            subtotal NUMERIC(12,2) NOT NULL
        );

        CREATE INDEX IF NOT EXISTS idx_detalle_venta ON detalles_venta (venta_id);
        CREATE INDEX IF NOT EXISTS idx_detalle_atraccion ON detalles_venta (atraccion_id);
        CREATE INDEX IF NOT EXISTS idx_detalle_menor ON detalles_venta (menor_id);
    END IF;
END $$;
