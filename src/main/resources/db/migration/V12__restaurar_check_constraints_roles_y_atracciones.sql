-- V12__restaurar_check_constraints_roles_y_atracciones.sql
-- Restaura las restricciones CHECK en roles.nombre y atracciones.estado incorporando COORDINADOR y FUERA_DE_SERVICIO.

DO $$
DECLARE
    rec RECORD;
BEGIN
    IF to_regclass('public.roles') IS NOT NULL THEN
        FOR rec IN
            SELECT conname FROM pg_constraint
            WHERE conrelid = 'public.roles'::regclass AND contype = 'c'
        LOOP
            EXECUTE format('ALTER TABLE public.roles DROP CONSTRAINT IF EXISTS %I', rec.conname);
        END LOOP;

        ALTER TABLE public.roles
            ADD CONSTRAINT chk_roles_nombre
            CHECK (nombre IN ('ADMINISTRADOR', 'TAQUILLERO', 'OPERARIO', 'COORDINADOR'));
    END IF;

    IF to_regclass('public.atracciones') IS NOT NULL THEN
        FOR rec IN
            SELECT conname FROM pg_constraint
            WHERE conrelid = 'public.atracciones'::regclass AND contype = 'c'
              AND pg_get_constraintdef(oid) ILIKE '%estado%'
        LOOP
            EXECUTE format('ALTER TABLE public.atracciones DROP CONSTRAINT IF EXISTS %I', rec.conname);
        END LOOP;

        ALTER TABLE public.atracciones
            ADD CONSTRAINT chk_atracciones_estado
            CHECK (estado IN ('ACTIVO', 'INACTIVO', 'MANTENIMIENTO', 'FUERA_DE_SERVICIO'));
    END IF;
END $$;
