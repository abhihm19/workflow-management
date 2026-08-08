package com.syllivo.erp.service;

import com.syllivo.erp.entity.Employee;
import com.syllivo.erp.entity.LeaveAccount;
import com.syllivo.erp.entity.LeaveAccountTransaction;
import com.syllivo.erp.enums.LeaveTransactionReason;
import com.syllivo.erp.enums.LeaveTransactionType;
import com.syllivo.erp.enums.LeaveType;
import com.syllivo.erp.repo.EmployeeRepository;
import com.syllivo.erp.repo.LeaveAccountRepository;
import com.syllivo.erp.repo.LeaveAccountTransactionRepository;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Ledger for per-employee, per-leave-type leave balances. Accrual credits the
 * account, approved consumption debits it, and cancellation of an already-approved
 * leave reverses (credits back) the debit.
 */
@Service
public class LeaveAccountService {

	private final LeaveAccountRepository leaveAccountRepository;
	private final LeaveAccountTransactionRepository leaveAccountTransactionRepository;
	private final EmployeeRepository employeeRepository;

	public LeaveAccountService(LeaveAccountRepository leaveAccountRepository,
			LeaveAccountTransactionRepository leaveAccountTransactionRepository,
			EmployeeRepository employeeRepository) {
		this.leaveAccountRepository = leaveAccountRepository;
		this.leaveAccountTransactionRepository = leaveAccountTransactionRepository;
		this.employeeRepository = employeeRepository;
	}

	@Transactional
	public LeaveAccount getOrCreateAccount(Long employeeId, LeaveType leaveType) {
		return leaveAccountRepository.findByEmployee_IdAndLeaveType(employeeId, leaveType)
				.orElseGet(() -> {
					Employee employee = employeeRepository.findById(employeeId)
							.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
					LeaveAccount account = new LeaveAccount();
					account.setEmployee(employee);
					account.setLeaveType(leaveType);
					account.setBalance(0);
					Instant now = Instant.now();
					account.setCreatedAt(now);
					account.setUpdatedAt(now);
					return leaveAccountRepository.save(account);
				});
	}

	@Transactional(readOnly = true)
	public int getAvailableBalance(Long employeeId, LeaveType leaveType) {
		return leaveAccountRepository.findByEmployee_IdAndLeaveType(employeeId, leaveType)
				.map(LeaveAccount::getBalance)
				.orElse(0);
	}

	@Transactional
	public LeaveAccountTransaction credit(Long employeeId, LeaveType leaveType, int amount, LeaveTransactionReason reason,
			Long referenceLeaveId) {
		if (amount <= 0) {
			throw new IllegalArgumentException("Credit amount must be positive");
		}
		LeaveAccount account = getOrCreateAccount(employeeId, leaveType);
		account.applyDelta(amount);
		return recordTransaction(account, LeaveTransactionType.CREDIT, amount, reason, referenceLeaveId);
	}

	@Transactional
	public LeaveAccountTransaction debit(Long employeeId, LeaveType leaveType, int amount, LeaveTransactionReason reason,
			Long referenceLeaveId) {
		if (amount <= 0) {
			throw new IllegalArgumentException("Debit amount must be positive");
		}
		LeaveAccount account = getOrCreateAccount(employeeId, leaveType);
		if (amount > account.getBalance()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Insufficient " + leaveType + " balance: available " + account.getBalance() + ", requested " + amount);
		}
		account.applyDelta(-amount);
		return recordTransaction(account, LeaveTransactionType.DEBIT, amount, reason, referenceLeaveId);
	}

	private LeaveAccountTransaction recordTransaction(LeaveAccount account, LeaveTransactionType type, int amount,
			LeaveTransactionReason reason, Long referenceLeaveId) {
		LeaveAccountTransaction transaction = new LeaveAccountTransaction();
		transaction.setLeaveAccount(account);
		transaction.setTransactionType(type);
		transaction.setAmount(amount);
		transaction.setReason(reason);
		transaction.setReferenceLeaveId(referenceLeaveId);
		transaction.setBalanceAfter(account.getBalance());
		transaction.setCreatedAt(Instant.now());
		return leaveAccountTransactionRepository.save(transaction);
	}
}
