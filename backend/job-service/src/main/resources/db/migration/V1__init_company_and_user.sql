-- DN-01: bảng companies (bản tối thiểu của NT-02) và liên kết users.company_id

CREATE TABLE companies (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    tax_code    VARCHAR(13)  NOT NULL UNIQUE,
    industry    VARCHAR(255),
    size        VARCHAR(20),
    address     VARCHAR(500),
    city        VARCHAR(100),
    website     VARCHAR(255),
    logo_url    VARCHAR(500),
    description TEXT,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_by  VARCHAR(100),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_companies_tax_code_len CHECK (CHAR_LENGTH(tax_code) IN (10, 13)),
    CONSTRAINT chk_companies_size   CHECK (size IS NULL OR size IN ('1-50', '51-200', '201-1000', '1000+')),
    CONSTRAINT chk_companies_status CHECK (status IN ('ACTIVE', 'LOCKED'))
);

-- LƯU Ý: bảng users thuộc NT-01. Đây là bản tối thiểu để DN-01 chạy độc lập.
-- Khi merge với develop, nếu NT-01 đã có bảng users thì BỎ khối này và chỉ giữ
-- phần thêm cột company_id (xem hướng dẫn trao đổi với nhóm).
CREATE TABLE users (
    id                   BIGSERIAL PRIMARY KEY,
    email                VARCHAR(255) NOT NULL UNIQUE,
    full_name            VARCHAR(255) NOT NULL,
    password_hash        VARCHAR(255) NOT NULL,
    role                 VARCHAR(50)  NOT NULL,
    company_id           BIGINT REFERENCES companies(id),
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_company_id ON users(company_id);