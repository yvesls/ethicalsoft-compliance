export interface MenuItem {
  label: string;
  icon?: string;
  route?: string;
  roles?: string[];
  children?: MenuItem[];
}

export const MENU_CONFIG: MenuItem[] = [
  {
    label: 'Home',
    icon: 'home',
    route: '/home',
    roles: ['ROLE_USER'],
  },
  {
    label: 'Projects',
    icon: 'projects',
    route: '/projects',
    roles: ['ROLE_ADMIN', 'ROLE_USER'],
  },
  {
    label: 'Dashboards',
    icon: 'dashboards',
    route: '/dashboards',
    roles: ['ROLE_ADMIN'],
  },
  {
    label: 'Settings',
    icon: 'settings',
    route: '/settings',
    roles: ['ROLE_ADMIN'],
  }
];
