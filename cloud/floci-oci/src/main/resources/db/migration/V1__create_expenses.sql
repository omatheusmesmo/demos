CREATE TABLE expenses (
    id                   UUID PRIMARY KEY,
    employee             VARCHAR(100)   NOT NULL,
    merchant             VARCHAR(200)   NOT NULL,
    amount               NUMERIC(12, 2) NOT NULL,
    currency             VARCHAR(3)     NOT NULL,
    spent_on             DATE           NOT NULL,
    status               VARCHAR(20)    NOT NULL,
    rejection_reason     VARCHAR(500),
    receipt_content_type VARCHAR(100),
    receipt_size         BIGINT,
    receipt_sha256       VARCHAR(64),
    receipt_object       VARCHAR(200),
    receipt_key          TEXT,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    submitted_at         TIMESTAMP WITH TIME ZONE,
    processed_at         TIMESTAMP WITH TIME ZONE
);

CREATE INDEX expenses_employee_created_at ON expenses (employee, created_at);
