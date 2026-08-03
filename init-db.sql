-- ============================================================
-- Initial Database Setup Script
-- Runs automatically on first container start
-- ============================================================

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Create application user (if not exists via POSTGRES_USER)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT FROM pg_catalog.pg_roles
        WHERE  rolname = 'hotel_user') THEN
        
        CREATE ROLE hotel_user WITH LOGIN PASSWORD 'hotel_password';
    END IF;
END
$$;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE hotel TO hotel_user;
GRANT ALL PRIVILEGES ON SCHEMA public TO hotel_user;

-- Set default search path
ALTER DATABASE hotel SET search_path TO public;

-- Note: Tables and indexes will be created by Flyway migrations
-- This script ensures the database is properly configured
