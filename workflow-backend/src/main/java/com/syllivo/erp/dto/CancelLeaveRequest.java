package com.syllivo.erp.dto;

public record CancelLeaveRequest(
		Long employeeId,
		String reason) {
}
