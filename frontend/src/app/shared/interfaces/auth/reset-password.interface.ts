export interface ResetPasswordInterface {
	email: string
	newPassword: string
	firstAccessFlow?: boolean
}

export const createResetPassword = (): ResetPasswordInterface => ({
	email: '',
	newPassword: '',
	firstAccessFlow: false,
})
