import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LeaveBalanceInfo, LeaveTypeInfo } from '../models/leave.model';

@Injectable({ providedIn: 'root' })
export class LeaveTypeService {
  private readonly baseUrl = `${environment.apiBaseUrl}/leave-types`;

  constructor(private readonly http: HttpClient) {}

  getLeaveTypes(): Observable<LeaveTypeInfo[]> {
    return this.http.get<LeaveTypeInfo[]>(this.baseUrl);
  }

  getBalances(employeeId: number): Observable<LeaveBalanceInfo[]> {
    const params = new HttpParams().set('employeeId', employeeId);
    return this.http.get<LeaveBalanceInfo[]>(`${this.baseUrl}/balances`, { params });
  }
}
