CREATE TABLE patient_inventory (
    id           BIGSERIAL    PRIMARY KEY,
    user_email   VARCHAR(255) NOT NULL,
    patient_id   VARCHAR(255) NOT NULL,
    item_type    VARCHAR(40)  NOT NULL,
    quantity     VARCHAR(60),
    status       VARCHAR(10)  NOT NULL DEFAULT 'OK',
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- Permite upsert idempotente por (cuidador, paciente, tipo) y aisla cuidadores que comparten paciente.
    CONSTRAINT uq_inventory_user_patient_item
        UNIQUE (user_email, patient_id, item_type)
);

CREATE INDEX idx_inv_user_patient
    ON patient_inventory (user_email, patient_id);
