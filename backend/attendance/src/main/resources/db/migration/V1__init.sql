-- V1__init.sql: Initial schema for lab attendance system

CREATE TABLE students (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    matricol_number VARCHAR(50)  NOT NULL UNIQUE,
    full_name       VARCHAR(200) NOT NULL,
    email           VARCHAR(200) UNIQUE,
    active          BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE professors (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    title        VARCHAR(100),
    full_name    VARCHAR(200) NOT NULL,
    email        VARCHAR(200) UNIQUE,
    office_room  VARCHAR(100),
    office_hours VARCHAR(200)
);

CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(200) NOT NULL,
    role          VARCHAR(50)  NOT NULL,
    professor_id  UUID REFERENCES professors(id)
);

CREATE TABLE courses (
    id   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50)  NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL
);

CREATE TABLE student_groups (
    id   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    year VARCHAR(20)
);

CREATE TABLE rooms (
    id       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name     VARCHAR(100) NOT NULL UNIQUE,
    building VARCHAR(200)
);

CREATE TABLE academic_terms (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code       VARCHAR(50)  NOT NULL UNIQUE,
    name       VARCHAR(200) NOT NULL,
    start_date DATE         NOT NULL,
    end_date   DATE         NOT NULL
);

CREATE TABLE academic_enrollments (
    id         UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(id),
    group_id   UUID NOT NULL REFERENCES student_groups(id),
    term_id    UUID NOT NULL REFERENCES academic_terms(id),
    UNIQUE (student_id, group_id, term_id)
);

CREATE TABLE scheduled_labs (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    term_id              UUID        NOT NULL REFERENCES academic_terms(id),
    course_id            UUID        NOT NULL REFERENCES courses(id),
    group_id             UUID        NOT NULL REFERENCES student_groups(id),
    room_id              UUID        NOT NULL REFERENCES rooms(id),
    primary_professor_id UUID        NOT NULL REFERENCES professors(id),
    weekday              VARCHAR(20) NOT NULL,
    start_time           TIME        NOT NULL,
    end_time             TIME        NOT NULL
);

CREATE TABLE lab_sessions (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    scheduled_lab_id UUID        NOT NULL REFERENCES scheduled_labs(id),
    session_date     DATE        NOT NULL,
    start_at         TIMESTAMP   NOT NULL,
    end_at           TIMESTAMP   NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'
);

CREATE TABLE lab_substitutions (
    id                      UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    scheduled_lab_id        UUID NOT NULL REFERENCES scheduled_labs(id),
    date                    DATE NOT NULL,
    substitute_professor_id UUID NOT NULL REFERENCES professors(id),
    UNIQUE (scheduled_lab_id, date)
);

CREATE TABLE holidays (
    id     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    date   DATE         NOT NULL UNIQUE,
    scope  VARCHAR(100),
    reason VARCHAR(500)
);

CREATE TABLE fingerprint_registrations (
    id            UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id    UUID      NOT NULL REFERENCES students(id),
    registered_at TIMESTAMP NOT NULL,
    active        BOOLEAN   NOT NULL DEFAULT TRUE
);

CREATE TABLE fingerprint_template_blobs (
    id                 UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    registration_id    UUID    NOT NULL REFERENCES fingerprint_registrations(id),
    finger_index       INTEGER NOT NULL,
    encrypted_template BYTEA   NOT NULL,
    data_key_encrypted BYTEA   NOT NULL,
    iv                 BYTEA   NOT NULL
);

CREATE TABLE biometric_consents (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id        UUID         NOT NULL REFERENCES students(id),
    consented_at      TIMESTAMP    NOT NULL,
    text_version_hash VARCHAR(200) NOT NULL,
    operator_id       UUID         NOT NULL REFERENCES users(id),
    withdrawn_at      TIMESTAMP
);

CREATE TABLE devices (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id      UUID         NOT NULL REFERENCES rooms(id),
    secret_hash  VARCHAR(200) NOT NULL,
    last_seen_at TIMESTAMP,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE scan_events (
    id             UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id      UUID             NOT NULL REFERENCES devices(id),
    scan_nonce     VARCHAR(100)     NOT NULL,
    scanned_at     TIMESTAMP        NOT NULL,
    lab_session_id UUID             REFERENCES lab_sessions(id),
    student_id     UUID             REFERENCES students(id),
    kind           VARCHAR(20)      NOT NULL,
    match_score    DOUBLE PRECISION NOT NULL DEFAULT 0,
    should_count   BOOLEAN          NOT NULL DEFAULT FALSE,
    UNIQUE (device_id, scan_nonce)
);
