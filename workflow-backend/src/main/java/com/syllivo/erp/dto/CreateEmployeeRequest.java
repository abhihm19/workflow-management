package com.syllivo.erp.dto;

import java.time.LocalDate;

public record CreateEmployeeRequest(
		String name,
		String email,
		Long reportingManagerId,
		LocalDate dateOfJoining) {
}
