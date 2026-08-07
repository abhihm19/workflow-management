package com.syllivo.erp.dto;

import java.time.LocalDate;

public record SubmitLeaveRequest(
		Long employeeId,
		LocalDate fromDate,
		LocalDate toDate,
		String reason) {
}
