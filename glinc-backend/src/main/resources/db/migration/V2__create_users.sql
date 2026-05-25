CREATE TABLE users (
    email        VARCHAR(255) PRIMARY KEY,
    first_name   VARCHAR(100),
    last_name    VARCHAR(100),
    birth_date   DATE,
    phone        VARCHAR(30),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
