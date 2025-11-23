export enum RepresentativeRoleEnum {
  CLIENTE = 'Cliente',
  CONSULTOR = 'Consultor',
  DESENVOLVEDOR = 'Desenvolvedor',
  LIDER_TECNICO = 'Líder Técnico',
  GERENTE_DE_PROJETO = 'Gerente de Projeto',
  ARQUITETO = 'Arquiteto',
  ANALISTA = 'Analista',
  TESTADOR = 'Testador',
  DESIGNER = 'Designer',
  STAKEHOLDER = 'Stakeholder',
}

export const REPRESENTATIVE_ROLE_OPTIONS = [
  { value: RepresentativeRoleEnum.CLIENTE, label: RepresentativeRoleEnum.CLIENTE },
  { value: RepresentativeRoleEnum.CONSULTOR, label: RepresentativeRoleEnum.CONSULTOR },
  { value: RepresentativeRoleEnum.DESENVOLVEDOR, label: RepresentativeRoleEnum.DESENVOLVEDOR },
  { value: RepresentativeRoleEnum.GERENTE_DE_PROJETO, label: RepresentativeRoleEnum.GERENTE_DE_PROJETO },
  { value: RepresentativeRoleEnum.ARQUITETO, label: RepresentativeRoleEnum.ARQUITETO },
  { value: RepresentativeRoleEnum.ANALISTA, label: RepresentativeRoleEnum.ANALISTA },
  { value: RepresentativeRoleEnum.TESTADOR, label: RepresentativeRoleEnum.TESTADOR },
  { value: RepresentativeRoleEnum.DESIGNER, label: RepresentativeRoleEnum.DESIGNER },
  { value: RepresentativeRoleEnum.STAKEHOLDER, label: RepresentativeRoleEnum.STAKEHOLDER },
];
