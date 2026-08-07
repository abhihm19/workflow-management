package com.syllivo.erp.dto;

public record EmployeeResponse(
		Long id,
		String name,
		String email,
		Long reportingManagerId) {
}
