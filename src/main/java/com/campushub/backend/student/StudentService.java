
package com.campushub.backend.student;

import com.campushub.backend.common.DuplicateFieldException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Map;

@Service
@Transactional
public class StudentService {

    private static final String DEFAULT_SORT_BY = "createdAt";
    private static final String DEFAULT_SORT_DIR = "desc";

    private static final Map<String, String> SORT_FIELDS = Map.ofEntries(
            Map.entry("studentid", "studentId"),
            Map.entry("fullname", "fullName"),
            Map.entry("email", "email"),
            Map.entry("department", "department"),
            Map.entry("program", "program"),
            Map.entry("enrollmentdate", "enrollmentDate"),
            Map.entry("status", "status"),
            Map.entry("createdat", "createdAt")
    );

    private final StudentRepository repository;

    public StudentService(StudentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> getStudents(
            String search,
            String status,
            String department,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        return getStudents(
                search, status, department, page, size, sortBy, sortDir, false
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> getStudents(
            String search,
            String status,
            String department,
            int page,
            int size,
            String sortBy,
            String sortDir,
            boolean includeDeleted) {

        validatePagination(page, size);

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

        String cleanDepartment =
                department == null || department.isBlank()
                        ? null : department.trim();

        String sortField = resolveSortField(sortBy);
        Sort.Direction direction = resolveSortDirection(sortDir);

        Sort sort = Sort.by(
                new Sort.Order(direction, sortField),
                new Sort.Order(direction, "id")
        );

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<StudentResponse> result = repository
                .searchStudentsForHistory(
                        cleanSearch,
                        cleanStatus,
                        cleanDepartment,
                        includeDeleted,
                        pageable
                )
                .map(StudentResponse::from);

        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public StudentSummaryResponse getSummary() {
        long totalStudents = repository.countLiveStudents();
        long activeStudents =
                repository.countByStatusIgnoreCase("ACTIVE");
        long inactiveStudents =
                repository.countByStatusIgnoreCase("INACTIVE");
        long departmentCount =
                repository.countStudentsByDepartment().size();

        return new StudentSummaryResponse(
                totalStudents,
                activeStudents,
                inactiveStudents,
                departmentCount
        );
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
        student.markDeleted();
        repository.saveAndFlush(student);
    }

    private Student findStudent(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student not found"
                ));
    }

    private void validatePagination(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be >= 0 and size must be between 1 and 100"
            );
        }
    }

    private String resolveSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return DEFAULT_SORT_BY;
        }

        String field = SORT_FIELDS.get(
                sortBy.trim().toLowerCase(Locale.ROOT)
        );

        if (field == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported sort field: " + sortBy.trim()
            );
        }

        return field;
    }

    private Sort.Direction resolveSortDirection(String sortDir) {
        if (sortDir == null || sortDir.isBlank()) {
            return Sort.Direction.fromString(DEFAULT_SORT_DIR);
        }

        String direction = sortDir.trim().toLowerCase(Locale.ROOT);

        if (!direction.equals("asc") && !direction.equals("desc")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sort direction must be asc or desc"
            );
        }

        return Sort.Direction.fromString(direction);
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
                repository.existsByStudentIdIgnoreCase(request.studentId().trim());

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
            throw new DuplicateFieldException(
                    "studentId", "Student ID already exists"
            );
        }

        if (duplicateEmail) {
            throw new DuplicateFieldException(
                    "email", "Email already exists"
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
