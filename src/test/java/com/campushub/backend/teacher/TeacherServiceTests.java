package com.campushub.backend.teacher;

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
class TeacherServiceTests {

    @Autowired
    private TeacherService service;

    @Autowired
    private TeacherRepository repository;

    @Autowired
    private Validator validator;

    @Test
    void createsUpdatesSearchesAndDeletesTeacher() {
        TeacherResponse created = service.create(
                new TeacherRequest(
                        "TCH-1001",
                        "Ada Lovelace",
                        "ada@example.com",
                        "+1 555 0100",
                        "Computer Science",
                        "Algorithms",
                        LocalDate.of(2024, 9, 1),
                        "Active"
                )
        );

        assertNotNull(created.id());
        assertEquals("TCH-1001", created.teacherId());
        assertEquals("ACTIVE", created.status());

        TeacherResponse updated = service.update(
                created.id(),
                new TeacherRequest(
                        "TCH-1001",
                        "Ada Lovelace",
                        "ada@example.com",
                        null,
                        "Computer Science",
                        "Data Structures",
                        LocalDate.of(2024, 9, 1),
                        "Inactive"
                )
        );

        assertEquals(created.id(), updated.id());
        assertEquals("INACTIVE", updated.status());
        assertEquals("Data Structures", updated.specialization());
        assertNull(updated.phone());

        var bySearch = service.getTeachers("lovelace", null, null, 0, 20);
        assertEquals(1, bySearch.content().size());

        var byDepartment = service.getTeachers(
                null, "INACTIVE", "computer science", 0, 20
        );
        assertEquals(1, byDepartment.content().size());

        var noMatch = service.getTeachers(
                null, "ACTIVE", "Computer Science", 0, 20
        );
        assertTrue(noMatch.content().isEmpty());

        service.delete(created.id());
        assertTrue(repository.findById(created.id()).isEmpty());
    }

    @Test
    void rejectsDuplicateEmailAndInvalidStatus() {
        service.create(
                new TeacherRequest(
                        "TCH-2001",
                        "Grace Hopper",
                        "grace@example.com",
                        null,
                        "Computer Science",
                        "Compilers",
                        LocalDate.of(2023, 1, 10),
                        "Active"
                )
        );

        ResponseStatusException duplicateEmail = assertThrows(
                ResponseStatusException.class,
                () -> service.create(
                        new TeacherRequest(
                                "TCH-2002",
                                "Another Teacher",
                                "GRACE@example.com",
                                null,
                                "Engineering",
                                "Systems",
                                LocalDate.of(2023, 2, 10),
                                "Active"
                        )
                )
        );
        assertEquals(HttpStatus.CONFLICT, duplicateEmail.getStatusCode());

        ResponseStatusException invalidStatus = assertThrows(
                ResponseStatusException.class,
                () -> service.create(
                        new TeacherRequest(
                                "TCH-2003",
                                "Invalid Status",
                                "invalid@example.com",
                                null,
                                "Engineering",
                                "Systems",
                                LocalDate.of(2023, 3, 10),
                                "ON_LEAVE"
                        )
                )
        );
        assertEquals(HttpStatus.BAD_REQUEST, invalidStatus.getStatusCode());
    }

    @Test
    void validatesRequiredTeacherFields() {
        var violations = validator.validate(
                new TeacherRequest(
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
                violation -> violation.getPropertyPath().toString().equals("teacherId")
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
                violation -> violation.getPropertyPath().toString().equals("specialization")
        ));
        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath().toString().equals("hireDate")
        ));
        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath().toString().equals("status")
        ));
    }
}
