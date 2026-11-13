package com.campushub.backend.department;

import com.campushub.backend.student.Student;
import com.campushub.backend.student.StudentRepository;
import com.campushub.backend.teacher.Teacher;
import com.campushub.backend.teacher.TeacherRepository;
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
class DepartmentServiceTests {

    @Autowired
    private DepartmentService service;

    @Autowired
    private DepartmentRepository repository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private Validator validator;

    @Test
    void createsUpdatesSearchesAndDeletesDepartment() {
        DepartmentResponse created = service.create(
                new DepartmentRequest("Computer Science", "cs", "Dr. Elena Park", "Computing")
        );

        assertNotNull(created.id());
        assertEquals("Computer Science", created.name());
        assertEquals("CS", created.code());
        assertEquals("Dr. Elena Park", created.headOfDepartment());

        DepartmentResponse updated = service.update(
                created.id(),
                new DepartmentRequest("Computer Science", "CS", "Dr. Ada Lovelace", " ")
        );

        assertEquals(created.id(), updated.id());
        assertEquals("Dr. Ada Lovelace", updated.headOfDepartment());
        assertNull(updated.description());

        assertEquals(1, service.getDepartments("computer", 0, 20).content().size());
        assertEquals(1, service.getDepartments("CS", 0, 20).content().size());

        service.delete(created.id());
        assertTrue(repository.findById(created.id()).isEmpty());
    }

    @Test
    void rejectsDuplicateNameAndCodeCaseInsensitively() {
        service.create(
                new DepartmentRequest("Health Sciences", "HS", null, null)
        );

        ResponseStatusException duplicateName = assertThrows(
                ResponseStatusException.class,
                () -> service.create(
                        new DepartmentRequest("health sciences", "HLT", null, null)
                )
        );
        assertEquals(HttpStatus.CONFLICT, duplicateName.getStatusCode());

        ResponseStatusException duplicateCode = assertThrows(
                ResponseStatusException.class,
                () -> service.create(
                        new DepartmentRequest("Nursing", "hs", null, null)
                )
        );
        assertEquals(HttpStatus.CONFLICT, duplicateCode.getStatusCode());
    }

    @Test
    void blocksDeletionWhileStudentsOrTeachersReferenceDepartment() {
        DepartmentResponse department = service.create(
                new DepartmentRequest("Engineering", "ENG", null, null)
        );

        Student student = new Student();
        student.setStudentId("STU-5001");
        student.setFullName("Test Student");
        student.setEmail("student5001@example.com");
        student.setDepartment("Engineering");
        student.setProgram("BEng Civil");
        student.setEnrollmentDate(LocalDate.now());
        student.setStatus("ACTIVE");
        studentRepository.saveAndFlush(student);

        ResponseStatusException studentConflict = assertThrows(
                ResponseStatusException.class,
                () -> service.delete(department.id())
        );
        assertEquals(HttpStatus.CONFLICT, studentConflict.getStatusCode());

        studentRepository.delete(student);
        studentRepository.flush();

        Teacher teacher = new Teacher();
        teacher.setTeacherId("TCH-5001");
        teacher.setFullName("Test Teacher");
        teacher.setEmail("teacher5001@example.com");
        teacher.setDepartment("Engineering");
        teacher.setSpecialization("Civil Engineering");
        teacher.setHireDate(LocalDate.now());
        teacher.setStatus("ACTIVE");
        teacherRepository.saveAndFlush(teacher);

        ResponseStatusException teacherConflict = assertThrows(
                ResponseStatusException.class,
                () -> service.delete(department.id())
        );
        assertEquals(HttpStatus.CONFLICT, teacherConflict.getStatusCode());

        teacherRepository.delete(teacher);
        teacherRepository.flush();

        service.delete(department.id());
        assertTrue(repository.findById(department.id()).isEmpty());
    }

    @Test
    void validatesRequiredDepartmentFields() {
        var violations = validator.validate(
                new DepartmentRequest(" ", " ", null, null)
        );

        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath().toString().equals("name")
        ));
        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath().toString().equals("code")
        ));
    }
}
