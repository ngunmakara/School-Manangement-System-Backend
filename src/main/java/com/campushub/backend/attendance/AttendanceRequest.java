package com.campushub.backend.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AttendanceRequest(
        @NotNull
        Long studentId,

        @NotNull
        LocalDate attendanceDate,

        @NotBlank
        @Size(max = 10)
        String status,

        @Size(max = 255)
        String notes
) {}
