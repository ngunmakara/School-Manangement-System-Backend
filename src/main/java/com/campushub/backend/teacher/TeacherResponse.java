package com.campushub.backend.teacher;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TeacherResponse(
        Long id,
        String teacherId,
        String fullName,
        String email,
        String phone,
        String department,
        String specialization,
        LocalDate hireDate,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TeacherResponse from(Teacher teacher) {
        return new TeacherResponse(
                teacher.getId(),
                teacher.getTeacherId(),
                teacher.getFullName(),
                teacher.getEmail(),
                teacher.getPhone(),
                teacher.getDepartment(),
                teacher.getSpecialization(),
                teacher.getHireDate(),
                teacher.getStatus(),
                teacher.getCreatedAt(),
                teacher.getUpdatedAt()
        );
    }
}
