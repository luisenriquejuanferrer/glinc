-- Rol del cuidador: CAREGIVER (padres/tutores, vista actual) o DOCTOR (medico,
-- vista clinica con mas graficas y sin inventario/citas).
-- Nullable y sin default a proposito: NULL = el usuario aun no ha elegido rol,
-- lo que dispara el modal de seleccion en su proximo login.
ALTER TABLE caregivers ADD COLUMN role VARCHAR(20);

ALTER TABLE caregivers ADD CONSTRAINT chk_caregiver_role
    CHECK (role IS NULL OR role IN ('CAREGIVER', 'DOCTOR'));
