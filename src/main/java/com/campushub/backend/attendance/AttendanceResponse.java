package com.campushub.backend.attendance;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceResponse(
        Long id,
        Long studentId,
        String studentCode,
        String studentName,
        LocalDate attendanceDate,
        String status,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AttendanceResponse from(Attendance attendance) {
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getStudent().getId(),
                attendance.getStudent().getStudentId(),
                attendance.getStudent().getFullName(),
                attendance.getAttendanceDate(),
                attendance.getStatus(),
                attendance.getNotes(),
                attendance.getCreatedAt(),
                attendance.getUpdatedAt()
        );
    }
}
