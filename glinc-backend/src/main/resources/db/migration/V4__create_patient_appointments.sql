CREATE TABLE patient_appointments (
    id              BIGSERIAL    PRIMARY KEY,
    user_email      VARCHAR(255) NOT NULL,
    patient_id      VARCHAR(255) NOT NULL,
    appointment_at  TIMESTAMPTZ  NOT NULL,
    professional    VARCHAR(120) NOT NULL,
    reason          VARCHAR(300),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_appt_user_patient_date
    ON patient_appointments (user_email, patient_id, appointment_at DESC);
