package com.syllivo.erp.service;

import com.syllivo.erp.dto.ApprovalWorkflowResult;
import com.syllivo.erp.dto.LeaveSummary;
import com.syllivo.erp.dto.LeaveWorkflowResult;
import com.syllivo.erp.entity.ApprovalRequest;
import com.syllivo.erp.entity.Employee;
import com.syllivo.erp.entity.LeaveRequest;
import com.syllivo.erp.entity.LeaveTypeCap;
import com.syllivo.erp.enums.ApprovalObjectType;
import com.syllivo.erp.enums.ApprovalStatus;
import com.syllivo.erp.enums.LeaveCapUnit;
import com.syllivo.erp.enums.LeaveStatus;
import com.syllivo.erp.enums.LeaveTransactionReason;
import com.syllivo.erp.enums.LeaveType;
import com.syllivo.erp.repo.ApprovalRequestRepository;
import com.syllivo.erp.repo.EmployeeRepository;
import com.syllivo.erp.repo.LeaveRequestRepository;
import com.syllivo.erp.repo.LeaveTypeCapRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	private static final String CANCELLATION_PROCESS_KEY = "leaveCancellation";
	private static final Set<LeaveStatus> CANCELLABLE_STATUSES = EnumSet.of(LeaveStatus.SUBMITTED, LeaveStatus.APPROVED);
	private static final Set<LeaveStatus> OVERLAP_BLOCKING_STATUSES = EnumSet.of(LeaveStatus.SUBMITTED,
			LeaveStatus.APPROVED, LeaveStatus.CANCELLATION_PENDING);

	private final EmployeeRepository employeeRepository;
	private final LeaveRequestRepository leaveRequestRepository;
	private final ApprovalRequestRepository approvalRequestRepository;
	private final LeaveTypeCapRepository leaveTypeCapRepository;
	private final LeaveAccountService leaveAccountService;
	private final RuntimeService runtimeService;
	private final TaskService taskService;

	public LeaveWorkflowService(EmployeeRepository employeeRepository, LeaveRequestRepository leaveRequestRepository,
			ApprovalRequestRepository approvalRequestRepository, LeaveTypeCapRepository leaveTypeCapRepository,
			LeaveAccountService leaveAccountService, RuntimeService runtimeService, TaskService taskService) {
		this.employeeRepository = employeeRepository;
		this.leaveRequestRepository = leaveRequestRepository;
		this.approvalRequestRepository = approvalRequestRepository;
		this.leaveTypeCapRepository = leaveTypeCapRepository;
		this.leaveAccountService = leaveAccountService;
		this.runtimeService = runtimeService;
		this.taskService = taskService;
	}

	private boolean isAccrualEnabled(LeaveType leaveType) {
		return leaveTypeCapRepository.findByLookupValue_Code(leaveType.name())
				.map(LeaveTypeCap::isAccrualEnabled)
				.orElse(false);
	}

	private long requestedDays(LeaveRequest leaveRequest) {
		return ChronoUnit.DAYS.between(leaveRequest.getFromDate(), leaveRequest.getToDate()) + 1;
	}

	@Transactional
	public LeaveWorkflowResult submitLeave(Long employeeId, LeaveType leaveType, LocalDate fromDate, LocalDate toDate,
			String reason) {
		if (employeeId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee is required");
		}
		if (leaveType == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leave type is required");
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

		if (leaveRequestRepository.existsOverlapping(employeeId, OVERLAP_BLOCKING_STATUSES, fromDate, toDate)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"You already have a leave request that overlaps with the selected dates");
		}

		long requestedDays = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
		LeaveTypeCap leaveTypeCap = leaveTypeCapRepository.findByLookupValue_Code(leaveType.name()).orElse(null);
		if (leaveTypeCap != null && leaveTypeCap.isAccrualEnabled()) {
			int ledgerBalance = leaveAccountService.getAvailableBalance(employeeId, leaveType);
			int pendingDays = leaveRequestRepository
					.findByEmployee_IdAndLeaveTypeAndStatus(employeeId, leaveType, LeaveStatus.SUBMITTED)
					.stream()
					.mapToInt(leave -> (int) ChronoUnit.DAYS.between(leave.getFromDate(), leave.getToDate()) + 1)
					.sum();
			int available = Math.max(0, ledgerBalance - pendingDays);
			if (requestedDays > available) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Insufficient " + leaveType + " balance: available " + available + " (ledger " + ledgerBalance
								+ ", pending " + pendingDays + "), requested " + requestedDays);
			}
		}
		else if (leaveTypeCap != null && leaveTypeCap.getCapValue() != null) {
			BigDecimal capValue = leaveTypeCap.getCapValue();
			int capDays = leaveTypeCap.getCapUnit() == LeaveCapUnit.WEEKS
					? capValue.multiply(BigDecimal.valueOf(7)).setScale(0, RoundingMode.HALF_UP).intValue()
					: capValue.setScale(0, RoundingMode.HALF_UP).intValue();
			if (requestedDays > capDays) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						leaveType + " entitlement is " + capDays + " day(s); requested " + requestedDays);
			}
		}

		Instant now = Instant.now();
		LeaveRequest leaveRequest = new LeaveRequest();
		leaveRequest.setEmployee(employee);
		leaveRequest.setLeaveType(leaveType);
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

	@Transactional
	public LeaveWorkflowResult cancelLeave(Long leaveId, Long employeeId, String reason) {
		if (leaveId == null || employeeId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leave and employee are required");
		}

		LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));
		if (!leaveRequest.getEmployee().getId().equals(employeeId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Leave does not belong to this employee");
		}

		Employee employee = leaveRequest.getEmployee();
		Employee reportingManager = employee.getReportingManager();

		return switch (leaveRequest.getStatus()) {
			case SUBMITTED -> withdrawPendingLeave(leaveRequest, reason);
			case APPROVED -> requestCancellationApproval(leaveRequest, employee, reportingManager, reason);
			default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Leave in status " + leaveRequest.getStatus() + " cannot be cancelled");
		};
	}

	private LeaveWorkflowResult withdrawPendingLeave(LeaveRequest leaveRequest, String reason) {
		ApprovalRequest pendingApproval = approvalRequestRepository
				.findByObjectTypeAndObjectIdAndStatus(ApprovalObjectType.LEAVE, leaveRequest.getId(), ApprovalStatus.PENDING)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
						"Pending approval for leave not found"));

		if (pendingApproval.getProcessInstanceId() != null) {
			runtimeService.deleteProcessInstance(pendingApproval.getProcessInstanceId(), "Withdrawn by employee");
		}
		pendingApproval.cancel(reason);
		leaveRequest.cancel();

		return new LeaveWorkflowResult(leaveRequest.getId(), leaveRequest.getEmployee().getId(),
				pendingApproval.getApprover().getId(), leaveRequest.getStatus(), null, null);
	}

	private LeaveWorkflowResult requestCancellationApproval(LeaveRequest leaveRequest, Employee employee,
			Employee reportingManager, String reason) {
		if (reportingManager == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee does not have a reporting manager");
		}

		Instant now = Instant.now();
		ApprovalRequest cancellationApproval = new ApprovalRequest();
		cancellationApproval.setObjectType(ApprovalObjectType.LEAVE_CANCELLATION);
		cancellationApproval.setObjectId(leaveRequest.getId());
		cancellationApproval.setEmployee(employee);
		cancellationApproval.setApprover(reportingManager);
		cancellationApproval.setApprovalLevel("RM");
		cancellationApproval.setStatus(ApprovalStatus.PENDING);
		cancellationApproval.setComments(reason);
		cancellationApproval.setCreatedAt(now);
		cancellationApproval.setUpdatedAt(now);
		cancellationApproval = approvalRequestRepository.save(cancellationApproval);

		ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(CANCELLATION_PROCESS_KEY,
				"LEAVE-CANCEL-" + leaveRequest.getId(),
				Map.of(
						"objectType", ApprovalObjectType.LEAVE_CANCELLATION.name(),
						"objectId", leaveRequest.getId(),
						"leaveId", leaveRequest.getId(),
						"approvalId", cancellationApproval.getId(),
						"employeeId", employee.getId(),
						"approverId", String.valueOf(reportingManager.getId())));

		Task task = taskService.createTaskQuery()
				.processInstanceId(processInstance.getId())
				.singleResult();
		if (task == null) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "RM cancellation approval task was not created");
		}

		cancellationApproval.attachWorkflow(processInstance.getId(), task.getId());
		leaveRequest.markCancellationPending();

		return new LeaveWorkflowResult(leaveRequest.getId(), employee.getId(), reportingManager.getId(),
				leaveRequest.getStatus(), processInstance.getId(), task.getId());
	}

	@Transactional(readOnly = true)
	public List<LeaveSummary> getLeaves(Long employeeId) {
		List<LeaveRequest> leaveRequests = employeeId == null
				? leaveRequestRepository.findAllByOrderByCreatedAtDesc()
				: leaveRequestRepository.findByEmployee_IdOrderByCreatedAtDesc(employeeId);
		return leaveRequests.stream().map(this::toLeaveSummary).toList();
	}

	private LeaveSummary toLeaveSummary(LeaveRequest leaveRequest) {
		Employee employee = leaveRequest.getEmployee();
		Employee approver = employee.getReportingManager();
		return new LeaveSummary(
				leaveRequest.getId(),
				employee.getId(),
				employee.getName(),
				approver == null ? null : approver.getId(),
				approver == null ? null : approver.getName(),
				leaveRequest.getLeaveType(),
				leaveRequest.getFromDate(),
				leaveRequest.getToDate(),
				leaveRequest.getReason(),
				leaveRequest.getStatus(),
				CANCELLABLE_STATUSES.contains(leaveRequest.getStatus()),
				leaveRequest.getCreatedAt(),
				leaveRequest.getUpdatedAt());
	}

	@Transactional(readOnly = true)
	public LeaveSummary getLeave(Long leaveId) {
		LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));
		return toLeaveSummary(leaveRequest);
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
		ApprovalObjectType objectType = approvalRequest.getObjectType();
		if (objectType != ApprovalObjectType.LEAVE && objectType != ApprovalObjectType.LEAVE_CANCELLATION) {
			return;
		}

		LeaveRequest leaveRequest = leaveRequestRepository.findById(approvalRequest.getObjectId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));

		if (objectType == ApprovalObjectType.LEAVE_CANCELLATION) {
			if (approved) {
				leaveRequest.cancel();
				if (isAccrualEnabled(leaveRequest.getLeaveType())) {
					leaveAccountService.credit(leaveRequest.getEmployee().getId(), leaveRequest.getLeaveType(),
							(int) requestedDays(leaveRequest), LeaveTransactionReason.REVERSAL, leaveRequest.getId());
				}
			}
			else {
				leaveRequest.revertToApproved();
			}
			return;
		}

		if (approved) {
			leaveRequest.approve();
			if (isAccrualEnabled(leaveRequest.getLeaveType())) {
				leaveAccountService.debit(leaveRequest.getEmployee().getId(), leaveRequest.getLeaveType(),
						(int) requestedDays(leaveRequest), LeaveTransactionReason.CONSUMPTION, leaveRequest.getId());
			}
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
