package com.campushub.backend.teacher;

import com.campushub.backend.student.PageResponse;
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
public class TeacherService {

    private final TeacherRepository repository;

    public TeacherService(TeacherRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageResponse<TeacherResponse> getTeachers(
            String search, String status, String department, int page, int size) {

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

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );

        Page<TeacherResponse> result = repository
                .searchTeachers(cleanSearch, cleanStatus, cleanDepartment, pageable)
                .map(TeacherResponse::from);

        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public TeacherResponse getById(Long id) {
        return TeacherResponse.from(findTeacher(id));
    }

    public TeacherResponse create(TeacherRequest request) {
        validateStatus(request.status());
        validateUnique(request, null);

        Teacher teacher = new Teacher();
        applyRequest(teacher, request);

        return TeacherResponse.from(repository.saveAndFlush(teacher));
    }

    public TeacherResponse update(Long id, TeacherRequest request) {
        Teacher teacher = findTeacher(id);

        validateStatus(request.status());
        validateUnique(request, id);
        applyRequest(teacher, request);

        return TeacherResponse.from(repository.saveAndFlush(teacher));
    }

    public void delete(Long id) {
        Teacher teacher = findTeacher(id);
        repository.delete(teacher);
        repository.flush();
    }

    private Teacher findTeacher(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Teacher not found"
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

    private void validateUnique(TeacherRequest request, Long currentId) {
        boolean duplicateTeacherId =
                repository.existsByTeacherId(request.teacherId().trim());
        boolean duplicateEmail =
                repository.existsByEmailIgnoreCase(request.email().trim());

        if (currentId != null) {
            Teacher current = findTeacher(currentId);

            if (current.getTeacherId().equalsIgnoreCase(
                    request.teacherId().trim())) {
                duplicateTeacherId = false;
            }

            if (current.getEmail().equalsIgnoreCase(
                    request.email().trim())) {
                duplicateEmail = false;
            }
        }

        if (duplicateTeacherId) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Teacher ID already exists"
            );
        }

        if (duplicateEmail) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Email already exists"
            );
        }
    }

    private void applyRequest(Teacher teacher, TeacherRequest request) {
        teacher.setTeacherId(request.teacherId().trim());
        teacher.setFullName(request.fullName().trim());
        teacher.setEmail(request.email().trim());
        teacher.setPhone(cleanOptional(request.phone()));
        teacher.setDepartment(request.department().trim());
        teacher.setSpecialization(request.specialization().trim());
        teacher.setHireDate(request.hireDate());
        teacher.setStatus(
                request.status().trim().toUpperCase(Locale.ROOT)
        );
    }

    private String cleanOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
