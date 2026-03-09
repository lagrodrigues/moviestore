CREATE TABLE payment
(
    id                UUID PRIMARY KEY,
    customer_id       UUID                     NOT NULL,
    merchant_id       UUID                     NOT NULL,
    store_listing_id  UUID                     NOT NULL,
    idempotency_key   VARCHAR(100)             NOT NULL,
    external_movie_id VARCHAR(50)              NOT NULL,
    movie_title       VARCHAR(255)             NOT NULL,
    type              VARCHAR(20)              NOT NULL,
    amount            NUMERIC(10, 2)           NOT NULL,
    status            VARCHAR(20)              NOT NULL,
    card_last4        VARCHAR(4),
    failure_reason    VARCHAR(255),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    version           BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT fk_payment_customer
        FOREIGN KEY (customer_id) REFERENCES app_user (id),
    CONSTRAINT fk_payment_merchant
        FOREIGN KEY (merchant_id) REFERENCES app_user (id),
    CONSTRAINT fk_payment_store_listing
        FOREIGN KEY (store_listing_id) REFERENCES store_listing (id),
    CONSTRAINT uk_payment_customer_idempotency
        UNIQUE (customer_id, idempotency_key)
);