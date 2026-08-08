package com.syllivo.erp.dto;

public record CompleteApprovalRequest(
		Long approverId,
		boolean approved,
		String comments) {
}
