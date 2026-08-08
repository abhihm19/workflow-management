package com.syllivo.erp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lookup_value", uniqueConstraints = @UniqueConstraint(columnNames = { "lookup_type_id", "code" }))
@Getter
@Setter
@NoArgsConstructor
public class LookupValue {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lookup_type_id", nullable = false)
	private LookupType lookupType;

	@Column(nullable = false, length = 50)
	private String code;

	@Column(nullable = false, length = 100)
	private String label;

	@Column(name = "sort_order")
	private Integer sortOrder;

	@Column(nullable = false)
	private boolean active = true;
}
