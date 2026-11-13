-- CampusHub Milestone 4 attendance schema.
-- Run this against the campushub_db database before starting the backend,
-- because spring.jpa.hibernate.ddl-auto is set to "validate".
-- This script is additive and does not modify or remove existing student records.

CREATE TABLE IF NOT EXISTS attendance_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    status VARCHAR(10) NOT NULL,
    notes VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_attendance_student_date (student_id, attendance_date),
    KEY idx_attendance_date (attendance_date),
    KEY idx_attendance_status (status),
    CONSTRAINT fk_attendance_student
        FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    CONSTRAINT ck_attendance_status
        CHECK (status IN ('PRESENT', 'LATE', 'ABSENT', 'EXCUSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
