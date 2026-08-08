import { Component, Input } from '@angular/core';
import { LeaveStatus } from '../../core/models/leave.model';

interface StatusMeta {
  label: string;
  cssClass: string;
}

const STATUS_META: Record<LeaveStatus, StatusMeta> = {
  SUBMITTED: { label: 'Pending Approval', cssClass: 'status-submitted' },
  APPROVED: { label: 'Approved', cssClass: 'status-approved' },
  REJECTED: { label: 'Rejected', cssClass: 'status-rejected' },
  CANCELLATION_PENDING: { label: 'Cancellation Pending', cssClass: 'status-cancellation-pending' },
  CANCELLED: { label: 'Cancelled', cssClass: 'status-cancelled' },
};

@Component({
  selector: 'app-status-chip',
  templateUrl: './status-chip.component.html',
  styleUrls: ['./status-chip.component.scss'],
})
export class StatusChipComponent {
  @Input() status!: LeaveStatus;

  get meta(): StatusMeta {
    return STATUS_META[this.status] ?? { label: this.status, cssClass: 'status-default' };
  }
}
