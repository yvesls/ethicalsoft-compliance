export interface PasswordRule {
  message: string;
  test: (value: string) => boolean;
}
