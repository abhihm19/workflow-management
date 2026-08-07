package com.syllivo.erp.controller;

import com.syllivo.erp.dto.ApprovalResponse;
import com.syllivo.erp.dto.ApprovalWorkflowResult;
import com.syllivo.erp.dto.CompleteApprovalRequest;
import com.syllivo.erp.service.LeaveWorkflowService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

	private final LeaveWorkflowService leaveWorkflowService;

	public ApprovalController(LeaveWorkflowService leaveWorkflowService) {
		this.leaveWorkflowService = leaveWorkflowService;
	}

	@GetMapping("/pending/{approverId}")
	public List<ApprovalResponse> getPendingApprovals(@PathVariable Long approverId) {
		return leaveWorkflowService.getPendingApprovals(approverId).stream()
				.map(this::toResponse)
				.toList();
	}

	@PostMapping("/{approvalId}/complete")
	public ApprovalResponse completeApproval(@PathVariable Long approvalId,
			@RequestBody CompleteApprovalRequest request) {
		return toResponse(leaveWorkflowService.completeApproval(
				approvalId,
				request.approverId(),
				request.approved(),
				request.comments()));
	}

	private ApprovalResponse toResponse(ApprovalWorkflowResult result) {
		return new ApprovalResponse(
				result.approvalId(),
				result.objectType(),
				result.objectId(),
				result.employeeId(),
				result.approverId(),
				result.approvalLevel(),
				result.status(),
				result.processInstanceId(),
				result.taskId(),
				result.comments());
	}
}
