import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { Employee } from '../../core/models/employee.model';
import { LeaveBalanceInfo, LeaveSummary, LeaveTypeInfo } from '../../core/models/leave.model';
import { CurrentUserService } from '../../core/services/current-user.service';
import { LeaveTypeService } from '../../core/services/leave-type.service';
import { LeaveService } from '../../core/services/leave.service';

/** Leave statuses that hold a date range and therefore block a new overlapping request. */
const OVERLAP_BLOCKING_STATUSES = new Set(['SUBMITTED', 'APPROVED', 'CANCELLATION_PENDING']);

function formatDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function parseDate(value: string): Date {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function daysBetweenInclusive(from: Date, to: Date): number {
  const msPerDay = 24 * 60 * 60 * 1000;
  return Math.round((to.getTime() - from.getTime()) / msPerDay) + 1;
}

@Component({
  selector: 'app-submit-leave',
  templateUrl: './submit-leave.component.html',
  styleUrls: ['./submit-leave.component.scss'],
})
export class SubmitLeaveComponent implements OnInit {
  employees: Employee[] = [];
  leaveTypes: LeaveTypeInfo[] = [];
  balances: LeaveBalanceInfo[] = [];
  existingLeaves: LeaveSummary[] = [];
  submitting = false;
  loadingContext = false;
  minDate = new Date();

  form: FormGroup = this.fb.group({
    employeeId: [null, Validators.required],
    leaveType: [null, Validators.required],
    fromDate: [null, Validators.required],
    toDate: [null, Validators.required],
    reason: ['', [Validators.required, Validators.maxLength(500)]],
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly leaveService: LeaveService,
    private readonly leaveTypeService: LeaveTypeService,
    private readonly currentUserService: CurrentUserService,
    private readonly router: Router,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.leaveTypeService.getLeaveTypes().subscribe({
      next: (types) => (this.leaveTypes = types),
      error: () => this.snackBar.open('Could not load leave types.', 'Dismiss', { duration: 5000 }),
    });

    this.currentUserService.loadEmployees().subscribe((employees) => {
      this.employees = employees;
      const current = this.currentUserService.currentEmployee;
      if (current) {
        this.form.patchValue({ employeeId: current.id });
        this.refreshEmployeeContext(current.id);
      }
    });

    this.form.get('employeeId')?.valueChanges.subscribe((employeeId) => {
      this.refreshEmployeeContext(employeeId);
    });
  }

  private refreshEmployeeContext(employeeId: number | null): void {
    this.balances = [];
    this.existingLeaves = [];
    if (employeeId == null) {
      return;
    }
    this.loadingContext = true;
    this.leaveTypeService.getBalances(employeeId).subscribe({
      next: (balances) => (this.balances = balances),
      error: () => undefined,
    });
    this.leaveService.getLeaves(employeeId).subscribe({
      next: (leaves) => {
        this.existingLeaves = leaves.filter((leave) => OVERLAP_BLOCKING_STATUSES.has(leave.status));
        this.loadingContext = false;
      },
      error: () => (this.loadingContext = false),
    });
  }

  get selectedEmployee(): Employee | undefined {
    return this.employees.find((e) => e.id === this.form.value.employeeId);
  }

  get hasManager(): boolean {
    const employee = this.selectedEmployee;
    return !!employee && employee.reportingManagerId != null;
  }

  get selectedLeaveTypeInfo(): LeaveTypeInfo | undefined {
    return this.leaveTypes.find((t) => t.code === this.form.value.leaveType);
  }

  get selectedBalance(): LeaveBalanceInfo | undefined {
    return this.balances.find((b) => b.leaveType === this.form.value.leaveType);
  }

  get requestedDays(): number | null {
    const { fromDate, toDate } = this.form.value;
    if (!fromDate || !toDate) {
      return null;
    }
    const from = new Date(fromDate);
    const to = new Date(toDate);
    if (to < from) {
      return null;
    }
    return daysBetweenInclusive(from, to);
  }

  get overlappingLeave(): LeaveSummary | undefined {
    const { fromDate, toDate } = this.form.value;
    if (!fromDate || !toDate) {
      return undefined;
    }
    const from = new Date(fromDate);
    const to = new Date(toDate);
    if (to < from) {
      return undefined;
    }
    return this.existingLeaves.find((leave) => {
      const existingFrom = parseDate(leave.fromDate);
      const existingTo = parseDate(leave.toDate);
      return existingFrom <= to && existingTo >= from;
    });
  }

  /** Exceeds accruable available (ledger minus pending SUBMITTED holds). */
  get insufficientBalance(): boolean {
    const info = this.selectedLeaveTypeInfo;
    const days = this.requestedDays;
    if (!info || !info.accrualEnabled || days == null) {
      return false;
    }
    const available = this.selectedBalance?.available ?? 0;
    return days > available;
  }

  /** Exceeds fixed entitlement cap for non-accrual types (e.g. Paternity 3 days). */
  get exceedsCap(): boolean {
    const info = this.selectedLeaveTypeInfo;
    const days = this.requestedDays;
    if (!info || info.accrualEnabled || info.capValue == null || days == null) {
      return false;
    }
    const capInDays =
      info.capUnit === 'WEEKS' ? Math.round(Number(info.capValue) * 7) : Math.round(Number(info.capValue));
    return days > capInDays;
  }

  get canSubmit(): boolean {
    return (
      !this.submitting &&
      this.hasManager &&
      !this.overlappingLeave &&
      !this.insufficientBalance &&
      !this.exceedsCap
    );
  }

  leaveTypeLabel(code: string): string {
    return this.leaveTypes.find((t) => t.code === code)?.label ?? code;
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }

    const { employeeId, leaveType, fromDate, toDate, reason } = this.form.value;
    if (new Date(toDate) < new Date(fromDate)) {
      this.snackBar.open('End date cannot be before the start date.', 'Dismiss', { duration: 4000 });
      return;
    }
    if (this.overlappingLeave) {
      this.snackBar.open('These dates overlap with an existing leave request.', 'Dismiss', { duration: 5000 });
      return;
    }
    if (this.insufficientBalance) {
      this.snackBar.open('Insufficient leave balance for the selected dates.', 'Dismiss', { duration: 5000 });
      return;
    }
    if (this.exceedsCap) {
      this.snackBar.open('Requested days exceed the entitlement for this leave type.', 'Dismiss', {
        duration: 5000,
      });
      return;
    }

    this.submitting = true;
    this.leaveService
      .submitLeave({
        employeeId,
        leaveType,
        fromDate: formatDate(fromDate),
        toDate: formatDate(toDate),
        reason,
      })
      .subscribe({
        next: () => {
          this.submitting = false;
          this.snackBar.open('Leave submitted for approval.', 'Dismiss', { duration: 4000 });
          this.router.navigate(['/']);
        },
        error: (err) => {
          this.submitting = false;
          const message =
            err?.error?.message || err?.error?.error || 'Could not submit leave. Please try again.';
          this.snackBar.open(message, 'Dismiss', { duration: 5000 });
        },
      });
  }
}
