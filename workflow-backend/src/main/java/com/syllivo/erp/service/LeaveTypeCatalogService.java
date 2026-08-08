package com.syllivo.erp.service;

import com.syllivo.erp.config.LeaveTypeLookupSeeder;
import com.syllivo.erp.dto.LeaveBalanceInfo;
import com.syllivo.erp.dto.LeaveTypeInfo;
import com.syllivo.erp.entity.LeaveRequest;
import com.syllivo.erp.entity.LeaveTypeCap;
import com.syllivo.erp.entity.LookupValue;
import com.syllivo.erp.enums.LeaveStatus;
import com.syllivo.erp.enums.LeaveType;
import com.syllivo.erp.repo.LeaveRequestRepository;
import com.syllivo.erp.repo.LeaveTypeCapRepository;
import com.syllivo.erp.repo.LookupValueRepository;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only catalog of leave types (with their caps) and per-employee balances,
 * backed by the generic lookup_type/lookup_value tables and leave_type_cap.
 */
@Service
public class LeaveTypeCatalogService {

	private final LookupValueRepository lookupValueRepository;
	private final LeaveTypeCapRepository leaveTypeCapRepository;
	private final LeaveAccountService leaveAccountService;
	private final LeaveRequestRepository leaveRequestRepository;

	public LeaveTypeCatalogService(LookupValueRepository lookupValueRepository,
			LeaveTypeCapRepository leaveTypeCapRepository, LeaveAccountService leaveAccountService,
			LeaveRequestRepository leaveRequestRepository) {
		this.lookupValueRepository = lookupValueRepository;
		this.leaveTypeCapRepository = leaveTypeCapRepository;
		this.leaveAccountService = leaveAccountService;
		this.leaveRequestRepository = leaveRequestRepository;
	}

	@Transactional(readOnly = true)
	public List<LeaveTypeInfo> listLeaveTypes() {
		List<LookupValue> values = lookupValueRepository
				.findByLookupType_CodeOrderBySortOrderAsc(LeaveTypeLookupSeeder.LEAVE_TYPE_LOOKUP_CODE);
		return values.stream()
				.filter(LookupValue::isActive)
				.map(this::toLeaveTypeInfo)
				.toList();
	}

	private LeaveTypeInfo toLeaveTypeInfo(LookupValue value) {
		LeaveTypeCap cap = leaveTypeCapRepository.findByLookupValue_Code(value.getCode()).orElse(null);
		return new LeaveTypeInfo(
				value.getCode(),
				value.getLabel(),
				cap == null ? null : cap.getCapValue(),
				cap == null ? null : cap.getCapUnit(),
				cap != null && cap.isAccrualEnabled());
	}

	@Transactional(readOnly = true)
	public List<LeaveBalanceInfo> getBalances(Long employeeId) {
		return listLeaveTypes().stream()
				.filter(LeaveTypeInfo::accrualEnabled)
				.map(info -> {
					LeaveType leaveType = LeaveType.valueOf(info.code());
					int balance = leaveAccountService.getAvailableBalance(employeeId, leaveType);
					int pendingDays = sumPendingSubmittedDays(employeeId, leaveType);
					int available = Math.max(0, balance - pendingDays);
					return new LeaveBalanceInfo(info.code(), info.label(), balance, pendingDays, available);
				})
				.toList();
	}

	private int sumPendingSubmittedDays(Long employeeId, LeaveType leaveType) {
		return leaveRequestRepository
				.findByEmployee_IdAndLeaveTypeAndStatus(employeeId, leaveType, LeaveStatus.SUBMITTED)
				.stream()
				.mapToInt(this::daysOf)
				.sum();
	}

	private int daysOf(LeaveRequest leave) {
		return (int) ChronoUnit.DAYS.between(leave.getFromDate(), leave.getToDate()) + 1;
	}
}
