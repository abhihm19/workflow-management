package com.syllivo.erp.controller;

import com.syllivo.erp.dto.LeaveResponse;
import com.syllivo.erp.dto.SubmitLeaveRequest;
import com.syllivo.erp.service.LeaveWorkflowService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

	private final LeaveWorkflowService leaveWorkflowService;

	public LeaveController(LeaveWorkflowService leaveWorkflowService) {
		this.leaveWorkflowService = leaveWorkflowService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public LeaveResponse submitLeave(@RequestBody SubmitLeaveRequest request) {
		var result = leaveWorkflowService.submitLeave(
				request.employeeId(),
				request.fromDate(),
				request.toDate(),
				request.reason());
		return new LeaveResponse(
				result.leaveId(),
				result.employeeId(),
				result.approverId(),
				result.status(),
				result.processInstanceId(),
				result.taskId());
	}
}
