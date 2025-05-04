export interface PasswordRecoveryInterface {
	email: string
}

export const createPasswordRecovery = (): PasswordRecoveryInterface => ({
	email: '',
})
