package com.syllivo.erp.entity;

import com.syllivo.erp.enums.LeaveTransactionReason;
import com.syllivo.erp.enums.LeaveTransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "leave_account_transaction")
@Getter
@Setter
@NoArgsConstructor
public class LeaveAccountTransaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "leave_account_id", nullable = false)
	private LeaveAccount leaveAccount;

	@Enumerated(EnumType.STRING)
	@Column(name = "transaction_type", nullable = false, length = 10)
	private LeaveTransactionType transactionType;

	@Column(nullable = false)
	private int amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private LeaveTransactionReason reason;

	@Column(name = "reference_leave_id")
	private Long referenceLeaveId;

	@Column(name = "balance_after", nullable = false)
	private int balanceAfter;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;
}
