package com.campushub.backend.dashboard;

import com.campushub.backend.department.DepartmentRepository;
import com.campushub.backend.student.StudentRepository;
import com.campushub.backend.teacher.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DashboardService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final DepartmentRepository departmentRepository;

    public DashboardService(
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            DepartmentRepository departmentRepository) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatisticsResponse getStatistics() {
        long total = studentRepository.count();
        long active =
                studentRepository.countByStatusIgnoreCase("ACTIVE");
        long inactive =
                studentRepository.countByStatusIgnoreCase("INACTIVE");
        long totalTeachers = teacherRepository.count();
        long activeTeachers =
                teacherRepository.countByStatusIgnoreCase("ACTIVE");
        long totalDepartments = departmentRepository.count();

        List<DashboardStatisticsResponse.DepartmentCount> departments =
                studentRepository.countStudentsByDepartment()
                        .stream()
                        .map(row ->
                                new DashboardStatisticsResponse.DepartmentCount(
                                        (String) row[0],
                                        ((Number) row[1]).longValue()
                                )
                        )
                        .toList();

        return new DashboardStatisticsResponse(
                total,
                active,
                inactive,
                totalTeachers,
                activeTeachers,
                totalDepartments,
                departments
        );
    }

    @Transactional(readOnly = true)
    public EnrollmentTrendResponse getEnrollmentTrend() {
        LocalDate today = LocalDate.now();
        int currentStartYear = today.getMonthValue() >= 9
                ? today.getYear()
                : today.getYear() - 1;

        YearMonth currentStart = YearMonth.of(currentStartYear, 9);
        YearMonth previousStart = currentStart.minusYears(1);
        YearMonth nextStart = currentStart.plusYears(1);

        Map<YearMonth, Long> currentCounts = new HashMap<>();
        Map<YearMonth, Long> previousCounts = new HashMap<>();

        List<Object[]> rows = studentRepository.countEnrollmentsByMonth(
                previousStart.atDay(1),
                nextStart.atDay(1)
        );

        for (Object[] row : rows) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            long count = ((Number) row[2]).longValue();
            YearMonth key = YearMonth.of(year, month);

            if (key.isBefore(currentStart)) {
                previousCounts.put(key, count);
            } else if (key.isBefore(nextStart)) {
                currentCounts.put(key, count);
            }
        }

        int elapsedMonths =
                (today.getYear() - currentStartYear) * 12
                        + today.getMonthValue() - 9;
        int monthCount = elapsedMonths + 1;

        List<String> labels = new ArrayList<>(monthCount);
        List<Long> current = new ArrayList<>(monthCount);
        List<Long> previous = new ArrayList<>(monthCount);
        long currentTotal = 0;
        long previousTotal = 0;

        DateTimeFormatter labelFormatter =
                DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);

        for (int offset = 0; offset < monthCount; offset++) {
            YearMonth currentMonth = currentStart.plusMonths(offset);
            YearMonth previousMonth = previousStart.plusMonths(offset);
            long currentCount = currentCounts.getOrDefault(currentMonth, 0L);
            long previousCount = previousCounts.getOrDefault(previousMonth, 0L);

            labels.add(currentMonth.atDay(1).format(labelFormatter));
            current.add(currentCount);
            previous.add(previousCount);
            currentTotal += currentCount;
            previousTotal += previousCount;
        }

        return new EnrollmentTrendResponse(
                currentStartYear + " / " + (currentStartYear + 1),
                (currentStartYear - 1) + " / " + currentStartYear,
                labels,
                current,
                previous,
                currentTotal,
                previousTotal
        );
    }
}
