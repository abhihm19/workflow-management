package com.syllivo.erp.repo;

import com.syllivo.erp.entity.LookupType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LookupTypeRepository extends JpaRepository<LookupType, Long> {

	Optional<LookupType> findByCode(String code);
}
