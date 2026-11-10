-- Pedidos, itens e pagamentos.
-- total_amount e unit_price sao congelados no momento da compra: mudar o preco
-- do ingresso depois nao reescreve o historico.

CREATE TABLE orders (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID           NOT NULL REFERENCES users (id),
    event_id     UUID           NOT NULL REFERENCES events (id),
    total_amount NUMERIC(12, 2) NOT NULL,
    status       VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    expires_at   TIMESTAMPTZ    NOT NULL,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT ck_orders_total CHECK (total_amount > 0),
    CONSTRAINT ck_orders_status
        CHECK (status IN ('PENDING', 'PAID', 'CANCELLED', 'EXPIRED'))
);

CREATE INDEX idx_orders_user_created ON orders (user_id, created_at DESC);
-- O job de expiracao varre por status + expires_at.
CREATE INDEX idx_orders_status_expires ON orders (status, expires_at);

CREATE TABLE order_items (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id       UUID           NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    ticket_type_id UUID           NOT NULL REFERENCES ticket_types (id),
    quantity       INTEGER        NOT NULL,
    unit_price     NUMERIC(12, 2) NOT NULL,

    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_items_price CHECK (unit_price >= 0)
);

-- Um tipo de ingresso aparece uma unica vez por pedido.
CREATE UNIQUE INDEX uk_order_items_order_ticket ON order_items (order_id, ticket_type_id);

CREATE TABLE payments (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id   UUID           NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    amount     NUMERIC(12, 2) NOT NULL,
    status     VARCHAR(20)    NOT NULL,
    method     VARCHAR(20)    NOT NULL,
    paid_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT ck_payments_amount CHECK (amount >= 0),
    CONSTRAINT ck_payments_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REFUSED', 'REFUNDED'))
);

CREATE INDEX idx_payments_order ON payments (order_id);
