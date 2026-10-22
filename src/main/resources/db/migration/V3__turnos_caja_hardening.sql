-- V3__turnos_caja_hardening.sql
-- Endurecimiento de turnos de caja:
-- 1. Un cajero/usuario solo puede tener UN turno ABIERTO a la vez (evita condición de carrera).
-- 2. Cierre preventivo de turnos huérfanos antes de indexar.

DO $$
BEGIN
    IF to_regclass('public.turnos_caja') IS NOT NULL THEN
        -- Saneamiento: si existieran turnos duplicados con estado ABIERTO para un usuario,
        -- cerrar los antiguos manteniendo solo el más reciente.
        UPDATE turnos_caja
        SET estado = 'CERRADO',
            fecha_cierre = NOW(),
            observaciones = COALESCE(observaciones, '') || ' [Cierre preventivo V3]'
        WHERE estado = 'ABIERTO'
          AND id NOT IN (
              SELECT MAX(id)
              FROM turnos_caja
              WHERE estado = 'ABIERTO'
              GROUP BY usuario_id
          );

        -- Índice único condicional: usuario_id con estado ABIERTO
        CREATE UNIQUE INDEX IF NOT EXISTS ux_turno_caja_usuario_abierto
            ON turnos_caja (usuario_id)
            WHERE estado = 'ABIERTO';
    END IF;
END $$;
