-- ============================================================
-- Exam Hall Allocation System — PostgreSQL Schema
-- Equivalent of the MySQL schema auto-provisioned by DBConnection.java
--
-- Key differences from MySQL handled here:
--   • SERIAL instead of INT AUTO_INCREMENT
--   • CREATE TYPE enums instead of inline ENUM(...)
--   • CREATE OR REPLACE FUNCTION + CREATE TRIGGER for trigger logic
--   • INSERT IGNORE → INSERT ... ON CONFLICT DO NOTHING
--   • GROUP_CONCAT → STRING_AGG
-- ============================================================

-- ── Session enum (replaces ENUM('FN','AN') inline) ──
DO $$ BEGIN
    CREATE TYPE session_type AS ENUM ('FN', 'AN');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- ────────────────────────────────────────────────────────────
-- TABLES
-- ────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS COURSE (
    course_code VARCHAR(50)  PRIMARY KEY,
    course_name VARCHAR(100) NOT NULL,
    credits     INT          NOT NULL
);

CREATE TABLE IF NOT EXISTS STUDENT (
    roll_number VARCHAR(50)  PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    department  VARCHAR(100) NOT NULL,
    semester    INT          NOT NULL
);

CREATE TABLE IF NOT EXISTS COURSE_OFFERING (
    offering_id SERIAL PRIMARY KEY,
    course_code VARCHAR(50)  NOT NULL,
    department  VARCHAR(100) NOT NULL,
    semester    INT          NOT NULL,
    FOREIGN KEY (course_code)
        REFERENCES COURSE(course_code)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_course_offering_dept_sem
    ON COURSE_OFFERING(department, semester);

CREATE TABLE IF NOT EXISTS STUDENT_COURSE (
    student_course_id SERIAL PRIMARY KEY,
    roll_number       VARCHAR(50) NOT NULL,
    course_code       VARCHAR(50) NOT NULL,
    UNIQUE (roll_number, course_code),
    FOREIGN KEY (roll_number)
        REFERENCES STUDENT(roll_number)
        ON DELETE CASCADE,
    FOREIGN KEY (course_code)
        REFERENCES COURSE(course_code)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS HALL (
    hall_id          SERIAL PRIMARY KEY,
    hall_name        VARCHAR(100) UNIQUE NOT NULL,
    seating_capacity INT NOT NULL
);

CREATE TABLE IF NOT EXISTS INVIGILATOR (
    invigilator_id SERIAL PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    department     VARCHAR(100) NOT NULL,
    email          VARCHAR(100) UNIQUE
);

CREATE TABLE IF NOT EXISTS EXAM (
    exam_id     SERIAL PRIMARY KEY,
    course_code VARCHAR(50)  NOT NULL,
    exam_date   DATE         NOT NULL,
    session     session_type NOT NULL,
    UNIQUE (course_code, exam_date, session),
    FOREIGN KEY (course_code)
        REFERENCES COURSE(course_code)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS STUDENT_SEAT_ALLOCATION (
    allocation_id SERIAL PRIMARY KEY,
    roll_number   VARCHAR(50) NOT NULL,
    exam_id       INT         NOT NULL,
    hall_id       INT         NOT NULL,
    UNIQUE (roll_number, exam_id),
    FOREIGN KEY (roll_number)
        REFERENCES STUDENT(roll_number)
        ON DELETE CASCADE,
    FOREIGN KEY (exam_id)
        REFERENCES EXAM(exam_id)
        ON DELETE CASCADE,
    FOREIGN KEY (hall_id)
        REFERENCES HALL(hall_id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS INVIGILATOR_ALLOCATION (
    invigilator_allocation_id SERIAL PRIMARY KEY,
    invigilator_id            INT NOT NULL,
    exam_id                   INT NOT NULL,
    hall_id                   INT NOT NULL,
    UNIQUE (invigilator_id, exam_id),
    FOREIGN KEY (invigilator_id)
        REFERENCES INVIGILATOR(invigilator_id)
        ON DELETE CASCADE,
    FOREIGN KEY (exam_id)
        REFERENCES EXAM(exam_id)
        ON DELETE CASCADE,
    FOREIGN KEY (hall_id)
        REFERENCES HALL(hall_id)
        ON DELETE CASCADE
);

-- ────────────────────────────────────────────────────────────
-- TRIGGERS
-- PostgreSQL requires a trigger function first, then CREATE TRIGGER.
-- ────────────────────────────────────────────────────────────

-- Trigger 1: auto-enroll student into matching courses on INSERT
CREATE OR REPLACE FUNCTION fn_auto_allocate_courses()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO STUDENT_COURSE (roll_number, course_code)
    SELECT NEW.roll_number, co.course_code
    FROM   COURSE_OFFERING co
    WHERE  co.department = NEW.department
    AND    co.semester   = NEW.semester
    ON CONFLICT (roll_number, course_code) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS auto_allocate_courses ON STUDENT;
CREATE TRIGGER auto_allocate_courses
    AFTER INSERT ON STUDENT
    FOR EACH ROW
    EXECUTE FUNCTION fn_auto_allocate_courses();

-- Trigger 2: auto-enroll student into matching courses on UPDATE (semester/dept change)
CREATE OR REPLACE FUNCTION fn_auto_allocate_on_semester_update()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO STUDENT_COURSE (roll_number, course_code)
    SELECT NEW.roll_number, co.course_code
    FROM   COURSE_OFFERING co
    WHERE  co.department = NEW.department
    AND    co.semester   = NEW.semester
    ON CONFLICT (roll_number, course_code) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS auto_allocate_on_semester_update ON STUDENT;
CREATE TRIGGER auto_allocate_on_semester_update
    AFTER UPDATE ON STUDENT
    FOR EACH ROW
    EXECUTE FUNCTION fn_auto_allocate_on_semester_update();

-- ────────────────────────────────────────────────────────────
-- USAGE NOTES
-- ────────────────────────────────────────────────────────────
-- To connect from the Java app with PostgreSQL instead of MySQL:
--   1. Replace the MySQL JDBC driver with the PostgreSQL driver:
--        postgresql-42.x.x.jar
--   2. Update DBConnection.java:
--        URL  = "jdbc:postgresql://localhost:5432/exam_hall_allocation"
--        USER = "postgres"
--        PASS = "your_password"
--        Class.forName("org.postgresql.Driver");
--   3. Run this script once to create the schema:
--        psql -U postgres -d exam_hall_allocation -f schema_postgres.sql
--   4. The Java app will then work identically since all JDBC calls
--      use standard SQL — only the schema init DDL differs.
