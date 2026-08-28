-- ============================================================
-- V4__add_postgresql_search_indexes.sql
-- PostgreSQL-only search indexes.
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_faq_question_trgm
    ON faq_items USING gin (lower(question) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_faq_answer_trgm
    ON faq_items USING gin (lower(answer) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_faq_meta_keywords_trgm
    ON faq_items USING gin (lower(meta_keywords) gin_trgm_ops);
