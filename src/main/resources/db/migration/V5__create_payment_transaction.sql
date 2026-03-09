CREATE TABLE payment_transaction
(
    id          UUID PRIMARY KEY,
    payment_id  UUID                     NOT NULL,
    from_status VARCHAR(20),
    to_status   VARCHAR(20)              NOT NULL,
    amount      NUMERIC(10, 2)           NOT NULL,
    message     VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_payment_transaction_payment
        FOREIGN KEY (payment_id) REFERENCES payment (id)
);