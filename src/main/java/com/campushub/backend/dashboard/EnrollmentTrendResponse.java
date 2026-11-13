package com.campushub.backend.dashboard;

import java.util.List;

public record EnrollmentTrendResponse(
        String currentAcademicYear,
        String previousAcademicYear,
        List<String> labels,
        List<Long> current,
        List<Long> previous,
        long currentTotal,
        long previousTotal
) {}
