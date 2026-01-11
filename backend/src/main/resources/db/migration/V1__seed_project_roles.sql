INSERT INTO role (name, description)
VALUES
  ('Desenvolvedor', 'Responsável por implementar as funcionalidades, corrigir bugs, escrever testes e manter a qualidade técnica do código.'),
  ('Gerente de Projeto', 'Responsável por planejar e coordenar o projeto, gerir prazos, riscos, escopo, comunicação e alinhamento entre as partes.'),
  ('Cliente', 'Representa a área solicitante; valida requisitos, aprova entregas e fornece feedback sobre o produto/serviço.'),
  ('Analista de Qualidade', 'Responsável por validar a qualidade do software, definir e executar testes, registrar evidências e garantir conformidade com critérios de aceite.'),
  ('Analista de Requisitos', 'Responsável por levantar, detalhar e validar requisitos, documentar regras de negócio e garantir entendimento comum entre stakeholders.'),
  ('Designer', 'Responsável por conceber a experiência do usuário e a interface, garantindo usabilidade, consistência visual e aderência à identidade do produto.')
ON CONFLICT (name) DO UPDATE
SET description = EXCLUDED.description;
