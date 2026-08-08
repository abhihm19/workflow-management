package com.syllivo.erp.repo;

import com.syllivo.erp.entity.Employee;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

	List<Employee> findByDateOfJoiningBetween(LocalDate start, LocalDate end);
}
