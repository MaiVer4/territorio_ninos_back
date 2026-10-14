-- Migración: Eliminar restricciones y atributos de rango de edad en atracciones
-- Justificación: Requerimiento de restricción etaria retirado del dominio de atracciones.

ALTER TABLE atracciones DROP COLUMN IF EXISTS edad_minima;
ALTER TABLE atracciones DROP COLUMN IF EXISTS edad_maxima;
