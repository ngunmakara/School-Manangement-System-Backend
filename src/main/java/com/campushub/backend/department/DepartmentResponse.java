package com.campushub.backend.department;

import java.time.LocalDateTime;

public record DepartmentResponse(
        Long id,
        String name,
        String code,
        String headOfDepartment,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DepartmentResponse from(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getCode(),
                department.getHeadOfDepartment(),
                department.getDescription(),
                department.getCreatedAt(),
                department.getUpdatedAt()
        );
    }
}
