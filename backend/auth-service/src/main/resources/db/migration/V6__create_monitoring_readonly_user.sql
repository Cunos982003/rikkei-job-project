-- =============================================================================
-- V6: Create read-only PostgreSQL monitoring user for postgres_exporter
-- =============================================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'prometheus_exporter') THEN
        CREATE ROLE prometheus_exporter WITH LOGIN PASSWORD 'prometheus_exporter_readonly' NOSUPERUSER NOCREATEDB NOCREATEROLE;
    END IF;
    GRANT pg_monitor TO prometheus_exporter;
EXCEPTION
    WHEN insufficient_privilege THEN
        RAISE NOTICE 'Skipping prometheus_exporter role creation due to insufficient privileges';
END
$$;
