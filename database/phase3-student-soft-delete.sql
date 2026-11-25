-- CampusHub Phase 3 students schema plan.
-- Review only. Do not run against a live or existing user database without explicit authorization.
-- spring.jpa.hibernate.ddl-auto is "validate" and Flyway is disabled, so this must be applied manually first.

-- Duplicate checks before adding stricter uniqueness.
-- Both queries must return zero rows. Resolve any rows returned before applying collation or unique-key changes.
SELECT LOWER(student_id) AS normalized_student_id, COUNT(*) AS duplicate_count
FROM students
GROUP BY LOWER(student_id)
HAVING COUNT(*) > 1;

SELECT LOWER(email) AS normalized_email, COUNT(*) AS duplicate_count
FROM students
GROUP BY LOWER(email)
HAVING COUNT(*) > 1;

-- Fresh-install students reference.
-- Uses the same case-insensitive collation convention as the tracked milestone SQL scripts.
CREATE TABLE IF NOT EXISTS students (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL COLLATE utf8mb4_unicode_ci,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL COLLATE utf8mb4_unicode_ci,
    phone VARCHAR(30) NULL,
    department VARCHAR(80) NOT NULL,
    program VARCHAR(80) NOT NULL,
    enrollment_date DATE NOT NULL,
    status VARCHAR(10) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_students_student_id (student_id),
    UNIQUE KEY uk_students_email (email),
    KEY idx_students_department (department),
    KEY idx_students_status (status),
    KEY idx_students_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Existing-database upgrade after duplicate checks pass.
-- ALTER TABLE students
--     ADD COLUMN deleted_at DATETIME(6) NULL AFTER updated_at;
--
-- ALTER TABLE students
--     ADD INDEX idx_students_deleted_at (deleted_at);
--
-- If the table or identity columns were created with a case-sensitive collation,
-- normalize them before relying on application-level ignore-case checks:
-- ALTER TABLE students
--     MODIFY student_id VARCHAR(20) COLLATE utf8mb4_unicode_ci NOT NULL,
--     MODIFY email VARCHAR(160) COLLATE utf8mb4_unicode_ci NOT NULL;

-- The attendance_records FK currently uses ON DELETE CASCADE.
-- Phase 3 soft deletion must stop application code from issuing physical student deletes.
-- Review the FK policy separately before any non-application physical delete operation.
