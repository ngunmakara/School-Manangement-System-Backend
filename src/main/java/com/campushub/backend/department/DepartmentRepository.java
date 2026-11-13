package com.campushub.backend.department;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCase(String code);

    @Query("""
        SELECT d FROM Department d
        WHERE (
            :search IS NULL
            OR LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(d.code) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(d.headOfDepartment) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        """)
    Page<Department> searchDepartments(
            @Param("search") String search,
            Pageable pageable
    );
}
