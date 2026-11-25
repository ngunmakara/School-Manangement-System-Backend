package com.campushub.backend.dashboard;

import com.campushub.backend.student.Student;
import com.campushub.backend.student.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DashboardServiceTests {

    @Autowired
    private DashboardService service;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    void aggregatesEnrollmentTrendFromStudentEnrollmentDates() {
        LocalDate today = LocalDate.now();

        createStudent("STU-7001", "Trend One", "trend7001@example.com", today);
        createStudent("STU-7002", "Trend Two", "trend7002@example.com", today);
        createStudent(
                "STU-7003",
                "Trend Previous",
                "trend7003@example.com",
                today.minusYears(1)
        );

        EnrollmentTrendResponse trend = service.getEnrollmentTrend();

        assertNotNull(trend.currentAcademicYear());
        assertNotNull(trend.previousAcademicYear());
        assertFalse(trend.labels().isEmpty());
        assertEquals(trend.labels().size(), trend.current().size());
        assertEquals(trend.labels().size(), trend.previous().size());
        assertEquals(2, trend.currentTotal());
        assertEquals(1, trend.previousTotal());
    }

    @Test
    void excludesSoftDeletedStudentsFromLiveDashboardAggregates() {
        LocalDate today = LocalDate.now();

        createStudent("STU-7101", "Live Student", "live7101@example.com", today);
        Student deleted = createStudent(
                "STU-7102", "Deleted Student", "deleted7102@example.com", today
        );
        deleted.markDeleted();
        studentRepository.saveAndFlush(deleted);

        DashboardStatisticsResponse statistics = service.getStatistics();
        assertEquals(1, statistics.totalStudents());
        assertEquals(1, statistics.activeStudents());
        assertEquals(0, statistics.inactiveStudents());
        assertEquals(1, statistics.studentsByDepartment().size());
        assertEquals(1, statistics.studentsByDepartment().get(0).count());

        EnrollmentTrendResponse trend = service.getEnrollmentTrend();
        assertEquals(1, trend.currentTotal());
        assertEquals(0, trend.previousTotal());
    }

    private Student createStudent(
            String code, String name, String email, LocalDate enrollmentDate) {

        Student student = new Student();
        student.setStudentId(code);
        student.setFullName(name);
        student.setEmail(email);
        student.setDepartment("Computer Science");
        student.setProgram("BSc Computer Science");
        student.setEnrollmentDate(enrollmentDate);
        student.setStatus("ACTIVE");
        return studentRepository.saveAndFlush(student);
    }
}
