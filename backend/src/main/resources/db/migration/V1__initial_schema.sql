-- EventHub - schema inicial
-- Convencao: snake_case, UUID como PK, timestamptz em UTC.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(120)  NOT NULL,
    email         VARCHAR(180)  NOT NULL,
    password_hash VARCHAR(100)  NOT NULL,
    role          VARCHAR(20)   NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('ATTENDEE', 'ORGANIZER', 'ADMIN'))
);

-- Login e sempre por e-mail em minusculas; o indice unico cobre a busca.
CREATE INDEX idx_users_role ON users (role);
