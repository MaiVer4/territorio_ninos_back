-- V5__tickets_table.sql
-- Creación de la tabla tickets con clave primaria UUID, restricción única e índices (Notion: CA-02, CA-03)

DO $$
BEGIN
    IF to_regclass('public.detalles_venta') IS NOT NULL THEN
        CREATE TABLE IF NOT EXISTS tickets (
            uuid_codigo UUID PRIMARY KEY,
            detalle_venta_id BIGINT NOT NULL REFERENCES detalles_venta(id) ON DELETE CASCADE,
            menor_id BIGINT REFERENCES menores(id),
            estado_ticket VARCHAR(20) NOT NULL DEFAULT 'PAGADO',
            fecha_emision TIMESTAMP NOT NULL DEFAULT NOW(),
            CONSTRAINT uq_ticket_uuid_codigo UNIQUE (uuid_codigo)
        );

        CREATE INDEX IF NOT EXISTS idx_ticket_uuid_codigo ON tickets (uuid_codigo);
        CREATE INDEX IF NOT EXISTS idx_ticket_detalle_venta ON tickets (detalle_venta_id);
        CREATE INDEX IF NOT EXISTS idx_ticket_menor ON tickets (menor_id);
        CREATE INDEX IF NOT EXISTS idx_ticket_estado ON tickets (estado_ticket);
    END IF;
END $$;
