package com.syllivo.erp.entity;

import com.syllivo.erp.enums.ApprovalObjectType;
import com.syllivo.erp.enums.ApprovalStatus;
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
@Table(name = "approvals")
@Getter
@Setter
@NoArgsConstructor
public class ApprovalRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private ApprovalObjectType objectType;

	@Column(nullable = false)
	private Long objectId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "employee_id", nullable = false)
	private Employee employee;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "approver_id", nullable = false)
	private Employee approver;

	@Column(nullable = false, length = 50)
	private String approvalLevel;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ApprovalStatus status;

	@Column(length = 100)
	private String flowableTaskId;

	@Column(length = 100)
	private String processInstanceId;

	@Column(length = 1000)
	private String comments;

	private Instant approvedAt;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	public void attachWorkflow(String processInstanceId, String flowableTaskId) {
		this.processInstanceId = processInstanceId;
		this.flowableTaskId = flowableTaskId;
		touch();
	}

	public void approve(String comments) {
		this.status = ApprovalStatus.APPROVED;
		this.comments = comments;
		this.approvedAt = Instant.now();
		touch();
	}

	public void reject(String comments) {
		this.status = ApprovalStatus.REJECTED;
		this.comments = comments;
		this.approvedAt = Instant.now();
		touch();
	}

	public void cancel(String comments) {
		this.status = ApprovalStatus.CANCELLED;
		this.comments = comments;
		touch();
	}

	private void touch() {
		this.updatedAt = Instant.now();
	}
}
