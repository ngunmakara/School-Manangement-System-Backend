package com.campushub.backend.attendance;

import com.campushub.backend.student.Student;
import com.campushub.backend.student.StudentRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AttendanceServiceTests {

    @Autowired
    private AttendanceService service;

    @Autowired
    private AttendanceRepository repository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private Validator validator;

    @Test
    void createsUpdatesSearchesSummarizesAndDeletesAttendance() {
        Student student = createStudent("STU-6001", "Nora Test", "nora6001@example.com");
        LocalDate date = LocalDate.of(2026, 11, 10);

        AttendanceResponse created = service.create(
                new AttendanceRequest(student.getId(), date, "present", " On time ")
        );

        assertNotNull(created.id());
        assertEquals(student.getId(), created.studentId());
        assertEquals("STU-6001", created.studentCode());
        assertEquals("Nora Test", created.studentName());
        assertEquals("PRESENT", created.status());
        assertEquals("On time", created.notes());

        AttendanceSummaryResponse firstSummary = service.getSummary(date);
        assertEquals(1, firstSummary.total());
        assertEquals(1, firstSummary.present());
        assertEquals(0, firstSummary.late());

        AttendanceResponse updated = service.update(
                created.id(),
                new AttendanceRequest(student.getId(), date, "late", " ")
        );

        assertEquals(created.id(), updated.id());
        assertEquals("LATE", updated.status());
        assertNull(updated.notes());

        AttendanceSummaryResponse updatedSummary = service.getSummary(date);
        assertEquals(1, updatedSummary.total());
        assertEquals(0, updatedSummary.present());
        assertEquals(1, updatedSummary.late());

        var bySearch = service.getAttendance("nora", null, date, null, 0, 20);
        assertEquals(1, bySearch.content().size());

        var byStatusAndStudent = service.getAttendance(
                null, "late", null, student.getId(), 0, 20
        );
        assertEquals(1, byStatusAndStudent.content().size());

        var noMatch = service.getAttendance(
                null, "absent", null, student.getId(), 0, 20
        );
        assertTrue(noMatch.content().isEmpty());

        service.delete(created.id());
        assertTrue(repository.findById(created.id()).isEmpty());
    }

    @Test
    void rejectsDuplicateAttendanceInvalidStatusAndUnknownStudent() {
        Student student = createStudent("STU-6002", "Mina Test", "mina6002@example.com");
        Student other = createStudent("STU-6003", "Paul Test", "paul6003@example.com");
        LocalDate date = LocalDate.of(2026, 11, 11);

        service.create(new AttendanceRequest(student.getId(), date, "PRESENT", null));

        ResponseStatusException duplicate = assertThrows(
                ResponseStatusException.class,
                () -> service.create(
                        new AttendanceRequest(student.getId(), date, "ABSENT", null)
                )
        );
        assertEquals(HttpStatus.CONFLICT, duplicate.getStatusCode());

        AttendanceResponse otherStudent = service.create(
                new AttendanceRequest(other.getId(), date, "EXCUSED", null)
        );
        assertNotNull(otherStudent.id());

        ResponseStatusException invalidStatus = assertThrows(
                ResponseStatusException.class,
                () -> service.create(
                        new AttendanceRequest(
                                other.getId(), date.plusDays(1), "ON_LEAVE", null
                        )
                )
        );
        assertEquals(HttpStatus.BAD_REQUEST, invalidStatus.getStatusCode());

        ResponseStatusException unknownStudent = assertThrows(
                ResponseStatusException.class,
                () -> service.create(
                        new AttendanceRequest(999999L, date.plusDays(1), "PRESENT", null)
                )
        );
        assertEquals(HttpStatus.NOT_FOUND, unknownStudent.getStatusCode());
    }

    @Test
    void rejectsNewAttendanceForInactiveOrSoftDeletedStudentsButKeepsHistory() {
        Student inactive = createStudent(
                "STU-6004", "Inactive Test", "inactive6004@example.com"
        );
        inactive.setStatus("INACTIVE");
        studentRepository.saveAndFlush(inactive);

        ResponseStatusException inactiveConflict = assertThrows(
                ResponseStatusException.class,
                () -> service.create(new AttendanceRequest(
                        inactive.getId(),
                        LocalDate.of(2026, 11, 12),
                        "PRESENT",
                        null
                ))
        );
        assertEquals(HttpStatus.CONFLICT, inactiveConflict.getStatusCode());

        Student historyStudent = createStudent(
                "STU-6005", "History Test", "history6005@example.com"
        );
        LocalDate historyDate = LocalDate.of(2026, 11, 13);
        AttendanceResponse historical = service.create(
                new AttendanceRequest(
                        historyStudent.getId(), historyDate, "PRESENT", null
                )
        );

        historyStudent.markDeleted();
        studentRepository.saveAndFlush(historyStudent);

        ResponseStatusException deletedConflict = assertThrows(
                ResponseStatusException.class,
                () -> service.create(new AttendanceRequest(
                        historyStudent.getId(),
                        historyDate.plusDays(1),
                        "ABSENT",
                        null
                ))
        );
        assertEquals(HttpStatus.CONFLICT, deletedConflict.getStatusCode());

        AttendanceResponse preserved = service.getById(historical.id());
        assertEquals("STU-6005", preserved.studentCode());
        assertEquals("History Test", preserved.studentName());

        var byStudent = service.getAttendance(
                null, null, null, historyStudent.getId(), 0, 20
        );
        assertEquals(1, byStudent.content().size());

        AttendanceSummaryResponse summary = service.getSummary(historyDate);
        assertEquals(1, summary.total());
        assertEquals(1, summary.present());
    }

    @Test
    void validatesRequiredAttendanceFields() {
        var violations = validator.validate(
                new AttendanceRequest(null, null, " ", "x".repeat(256))
        );

        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath().toString().equals("studentId")
        ));
        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath().toString().equals("attendanceDate")
        ));
        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath().toString().equals("status")
        ));
        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath().toString().equals("notes")
        ));
    }

    private Student createStudent(String code, String name, String email) {
        Student student = new Student();
        student.setStudentId(code);
        student.setFullName(name);
        student.setEmail(email);
        student.setDepartment("Computer Science");
        student.setProgram("BSc Computer Science");
        student.setEnrollmentDate(LocalDate.of(2026, 9, 1));
        student.setStatus("ACTIVE");
        return studentRepository.saveAndFlush(student);
    }
}
