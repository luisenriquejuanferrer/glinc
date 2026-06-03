-- Normalizacion mayor (post-V6):
-- 1) Renombrar `users` a `caregivers` (mas semantico: el usuario de la app es el cuidador).
-- 2) Crear relacion N:N `caregiver_patients` para tener la asociacion cuidador<->paciente persistida.
-- 3) Quitar `user_email` de `patient_inventory` y `patient_appointments` (pasan a ser per-paciente, compartidos entre cuidadores que tengan acceso).
-- 4) CHECK constraints en glucose_readings, patient_inventory y caregivers.

-- 1. RENAME users -> caregivers
ALTER TABLE users RENAME TO caregivers;

-- 2. Relacion N:N
CREATE TABLE caregiver_patients (
    id               BIGSERIAL    PRIMARY KEY,
    caregiver_email  VARCHAR(255) NOT NULL,
    patient_id       VARCHAR(255) NOT NULL,
    linked_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_caregiver_patient UNIQUE (caregiver_email, patient_id),
    CONSTRAINT fk_cp_caregiver FOREIGN KEY (caregiver_email) REFERENCES caregivers(email),
    CONSTRAINT fk_cp_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
);
CREATE INDEX idx_cp_caregiver ON caregiver_patients (caregiver_email);
CREATE INDEX idx_cp_patient ON caregiver_patients (patient_id);

-- 3. Backfill relaciones desde lo que ya teniamos en inventory y appointments.
INSERT INTO caregiver_patients (caregiver_email, patient_id)
SELECT DISTINCT user_email, patient_id FROM patient_inventory
ON CONFLICT (caregiver_email, patient_id) DO NOTHING;

INSERT INTO caregiver_patients (caregiver_email, patient_id)
SELECT DISTINCT user_email, patient_id FROM patient_appointments
ON CONFLICT (caregiver_email, patient_id) DO NOTHING;

-- 4. Inventario: dedupe por (patient_id, item_type) conservando la fila mas reciente (max id) si dos cuidadores tenian la misma.
DELETE FROM patient_inventory
WHERE id NOT IN (
    SELECT MAX(id) FROM patient_inventory GROUP BY patient_id, item_type
);
ALTER TABLE patient_inventory DROP CONSTRAINT uq_inventory_user_patient_item;
ALTER TABLE patient_inventory DROP COLUMN user_email;
ALTER TABLE patient_inventory ADD CONSTRAINT uq_inventory_patient_item UNIQUE (patient_id, item_type);
DROP INDEX IF EXISTS idx_inv_user_patient;
CREATE INDEX idx_inv_patient ON patient_inventory (patient_id);

-- 5. Citas: drop columna user_email. No hace falta dedup (cada cita tiene id propio).
ALTER TABLE patient_appointments DROP COLUMN user_email;
DROP INDEX IF EXISTS idx_appt_user_patient_date;
CREATE INDEX idx_appt_patient_date ON patient_appointments (patient_id, appointment_at DESC);

-- 6. CHECK constraints (validacion a nivel BD).
ALTER TABLE glucose_readings
    ADD CONSTRAINT chk_glucose_mgdl CHECK (mg_dl BETWEEN 20 AND 600),
    ADD CONSTRAINT chk_glucose_trend CHECK (trend IN ('rising_fast','rising','flat','falling','falling_fast','unknown'));

ALTER TABLE patient_inventory
    ADD CONSTRAINT chk_inv_status CHECK (status IN ('OK','WARN','DANGER')),
    ADD CONSTRAINT chk_inv_item_type CHECK (item_type IN ('SENSORS','INSULIN_FAST','INSULIN_SLOW','GLUCAGON'));

ALTER TABLE caregivers
    ADD CONSTRAINT chk_caregiver_email_format CHECK (email LIKE '%@%'),
    ADD CONSTRAINT chk_caregiver_birth_date_past CHECK (birth_date IS NULL OR birth_date < CURRENT_DATE);
