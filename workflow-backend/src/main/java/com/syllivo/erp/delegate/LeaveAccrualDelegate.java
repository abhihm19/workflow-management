package com.syllivo.erp.delegate;

import com.syllivo.erp.service.LeaveAccrualService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Thin adapter invoked by the {@code leaveAccrual} BPMN process's timer-triggered
 * service task. Bean name {@code leaveAccrualDelegate} is referenced from
 * {@code leave-accrual.bpmn} via {@code flowable:delegateExpression}.
 */
@Component("leaveAccrualDelegate")
public class LeaveAccrualDelegate implements JavaDelegate {

	private final LeaveAccrualService leaveAccrualService;

	public LeaveAccrualDelegate(LeaveAccrualService leaveAccrualService) {
		this.leaveAccrualService = leaveAccrualService;
	}

	@Override
	public void execute(DelegateExecution execution) {
		leaveAccrualService.runDailyAccrual();
	}
}
