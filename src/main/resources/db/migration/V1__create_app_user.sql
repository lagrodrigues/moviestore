CREATE TABLE app_user
(
    id         UUID PRIMARY KEY,
    username   VARCHAR(100)             NOT NULL UNIQUE,
    api_key    VARCHAR(100)             NOT NULL UNIQUE,
    role       VARCHAR(20)              NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);