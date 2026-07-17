-- A group ("1") is a single shared row reused across years of study, so a
-- ScheduledLab tied only to a group pulled every year's "group 1" students into
-- one roster (and mis-classified cross-year scans as REGULAR). Add the year the
-- lab serves so the roster + scan match on (group, year).
--
-- Nullable on purpose: existing rows pre-date this column and we can't infer their
-- year. A NULL year keeps the legacy "all years of the group" behaviour until an
-- operator sets it; new labs (form + import) always carry a year.
ALTER TABLE scheduled_labs ADD COLUMN year_of_study INT;
