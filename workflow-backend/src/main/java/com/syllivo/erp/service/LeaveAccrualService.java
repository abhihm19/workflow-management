package com.syllivo.erp.service;

import com.syllivo.erp.entity.Employee;
import com.syllivo.erp.entity.LeaveTypeCap;
import com.syllivo.erp.enums.LeaveTransactionReason;
import com.syllivo.erp.enums.LeaveType;
import com.syllivo.erp.repo.EmployeeRepository;
import com.syllivo.erp.repo.LeaveAccountTransactionRepository;
import com.syllivo.erp.repo.LeaveTypeCapRepository;
import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grants a prorated, whole-number PTO balance to employees who joined during the
 * current calendar year. Triggered daily by the {@code leaveAccrual} Flowable
 * timer process; safe to run more than once since already-accrued employees are
 * skipped (idempotent on the presence of an ACCRUAL transaction).
 */
@Service
public class LeaveAccrualService {

	private static final Logger log = LoggerFactory.getLogger(LeaveAccrualService.class);
	private static final LeaveType ACCRUAL_LEAVE_TYPE = LeaveType.PTO;

	private final EmployeeRepository employeeRepository;
	private final LeaveTypeCapRepository leaveTypeCapRepository;
	private final LeaveAccountTransactionRepository leaveAccountTransactionRepository;
	private final LeaveAccountService leaveAccountService;

	public LeaveAccrualService(EmployeeRepository employeeRepository, LeaveTypeCapRepository leaveTypeCapRepository,
			LeaveAccountTransactionRepository leaveAccountTransactionRepository,
			LeaveAccountService leaveAccountService) {
		this.employeeRepository = employeeRepository;
		this.leaveTypeCapRepository = leaveTypeCapRepository;
		this.leaveAccountTransactionRepository = leaveAccountTransactionRepository;
		this.leaveAccountService = leaveAccountService;
	}

	@Transactional
	public void runDailyAccrual() {
		LeaveTypeCap cap = leaveTypeCapRepository.findByLookupValue_Code(ACCRUAL_LEAVE_TYPE.name()).orElse(null);
		if (cap == null || !cap.isAccrualEnabled() || cap.getCapValue() == null) {
			log.info("Leave accrual skipped: {} is not configured for accrual", ACCRUAL_LEAVE_TYPE);
			return;
		}
		double annualCap = cap.getCapValue().doubleValue();

		Year currentYear = Year.now();
		LocalDate yearStart = currentYear.atDay(1);
		LocalDate yearEnd = currentYear.atMonth(12).atEndOfMonth();
		int daysInYear = currentYear.length();

		List<Employee> newJoiners = employeeRepository.findByDateOfJoiningBetween(yearStart, yearEnd);

		int accruedCount = 0;
		for (Employee employee : newJoiners) {
			boolean alreadyAccrued = leaveAccountTransactionRepository
					.existsByLeaveAccount_Employee_IdAndLeaveAccount_LeaveTypeAndReason(
							employee.getId(), ACCRUAL_LEAVE_TYPE, LeaveTransactionReason.ACCRUAL);
			if (alreadyAccrued) {
				continue;
			}

			long remainingDays = ChronoUnit.DAYS.between(employee.getDateOfJoining(), yearEnd) + 1;
			int proratedDays = (int) Math.round(annualCap * remainingDays / (double) daysInYear);
			if (proratedDays <= 0) {
				continue;
			}

			leaveAccountService.credit(employee.getId(), ACCRUAL_LEAVE_TYPE, proratedDays,
					LeaveTransactionReason.ACCRUAL, null);
			accruedCount++;
			log.info("Accrued {} {} day(s) for employee {} (joined {})", proratedDays, ACCRUAL_LEAVE_TYPE,
					employee.getId(), employee.getDateOfJoining());
		}

		log.info("Leave accrual run complete: {} employee(s) newly credited", accruedCount);
	}
}
