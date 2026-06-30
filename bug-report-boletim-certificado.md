# Bug Report — Boletim de Não Conformidade & Certificado de Conformidade

> Varredura realizada em 2026-06-27. Todos os itens foram verificados diretamente no código-fonte.

---

## Crítico

### BUG-01 — Mismatch entre `BulletinEmissionResult` do backend e interface do frontend

**Impacto:** A mensagem de sucesso exibida ao usuário após emitir o boletim sempre mostrará `undefined` para contagem de envios e código de autenticidade. A ação foi executada — o bug é silencioso na UX.

**Backend retorna** (`EmitNonComplianceBulletinUseCase.java:37`)
```java
record BulletinEmissionResult(String documentCode, int totalRecipients, int sent, int skipped)
```

**Frontend espera** (`document-emission.service.ts:23`)
```ts
interface BulletinEmissionResult {
  emittedCount: number;       // ← campo inexistente no backend (deveria ser `sent`)
  questionnaireId: number;    // ← campo inexistente no backend
  questionnaireName: string;  // ← campo inexistente no backend
  authenticityCode: string;   // ← campo inexistente no backend (deveria ser `documentCode`)
  emittedAt: string;          // ← campo inexistente no backend
}
```

**Componente afetado** (`questionnaire-dashboard-page.component.ts:263`)
```ts
this.translate.instant('dashboard.bulletin.emit_success', {
  count: result.emittedCount,    // → undefined
  code: result.authenticityCode, // → undefined
})
```

**Correção:** Alinhar a interface TypeScript com o contrato real do backend:
```ts
interface BulletinEmissionResult {
  documentCode: string;
  totalRecipients: number;
  sent: number;
  skipped: number;
}
```
E no componente usar `result.documentCode` e `result.sent`.

---

### BUG-02 — Botões de boletim sempre visíveis no dashboard do questionário, sem guard de estado

**Impacto:** Os botões "Baixar PDF", "Emitir para representantes" e "Registrar emissão" do boletim estão **sempre visíveis e clicáveis**, independente de:
- O questionário ter ISEP calculado
- O questionário estar na faixa conforme (banda C ou acima) — quando o boletim não deveria existir
- O status do projeto

**Frontend** (`questionnaire-dashboard-page.component.html:25-48`)
```html
<button (click)="downloadBulletinPdf()">...</button>          <!-- sem @if -->
<button (click)="emitBulletinToRepresentatives()">...</button> <!-- sem @if -->
<button (click)="registerBulletinEmission()">...</button>       <!-- sem @if -->
```

Comparação: no projeto, o botão "Encerrar Projeto" usa `[disabled]="d.projectIsep !== null"` corretamente. Aqui não há nenhuma proteção.

**Correção:** Envolver os botões com uma guarda baseada no campo `band` do dashboard:
```html
@if (d.band && !meetsMinimum(d.band)) {
  <!-- botões do boletim -->
}
```
O método `meetsMinimum` deve replicar a lógica de `EthicalComplianceBand.meetsMinimum()` (banda C ou acima).

---

### BUG-03 — Botões de certificado visíveis sem verificar se o projeto está `CONCLUIDO`

**Impacto:** Um projeto **aberto** que tenha ISEP calculado (calculado manualmente ou via encerramento parcial) exibe os botões de download e registro do certificado. O backend vai rejeitar a requisição com erro 422, mas o usuário vê botões habilitados em estado incorreto.

**Frontend** (`project-dashboard-page.component.html:22`)
```html
@if (d.projectIsep !== null) {
  <button (click)="downloadCertificatePdf()">...</button>
  <button (click)="registerCertificateEmission()">...</button>
}
```

**Problema:** O campo `projectStatus` **não existe** na interface `ProjectIsepDashboardDTO` do frontend, pois o backend não o expõe no DTO.

**Correção em duas partes:**

1. **Backend** — adicionar `projectStatus` ao `ProjectIsepDashboardDTO`:
```java
public record ProjectIsepDashboardDTO(
    // ... campos existentes ...
    String projectStatus  // ← adicionar
) {}
```
E populá-lo no use case correspondente.

2. **Frontend** — adicionar `projectStatus` à interface e usar na guarda:
```html
@if (d.projectIsep !== null && d.projectStatus === 'CONCLUIDO') {
  <!-- botões de certificado -->
}
```

---

### BUG-04 — Boletim pode ser emitido em projeto já `CONCLUIDO`

**Impacto:** Violação de regra de negócio. Após o encerramento do projeto, o ciclo de correções terminou. Emitir boletim de não conformidade em projeto concluído gera documento sem validade operacional e polui a trilha de auditoria.

**Backend** — `GenerateNonComplianceBulletinUseCase.java:87` (`execute`) e `prepareMetadata` não validam status do projeto.

**Contraste** — `GenerateComplianceCertificateUseCase.java:51` faz a validação equivalente:
```java
if (project.getStatus() != ProjectStatusEnum.CONCLUIDO) {
    throw new BusinessException("...");
}
```

**Correção:** Adicionar validação de status em `GenerateNonComplianceBulletinUseCase` em ambos os métodos (`prepareMetadata` e `execute`):
```java
if (project.getStatus() == ProjectStatusEnum.CONCLUIDO) {
    throw new BusinessException(
        "O projeto já foi encerrado. O boletim de não conformidade não pode ser emitido após o encerramento.");
}
```

---

## Médio

### BUG-05 — `hashSourceParts` gera hashes diferentes para o mesmo documento dependendo do caminho de emissão

**Impacto:** O `dataHash` da trilha de auditoria é inconsistente para o mesmo questionário. Impossível comparar registros gerados por caminhos diferentes.

O hash do boletim é calculado com `isepPercent` em dois formatos diferentes conforme o endpoint chamado:

| Endpoint | `isepPercent` passado ao hash | Tipo |
|---|---|---|
| `POST /bulletin/emit` | `bulletin.isepPercent()` | `String` — ex: `"75,23%"` (já formatada por `DocumentFormatUtil.percent()`) |
| `POST /bulletin/register-emission` | `meta.isepPercent()` | `BigDecimal` — ex: `75.23` |

Para um questionário com ISEP 75,23%, os hashes gerados pelos dois endpoints serão diferentes, pois `"75,23%"` ≠ `"75.23"`.

**Correção:** Padronizar o campo `isepPercent` nos `hashSourceParts` sempre como `BigDecimal` (o valor bruto antes de formatar):
```java
// Em EmitNonComplianceBulletinUseCase
Arrays.asList(projectId, questionnaireId, bulletin.band(), bulletin.isepValue()) // isepValue() é BigDecimal
```

---

### BUG-06 — `documentCode` gerado independentemente em `prepareMetadata()` e `execute()`; pode divergir se o ISEP for recalculado entre as chamadas

**Impacto:** O código exibido no preview ao usuário pode ser diferente do código gravado no PDF e na trilha de auditoria. O usuário recebe um PDF com um código de autenticidade diferente do que viu antes de confirmar.

**Código** (`GenerateNonComplianceBulletinUseCase.java`)
```java
// prepareMetadata() — linha 78
String documentCode = DocumentFormatUtil.authenticityCode("BNC",
        project.getId(), questionnaire.getId(), dashboard.band(),
        dashboard.iseqPercent(), dashboard.calculatedAt());  // ← usa calculatedAt

// execute() — linha 149 (código separado, sem reuso)
String documentCode = DocumentFormatUtil.authenticityCode("BNC",
        project.getId(), questionnaire.getId(), dashboard.band(),
        dashboard.iseqPercent(), dashboard.calculatedAt());  // ← calculatedAt pode ser diferente
```

Se um recálculo de ISEP ocorrer entre as duas chamadas, `dashboard.calculatedAt()` muda e os dois códigos divergem.

**O mesmo bug existe no certificado** (`GenerateComplianceCertificateUseCase.java:68` vs `132`).

**Correção:** `prepareMetadata()` deve retornar o `documentCode` como parte do contrato, e o frontend deve passá-lo de volta ao chamar o `execute()`, ou o `execute()` deve receber o código como parâmetro opcional para reuso.

---

### BUG-07 — `@Transactional(readOnly = true)` em método que grava no MongoDB

**Impacto:** Semanticamente incorreto. Em cenários com listeners de transação ou em futuras refatorações que dependam do contrato de read-only, o comportamento pode ser imprevisível.

**Arquivo** `EmitNonComplianceBulletinUseCase.java:41`
```java
@Transactional(readOnly = true)
public BulletinEmissionResult execute(...) {
    // ...
    emailSender.sendWithAttachment(...);         // side effect externo
    registerEmissionUseCase.execute(...);        // grava no MongoDB
}
```

**Correção:** Remover `readOnly = true` ou remover a anotação `@Transactional` inteiramente (MongoDB não participa da transação JPA de qualquer forma):
```java
@Transactional  // ou remover completamente
public BulletinEmissionResult execute(...) { ... }
```

---

### BUG-08 — Representantes excluídos logicamente recebem o boletim por email

**Impacto:** Representantes removidos do projeto (com `deletionDate != null`) continuam sendo incluídos na lista de destinatários do boletim.

**Código** (`EmitNonComplianceBulletinUseCase.java:49`)
```java
List<Representative> representatives = representativeRepository.findByProjectId(projectId);
```

Não há filtro de `deletionDate`. Contraste com `ProjectQuestionnaireAdapter.java` que filtra corretamente:
```java
.filter(r -> r.getDeletionDate() == null)
```

**Correção:** Adicionar filtro após a busca:
```java
List<Representative> representatives = representativeRepository.findByProjectId(projectId)
        .stream()
        .filter(r -> r.getDeletionDate() == null)
        .toList();
```
Ou adicionar o filtro diretamente na query do repositório.

---

### BUG-09 — Falha silenciosa no salvamento de registro de emissão não sinaliza erro ao chamador

**Impacto:** O PDF já foi gerado e o email já foi enviado, mas se o MongoDB falhar ao salvar o `DocumentEmissionRecord`, a operação retorna sucesso e nenhum erro chega ao usuário nem ao frontend. O documento existe sem trilha de auditoria.

**Código** (`RegisterDocumentEmissionUseCase.java:82`)
```java
try {
    return repository.save(doc);
} catch (RuntimeException ex) {
    log.warn("...");
    return doc;  // ← retorna o doc NÃO persistido como se fosse sucesso
}
```

A decisão de silenciar pode ser intencional para não bloquear a emissão por falha de registro. Se for esse o caso, **ao menos o frontend deveria ser informado** — o campo `dataHash` e o `authenticityCode` da resposta seriam do documento não persistido, sem valor de auditoria real.

**Correção sugerida:** Separar a emissão (geração + envio) do registro de auditoria, ou propagar um aviso estruturado ao chamador:
```java
public record SaveResult(DocumentEmissionRecord record, boolean persisted) {}
```

---

## Baixo

### BUG-10 — Chave i18n `emit_success` referencia campos inexistentes após o BUG-01

**Arquivo:** `questionnaire-dashboard-page.component.ts:262`

Mesmo após corrigir o BUG-01, a chave de tradução `dashboard.bulletin.emit_success` usa os placeholders `{count}` e `{code}`. Verificar se o arquivo de tradução usa os nomes corretos alinhados com os campos reais do DTO corrigido.

---

### BUG-11 — `download` do boletim (`GET /bulletin`) não registra a emissão

**Impacto:** O endpoint `GET /questionnaires/{id}/bulletin` gera e retorna o PDF, mas **não chama** `registerEmissionUseCase`. O usuário pode baixar o PDF de boletim sem que haja qualquer rastro de auditoria. Diferente do fluxo de registro explícito (`POST /register-emission`), que é opcional e depende de o usuário clicar no botão.

**Contraste:** O frontend tem botões separados para "Baixar PDF" e "Registrar emissão", mas não os vincula — o download não dispara o registro automaticamente.

**Impacto real:** Baixar o boletim sem registrar emissão deixa a tabela de emissões vazia, podendo dar falsa impressão de que nenhum documento foi emitido.

---

### BUG-12 — Botão "Encerrar Projeto" fica desabilitado permanentemente após ISEP calculado

**Arquivo:** `project-dashboard-page.component.html:43`
```html
[disabled]="closing() || d.projectIsep !== null"
```

Isso impede reencerrar ou acionar o fechamento mesmo após situações de erro. Se o encerramento falhar no meio e o `projectIsep` já estiver parcialmente salvo, o botão ficará permanentemente desabilitado sem que o projeto esteja em estado `CONCLUIDO`.

**Correção:** Usar `d.projectStatus === 'CONCLUIDO'` (após corrigir o BUG-03) para desabilitar, não a presença do ISEP.

---

## Resumo

| ID | Severidade | Arquivo principal | Descrição curta |
|---|---|---|---|
| BUG-01 | Crítico | `document-emission.service.ts:23` | Interface `BulletinEmissionResult` com campos errados |
| BUG-02 | Crítico | `questionnaire-dashboard-page.component.html:25` | Botões de boletim sempre visíveis sem guard de banda |
| BUG-03 | Crítico | `project-dashboard-page.component.html:22` | Botões de certificado sem verificação de status `CONCLUIDO` |
| BUG-04 | Crítico | `GenerateNonComplianceBulletinUseCase.java:87` | Boletim emissível em projeto já encerrado |
| BUG-05 | Médio | `EmitNonComplianceBulletinUseCase.java:97` | `dataHash` diverge entre endpoints para mesmo documento |
| BUG-06 | Médio | `GenerateNonComplianceBulletinUseCase.java:78,149` | `documentCode` gerado duas vezes; pode divergir entre preview e emissão |
| BUG-07 | Médio | `EmitNonComplianceBulletinUseCase.java:41` | `@Transactional(readOnly=true)` em método que grava |
| BUG-08 | Médio | `EmitNonComplianceBulletinUseCase.java:49` | Representantes excluídos recebem email de boletim |
| BUG-09 | Médio | `RegisterDocumentEmissionUseCase.java:82` | Falha de salvamento silenciosa; PDF emitido sem auditoria |
| BUG-10 | Baixo | `questionnaire-dashboard-page.component.ts:262` | Chave i18n com placeholders desalinhados |
| BUG-11 | Baixo | `ComplianceDocumentController.java:34` | Download de PDF não registra emissão automaticamente |
| BUG-12 | Baixo | `project-dashboard-page.component.html:43` | Botão "Encerrar Projeto" desabilitado permanentemente pelo ISEP |
