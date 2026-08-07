package com.syllivo.erp.controller;

import com.syllivo.erp.dto.CreateEmployeeRequest;
import com.syllivo.erp.dto.EmployeeResponse;
import com.syllivo.erp.entity.Employee;
import com.syllivo.erp.repo.EmployeeRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

	private final EmployeeRepository employeeRepository;

	public EmployeeController(EmployeeRepository employeeRepository) {
		this.employeeRepository = employeeRepository;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public EmployeeResponse createEmployee(@RequestBody CreateEmployeeRequest request) {
		Employee reportingManager = null;
		if (request.reportingManagerId() != null) {
			reportingManager = employeeRepository.findById(request.reportingManagerId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporting manager not found"));
		}

		Employee employee = new Employee();
		employee.setName(request.name());
		employee.setEmail(request.email());
		employee.setReportingManager(reportingManager);
		employee = employeeRepository.save(employee);
		return toResponse(employee);
	}

	@GetMapping
	public List<EmployeeResponse> getEmployees() {
		return employeeRepository.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	private EmployeeResponse toResponse(Employee employee) {
		Long reportingManagerId = employee.getReportingManager() == null ? null : employee.getReportingManager().getId();
		return new EmployeeResponse(employee.getId(), employee.getName(), employee.getEmail(), reportingManagerId);
	}
}
