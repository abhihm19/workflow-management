package com.syllivo.erp.dto;

/**
 * Per-employee leave account snapshot for an accrual-enabled leave type.
 *
 * <ul>
 *   <li>{@code balance} — ledger balance (credits minus approved consumption)</li>
 *   <li>{@code pendingDays} — days held by still-SUBMITTED requests (not yet debited)</li>
 *   <li>{@code available} — days free to request now ({@code balance - pendingDays})</li>
 * </ul>
 */
public record LeaveBalanceInfo(
		String leaveType,
		String label,
		int balance,
		int pendingDays,
		int available) {
}
