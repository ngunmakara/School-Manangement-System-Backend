-- CampusHub Milestone 5 schema changes.
-- Run this against the campushub_db database before starting the backend,
-- because spring.jpa.hibernate.ddl-auto is set to "validate".

CREATE TABLE IF NOT EXISTS departments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL,
    code VARCHAR(20) NOT NULL,
    head_of_department VARCHAR(120) NULL,
    description VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_departments_name (name),
    UNIQUE KEY uk_departments_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS teachers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    teacher_id VARCHAR(20) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL,
    phone VARCHAR(30) NULL,
    department VARCHAR(80) NOT NULL,
    specialization VARCHAR(120) NOT NULL,
    hire_date DATE NOT NULL,
    status VARCHAR(10) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_teachers_teacher_id (teacher_id),
    UNIQUE KEY uk_teachers_email (email),
    KEY idx_teachers_department (department),
    KEY idx_teachers_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
