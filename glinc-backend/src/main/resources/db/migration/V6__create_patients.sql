-- Tabla maestra de pacientes. Normaliza first_name / last_name que hasta ahora
-- se duplicaban en cada fila de glucose_readings, y prepara FKs desde
-- glucose_readings, patient_inventory y patient_appointments.

CREATE TABLE patients (
    patient_id   VARCHAR(255) PRIMARY KEY,    -- ID original de LibreLink Up
    first_name   VARCHAR(100),
    last_name    VARCHAR(100),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Backfill desde glucose_readings (unica fuente con nombres hasta ahora).
-- Si un paciente tiene multiples filas con el mismo nombre, MAX las colapsa.
INSERT INTO patients (patient_id, first_name, last_name)
SELECT patient_id, MAX(first_name), MAX(last_name)
FROM glucose_readings
GROUP BY patient_id
ON CONFLICT (patient_id) DO NOTHING;

-- Backfill de pacientes que solo tienen inventario o citas (sin lecturas).
-- No tienen nombre conocido todavia; se completara cuando vuelvan a loggearse.
INSERT INTO patients (patient_id)
SELECT DISTINCT patient_id FROM patient_inventory
ON CONFLICT (patient_id) DO NOTHING;

INSERT INTO patients (patient_id)
SELECT DISTINCT patient_id FROM patient_appointments
ON CONFLICT (patient_id) DO NOTHING;

-- Ya no se duplican en cada lectura.
ALTER TABLE glucose_readings DROP COLUMN first_name;
ALTER TABLE glucose_readings DROP COLUMN last_name;

-- Integridad referencial.
ALTER TABLE glucose_readings
    ADD CONSTRAINT fk_glucose_patient
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id);

ALTER TABLE patient_inventory
    ADD CONSTRAINT fk_inventory_patient
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id);

ALTER TABLE patient_appointments
    ADD CONSTRAINT fk_appointment_patient
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id);
