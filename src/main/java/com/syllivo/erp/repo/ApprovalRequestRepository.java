package com.syllivo.erp.repo;

import com.syllivo.erp.entity.ApprovalRequest;
import com.syllivo.erp.enums.ApprovalStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

	List<ApprovalRequest> findByApprover_IdAndStatus(Long approverId, ApprovalStatus status);
}
