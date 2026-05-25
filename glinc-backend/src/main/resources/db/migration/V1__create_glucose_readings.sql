CREATE TABLE glucose_readings (
    id          BIGSERIAL    PRIMARY KEY,
    patient_id  VARCHAR(255) NOT NULL,
    first_name  VARCHAR(255),
    last_name   VARCHAR(255),
    mg_dl       INTEGER      NOT NULL,
    trend       VARCHAR(20)  NOT NULL,
    source      VARCHAR(10)  NOT NULL DEFAULT 'REAL',
    read_at     TIMESTAMPTZ  NOT NULL,

    -- Evita duplicar la misma lectura del mismo paciente cuando el bridge la devuelve dos veces.
    CONSTRAINT uq_patient_read_at UNIQUE (patient_id, read_at)
);

CREATE INDEX idx_glucose_patient_time ON glucose_readings (patient_id, read_at DESC);
