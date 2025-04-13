import { RoleEnum } from "../../enums/role.enum";

export interface RegisterInterface {
  firstName: string,
  lastName: string,
  email: string,
  password: string,
  acceptedTerms: boolean,
  firstAcess: boolean,
  role: RoleEnum,
}

export const createRegister = (): RegisterInterface => (
  {
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    acceptedTerms: false,
    firstAcess: true,
    role: RoleEnum.ADMIN,
  }
);
