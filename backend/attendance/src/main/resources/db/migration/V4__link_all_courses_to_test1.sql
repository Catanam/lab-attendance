-- V4 (2026-06-02): every course must have a professor, and all existing courses
-- are assigned to "Profesor Test1". Data-only (no DDL) so it is safe under
-- spring.jpa.hibernate.ddl-auto=validate. Idempotent.
--
-- Going forward, course creation enforces the link in CourseController; this
-- migration backfills the courses that predate that rule.

-- 1. Link every course to Profesor Test1 as PRIMARY. UNIQUE(course_id, professor_id)
--    makes ON CONFLICT a no-op on re-run. If the professor is absent the CROSS JOIN
--    yields no rows, so this is a safe no-op rather than an error.
INSERT INTO course_teachers (course_id, professor_id, role)
SELECT c.id, p.id, 'PRIMARY'
FROM courses c
CROSS JOIN (SELECT id FROM professors WHERE full_name = 'Profesor Test1' ORDER BY id LIMIT 1) p
ON CONFLICT (course_id, professor_id) DO NOTHING;

-- 2. Point the seeded `prof` login at Profesor Test1 so it is scoped to (and only to)
--    those courses' attendance. Guarded by EXISTS so a missing professor never nulls
--    an existing link.
UPDATE users
SET professor_id = (SELECT id FROM professors WHERE full_name = 'Profesor Test1' ORDER BY id LIMIT 1)
WHERE username = 'prof'
  AND EXISTS (SELECT 1 FROM professors WHERE full_name = 'Profesor Test1');
