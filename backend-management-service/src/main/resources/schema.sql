-- Migration: add scheduling columns to services table if they do not already exist.
-- This script is idempotent and safe to run on every application start.
-- Wrapped in a DO block so it is a no-op when the table does not yet exist
-- (e.g., on a fresh database where Hibernate has not run yet).

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'services'
    ) THEN
        ALTER TABLE services
            ADD COLUMN IF NOT EXISTS scheduled        BOOLEAN   NOT NULL DEFAULT FALSE,
            ADD COLUMN IF NOT EXISTS scheduled_for    TIMESTAMP,
            ADD COLUMN IF NOT EXISTS scheduled_end_at TIMESTAMP;
    END IF;
END
$$;
