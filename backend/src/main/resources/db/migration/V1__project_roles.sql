CREATE TABLE IF NOT EXISTS role (
  role_id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL UNIQUE,
  description TEXT
);

INSERT INTO role (name, description)
VALUES
  ('Desenvolvedor', 'Responsável por implementar as funcionalidades, corrigir bugs, escrever testes e manter a qualidade técnica do código.'),
  ('Gerente de Projeto', 'Responsável por planejar e coordenar o projeto, gerir prazos, riscos, escopo, comunicação e alinhamento entre as partes.'),
  ('Cliente', 'Representa a área solicitante; valida requisitos, aprova entregas e fornece feedback sobre o produto/serviço.'),
  ('Analista de Qualidade', 'Responsável por validar a qualidade do software, definir e executar testes, registrar evidências e garantir conformidade com critérios de aceite.'),
  ('Analista de Requisitos', 'Responsável por levantar, detalhar e validar requisitos, documentar regras de negócio e garantir entendimento comum entre stakeholders.'),
  ('Designer', 'Responsável por conceber a experiência do usuário e a interface, garantindo usabilidade, consistência visual e aderência à identidade do produto.'),
  ('Responsável do Negócio', 'Responsável por definir prioridades do negócio, aprovar requisitos críticos e garantir alinhamento com os objetivos estratégicos.'),
  ('Suporte', 'Responsável por atender usuários, registrar incidentes, orientar uso correto e retroalimentar melhorias com base nas ocorrências.'),
  ('Stakeholder', 'Parte interessada que influencia ou é impactada pelo projeto; fornece expectativas, valida decisões e acompanha resultados.')
ON CONFLICT (name) DO UPDATE
SET description = EXCLUDED.description;

