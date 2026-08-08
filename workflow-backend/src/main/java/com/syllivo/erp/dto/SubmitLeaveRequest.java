package com.syllivo.erp.dto;

import com.syllivo.erp.enums.LeaveType;
import java.time.LocalDate;

public record SubmitLeaveRequest(
		Long employeeId,
		LeaveType leaveType,
		LocalDate fromDate,
		LocalDate toDate,
		String reason) {
}
