export interface NavItem {
  label: string;
  route: string;
  icon: string;
  exact?: boolean;
}

export interface NavGroup {
  label: string;
  icon: string;
  children: NavItem[];
}
