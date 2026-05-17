CREATE TABLE stock (
    product_id   UUID         NOT NULL PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    available    INTEGER      NOT NULL DEFAULT 0,
    reserved     INTEGER      NOT NULL DEFAULT 0,
    last_updated TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_available_non_negative CHECK (available >= 0),
    CONSTRAINT chk_reserved_non_negative  CHECK (reserved >= 0)
);
