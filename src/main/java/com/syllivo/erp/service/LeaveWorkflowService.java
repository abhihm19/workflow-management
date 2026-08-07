package com.syllivo.erp.service;

import com.syllivo.erp.dto.ApprovalWorkflowResult;
import com.syllivo.erp.dto.LeaveWorkflowResult;
import com.syllivo.erp.entity.ApprovalRequest;
import com.syllivo.erp.entity.Employee;
import com.syllivo.erp.entity.LeaveRequest;
import com.syllivo.erp.enums.ApprovalObjectType;
import com.syllivo.erp.enums.ApprovalStatus;
import com.syllivo.erp.enums.LeaveStatus;
import com.syllivo.erp.repo.ApprovalRequestRepository;
import com.syllivo.erp.repo.EmployeeRepository;
import com.syllivo.erp.repo.LeaveRequestRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LeaveWorkflowService {

	private static final String PROCESS_KEY = "leaveApproval";

	private final EmployeeRepository employeeRepository;
	private final LeaveRequestRepository leaveRequestRepository;
	private final ApprovalRequestRepository approvalRequestRepository;
	private final RuntimeService runtimeService;
	private final TaskService taskService;

	public LeaveWorkflowService(EmployeeRepository employeeRepository, LeaveRequestRepository leaveRequestRepository,
			ApprovalRequestRepository approvalRequestRepository, RuntimeService runtimeService, TaskService taskService) {
		this.employeeRepository = employeeRepository;
		this.leaveRequestRepository = leaveRequestRepository;
		this.approvalRequestRepository = approvalRequestRepository;
		this.runtimeService = runtimeService;
		this.taskService = taskService;
	}

	@Transactional
	public LeaveWorkflowResult submitLeave(Long employeeId, LocalDate fromDate, LocalDate toDate, String reason) {
		if (employeeId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee is required");
		}
		if (fromDate == null || toDate == null || toDate.isBefore(fromDate)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leave dates are invalid");
		}

		Employee employee = employeeRepository.findById(employeeId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
		Employee reportingManager = employee.getReportingManager();
		if (reportingManager == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee does not have a reporting manager");
		}

		Instant now = Instant.now();
		LeaveRequest leaveRequest = new LeaveRequest();
		leaveRequest.setEmployee(employee);
		leaveRequest.setFromDate(fromDate);
		leaveRequest.setToDate(toDate);
		leaveRequest.setReason(reason);
		leaveRequest.setStatus(LeaveStatus.SUBMITTED);
		leaveRequest.setCreatedAt(now);
		leaveRequest.setUpdatedAt(now);
		leaveRequest = leaveRequestRepository.save(leaveRequest);

		ApprovalRequest approvalRequest = new ApprovalRequest();
		approvalRequest.setObjectType(ApprovalObjectType.LEAVE);
		approvalRequest.setObjectId(leaveRequest.getId());
		approvalRequest.setEmployee(employee);
		approvalRequest.setApprover(reportingManager);
		approvalRequest.setApprovalLevel("RM");
		approvalRequest.setStatus(ApprovalStatus.PENDING);
		approvalRequest.setCreatedAt(now);
		approvalRequest.setUpdatedAt(now);
		approvalRequest = approvalRequestRepository.save(approvalRequest);

		ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY,
				"LEAVE-" + leaveRequest.getId(),
				Map.of(
						"objectType", ApprovalObjectType.LEAVE.name(),
						"objectId", leaveRequest.getId(),
						"leaveId", leaveRequest.getId(),
						"approvalId", approvalRequest.getId(),
						"employeeId", employee.getId(),
						"approverId", String.valueOf(reportingManager.getId())));

		Task task = taskService.createTaskQuery()
				.processInstanceId(processInstance.getId())
				.singleResult();
		if (task == null) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "RM approval task was not created");
		}

		leaveRequest.attachProcess(processInstance.getId());
		approvalRequest.attachWorkflow(processInstance.getId(), task.getId());

		return new LeaveWorkflowResult(leaveRequest.getId(), employee.getId(), reportingManager.getId(),
				leaveRequest.getStatus(), processInstance.getId(), task.getId());
	}

	@Transactional(readOnly = true)
	public List<ApprovalWorkflowResult> getPendingApprovals(Long approverId) {
		return approvalRequestRepository.findByApprover_IdAndStatus(approverId, ApprovalStatus.PENDING).stream()
				.map(this::toApprovalResponse)
				.toList();
	}

	@Transactional
	public ApprovalWorkflowResult completeApproval(Long approvalId, Long approverId, boolean approved, String comments) {
		if (approvalId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval is required");
		}

		ApprovalRequest approvalRequest = approvalRequestRepository.findById(approvalId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval not found"));

		if (!approvalRequest.getApprover().getId().equals(approverId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Task is not assigned to this approver");
		}

		String taskId = approvalRequest.getFlowableTaskId();
		Task task = taskService.createTaskQuery()
				.taskId(taskId)
				.taskAssignee(String.valueOf(approverId))
				.singleResult();
		if (task == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Flowable task not found");
		}

		taskService.complete(taskId, Map.of("approved", approved));

		if (approved) {
			approvalRequest.approve(comments);
			updateLeaveStatus(approvalRequest, true);
		}
		else {
			approvalRequest.reject(comments);
			updateLeaveStatus(approvalRequest, false);
		}

		return toApprovalResponse(approvalRequest);
	}

	private void updateLeaveStatus(ApprovalRequest approvalRequest, boolean approved) {
		if (approvalRequest.getObjectType() != ApprovalObjectType.LEAVE) {
			return;
		}

		LeaveRequest leaveRequest = leaveRequestRepository.findById(approvalRequest.getObjectId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));
		if (approved) {
			leaveRequest.approve();
		}
		else {
			leaveRequest.reject();
		}
	}

	private ApprovalWorkflowResult toApprovalResponse(ApprovalRequest approvalRequest) {
		return new ApprovalWorkflowResult(
				approvalRequest.getId(),
				approvalRequest.getObjectType(),
				approvalRequest.getObjectId(),
				approvalRequest.getEmployee().getId(),
				approvalRequest.getApprover().getId(),
				approvalRequest.getApprovalLevel(),
				approvalRequest.getStatus(),
				approvalRequest.getProcessInstanceId(),
				approvalRequest.getFlowableTaskId(),
				approvalRequest.getComments());
	}
}
