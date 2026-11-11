-- Ingressos individuais e check-in.
-- Um ticket por unidade comprada; o QR Code carrega apenas o qr_token, que nao
-- expoe dado pessoal nenhum.

CREATE TABLE tickets (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id       UUID        NOT NULL REFERENCES orders (id),
    order_item_id  UUID        NOT NULL REFERENCES order_items (id),
    event_id       UUID        NOT NULL REFERENCES events (id),
    ticket_type_id UUID        NOT NULL REFERENCES ticket_types (id),
    user_id        UUID        NOT NULL REFERENCES users (id),
    qr_token       VARCHAR(64) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'VALID',
    issued_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_tickets_qr_token UNIQUE (qr_token),
    CONSTRAINT ck_tickets_status CHECK (status IN ('VALID', 'USED', 'CANCELLED'))
);

CREATE INDEX idx_tickets_user ON tickets (user_id, issued_at DESC);
CREATE INDEX idx_tickets_event ON tickets (event_id);

CREATE TABLE check_ins (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id    UUID        NOT NULL REFERENCES tickets (id) ON DELETE CASCADE,
    validated_by UUID        NOT NULL REFERENCES users (id),
    checked_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Rede de seguranca: mesmo que a validacao atomica falhe, o banco recusa
    -- um segundo check-in do mesmo ingresso.
    CONSTRAINT uk_check_ins_ticket UNIQUE (ticket_id)
);

CREATE INDEX idx_check_ins_checked_at ON check_ins (checked_at);
