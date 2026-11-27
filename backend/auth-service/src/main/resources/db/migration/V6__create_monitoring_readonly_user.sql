-- =============================================================================
-- V6: Create read-only PostgreSQL monitoring user for postgres_exporter
-- =============================================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'pg_exporter') THEN
        CREATE ROLE pg_exporter WITH LOGIN PASSWORD 'pg_exporter_readonly' NOSUPERUSER NOCREATEDB NOCREATEROLE;
    END IF;
    GRANT pg_monitor TO pg_exporter;
EXCEPTION
    WHEN insufficient_privilege THEN
        RAISE NOTICE 'Skipping pg_exporter role creation due to insufficient privileges';
END
$$;
