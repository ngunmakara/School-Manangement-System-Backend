package com.campushub.backend.attendance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    boolean existsByStudent_IdAndAttendanceDate(Long studentId, LocalDate attendanceDate);

    boolean existsByStudent_IdAndAttendanceDateAndIdNot(
            Long studentId, LocalDate attendanceDate, Long id);

    @Query("""
        SELECT a FROM Attendance a
        JOIN a.student s
        WHERE (
            :search IS NULL
            OR LOWER(s.studentId) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        AND (:status IS NULL OR a.status = :status)
        AND (:date IS NULL OR a.attendanceDate = :date)
        AND (:studentId IS NULL OR s.id = :studentId)
        """)
    Page<Attendance> searchAttendance(
            @Param("search") String search,
            @Param("status") String status,
            @Param("date") LocalDate date,
            @Param("studentId") Long studentId,
            Pageable pageable
    );

    @Query("""
        SELECT a.status, COUNT(a)
        FROM Attendance a
        WHERE a.attendanceDate = :date
        GROUP BY a.status
        """)
    List<Object[]> countByStatusForDate(@Param("date") LocalDate date);
}
