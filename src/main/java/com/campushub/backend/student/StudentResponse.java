
package com.campushub.backend.student;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentResponse(
        Long id,
        String studentId,
        String fullName,
        String email,
        String phone,
        String department,
        String program,
        LocalDate enrollmentDate,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static StudentResponse from(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getStudentId(),
                student.getFullName(),
                student.getEmail(),
                student.getPhone(),
                student.getDepartment(),
                student.getProgram(),
                student.getEnrollmentDate(),
                student.getStatus(),
                student.getCreatedAt(),
                student.getUpdatedAt()
        );
    }
}
