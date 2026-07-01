EthicalSoft Compliance

Plataforma web para acompanhamento da conformidade ética em projetos de engenharia de software.

Trabalho de Conclusão de Curso — Engenharia de Software

Autor: Yves Silva

Repositório: github.com/yvesls/ethicalsoft-compliance


Versão avaliada

ItemValorBranchmainCommit avaliado4175115dVersão0.0.1-SNAPSHOTData da última execução dos testes30 de junho de 2026Resultado da suíte automatizada43 testes, 0 falhas, 0 erros

Para obter exatamente a versão avaliada na banca:

bashgit checkout 4175115d


Demonstração ao vivo

O sistema está disponível no endereço abaixo durante o período de avaliação:


https://unsubtly-tables-flattery.ngrok-free.dev/




O endereço acima utiliza túnel Ngrok e pode expirar. Caso esteja inacessível, siga as instruções de execução local na seção abaixo. As credenciais de demonstração funcionam em ambos os ambientes.




Credenciais de demonstração para a banca

Perfil Administrador (acesso completo)

CampoValorE-mailadmin@ethicalsoft.demoSenhaDemo@2026

Perfil Representante (acesso restrito à resposta de questionários)

CampoValorE-mailrepresentante@ethicalsoft.demoSenhaDemo@2026


O projeto demonstrativo SelecIA RH (sistema fictício de recrutamento com IA) já está configurado e com o ciclo completo de 4 sprints encerrado. O certificado de conformidade e os boletins de não conformidade estão disponíveis para visualização imediata após o login.




Estrutura do repositório

ethicalsoft-compliance/
├── backend/          # API REST — Java 21 + Spring Boot 3.4.2
├── frontend/         # Interface web — Angular 22
├── docker-compose.yml
├── .env.example      # Variáveis de ambiente sem segredos
└── README.md


Pré-requisitos

FerramentaVersão mínimaJava (JDK)21Maven3.9+Node.js20+Angular CLI22+Docker24+Docker Compose2.20+


Configuração do ambiente

Copie o arquivo de exemplo e preencha os valores antes de executar:

bashcp .env.example .env

O arquivo .env.example contém todas as variáveis necessárias com descrição e sem segredos. Veja a lista completa na seção Variáveis de ambiente.


Execução com Docker Compose (infraestrutura)

Sobe o PostgreSQL 16, o MongoDB 8 e o Nginx:

bashdocker compose up -d

O backend e o frontend podem ser executados fora do contêiner durante o desenvolvimento. Para parar:

bashdocker compose down


Execução do backend

bashcd backend
./mvnw clean install -DskipTests
./mvnw spring-boot:run

A API ficará disponível em http://localhost:8080.

As migrações Flyway são executadas automaticamente na inicialização.


Execução do frontend

bashcd frontend
npm install
ng serve

O frontend ficará disponível em http://localhost:4200.

A URL base da API é configurável em src/environments/environment.ts.


Execução dos testes automatizados

bashcd backend
./mvnw test

O relatório de cobertura JaCoCo é gerado em:

backend/target/site/jacoco/index.html

Para executar os testes e gerar o relatório em sequência:

bash./mvnw clean verify

Resultado esperado na versão avaliada (commit 4175115d):

Tests run: 43, Failures: 0, Errors: 0, Skipped: 0


Migrações do banco de dados

As migrações SQL do PostgreSQL estão versionadas com Flyway em:

backend/src/main/resources/db/migration/

São executadas automaticamente ao iniciar o backend. Para inspecionar o histórico de migrações aplicadas, acesse a tabela flyway_schema_history no banco.

O MongoDB não possui migrações controladas por ferramenta; as coleções são criadas automaticamente pelo Spring Data MongoDB na primeira gravação.


Variáveis de ambiente

O arquivo .env.example contém as seguintes variáveis. Preencha no .env local antes de executar:

dotenv# ── PostgreSQL ────────────────────────────────────────────
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ethicalsoft
SPRING_DATASOURCE_USERNAME=seu_usuario
SPRING_DATASOURCE_PASSWORD=sua_senha

# ── MongoDB ───────────────────────────────────────────────
SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/ethicalsoft

# ── JWT ───────────────────────────────────────────────────
# Chave secreta para assinatura dos tokens de acesso.
# Use uma string aleatória de ao menos 64 caracteres.
JWT_SECRET=substitua_por_chave_segura

# ── Google OAuth ──────────────────────────────────────────
# Client ID gerado no Google Cloud Console.
GOOGLE_CLIENT_ID=substitua_pelo_client_id

# ── SMTP (envio de e-mails) ───────────────────────────────
SPRING_MAIL_HOST=smtp.exemplo.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=seu_email
SPRING_MAIL_PASSWORD=sua_senha_smtp

# ── Criptografia do token de IA ───────────────────────────
# Chave AES usada para criptografar os tokens pessoais de IA dos usuários.
# Use uma string aleatória de ao menos 32 caracteres.
ENCRYPTION_KEY=substitua_por_chave_segura

# ── Inteligência artificial (opcional) ───────────────────
# Os recursos de IA são opcionais. Sem token configurado,
# os widgets de IA exibem mensagem de indisponibilidade.
# Cada usuário pode configurar seu próprio token na tela de Configurações.
# Não é necessário configurar uma chave global aqui.


Segurança: nunca versione o arquivo .env com valores reais. Ele está listado no .gitignore.




Tecnologias principais

CamadaTecnologiaFrontendAngular 22, Angular Material, ngx-echartsBackendJava 21, Spring Boot 3.4.2, Arquitetura HexagonalBanco relacionalPostgreSQL 16 (migrações via Flyway)Banco de documentosMongoDB 8AutenticaçãoJWT (java-jwt), Google Identity ServicesInteligência artificialSpring AI + Groq Cloud (OpenAI-compatible), Resilience4jGeração de PDFOpenHTMLtoPDF 1.0.10 + FreeMarker (licença Apache 2.0)ExportaçãoApache Commons CSV, GsonTestesJUnit 5, Mockito, JaCoCo 0.8.11Análise estáticaSonarQube CloudInfraestruturaDocker Compose, Nginx, Ngrok


Observações sobre os recursos de IA

Os recursos de inteligência artificial (explicação de resultados, análise de justificativas, relatório de risco e chat contextual) têm função interpretativa e assistiva. Eles não participam do cálculo do ICP, ISEQ, ISEP ou IEM, não substituem a decisão dos participantes e não são utilizados como fundamento para emissão de documentos de conformidade.

Para utilizar os widgets de IA, cada usuário deve configurar seu próprio token de acesso ao Groq Cloud na tela Configurações → Token de IA. Sem token configurado, os widgets exibem mensagem de indisponibilidade sem afetar as demais funcionalidades.


Licença

Este projeto foi desenvolvido como Trabalho de Conclusão de Curso e não possui licença de distribuição comercial.


EthicalSoft Compliance — Plataforma Web para Conformidade Ética em Engenharia de Software
