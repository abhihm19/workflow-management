import { Component } from '@angular/core';

const SIDENAV_STATE_KEY = 'workflow.sidenavCollapsed';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent {
  sidenavCollapsed = localStorage.getItem(SIDENAV_STATE_KEY) === 'true';

  toggleSidenav(): void {
    this.sidenavCollapsed = !this.sidenavCollapsed;
    localStorage.setItem(SIDENAV_STATE_KEY, String(this.sidenavCollapsed));
  }
}
