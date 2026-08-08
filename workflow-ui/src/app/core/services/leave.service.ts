import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CancelLeaveRequest,
  LeaveResponse,
  LeaveSummary,
  SubmitLeaveRequest,
} from '../models/leave.model';

@Injectable({ providedIn: 'root' })
export class LeaveService {
  private readonly baseUrl = `${environment.apiBaseUrl}/leaves`;

  constructor(private readonly http: HttpClient) {}

  getLeaves(employeeId?: number | null): Observable<LeaveSummary[]> {
    let params = new HttpParams();
    if (employeeId != null) {
      params = params.set('employeeId', employeeId);
    }
    return this.http.get<LeaveSummary[]>(this.baseUrl, { params });
  }

  getLeave(leaveId: number): Observable<LeaveSummary> {
    return this.http.get<LeaveSummary>(`${this.baseUrl}/${leaveId}`);
  }

  submitLeave(request: SubmitLeaveRequest): Observable<LeaveResponse> {
    return this.http.post<LeaveResponse>(this.baseUrl, request);
  }

  cancelLeave(leaveId: number, request: CancelLeaveRequest): Observable<LeaveResponse> {
    return this.http.post<LeaveResponse>(`${this.baseUrl}/${leaveId}/cancel`, request);
  }
}
