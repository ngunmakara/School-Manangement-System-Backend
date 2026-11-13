
package com.campushub.backend.student;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
@Transactional
public class StudentService {

    private final StudentRepository repository;

    public StudentService(StudentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> getStudents(
            String search, String status, int page, int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be >= 0 and size must be between 1 and 100"
            );
        }

        String cleanSearch =
                search == null || search.isBlank() ? null : search.trim();

        String cleanStatus =
                status == null || status.isBlank()
                        ? null : status.trim().toUpperCase(Locale.ROOT);

        if (cleanStatus != null
                && !cleanStatus.equals("ACTIVE")
                && !cleanStatus.equals("INACTIVE")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be ACTIVE or INACTIVE"
            );
        }

        Pageable pageable = PageRequest.of(
                page, size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<StudentResponse> result = repository
                .searchStudents(cleanSearch, cleanStatus, pageable)
                .map(StudentResponse::from);

        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public StudentResponse getById(Long id) {
        return StudentResponse.from(findStudent(id));
    }

    public StudentResponse create(StudentRequest request) {
        validateStatus(request.status());
        validateUnique(request, null);

        Student student = new Student();
        applyRequest(student, request);

        return StudentResponse.from(repository.saveAndFlush(student));
    }

    public StudentResponse update(Long id, StudentRequest request) {
        Student student = findStudent(id);

        validateStatus(request.status());
        validateUnique(request, id);
        applyRequest(student, request);

        return StudentResponse.from(repository.saveAndFlush(student));
    }

    public void delete(Long id) {
        Student student = findStudent(id);
        repository.delete(student);
        repository.flush();
    }

    private Student findStudent(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student not found"
                ));
    }

    private void validateStatus(String status) {
        if (status == null
                || (!status.equalsIgnoreCase("ACTIVE")
                && !status.equalsIgnoreCase("INACTIVE"))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be ACTIVE or INACTIVE"
            );
        }
    }

    private void validateUnique(StudentRequest request, Long currentId) {
        boolean duplicateStudentId =
                repository.existsByStudentId(request.studentId().trim());

        boolean duplicateEmail =
                repository.existsByEmailIgnoreCase(request.email().trim());

        if (currentId != null) {
            Student current = findStudent(currentId);

            if (current.getStudentId().equalsIgnoreCase(
                    request.studentId().trim())) {
                duplicateStudentId = false;
            }

            if (current.getEmail().equalsIgnoreCase(
                    request.email().trim())) {
                duplicateEmail = false;
            }
        }

        if (duplicateStudentId) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Student ID already exists"
            );
        }

        if (duplicateEmail) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Email already exists"
            );
        }
    }

    private void applyRequest(Student student, StudentRequest request) {
        student.setStudentId(request.studentId().trim());
        student.setFullName(request.fullName().trim());
        student.setEmail(request.email().trim());
        student.setPhone(
                request.phone() == null || request.phone().isBlank()
                        ? null : request.phone().trim()
        );
        student.setDepartment(request.department().trim());
        student.setProgram(request.program().trim());
        student.setEnrollmentDate(request.enrollmentDate());
        student.setStatus(
                request.status().trim().toUpperCase(Locale.ROOT)
        );
    }
}
