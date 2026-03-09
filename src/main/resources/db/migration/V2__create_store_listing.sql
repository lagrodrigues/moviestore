CREATE TABLE store_listing
(
    id                UUID PRIMARY KEY,
    merchant_id       UUID                     NOT NULL,
    external_movie_id VARCHAR(50)              NOT NULL,
    movie_title       VARCHAR(255)             NOT NULL,
    movie_year        VARCHAR(10),
    movie_poster_url  VARCHAR(500),
    purchase_price    NUMERIC(10, 2),
    rental_price      NUMERIC(10, 2),
    stock             INTEGER                  NOT NULL,
    active            BOOLEAN                  NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    version           BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT fk_store_listing_merchant
        FOREIGN KEY (merchant_id) REFERENCES app_user (id),
    CONSTRAINT chk_store_listing_price_presence
        CHECK (purchase_price IS NOT NULL OR rental_price IS NOT NULL)
);