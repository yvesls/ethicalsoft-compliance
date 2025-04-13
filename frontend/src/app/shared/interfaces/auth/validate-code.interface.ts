export interface ValidateCodeInterface {
	email: string
	code: string
}

export const createValidateCode = (): ValidateCodeInterface => ({
	email: '',
	code: '',
})
