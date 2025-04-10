export interface ResetPasswordInterface {
  email: string,
  newPassword: string,
}

export const createResetPassword = (): ResetPasswordInterface => (
  {
    email: '',
    newPassword: ''
  }
);
