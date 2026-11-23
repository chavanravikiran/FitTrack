export interface NavItem {
  label: string;
  icon: string;
  path: string;
  /** Shown in the mobile bottom bar. */
  primary?: boolean;
}

export const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', icon: 'space_dashboard', path: '/dashboard', primary: true },
  { label: 'My Workout', icon: 'fitness_center', path: '/workout', primary: true },
  { label: 'Activity', icon: 'directions_walk', path: '/activity', primary: true },
  { label: 'Nutrition', icon: 'restaurant', path: '/nutrition', primary: true },
  { label: 'Progress', icon: 'trending_up', path: '/progress' },
  { label: 'Exercise Videos', icon: 'smart_display', path: '/videos' },
  { label: 'Reminders', icon: 'notifications', path: '/reminders' },
  { label: 'My Profile', icon: 'person', path: '/profile', primary: true },
  { label: 'Subscription', icon: 'workspace_premium', path: '/billing' },
];

export const ADMIN_NAV_ITEM: NavItem = { label: 'Admin', icon: 'admin_panel_settings', path: '/admin' };
