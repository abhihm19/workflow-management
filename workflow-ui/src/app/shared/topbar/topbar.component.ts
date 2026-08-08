import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Observable } from 'rxjs';
import { Employee } from '../../core/models/employee.model';
import { CurrentUserService } from '../../core/services/current-user.service';

@Component({
  selector: 'app-topbar',
  templateUrl: './topbar.component.html',
  styleUrls: ['./topbar.component.scss'],
})
export class TopbarComponent implements OnInit {
  @Input() sidenavCollapsed = false;
  @Output() toggleSidenav = new EventEmitter<void>();

  employees$: Observable<Employee[]> = this.currentUserService.employees$;
  currentEmployee$: Observable<Employee | null> = this.currentUserService.currentEmployee$;

  constructor(private readonly currentUserService: CurrentUserService) {}

  ngOnInit(): void {
    this.currentUserService.loadEmployees().subscribe();
  }

  onEmployeeChange(employeeId: number, employees: Employee[]): void {
    const employee = employees.find((e) => e.id === employeeId) ?? null;
    this.currentUserService.setCurrentEmployee(employee);
  }
}
