package com.syllivo.erp.repo;

import com.syllivo.erp.entity.LeaveAccountTransaction;
import com.syllivo.erp.enums.LeaveTransactionReason;
import com.syllivo.erp.enums.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveAccountTransactionRepository extends JpaRepository<LeaveAccountTransaction, Long> {

	boolean existsByLeaveAccount_Employee_IdAndLeaveAccount_LeaveTypeAndReason(
			Long employeeId, LeaveType leaveType, LeaveTransactionReason reason);
}
