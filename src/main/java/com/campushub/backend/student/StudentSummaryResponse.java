package com.campushub.backend.student;

public record StudentSummaryResponse(
        long totalStudents,
        long activeStudents,
        long inactiveStudents,
        long departmentCount
) {}
