package com.syllivo.erp.repo;

import com.syllivo.erp.entity.LeaveRequest;
import com.syllivo.erp.enums.LeaveStatus;
import com.syllivo.erp.enums.LeaveType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

	List<LeaveRequest> findAllByOrderByCreatedAtDesc();

	List<LeaveRequest> findByEmployee_IdOrderByCreatedAtDesc(Long employeeId);

	List<LeaveRequest> findByEmployee_IdAndLeaveTypeAndStatus(Long employeeId, LeaveType leaveType, LeaveStatus status);

	@Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM LeaveRequest l "
			+ "WHERE l.employee.id = :employeeId AND l.status IN :statuses "
			+ "AND l.fromDate <= :toDate AND l.toDate >= :fromDate")
	boolean existsOverlapping(@Param("employeeId") Long employeeId, @Param("statuses") Collection<LeaveStatus> statuses,
			@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
}
