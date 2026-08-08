package com.syllivo.erp.repo;

import com.syllivo.erp.entity.LeaveTypeCap;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveTypeCapRepository extends JpaRepository<LeaveTypeCap, Long> {

	Optional<LeaveTypeCap> findByLookupValue_Code(String leaveTypeCode);
}
