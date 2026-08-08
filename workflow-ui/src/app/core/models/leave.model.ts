export type LeaveStatus =
  | 'SUBMITTED'
  | 'APPROVED'
  | 'REJECTED'
  | 'CANCELLATION_PENDING'
  | 'CANCELLED';

export type LeaveType = 'LOP' | 'PTO' | 'PATERNITY' | 'MATERNITY' | 'BEREAVEMENT';

export type LeaveCapUnit = 'DAYS' | 'WEEKS';

/** Leave type + its cap/accrual configuration, as sourced from the backend lookup tables. */
export interface LeaveTypeInfo {
  code: LeaveType;
  label: string;
  capValue: number | null;
  capUnit: LeaveCapUnit | null;
  accrualEnabled: boolean;
}

/** Current leave account balance for an accrual-enabled leave type. */
export interface LeaveBalanceInfo {
  leaveType: LeaveType;
  label: string;
  /** Ledger balance after approved consumption/credits. */
  balance: number;
  /** Days held by still-SUBMITTED requests (not yet debited). */
  pendingDays: number;
  /** Days free to request now (balance - pendingDays). */
  available: number;
}

export interface LeaveSummary {
  leaveId: number;
  employeeId: number;
  employeeName: string;
  approverId: number | null;
  approverName: string | null;
  leaveType: LeaveType;
  fromDate: string;
  toDate: string;
  reason: string | null;
  status: LeaveStatus;
  cancellable: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SubmitLeaveRequest {
  employeeId: number;
  leaveType: LeaveType;
  fromDate: string;
  toDate: string;
  reason: string;
}

export interface CancelLeaveRequest {
  employeeId: number;
  reason: string;
}

export interface LeaveResponse {
  leaveId: number;
  employeeId: number;
  approverId: number | null;
  status: LeaveStatus;
  processInstanceId: string | null;
  taskId: string | null;
}
