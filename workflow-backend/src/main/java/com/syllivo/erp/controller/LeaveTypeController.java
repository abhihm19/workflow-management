package com.syllivo.erp.controller;

import com.syllivo.erp.dto.LeaveBalanceInfo;
import com.syllivo.erp.dto.LeaveTypeInfo;
import com.syllivo.erp.service.LeaveTypeCatalogService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leave-types")
public class LeaveTypeController {

	private final LeaveTypeCatalogService leaveTypeCatalogService;

	public LeaveTypeController(LeaveTypeCatalogService leaveTypeCatalogService) {
		this.leaveTypeCatalogService = leaveTypeCatalogService;
	}

	@GetMapping
	public List<LeaveTypeInfo> getLeaveTypes() {
		return leaveTypeCatalogService.listLeaveTypes();
	}

	@GetMapping("/balances")
	public List<LeaveBalanceInfo> getBalances(@RequestParam Long employeeId) {
		return leaveTypeCatalogService.getBalances(employeeId);
	}
}
