CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    name VARCHAR(255) UNIQUE,
    status SMALLINT,
    balance NUMERIC(18, 2) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE transfers (
    id UUID PRIMARY KEY,
    source_account_id UUID REFERENCES accounts(id),
    destination_account_id UUID REFERENCES accounts(id),
    amount NUMERIC(18, 2) NOT NULL,
    status VARCHAR(255),
    created_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id),
    transfer_id UUID REFERENCES transfers(id),
    entry_type VARCHAR(255) NOT NULL,
    nature SMALLINT,
    amount NUMERIC(18, 2) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE idempotency_records (
    id UUID PRIMARY KEY,
    idempotency_key UUID UNIQUE,
    request_hash VARCHAR(255),
    transfer_id UUID NOT NULL UNIQUE REFERENCES transfers(id),
    status VARCHAR(255),
    response_data TIMESTAMP(6) NOT NULL
);
