import { NavGroup } from './models/nav.model';

// Add new modules (Expenses, Projects, Onboarding, ...) here as additional groups.
export const NAV_GROUPS: NavGroup[] = [
  {
    label: 'Leaves',
    icon: 'beach_access',
    children: [
      { label: 'Leave Requests', route: '/', icon: 'list_alt', exact: true },
      { label: 'Submit Leave', route: '/submit-leave', icon: 'add_circle_outline' },
    ],
  },
];
