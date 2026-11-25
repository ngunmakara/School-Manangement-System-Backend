package com.campushub.backend.student;

import com.campushub.backend.common.DuplicateFieldException;
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
class StudentServiceTests {

    @Autowired
    private StudentService service;

    @Autowired
    private StudentRepository repository;

    @Autowired
    private Validator validator;

    @Test
    void searchesFiltersSortsAndPaginatesStudentDirectory() {
        createStudent(
                "STU-1001",
                "Alice Johnson",
                "alice@example.com",
                "+1 555 0199",
                "Computer Science",
                "BSc Computer Science",
                LocalDate.of(2024, 9, 1),
                "ACTIVE"
        );
        createStudent(
                "STU-1002",
                "Zoe Adams",
                "zoe@example.com",
                null,
                "Business Administration",
                "BBA Finance",
                LocalDate.of(2023, 9, 1),
                "INACTIVE"
        );
        createStudent(
                "STU-1003",
                "Mia Chen",
                "mia@example.com",
                "+1 555 0100",
                "Computer Science",
                "BSc Software Engineering",
                LocalDate.of(2025, 1, 15),
                "ACTIVE"
        );

        var byName = service.getStudents(
                "alice", null, null, 0, 20, null, null
        );
        assertEquals(1, byName.content().size());
        assertEquals("STU-1001", byName.content().get(0).studentId());

        var byStudentId = service.getStudents(
                "stu-1002", null, null, 0, 20, null, null
        );
        assertEquals(1, byStudentId.content().size());

        var byEmail = service.getStudents(
                "mia@example.com", null, null, 0, 20, null, null
        );
        assertEquals(1, byEmail.content().size());

        var byPhone = service.getStudents(
                "555 0199", null, null, 0, 20, null, null
        );
        assertEquals(1, byPhone.content().size());
        assertEquals("Alice Johnson", byPhone.content().get(0).fullName());

        var byDepartmentAndStatus = service.getStudents(
                null, "active", "computer science", 0, 20, null, null
        );
        assertEquals(2, byDepartmentAndStatus.content().size());

        var noMatch = service.getStudents(
                null, "INACTIVE", "Computer Science", 0, 20, null, null
        );
        assertTrue(noMatch.content().isEmpty());

        var sortedAscending = service.getStudents(
                null, null, null, 0, 20, "fullName", "asc"
        );
        assertEquals("Alice Johnson", sortedAscending.content().get(0).fullName());
        assertEquals("Zoe Adams", sortedAscending.content().get(2).fullName());

        var sortedDescending = service.getStudents(
                null, null, null, 0, 20, "FULLNAME", "DESC"
        );
        assertEquals("Zoe Adams", sortedDescending.content().get(0).fullName());

        var firstPage = service.getStudents(
                null, null, null, 0, 2, "studentId", "asc"
        );
        assertEquals(3, firstPage.totalElements());
        assertEquals(2, firstPage.totalPages());
        assertEquals(2, firstPage.content().size());
        assertTrue(firstPage.first());
        assertFalse(firstPage.last());

        var lastPage = service.getStudents(
                null, null, null, 1, 2, "studentId", "asc"
        );
        assertEquals(1, lastPage.content().size());
        assertFalse(lastPage.first());
        assertTrue(lastPage.last());
    }

    @Test
    void preservesNewestFirstOrderingWhenSortIsNotProvided() {
        StudentResponse older = createStudent(
                "STU-1101",
                "Older Student",
                "older@example.com",
                null,
                "Computer Science",
                "BSc Computer Science",
                LocalDate.of(2022, 9, 1),
                "ACTIVE"
        );
        StudentResponse newer = createStudent(
                "STU-1102",
                "Newer Student",
                "newer@example.com",
                null,
                "Computer Science",
                "BSc Computer Science",
                LocalDate.of(2026, 9, 1),
                "ACTIVE"
        );

        var page = service.getStudents(null, null, null, 0, 20, null, null);

        assertEquals(2, page.content().size());
        assertEquals(newer.id(), page.content().get(0).id());
        assertEquals(older.id(), page.content().get(1).id());
    }

    @Test
    void returnsAccurateStudentSummaryCounts() {
        createStudent(
                "STU-1201",
                "Active One",
                "active1201@example.com",
                null,
                "Computer Science",
                "BSc Computer Science",
                LocalDate.of(2024, 9, 1),
                "ACTIVE"
        );
        createStudent(
                "STU-1202",
                "Active Two",
                "active1202@example.com",
                null,
                "Computer Science",
                "BSc Software Engineering",
                LocalDate.of(2024, 9, 1),
                "ACTIVE"
        );
        createStudent(
                "STU-1203",
                "Active Three",
                "active1203@example.com",
                null,
                "Business Administration",
                "BBA Finance",
                LocalDate.of(2024, 9, 1),
                "ACTIVE"
        );
        createStudent(
                "STU-1204",
                "Inactive Student",
                "inactive1204@example.com",
                null,
                "Business Administration",
                "BBA Marketing",
                LocalDate.of(2024, 9, 1),
                "INACTIVE"
        );

        StudentSummaryResponse summary = service.getSummary();

        assertEquals(4, summary.totalStudents());
        assertEquals(3, summary.activeStudents());
        assertEquals(1, summary.inactiveStudents());
        assertEquals(2, summary.departmentCount());
    }

    @Test
    void rejectsInvalidPaginationStatusSortFieldAndSortDirection() {
        ResponseStatusException invalidPage = assertThrows(
                ResponseStatusException.class,
                () -> service.getStudents(null, null, null, -1, 20, null, null)
        );
        assertEquals(HttpStatus.BAD_REQUEST, invalidPage.getStatusCode());

        ResponseStatusException invalidSize = assertThrows(
                ResponseStatusException.class,
                () -> service.getStudents(null, null, null, 0, 101, null, null)
        );
        assertEquals(HttpStatus.BAD_REQUEST, invalidSize.getStatusCode());

        ResponseStatusException invalidStatus = assertThrows(
                ResponseStatusException.class,
                () -> service.getStudents(null, "GRADUATED", null, 0, 20, null, null)
        );
        assertEquals(HttpStatus.BAD_REQUEST, invalidStatus.getStatusCode());

        ResponseStatusException invalidSortField = assertThrows(
                ResponseStatusException.class,
                () -> service.getStudents(null, null, null, 0, 20, "password", null)
        );
        assertEquals(HttpStatus.BAD_REQUEST, invalidSortField.getStatusCode());

        ResponseStatusException invalidSortDirection = assertThrows(
                ResponseStatusException.class,
                () -> service.getStudents(null, null, null, 0, 20, "fullName", "sideways")
        );
        assertEquals(HttpStatus.BAD_REQUEST, invalidSortDirection.getStatusCode());
    }

    @Test
    void createsUpdatesAndDeletesStudent() {
        StudentResponse created = createStudent(
                "STU-1301",
                "Grace Hopper",
                "grace1301@example.com",
                "+1 555 0111",
                "Computer Science",
                "BSc Computer Science",
                LocalDate.of(2024, 9, 1),
                "Active"
        );

        assertNotNull(created.id());
        assertEquals("STU-1301", created.studentId());
        assertEquals("ACTIVE", created.status());

        StudentResponse updated = service.update(
                created.id(),
                new StudentRequest(
                        "STU-1301",
                        "Grace Hopper",
                        "grace1301@example.com",
                        " ",
                        "Engineering",
                        "BEng Electrical",
                        LocalDate.of(2024, 9, 2),
                        "Inactive"
                )
        );

        assertEquals(created.id(), updated.id());
        assertEquals("Engineering", updated.department());
        assertEquals("INACTIVE", updated.status());
        assertNull(updated.phone());

        assertEquals(created.id(), service.getById(created.id()).id());

        service.delete(created.id());
        assertTrue(repository.findById(created.id()).isPresent());
        assertNotNull(repository.findById(created.id()).orElseThrow().getDeletedAt());
        assertTrue(repository.findByIdAndDeletedAtIsNull(created.id()).isEmpty());
        assertTrue(service.getStudents(null, null, null, 0, 20, null, null).content().isEmpty());
    }

    @Test
    void rejectsDuplicateStudentIdEmailAndMissingRecords() {
        createStudent(
                "STU-1401",
                "Existing Student",
                "existing1401@example.com",
                null,
                "Computer Science",
                "BSc Computer Science",
                LocalDate.of(2024, 9, 1),
                "ACTIVE"
        );

        ResponseStatusException duplicateStudentId = assertThrows(
                ResponseStatusException.class,
                () -> createStudent(
                        "STU-1401",
                        "Another Student",
                        "another1401@example.com",
                        null,
                        "Computer Science",
                        "BSc Computer Science",
                        LocalDate.of(2024, 9, 1),
                        "ACTIVE"
                )
        );
        assertEquals(HttpStatus.CONFLICT, duplicateStudentId.getStatusCode());
        assertEquals(
                "Student ID already exists",
                ((DuplicateFieldException) duplicateStudentId).getErrors().get("studentId")
        );

        ResponseStatusException duplicateStudentIdCaseVariant = assertThrows(
                ResponseStatusException.class,
                () -> createStudent(
                        "stu-1401",
                        "Case Variant Student",
                        "case1401@example.com",
                        null,
                        "Computer Science",
                        "BSc Computer Science",
                        LocalDate.of(2024, 9, 1),
                        "ACTIVE"
                )
        );
        assertEquals(HttpStatus.CONFLICT, duplicateStudentIdCaseVariant.getStatusCode());

        ResponseStatusException duplicateEmail = assertThrows(
                ResponseStatusException.class,
                () -> createStudent(
                        "STU-1402",
                        "Another Student",
                        "EXISTING1401@example.com",
                        null,
                        "Computer Science",
                        "BSc Computer Science",
                        LocalDate.of(2024, 9, 1),
                        "ACTIVE"
                )
        );
        assertEquals(HttpStatus.CONFLICT, duplicateEmail.getStatusCode());
        assertEquals(
                "Email already exists",
                ((DuplicateFieldException) duplicateEmail).getErrors().get("email")
        );

        ResponseStatusException missingUpdate = assertThrows(
                ResponseStatusException.class,
                () -> service.update(
                        999999L,
                        new StudentRequest(
                                "STU-1403",
                                "Missing Student",
                                "missing1403@example.com",
                                null,
                                "Computer Science",
                                "BSc Computer Science",
                                LocalDate.of(2024, 9, 1),
                                "ACTIVE"
                        )
                )
        );
        assertEquals(HttpStatus.NOT_FOUND, missingUpdate.getStatusCode());

        ResponseStatusException missingDelete = assertThrows(
                ResponseStatusException.class,
                () -> service.delete(999999L)
        );
        assertEquals(HttpStatus.NOT_FOUND, missingDelete.getStatusCode());

        ResponseStatusException missingLookup = assertThrows(
                ResponseStatusException.class,
                () -> service.getById(999999L)
        );
        assertEquals(HttpStatus.NOT_FOUND, missingLookup.getStatusCode());
    }

    @Test
    void softDeletionHidesLiveDirectoryButPreservesIdentityAndBlocksReuse() {
        StudentResponse created = createStudent(
                "STU-1501",
                "Soft Deleted Student",
                "soft1501@example.com",
                null,
                "Computer Science",
                "BSc Computer Science",
                LocalDate.of(2024, 9, 1),
                "ACTIVE"
        );

        service.delete(created.id());

        assertTrue(service.getStudents(null, null, null, 0, 20, null, null)
                .content().isEmpty());

        var history = service.getStudents(
                null, null, null, 0, 20, null, null, true
        );
        assertEquals(1, history.content().size());
        assertEquals("STU-1501", history.content().get(0).studentId());

        Student stored = repository.findById(created.id()).orElseThrow();
        assertNotNull(stored.getDeletedAt());
        assertEquals("STU-1501", stored.getStudentId());
        assertEquals("soft1501@example.com", stored.getEmail());

        StudentSummaryResponse summary = service.getSummary();
        assertEquals(0, summary.totalStudents());
        assertEquals(0, summary.activeStudents());
        assertEquals(0, summary.inactiveStudents());
        assertEquals(0, summary.departmentCount());

        DuplicateFieldException idReuse = assertThrows(
                DuplicateFieldException.class,
                () -> createStudent(
                        "stu-1501",
                        "Reused ID Student",
                        "reuse1501@example.com",
                        null,
                        "Computer Science",
                        "BSc Computer Science",
                        LocalDate.of(2024, 9, 1),
                        "ACTIVE"
                )
        );
        assertEquals(HttpStatus.CONFLICT, idReuse.getStatusCode());
        assertTrue(idReuse.getErrors().containsKey("studentId"));

        DuplicateFieldException emailReuse = assertThrows(
                DuplicateFieldException.class,
                () -> createStudent(
                        "STU-1502",
                        "Reused Email Student",
                        "SOFT1501@example.com",
                        null,
                        "Computer Science",
                        "BSc Computer Science",
                        LocalDate.of(2024, 9, 1),
                        "ACTIVE"
                )
        );
        assertEquals(HttpStatus.CONFLICT, emailReuse.getStatusCode());
        assertTrue(emailReuse.getErrors().containsKey("email"));

        ResponseStatusException missingLookup = assertThrows(
                ResponseStatusException.class,
                () -> service.getById(created.id())
        );
        assertEquals(HttpStatus.NOT_FOUND, missingLookup.getStatusCode());
    }

    @Test
    void validatesRequiredStudentFields() {
        var violations = validator.validate(
                new StudentRequest(
                        " ",
                        " ",
                        "not-an-email",
                        null,
                        " ",
                        " ",
                        null,
                        " "
                )
        );

        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath().toString().equals("studentId")
        ));
        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath().toString().equals("fullName")
        ));
        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath().toString().equals("email")
        ));
        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath().toString().equals("department")
        ));
        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath().toString().equals("program")
        ));
        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath().toString().equals("enrollmentDate")
        ));
        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath().toString().equals("status")
        ));
    }

    private StudentResponse createStudent(
            String studentId,
            String fullName,
            String email,
            String phone,
            String department,
            String program,
            LocalDate enrollmentDate,
            String status) {

        return service.create(
                new StudentRequest(
                        studentId,
                        fullName,
                        email,
                        phone,
                        department,
                        program,
                        enrollmentDate,
                        status
                )
        );
    }
}
