-- V1: Extensions va ham dung chung.
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE OR REPLACE FUNCTION trg_set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$;
