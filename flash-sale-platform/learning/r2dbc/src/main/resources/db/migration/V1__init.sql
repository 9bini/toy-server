-- 학습용 테이블: 상품
CREATE TABLE IF NOT EXISTS products (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255)    NOT NULL,
    price       BIGINT          NOT NULL,
    stock       INT             NOT NULL DEFAULT 0,
    status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- 학습용 테이블: 주문
CREATE TABLE IF NOT EXISTS orders (
    id          BIGSERIAL PRIMARY KEY,
    order_id    VARCHAR(50)     NOT NULL UNIQUE,
    product_id  BIGINT          NOT NULL REFERENCES products(id),
    user_id     VARCHAR(100)    NOT NULL,
    quantity    INT             NOT NULL,
    total_price BIGINT          NOT NULL,
    status      VARCHAR(20)     NOT NULL DEFAULT 'CREATED',
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- 인덱스
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_product_id_status ON orders(product_id, status);
CREATE INDEX idx_products_status ON products(status);
