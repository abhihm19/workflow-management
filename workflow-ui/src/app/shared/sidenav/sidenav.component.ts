import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Router } from '@angular/router';
import { NAV_GROUPS } from '../../core/nav-config';
import { NavGroup } from '../../core/models/nav.model';

@Component({
  selector: 'app-sidenav',
  templateUrl: './sidenav.component.html',
  styleUrls: ['./sidenav.component.scss'],
})
export class SidenavComponent {
  @Input() collapsed = false;
  @Output() toggleCollapse = new EventEmitter<void>();

  readonly groups: NavGroup[] = NAV_GROUPS;
  private readonly expandedGroups = new Set<string>();

  constructor(private readonly router: Router) {}

  isExpanded(group: NavGroup): boolean {
    return this.expandedGroups.has(group.label);
  }

  isGroupActive(group: NavGroup): boolean {
    return group.children.some((item) =>
      item.exact ? this.router.url === item.route : this.router.url.startsWith(item.route)
    );
  }

  onGroupClick(group: NavGroup): void {
    if (this.collapsed) {
      this.expandedGroups.add(group.label);
      this.toggleCollapse.emit();
      return;
    }

    if (this.isExpanded(group)) {
      this.expandedGroups.delete(group.label);
    } else {
      this.expandedGroups.add(group.label);
    }
  }
}
