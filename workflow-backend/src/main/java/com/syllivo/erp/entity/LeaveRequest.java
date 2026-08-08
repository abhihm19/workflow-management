package com.syllivo.erp.entity;

import com.syllivo.erp.enums.LeaveStatus;
import com.syllivo.erp.enums.LeaveType;
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
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "leaves")
@Getter
@Setter
@NoArgsConstructor
public class LeaveRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "employee_id", nullable = false)
	private Employee employee;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private LeaveType leaveType;

	@Column(nullable = false)
	private LocalDate fromDate;

	@Column(nullable = false)
	private LocalDate toDate;

	@Column(length = 500)
	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private LeaveStatus status;

	@Column(length = 100)
	private String processInstanceId;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	public void attachProcess(String processInstanceId) {
		this.processInstanceId = processInstanceId;
		touch();
	}

	public void approve() {
		this.status = LeaveStatus.APPROVED;
		touch();
	}

	public void reject() {
		this.status = LeaveStatus.REJECTED;
		touch();
	}

	public void cancel() {
		this.status = LeaveStatus.CANCELLED;
		touch();
	}

	public void markCancellationPending() {
		this.status = LeaveStatus.CANCELLATION_PENDING;
		touch();
	}

	public void revertToApproved() {
		this.status = LeaveStatus.APPROVED;
		touch();
	}

	private void touch() {
		this.updatedAt = Instant.now();
	}
}
