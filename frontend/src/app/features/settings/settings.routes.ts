import { Routes } from '@angular/router'
import { SettingsComponent } from './settings.component'
import { SettingsResetPasswordComponent } from './settings-reset-password/settings-reset-password.component'
import { RoleEnum } from '../../shared/enums/role.enum'

export const SETTINGS_ROUTES: Routes = [
	{
		path: '',
		component: SettingsComponent,
		data: {
			roles: [RoleEnum.USER],
			showLayout: true,
		},
	},
	{
		path: 'reset-password',
		component: SettingsResetPasswordComponent,
		data: {
			roles: [RoleEnum.USER],
			showLayout: true,
		},
	},
	{
		path: 'ai-token',
		loadComponent: () =>
			import('./components/ai-token-settings/ai-token-settings.component').then(
				(m) => m.AiTokenSettingsComponent
			),
		data: {
			roles: [RoleEnum.USER],
			showLayout: true,
		},
	},
]
