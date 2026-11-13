package com.campushub.backend.dashboard;

import java.util.List;

public record DashboardStatisticsResponse(
        long totalStudents,
        long activeStudents,
        long inactiveStudents,
        long totalTeachers,
        long activeTeachers,
        long totalDepartments,
        List<DepartmentCount> studentsByDepartment
) {
    public record DepartmentCount(
            String department,
            long count
    ) {}
}
