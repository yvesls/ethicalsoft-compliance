export interface SidebarItem {
  label: string;
  icon?: string;
  route?: string;
  children?: SidebarItem[];
}

export const SIDEBAR_MENU: SidebarItem[] = [
  {
    label: 'Dashboard',
    icon: 'dashboard',
    route: '/dashboard',
  },
  {
    label: 'General',
    icon: 'category',
    children: [
      { label: 'Components', route: '/components' },
      { label: 'Charts', route: '/charts' },
      { label: 'E-commerce', route: '/ecommerce' },
      { label: 'Maps', route: '/maps' },
      { label: 'Theme', route: '/theme' },
    ],
  },
  {
    label: 'Extra',
    icon: 'extension',
    children: [
      { label: 'Documentation', route: '/documentation' },
      { label: 'Calendar', route: '/calendar' },
      { label: 'Examples', route: '/examples' },
    ],
  },
];
