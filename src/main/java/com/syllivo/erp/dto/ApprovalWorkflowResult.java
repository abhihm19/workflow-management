package com.syllivo.erp.dto;

import com.syllivo.erp.enums.ApprovalObjectType;
import com.syllivo.erp.enums.ApprovalStatus;

public record ApprovalWorkflowResult(
		Long approvalId,
		ApprovalObjectType objectType,
		Long objectId,
		Long employeeId,
		Long approverId,
		String approvalLevel,
		ApprovalStatus status,
		String processInstanceId,
		String taskId,
		String comments) {
}
