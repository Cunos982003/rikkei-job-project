-- Migration V3: Drop local users table and associated objects since auth-service manages users
DROP TABLE IF EXISTS users CASCADE;

