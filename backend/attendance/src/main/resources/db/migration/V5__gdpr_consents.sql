-- V5__gdpr_consents.sql
--
-- Student-confirmed GDPR data-processing consent.
--
-- At the start of the year, after students are entered (with an email), staff send
-- each student a confirmation link. The student opens it and accepts (or declines).
-- A student's fingerprint can only be registered once this consent is ACCEPTED.
--
-- One row per student (unique student_id). Re-sending replaces the token and resets
-- the row to PENDING, which invalidates any previously issued link.

CREATE TABLE gdpr_consents (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id        UUID         NOT NULL UNIQUE REFERENCES students(id),
    token             VARCHAR(100) NOT NULL UNIQUE,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    text_version_hash VARCHAR(200) NOT NULL,
    sent_at           TIMESTAMP,
    confirmed_at      TIMESTAMP
);

CREATE INDEX gdpr_consents_status_idx ON gdpr_consents(status);
