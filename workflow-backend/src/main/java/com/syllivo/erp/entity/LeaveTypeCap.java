package com.syllivo.erp.entity;

import com.syllivo.erp.enums.LeaveCapUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "leave_type_cap")
@Getter
@Setter
@NoArgsConstructor
public class LeaveTypeCap {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lookup_value_id", nullable = false, unique = true)
	private LookupValue lookupValue;

	@Column(name = "cap_value", precision = 6, scale = 2)
	private BigDecimal capValue;

	@Enumerated(EnumType.STRING)
	@Column(name = "cap_unit", length = 10)
	private LeaveCapUnit capUnit;

	@Column(name = "accrual_enabled", nullable = false)
	private boolean accrualEnabled = false;
}
