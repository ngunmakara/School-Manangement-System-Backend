package com.campushub.backend.department;

import com.campushub.backend.student.PageResponse;
import com.campushub.backend.student.StudentRepository;
import com.campushub.backend.teacher.TeacherRepository;
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
public class DepartmentService {

    private final DepartmentRepository repository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    public DepartmentService(
            DepartmentRepository repository,
            StudentRepository studentRepository,
            TeacherRepository teacherRepository) {
        this.repository = repository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> getDepartments(
            String search, int page, int size) {

        validatePagination(page, size);

        String cleanSearch =
                search == null || search.isBlank() ? null : search.trim();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"))
        );

        Page<DepartmentResponse> result = repository
                .searchDepartments(cleanSearch, pageable)
                .map(DepartmentResponse::from);

        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getById(Long id) {
        return DepartmentResponse.from(findDepartment(id));
    }

    public DepartmentResponse create(DepartmentRequest request) {
        validateUnique(request, null);

        Department department = new Department();
        applyRequest(department, request);

        return DepartmentResponse.from(repository.saveAndFlush(department));
    }

    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = findDepartment(id);

        validateUnique(request, id);
        applyRequest(department, request);

        return DepartmentResponse.from(repository.saveAndFlush(department));
    }

    public void delete(Long id) {
        Department department = findDepartment(id);

        long studentCount =
                studentRepository.countByDepartmentIgnoreCase(department.getName());
        long teacherCount =
                teacherRepository.countByDepartmentIgnoreCase(department.getName());

        if (studentCount > 0 || teacherCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Department cannot be deleted because it is assigned to "
                            + studentCount + " student(s) and "
                            + teacherCount + " teacher(s)"
            );
        }

        repository.delete(department);
        repository.flush();
    }

    private Department findDepartment(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Department not found"
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

    private void validateUnique(DepartmentRequest request, Long currentId) {
        boolean duplicateName =
                repository.existsByNameIgnoreCase(request.name().trim());
        boolean duplicateCode =
                repository.existsByCodeIgnoreCase(request.code().trim());

        if (currentId != null) {
            Department current = findDepartment(currentId);

            if (current.getName().equalsIgnoreCase(request.name().trim())) {
                duplicateName = false;
            }

            if (current.getCode().equalsIgnoreCase(request.code().trim())) {
                duplicateCode = false;
            }
        }

        if (duplicateName) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Department name already exists"
            );
        }

        if (duplicateCode) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Department code already exists"
            );
        }
    }

    private void applyRequest(Department department, DepartmentRequest request) {
        department.setName(request.name().trim());
        department.setCode(request.code().trim().toUpperCase(Locale.ROOT));
        department.setHeadOfDepartment(cleanOptional(request.headOfDepartment()));
        department.setDescription(cleanOptional(request.description()));
    }

    private String cleanOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
