package com.syllivo.erp.repo;

import com.syllivo.erp.entity.LookupValue;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LookupValueRepository extends JpaRepository<LookupValue, Long> {

	Optional<LookupValue> findByLookupType_CodeAndCode(String lookupTypeCode, String code);

	List<LookupValue> findByLookupType_CodeOrderBySortOrderAsc(String lookupTypeCode);
}
