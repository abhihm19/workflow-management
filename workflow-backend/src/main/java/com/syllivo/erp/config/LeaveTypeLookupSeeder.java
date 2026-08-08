package com.syllivo.erp.config;

import com.syllivo.erp.entity.LeaveTypeCap;
import com.syllivo.erp.entity.LookupType;
import com.syllivo.erp.entity.LookupValue;
import com.syllivo.erp.enums.LeaveCapUnit;
import com.syllivo.erp.repo.LeaveTypeCapRepository;
import com.syllivo.erp.repo.LookupTypeRepository;
import com.syllivo.erp.repo.LookupValueRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures the LEAVE_TYPE lookup type/values and their leave-specific caps exist on
 * startup. Runs as an idempotent upsert so it is safe with {@code ddl-auto=update}.
 */
@Component
public class LeaveTypeLookupSeeder implements CommandLineRunner {

	public static final String LEAVE_TYPE_LOOKUP_CODE = "LEAVE_TYPE";

	private record LeaveTypeSeed(String code, String label, int sortOrder, BigDecimal capValue, LeaveCapUnit capUnit,
			boolean accrualEnabled) {
	}

	private static final LeaveTypeSeed[] SEEDS = {
			new LeaveTypeSeed("LOP", "Loss of Pay (LOP)", 1, null, null, false),
			new LeaveTypeSeed("PTO", "Paid Time Off (PTO)", 2, new BigDecimal("25"), LeaveCapUnit.DAYS, true),
			new LeaveTypeSeed("PATERNITY", "Paternity Leave", 3, new BigDecimal("3"), LeaveCapUnit.DAYS, false),
			new LeaveTypeSeed("MATERNITY", "Maternity Leave", 4, new BigDecimal("26"), LeaveCapUnit.WEEKS, false),
			new LeaveTypeSeed("BEREAVEMENT", "Bereavement Leave", 5, new BigDecimal("3"), LeaveCapUnit.DAYS, false),
	};

	private final LookupTypeRepository lookupTypeRepository;
	private final LookupValueRepository lookupValueRepository;
	private final LeaveTypeCapRepository leaveTypeCapRepository;

	public LeaveTypeLookupSeeder(LookupTypeRepository lookupTypeRepository, LookupValueRepository lookupValueRepository,
			LeaveTypeCapRepository leaveTypeCapRepository) {
		this.lookupTypeRepository = lookupTypeRepository;
		this.lookupValueRepository = lookupValueRepository;
		this.leaveTypeCapRepository = leaveTypeCapRepository;
	}

	@Override
	@Transactional
	public void run(String... args) {
		LookupType leaveType = lookupTypeRepository.findByCode(LEAVE_TYPE_LOOKUP_CODE)
				.orElseGet(() -> {
					LookupType type = new LookupType();
					type.setCode(LEAVE_TYPE_LOOKUP_CODE);
					type.setDescription("Types of leave an employee can request");
					return lookupTypeRepository.save(type);
				});

		for (LeaveTypeSeed seed : SEEDS) {
			LookupValue value = lookupValueRepository.findByLookupType_CodeAndCode(LEAVE_TYPE_LOOKUP_CODE, seed.code())
					.orElseGet(() -> {
						LookupValue newValue = new LookupValue();
						newValue.setLookupType(leaveType);
						newValue.setCode(seed.code());
						return newValue;
					});
			value.setLabel(seed.label());
			value.setSortOrder(seed.sortOrder());
			value.setActive(true);
			value = lookupValueRepository.save(value);

			LookupValue savedValue = value;
			LeaveTypeCap cap = leaveTypeCapRepository.findByLookupValue_Code(seed.code())
					.orElseGet(() -> {
						LeaveTypeCap newCap = new LeaveTypeCap();
						newCap.setLookupValue(savedValue);
						return newCap;
					});
			cap.setCapValue(seed.capValue());
			cap.setCapUnit(seed.capUnit());
			cap.setAccrualEnabled(seed.accrualEnabled());
			leaveTypeCapRepository.save(cap);
		}
	}
}
