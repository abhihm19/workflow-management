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
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
public class Employee {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, unique = true)
	private String email;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reporting_manager_id")
	private Employee reportingManager;

	// Nullable at the DB level so existing rows created before this field was introduced
	// don't break `ddl-auto=update`; enforced as required for new employees in the API layer.
	@Column(name = "date_of_joining")
	private LocalDate dateOfJoining;
}
