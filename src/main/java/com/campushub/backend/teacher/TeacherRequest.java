package com.campushub.backend.teacher;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TeacherRequest(
        @NotBlank
        @Size(max = 20)
        String teacherId,

        @NotBlank
        @Size(max = 120)
        String fullName,

        @NotBlank
        @Email
        @Size(max = 160)
        String email,

        @Size(max = 30)
        String phone,

        @NotBlank
        @Size(max = 80)
        String department,

        @NotBlank
        @Size(max = 120)
        String specialization,

        @NotNull
        LocalDate hireDate,

        @NotBlank
        @Size(max = 10)
        String status
) {}
