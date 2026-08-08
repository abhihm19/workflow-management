package com.syllivo.erp.dto;

import com.syllivo.erp.enums.LeaveStatus;
import com.syllivo.erp.enums.LeaveType;
import java.time.Instant;
import java.time.LocalDate;

public record LeaveSummary(
		Long leaveId,
		Long employeeId,
		String employeeName,
		Long approverId,
		String approverName,
		LeaveType leaveType,
		LocalDate fromDate,
		LocalDate toDate,
		String reason,
		LeaveStatus status,
		boolean cancellable,
		Instant createdAt,
		Instant updatedAt) {
}
