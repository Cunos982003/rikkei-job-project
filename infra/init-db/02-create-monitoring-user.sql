-- =============================================================================
-- 02-create-monitoring-user.sql : Read-only PostgreSQL user for postgres_exporter
-- =============================================================================
-- Chạy tự động bởi docker-entrypoint-initdb.d khi khởi tạo cluster mới.
-- Nếu cluster đã tồn tại từ trước, Flyway V6 trong auth-service hoặc chạy
-- thủ công script này sẽ tạo role `pg_exporter` với quyền chỉ đọc `pg_monitor`.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'pg_exporter') THEN
        CREATE ROLE pg_exporter WITH LOGIN PASSWORD 'pg_exporter_readonly' NOSUPERUSER NOCREATEDB NOCREATEROLE;
    END IF;
END
$$;

GRANT pg_monitor TO pg_exporter;
GRANT CONNECT ON DATABASE rikkei_jobs TO pg_exporter;
GRANT CONNECT ON DATABASE auth_db TO pg_exporter;
GRANT CONNECT ON DATABASE profile_db TO pg_exporter;
GRANT CONNECT ON DATABASE job_db TO pg_exporter;
GRANT CONNECT ON DATABASE recruitment_db TO pg_exporter;
GRANT CONNECT ON DATABASE learning_db TO pg_exporter;
GRANT CONNECT ON DATABASE ai_db TO pg_exporter;
GRANT CONNECT ON DATABASE assessment_db TO pg_exporter;
GRANT CONNECT ON DATABASE evidence_db TO pg_exporter;
GRANT CONNECT ON DATABASE notification_db TO pg_exporter;
GRANT CONNECT ON DATABASE audit_db TO pg_exporter;
