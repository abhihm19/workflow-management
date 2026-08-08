package com.syllivo.erp.dto;

import com.syllivo.erp.enums.LeaveStatus;

public record LeaveWorkflowResult(
		Long leaveId,
		Long employeeId,
		Long approverId,
		LeaveStatus status,
		String processInstanceId,
		String taskId) {
}
