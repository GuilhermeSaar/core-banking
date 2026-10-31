INSERT INTO accounts (id, name, status, balance, created_at)
VALUES (
    '11111111-1111-4111-8111-111111111111',
    'Conta Demo Origem',
    0,
    1000.00,
    CURRENT_TIMESTAMP
)
ON CONFLICT DO NOTHING;

INSERT INTO accounts (id, name, status, balance, created_at)
VALUES (
    '22222222-2222-4222-8222-222222222222',
    'Conta Demo Destino',
    0,
    1000.00,
    CURRENT_TIMESTAMP
)
ON CONFLICT DO NOTHING;
