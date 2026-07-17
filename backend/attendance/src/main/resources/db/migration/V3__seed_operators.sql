-- V3 (2026-06-01): seed the operator accounts into the `users` table.
--
-- Authentication is currently in-memory (SecurityConfig.InMemoryUserDetailsManager),
-- so the `users` table was empty. But `biometric_consents.operator_id` is a
-- NOT NULL FK to users(id) — every consent must record WHICH staff member captured
-- it (GDPR accountability). To make that FK valid and the audit trail meaningful,
-- we materialise the three demo operators here, with usernames matching the
-- in-memory credentials so the consent service can resolve operator_id by username.
--
-- password_hash is a non-login placeholder: login is handled in-memory, these rows
-- exist only as audit principals. If auth is ever switched to DB-backed, replace the
-- placeholder with a real bcrypt hash.
--
-- Idempotent: ON CONFLICT (username) DO NOTHING so re-running (or running against a
-- DB that already has these usernames) is a no-op.

INSERT INTO users (username, password_hash, role) VALUES
    ('superadmin', '{noop-placeholder:in-memory-auth}', 'SUPER_ADMIN'),
    ('staff',      '{noop-placeholder:in-memory-auth}', 'ADMIN_STAFF'),
    ('prof',       '{noop-placeholder:in-memory-auth}', 'PROFESSOR')
ON CONFLICT (username) DO NOTHING;
