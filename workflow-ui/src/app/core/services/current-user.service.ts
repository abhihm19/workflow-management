import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { shareReplay, tap } from 'rxjs/operators';
import { Employee } from '../models/employee.model';
import { EmployeeService } from './employee.service';

const STORAGE_KEY = 'workflow.currentEmployeeId';

@Injectable({ providedIn: 'root' })
export class CurrentUserService {
  private readonly employeesSubject = new BehaviorSubject<Employee[]>([]);
  readonly employees$: Observable<Employee[]> = this.employeesSubject.asObservable();

  private readonly currentEmployeeSubject = new BehaviorSubject<Employee | null>(null);
  readonly currentEmployee$: Observable<Employee | null> = this.currentEmployeeSubject.asObservable();

  private loaded$?: Observable<Employee[]>;

  constructor(private readonly employeeService: EmployeeService) {}

  loadEmployees(): Observable<Employee[]> {
    if (!this.loaded$) {
      this.loaded$ = this.employeeService.getEmployees().pipe(
        tap((employees) => {
          this.employeesSubject.next(employees);
          this.restoreOrDefaultSelection(employees);
        }),
        shareReplay(1)
      );
    }
    return this.loaded$;
  }

  refreshEmployees(): void {
    this.employeeService.getEmployees().subscribe((employees) => {
      this.employeesSubject.next(employees);
      this.restoreOrDefaultSelection(employees);
    });
  }

  setCurrentEmployee(employee: Employee | null): void {
    this.currentEmployeeSubject.next(employee);
    if (employee) {
      localStorage.setItem(STORAGE_KEY, String(employee.id));
    } else {
      localStorage.removeItem(STORAGE_KEY);
    }
  }

  get currentEmployee(): Employee | null {
    return this.currentEmployeeSubject.value;
  }

  private restoreOrDefaultSelection(employees: Employee[]): void {
    if (this.currentEmployeeSubject.value) {
      const stillExists = employees.some((e) => e.id === this.currentEmployeeSubject.value?.id);
      if (stillExists) {
        return;
      }
    }
    const storedId = localStorage.getItem(STORAGE_KEY);
    const restored = storedId ? employees.find((e) => e.id === Number(storedId)) : undefined;
    this.currentEmployeeSubject.next(restored ?? employees[0] ?? null);
  }
}
