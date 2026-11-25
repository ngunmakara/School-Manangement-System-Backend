
package com.campushub.backend.student;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByStudentId(String studentId);

    boolean existsByEmailIgnoreCase(String email);

    long countByStatusIgnoreCase(String status);

    long countByDepartmentIgnoreCase(String department);

    @Query("""
        SELECT s FROM Student s
        WHERE (
            :search IS NULL
            OR LOWER(s.studentId) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(s.phone) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        AND (:status IS NULL OR s.status = :status)
        AND (:department IS NULL OR LOWER(s.department) = LOWER(:department))
        """)
    Page<Student> searchStudents(
            @Param("search") String search,
            @Param("status") String status,
            @Param("department") String department,
            Pageable pageable
    );

    @Query("""
        SELECT s.department, COUNT(s)
        FROM Student s
        GROUP BY s.department
        ORDER BY s.department ASC
        """)
    List<Object[]> countStudentsByDepartment();

    @Query("""
        SELECT YEAR(s.enrollmentDate), MONTH(s.enrollmentDate), COUNT(s)
        FROM Student s
        WHERE s.enrollmentDate >= :startDate
          AND s.enrollmentDate < :endDate
        GROUP BY YEAR(s.enrollmentDate), MONTH(s.enrollmentDate)
        ORDER BY YEAR(s.enrollmentDate), MONTH(s.enrollmentDate)
        """)
    List<Object[]> countEnrollmentsByMonth(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
