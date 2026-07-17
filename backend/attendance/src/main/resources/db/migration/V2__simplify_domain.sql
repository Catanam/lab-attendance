-- V2__simplify_domain.sql
--
-- Domain simplification (data-preserving):
--   * students gains year_of_study + primary_group_id.
--   * academic_enrollments rows collapse into students.primary_group_id
--     (most recent term wins on duplicates).
--   * scheduled_labs.primary_professor_id rows migrate into course_teachers
--     as PRIMARY-role mappings, then the column is dropped.
--   * holidays table dropped (operator handles edge cases at report time).
--   * academic_terms gains is_current with a partial-unique guard so at most
--     one row can be flagged current.

-- 1. Add new student columns BEFORE migrating data into them.
ALTER TABLE students
    ADD COLUMN year_of_study    INTEGER,
    ADD COLUMN primary_group_id UUID REFERENCES student_groups(id);

CREATE INDEX students_primary_group_idx ON students(primary_group_id);

-- 2. Collapse academic_enrollments → students.primary_group_id.
-- DISTINCT ON keeps one row per student; ORDER BY end_date DESC picks the
-- enrollment whose term ends latest, so an active enrollment beats a stale one.
WITH latest_per_student AS (
    SELECT DISTINCT ON (ae.student_id)
           ae.student_id,
           ae.group_id
    FROM academic_enrollments ae
    JOIN academic_terms t ON t.id = ae.term_id
    ORDER BY ae.student_id, t.end_date DESC, ae.id
)
UPDATE students s
SET primary_group_id = lps.group_id
FROM latest_per_student lps
WHERE s.id = lps.student_id;

DROP TABLE academic_enrollments;
DROP TABLE holidays;

-- 3. Create course_teachers, then migrate scheduled_labs.primary_professor_id
-- as PRIMARY-role rows. ON CONFLICT keeps the table idempotent if a future
-- migration re-runs against a partially-migrated DB.
CREATE TABLE course_teachers (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id     UUID         NOT NULL REFERENCES courses(id)    ON DELETE CASCADE,
    professor_id  UUID         NOT NULL REFERENCES professors(id) ON DELETE CASCADE,
    role          VARCHAR(20)  NOT NULL,
    UNIQUE (course_id, professor_id)
);

CREATE INDEX course_teachers_course_idx    ON course_teachers(course_id);
CREATE INDEX course_teachers_professor_idx ON course_teachers(professor_id);

INSERT INTO course_teachers (id, course_id, professor_id, role)
SELECT DISTINCT
       gen_random_uuid(),
       sl.course_id,
       sl.primary_professor_id,
       'PRIMARY'
FROM scheduled_labs sl
WHERE sl.primary_professor_id IS NOT NULL
ON CONFLICT (course_id, professor_id) DO NOTHING;

ALTER TABLE scheduled_labs DROP COLUMN primary_professor_id;

-- 4. academic_terms current flag.
ALTER TABLE academic_terms ADD COLUMN is_current BOOLEAN NOT NULL DEFAULT FALSE;
CREATE UNIQUE INDEX academic_terms_current_idx ON academic_terms(is_current) WHERE is_current = true;
