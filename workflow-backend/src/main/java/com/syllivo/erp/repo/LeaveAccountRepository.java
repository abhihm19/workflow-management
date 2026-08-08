package com.syllivo.erp.repo;

import com.syllivo.erp.entity.LeaveAccount;
import com.syllivo.erp.enums.LeaveType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveAccountRepository extends JpaRepository<LeaveAccount, Long> {

	Optional<LeaveAccount> findByEmployee_IdAndLeaveType(Long employeeId, LeaveType leaveType);
}
