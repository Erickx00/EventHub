-- Eventos. O organizador vem do token; a coluna existe para consulta e
-- verificacao de propriedade, nunca para confiar no cliente.

CREATE TABLE events (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organizer_id UUID         NOT NULL REFERENCES users (id),
    name         VARCHAR(160) NOT NULL,
    description  TEXT,
    category     VARCHAR(40)  NOT NULL,
    city         VARCHAR(120) NOT NULL,
    venue        VARCHAR(160),
    start_date   TIMESTAMPTZ  NOT NULL,
    end_date     TIMESTAMPTZ  NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_events_period CHECK (end_date > start_date),
    CONSTRAINT ck_events_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED', 'FINISHED')),
    CONSTRAINT ck_events_category
        CHECK (category IN ('TECHNOLOGY', 'MUSIC', 'BUSINESS', 'SPORTS', 'ARTS', 'EDUCATION', 'FOOD', 'OTHER'))
);

-- A listagem publica filtra por status e ordena por data de inicio.
CREATE INDEX idx_events_status_start ON events (status, start_date);
CREATE INDEX idx_events_organizer ON events (organizer_id);
-- Busca por cidade sem diferenciar maiusculas.
CREATE INDEX idx_events_city_lower ON events (lower(city));
