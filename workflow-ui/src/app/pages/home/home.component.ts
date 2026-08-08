import { Component, OnDestroy, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Employee } from '../../core/models/employee.model';
import { LeaveBalanceInfo, LeaveSummary, LeaveTypeInfo } from '../../core/models/leave.model';
import { CurrentUserService } from '../../core/services/current-user.service';
import { LeaveTypeService } from '../../core/services/leave-type.service';
import { LeaveService } from '../../core/services/leave.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
})
export class HomeComponent implements OnInit, OnDestroy {
  readonly displayedColumns = ['leaveId', 'leaveType', 'dates', 'reason', 'approver', 'status', 'actions'];

  leaves: LeaveSummary[] = [];
  balances: LeaveBalanceInfo[] = [];
  leaveTypes: LeaveTypeInfo[] = [];
  loading = false;
  currentEmployee: Employee | null = null;

  private readonly destroyed$ = new Subject<void>();

  constructor(
    private readonly leaveService: LeaveService,
    private readonly leaveTypeService: LeaveTypeService,
    private readonly currentUserService: CurrentUserService,
    private readonly router: Router,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.leaveTypeService
      .getLeaveTypes()
      .pipe(takeUntil(this.destroyed$))
      .subscribe({
        next: (types) => (this.leaveTypes = types),
        error: () => undefined,
      });

    this.currentUserService
      .loadEmployees()
      .pipe(takeUntil(this.destroyed$))
      .subscribe();

    this.currentUserService.currentEmployee$.pipe(takeUntil(this.destroyed$)).subscribe((employee) => {
      this.currentEmployee = employee;
      this.fetchLeaves();
      this.fetchBalances();
    });
  }

  leaveTypeLabel(code: string): string {
    return this.leaveTypes.find((t) => t.code === code)?.label ?? code;
  }

  fetchBalances(): void {
    if (!this.currentEmployee) {
      this.balances = [];
      return;
    }
    this.leaveTypeService.getBalances(this.currentEmployee.id).subscribe({
      next: (balances) => (this.balances = balances),
      error: () => (this.balances = []),
    });
  }

  ngOnDestroy(): void {
    this.destroyed$.next();
    this.destroyed$.complete();
  }

  fetchLeaves(): void {
    if (!this.currentEmployee) {
      this.leaves = [];
      return;
    }
    this.loading = true;
    this.leaveService.getLeaves(this.currentEmployee.id).subscribe({
      next: (leaves) => {
        this.leaves = leaves;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Could not load leaves. Is the backend running?', 'Dismiss', {
          duration: 5000,
        });
      },
    });
  }

  onCancel(leave: LeaveSummary): void {
    this.router.navigate(['/leaves', leave.leaveId, 'cancel']);
  }

  trackByLeaveId(_index: number, leave: LeaveSummary): number {
    return leave.leaveId;
  }
}
