-- Endurecimiento de integridad (ejecutar manualmente en PostgreSQL).
-- ddl-auto=update NO amplía columnas existentes ni crea índices parciales.

-- 1. Ampliar pie legal del ticket (el fallback copia politicas_seguridad, 2000 chars).
ALTER TABLE configuracion_parque ALTER COLUMN pie_ticket_legal TYPE VARCHAR(2000);

-- 2. Un operario solo puede tener una asignación ACTIVA (evita condición de carrera).
CREATE UNIQUE INDEX IF NOT EXISTS ux_asignacion_operario_activa
    ON asignaciones_operario (usuario_id)
    WHERE estado = 'ACTIVA';
