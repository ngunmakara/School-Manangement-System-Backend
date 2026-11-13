package com.campushub.backend.teacher;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    boolean existsByTeacherId(String teacherId);

    boolean existsByEmailIgnoreCase(String email);

    long countByStatusIgnoreCase(String status);

    long countByDepartmentIgnoreCase(String department);

    @Query("""
        SELECT t FROM Teacher t
        WHERE (
            :search IS NULL
            OR LOWER(t.teacherId) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(t.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(t.email) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(t.specialization) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        AND (:status IS NULL OR t.status = :status)
        AND (:department IS NULL OR LOWER(t.department) = LOWER(:department))
        """)
    Page<Teacher> searchTeachers(
            @Param("search") String search,
            @Param("status") String status,
            @Param("department") String department,
            Pageable pageable
    );
}
