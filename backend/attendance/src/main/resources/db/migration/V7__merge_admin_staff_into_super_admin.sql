-- V7 (2026-06-23): collapse the ADMIN_STAFF role into SUPER_ADMIN.
--
-- SUPER_ADMIN and ADMIN_STAFF had drifted to mean the same thing — the only power
-- ADMIN_STAFF lacked was the GDPR retention-purge tab (/admin/**). The two-role split
-- added confusion without value, so we merge them into a single administrative role.
-- Every account that was ADMIN_STAFF becomes a full SUPER_ADMIN; the seeded `staff`
-- login keeps working and is now a full admin.
--
-- The Role enum no longer has ADMIN_STAFF, so any row left with that string would fail
-- to map on load. Idempotent: re-running matches no rows once converted.

UPDATE users SET role = 'SUPER_ADMIN' WHERE role = 'ADMIN_STAFF';
