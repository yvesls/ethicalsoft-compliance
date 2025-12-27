import { RoleEnum } from '../../shared/enums/role.enum'

export interface MenuItem {
	label: string
	icon?: string
	route: string
	roles: RoleEnum[]
	children?: MenuItem[]
}

export const MENU_CONFIG: MenuItem[] = [
	{
		label: 'Home',
		icon: 'home',
		route: '/home',
		roles: [RoleEnum.USER],
	},
	{
		label: 'Projects',
		icon: 'projects',
		route: '/projects',
		roles: [RoleEnum.ADMIN, RoleEnum.USER],
	},
	{
		label: 'Dashboards',
		icon: 'dashboards',
		route: '/dashboards',
		roles: [RoleEnum.ADMIN],
	},
	{
		label: 'Settings',
		icon: 'settings',
		route: '/settings',
		roles: [RoleEnum.ADMIN, RoleEnum.USER],
	},
]
