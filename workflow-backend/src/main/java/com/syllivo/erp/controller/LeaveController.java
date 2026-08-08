package com.syllivo.erp.controller;

import com.syllivo.erp.dto.CancelLeaveRequest;
import com.syllivo.erp.dto.LeaveResponse;
import com.syllivo.erp.dto.LeaveSummary;
import com.syllivo.erp.dto.LeaveWorkflowResult;
import com.syllivo.erp.dto.SubmitLeaveRequest;
import com.syllivo.erp.service.LeaveWorkflowService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

	private final LeaveWorkflowService leaveWorkflowService;

	public LeaveController(LeaveWorkflowService leaveWorkflowService) {
		this.leaveWorkflowService = leaveWorkflowService;
	}

	@GetMapping
	public List<LeaveSummary> getLeaves(@RequestParam(required = false) Long employeeId) {
		return leaveWorkflowService.getLeaves(employeeId);
	}

	@GetMapping("/{leaveId}")
	public LeaveSummary getLeave(@PathVariable Long leaveId) {
		return leaveWorkflowService.getLeave(leaveId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public LeaveResponse submitLeave(@RequestBody SubmitLeaveRequest request) {
		var result = leaveWorkflowService.submitLeave(
				request.employeeId(),
				request.leaveType(),
				request.fromDate(),
				request.toDate(),
				request.reason());
		return toResponse(result);
	}

	@PostMapping("/{leaveId}/cancel")
	public LeaveResponse cancelLeave(@PathVariable Long leaveId, @RequestBody CancelLeaveRequest request) {
		var result = leaveWorkflowService.cancelLeave(leaveId, request.employeeId(), request.reason());
		return toResponse(result);
	}

	private LeaveResponse toResponse(LeaveWorkflowResult result) {
		return new LeaveResponse(
				result.leaveId(),
				result.employeeId(),
				result.approverId(),
				result.status(),
				result.processInstanceId(),
				result.taskId());
	}
}
