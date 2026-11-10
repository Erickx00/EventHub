-- Tipos de ingresso de um evento.
-- available_quantity e alterado por UPDATE condicional atomico (nunca por
-- leitura seguida de escrita), para nao vender acima do estoque.

CREATE TABLE ticket_types (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id           UUID           NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    name               VARCHAR(80)    NOT NULL,
    description        VARCHAR(400),
    price              NUMERIC(12, 2) NOT NULL,
    total_quantity     INTEGER        NOT NULL,
    available_quantity INTEGER        NOT NULL,
    max_per_order      INTEGER        NOT NULL,
    sales_start        TIMESTAMPTZ    NOT NULL,
    sales_end          TIMESTAMPTZ    NOT NULL,
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT ck_ticket_types_price CHECK (price >= 0),
    CONSTRAINT ck_ticket_types_total CHECK (total_quantity > 0),
    CONSTRAINT ck_ticket_types_available
        CHECK (available_quantity >= 0 AND available_quantity <= total_quantity),
    CONSTRAINT ck_ticket_types_max_per_order
        CHECK (max_per_order > 0 AND max_per_order <= total_quantity),
    CONSTRAINT ck_ticket_types_sales_window CHECK (sales_end > sales_start),
    CONSTRAINT uk_ticket_types_event_name UNIQUE (event_id, name)
);

CREATE INDEX idx_ticket_types_event ON ticket_types (event_id);
