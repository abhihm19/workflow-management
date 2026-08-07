package com.syllivo.erp.dto;

public record CreateEmployeeRequest(
		String name,
		String email,
		Long reportingManagerId) {
}
