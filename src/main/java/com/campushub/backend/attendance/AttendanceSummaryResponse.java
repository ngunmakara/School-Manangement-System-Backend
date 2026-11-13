package com.campushub.backend.attendance;

import java.time.LocalDate;

public record AttendanceSummaryResponse(
        LocalDate date,
        long total,
        long present,
        long late,
        long absent,
        long excused
) {}
