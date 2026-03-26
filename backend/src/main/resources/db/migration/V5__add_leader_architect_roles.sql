INSERT INTO role (name, description)
VALUES
  ('Líder de Equipe', 'Responsável por coordenar a equipe técnica, distribuir tarefas, garantir aderência a padrões e facilitar a comunicação entre membros.'),
  ('Arquiteto de Software', 'Responsável por definir a arquitetura técnica, padrões de design, decisões estruturais e garantir escalabilidade e manutenibilidade.')
ON CONFLICT (name) DO UPDATE
SET description = EXCLUDED.description;

