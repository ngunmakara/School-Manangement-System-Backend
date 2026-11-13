package com.campushub.backend.attendance;

import com.campushub.backend.student.PageResponse;
import com.campushub.backend.student.Student;
import com.campushub.backend.student.StudentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class AttendanceService {

    private static final Set<String> ALLOWED_STATUSES =
            Set.of("PRESENT", "LATE", "ABSENT", "EXCUSED");

    private final AttendanceRepository repository;
    private final StudentRepository studentRepository;

    public AttendanceService(
            AttendanceRepository repository,
            StudentRepository studentRepository) {
        this.repository = repository;
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> getAttendance(
            String search,
            String status,
            LocalDate date,
            Long studentId,
            int page,
            int size) {

        validatePagination(page, size);

        String cleanSearch =
                search == null || search.isBlank() ? null : search.trim();

        String cleanStatus =
                status == null || status.isBlank()
                        ? null : status.trim().toUpperCase(Locale.ROOT);

        if (cleanStatus != null && !ALLOWED_STATUSES.contains(cleanStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be PRESENT, LATE, ABSENT, or EXCUSED"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("attendanceDate"),
                        Sort.Order.desc("id")
                )
        );

        Page<AttendanceResponse> result = repository
                .searchAttendance(cleanSearch, cleanStatus, date, studentId, pageable)
                .map(AttendanceResponse::from);

        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public AttendanceResponse getById(Long id) {
        return AttendanceResponse.from(findAttendance(id));
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getSummary(LocalDate date) {
        LocalDate summaryDate = date == null ? LocalDate.now() : date;
        List<Object[]> rows = repository.countByStatusForDate(summaryDate);

        long present = 0;
        long late = 0;
        long absent = 0;
        long excused = 0;

        for (Object[] row : rows) {
            String status = String.valueOf(row[0]).toUpperCase(Locale.ROOT);
            long count = ((Number) row[1]).longValue();

            switch (status) {
                case "PRESENT" -> present = count;
                case "LATE" -> late = count;
                case "ABSENT" -> absent = count;
                case "EXCUSED" -> excused = count;
                default -> { }
            }
        }

        long total = present + late + absent + excused;

        return new AttendanceSummaryResponse(
                summaryDate, total, present, late, absent, excused
        );
    }

    public AttendanceResponse create(AttendanceRequest request) {
        Student student = findStudent(request.studentId());
        String status = validateStatus(request.status());

        if (repository.existsByStudent_IdAndAttendanceDate(
                request.studentId(), request.attendanceDate())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Attendance is already recorded for this student on this date"
            );
        }

        Attendance attendance = new Attendance();
        applyRequest(attendance, request, student, status);

        return AttendanceResponse.from(repository.saveAndFlush(attendance));
    }

    public AttendanceResponse update(Long id, AttendanceRequest request) {
        Attendance attendance = findAttendance(id);
        Student student = findStudent(request.studentId());
        String status = validateStatus(request.status());

        if (repository.existsByStudent_IdAndAttendanceDateAndIdNot(
                request.studentId(), request.attendanceDate(), id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Attendance is already recorded for this student on this date"
            );
        }

        applyRequest(attendance, request, student, status);

        return AttendanceResponse.from(repository.saveAndFlush(attendance));
    }

    public void delete(Long id) {
        Attendance attendance = findAttendance(id);
        repository.delete(attendance);
        repository.flush();
    }

    private Attendance findAttendance(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Attendance record not found"
                ));
    }

    private Student findStudent(Long studentId) {
        return studentRepository.findById(studentId)
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

    private String validateStatus(String status) {
        String cleanStatus = status == null
                ? null : status.trim().toUpperCase(Locale.ROOT);

        if (cleanStatus == null || !ALLOWED_STATUSES.contains(cleanStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be PRESENT, LATE, ABSENT, or EXCUSED"
            );
        }

        return cleanStatus;
    }

    private void applyRequest(
            Attendance attendance,
            AttendanceRequest request,
            Student student,
            String status) {

        attendance.setStudent(student);
        attendance.setAttendanceDate(request.attendanceDate());
        attendance.setStatus(status);
        attendance.setNotes(cleanOptional(request.notes()));
    }

    private String cleanOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
