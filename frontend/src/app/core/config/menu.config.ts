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
		label: 'sidebar.home',
		icon: 'home',
		route: '/home',
		roles: [RoleEnum.USER],
	},
	{
		label: 'sidebar.projects',
		icon: 'projects',
		route: '/projects',
		roles: [RoleEnum.ADMIN, RoleEnum.USER],
	},
	{
		label: 'sidebar.settings',
		icon: 'settings',
		route: '/settings',
		roles: [RoleEnum.ADMIN, RoleEnum.USER],
	},
]
