package com.syllivo.erp.dto;

import com.syllivo.erp.enums.LeaveCapUnit;
import java.math.BigDecimal;

public record LeaveTypeInfo(
		String code,
		String label,
		BigDecimal capValue,
		LeaveCapUnit capUnit,
		boolean accrualEnabled) {
}
