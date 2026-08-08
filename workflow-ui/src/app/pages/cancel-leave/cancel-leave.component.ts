import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { LeaveSummary } from '../../core/models/leave.model';
import { CurrentUserService } from '../../core/services/current-user.service';
import { LeaveService } from '../../core/services/leave.service';

@Component({
  selector: 'app-cancel-leave',
  templateUrl: './cancel-leave.component.html',
  styleUrls: ['./cancel-leave.component.scss'],
})
export class CancelLeaveComponent implements OnInit {
  leave: LeaveSummary | null = null;
  loading = true;
  submitting = false;
  loadError = false;

  form: FormGroup = this.fb.group({
    reason: ['', [Validators.required, Validators.maxLength(500)]],
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly leaveService: LeaveService,
    private readonly currentUserService: CurrentUserService,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const leaveId = Number(this.route.snapshot.paramMap.get('id'));
    this.currentUserService.loadEmployees().subscribe();
    this.leaveService.getLeave(leaveId).subscribe({
      next: (leave) => {
        this.leave = leave;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.loadError = true;
      },
    });
  }

  get currentEmployeeId(): number | null {
    return this.currentUserService.currentEmployee?.id ?? null;
  }

  get isOwner(): boolean {
    return !!this.leave && this.leave.employeeId === this.currentEmployeeId;
  }

  get willNeedApproval(): boolean {
    return this.leave?.status === 'APPROVED';
  }

  confirmCancel(): void {
    if (!this.leave || this.form.invalid || !this.isOwner || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.leaveService
      .cancelLeave(this.leave.leaveId, {
        employeeId: this.currentEmployeeId as number,
        reason: this.form.value.reason,
      })
      .subscribe({
        next: (result) => {
          this.submitting = false;
          const message =
            result.status === 'CANCELLED'
              ? 'Leave withdrawn successfully.'
              : 'Cancellation request sent to your manager for approval.';
          this.snackBar.open(message, 'Dismiss', { duration: 5000 });
          this.router.navigate(['/']);
        },
        error: (err) => {
          this.submitting = false;
          const message =
            err?.error?.message || err?.error?.error || 'Could not cancel this leave. Please try again.';
          this.snackBar.open(message, 'Dismiss', { duration: 5000 });
        },
      });
  }
}
