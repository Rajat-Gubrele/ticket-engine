create table idempotency_records
(
    key           VARCHAR(255) PRIMARY KEY,
    user_id       VARCHAR(255) NOT NULL,
    show_id       VARCHAR(255) NOT NULL,
    seat_id       VARCHAR(255) NOT NULL,
    response_code INT          NOT NULL,
    response_body TEXT         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
)