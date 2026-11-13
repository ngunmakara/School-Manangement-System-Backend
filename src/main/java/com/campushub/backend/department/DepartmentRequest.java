package com.campushub.backend.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
        @NotBlank
        @Size(max = 80)
        String name,

        @NotBlank
        @Size(max = 20)
        String code,

        @Size(max = 120)
        String headOfDepartment,

        @Size(max = 500)
        String description
) {}
