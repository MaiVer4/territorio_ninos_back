-- Migración: Eliminar restricciones y atributos de rango de edad en atracciones
-- Justificación: Requerimiento de restricción etaria retirado del dominio de atracciones.

ALTER TABLE atracciones DROP COLUMN IF EXISTS edad_minima;
ALTER TABLE atracciones DROP COLUMN IF EXISTS edad_maxima;

-- Limpieza de texto remanente de edad en descripciones históricas
UPDATE atracciones SET descripcion = regexp_replace(descripcion, '\s*·\s*edad\s*\d+[–-]\d+', '', 'g') WHERE descripcion ILIKE '%edad%';
