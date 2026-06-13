<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8"/>
<title>${documentTitle!"Boletim de Não Conformidade Ética"}</title>
<style>
  @page { size: A4 portrait; margin: 2cm 1.8cm; }
  * { box-sizing: border-box; }
  body { font-family: "Helvetica Neue", Arial, sans-serif; color: #1f2933; font-size: 11px; line-height: 1.5; }
  h1 { font-size: 18px; margin: 0; color: #b3261e; }
  h2 { font-size: 12px; text-transform: uppercase; letter-spacing: 0.06em; color: #52606d;
       border-bottom: 1px solid #cbd2d9; padding-bottom: 4px; margin: 22px 0 10px; }
  .doc-header { border-bottom: 3px solid #b3261e; padding-bottom: 12px; margin-bottom: 6px; }
  .doc-header .subtitle { color: #52606d; font-size: 10px; margin-top: 2px; }
  .meta { width: 100%; margin-top: 8px; font-size: 10px; color: #52606d; }
  .meta td { padding: 1px 0; }
  .meta td.label { color: #9aa5b1; width: 130px; }
  table.data { width: 100%; border-collapse: collapse; margin-top: 4px; }
  table.data th { background: #f0f4f8; text-align: left; padding: 6px 8px; font-size: 10px;
                  text-transform: uppercase; letter-spacing: 0.04em; color: #52606d; border: 1px solid #cbd2d9; }
  table.data td { padding: 6px 8px; border: 1px solid #e1e5ea; vertical-align: top; }
  table.data tr:nth-child(even) td { background: #fafbfc; }
  .summary { display: table; width: 100%; margin-top: 4px; }
  .summary .cell { display: table-cell; width: 25%; padding: 10px; border: 1px solid #e1e5ea; text-align: center; }
  .summary .cell .value { font-size: 20px; font-weight: bold; }
  .summary .cell .caption { font-size: 9px; text-transform: uppercase; color: #9aa5b1; letter-spacing: 0.05em; }
  .band { display: inline-block; padding: 2px 8px; border-radius: 3px; font-weight: bold; color: #fff; font-size: 10px; }
  .band-A { background: #1e7d34; } .band-B { background: #5a9e1f; } .band-C { background: #c98a00; }
  .band-D { background: #d9601a; } .band-E { background: #b3261e; }
  .status-flag { background: #fdeceb; border-left: 4px solid #b3261e; padding: 10px 12px; margin-top: 8px; color: #8a1d16; }
  ul.actions { margin: 6px 0 0; padding-left: 18px; }
  ul.actions li { margin-bottom: 4px; }
  .footer { margin-top: 28px; border-top: 1px solid #cbd2d9; padding-top: 8px; font-size: 9px; color: #9aa5b1; }
  .empty { color: #9aa5b1; font-style: italic; padding: 8px 0; }
</style>
</head>
<body>

  <div class="doc-header">
    <h1>${documentTitle!"Boletim de Não Conformidade Ética"}</h1>
    <div class="subtitle">${systemName!"EthicalSoft Compliance"} — Governança Ética em Engenharia de Software</div>
    <table class="meta">
      <tr><td class="label">Código do boletim</td><td>${documentCode!"N/D"}</td></tr>
      <tr><td class="label">Gerado em</td><td>${generatedAtFormatted!"N/D"}</td></tr>
      <tr><td class="label">Gerado por</td><td>${generatedBy!"N/D"}</td></tr>
    </table>
  </div>

  <h2>Identificação do Projeto</h2>
  <table class="meta">
    <tr><td class="label">Projeto</td><td>${projectName!"N/D"} <#if projectCode??>(${projectCode})</#if></td></tr>
    <tr><td class="label">Tipo de estrutura</td><td>${projectType!"N/D"}</td></tr>
    <tr><td class="label">Escopo avaliado</td><td>${scopeLabel!"Escopo"}: ${scopeName!"N/D"}</td></tr>
    <tr><td class="label">Período de aplicação</td><td>${periodFormatted!"N/D"}</td></tr>
    <tr><td class="label">ISEP calculado em</td><td>${calculatedAtFormatted!"N/D"}</td></tr>
  </table>

  <h2>Resumo de Conformidade</h2>
  <div class="summary">
    <div class="cell">
      <div class="value">${isepPercent!"--"}%</div>
      <div class="caption">ISEP do escopo</div>
    </div>
    <div class="cell">
      <div class="value"><span class="band band-${band!"E"}">${band!"E"}</span></div>
      <div class="caption">${bandLabel!"Faixa obtida"}</div>
    </div>
    <div class="cell">
      <div class="value">${minimumBand!"--"}</div>
      <div class="caption">Faixa mínima exigida<#if minimumBandLabel??> (${minimumBandLabel})</#if></div>
    </div>
    <div class="cell">
      <div class="value">${teamStandardDeviationPercent!"--"}%</div>
      <div class="caption">Desvio padrão da equipe</div>
    </div>
  </div>
  <div class="status-flag">
    O escopo avaliado <strong>não atingiu a faixa mínima de conformidade definida</strong>.
    O sistema <strong>recomenda</strong> a revisão das perguntas não conformes, a reavaliação das
    justificativas e o registro das ações corretivas antes do encerramento da etapa ou iteração.
    A decisão final sobre prosseguir ou bloquear o ciclo é da organização responsável pelo projeto.
    Média simples da equipe: <strong>${teamAveragePercent!"--"}%</strong>.
  </div>

  <h2>Membros em Não Conformidade</h2>
  <#if nonCompliantMembers?? && nonCompliantMembers?size gt 0>
    <table class="data">
      <thead>
        <tr><th>Representante</th><th>Encargo</th><th>ICP</th><th>Faixa</th></tr>
      </thead>
      <tbody>
        <#list nonCompliantMembers as member>
        <tr>
          <td>${member.representativeName!"N/D"}</td>
          <td>${member.role!"N/D"}</td>
          <td>${member.icpPercent!"--"}%</td>
          <td><span class="band band-${member.band!"E"}">${member.band!"E"}</span> ${member.bandLabel!""}</td>
        </tr>
        </#list>
      </tbody>
    </table>
  <#else>
    <div class="empty">Nenhum membro individual abaixo da faixa mínima foi identificado.</div>
  </#if>

  <h2>Perguntas em Não Conformidade</h2>
  <#if nonCompliantQuestions?? && nonCompliantQuestions?size gt 0>
    <table class="data">
      <thead>
        <tr><th>Domínio / Etapa</th><th>Pergunta</th><th>SIM</th><th>NÃO</th><th>Conformidade</th></tr>
      </thead>
      <tbody>
        <#list nonCompliantQuestions as q>
        <tr>
          <td>${q.domain!"N/D"}</td>
          <td>${q.questionText!"N/D"}</td>
          <td>${q.yesCount!0}</td>
          <td>${q.noCount!0}</td>
          <td>${q.compliancePercent!"--"}%</td>
        </tr>
        </#list>
      </tbody>
    </table>
  <#else>
    <div class="empty">Nenhuma pergunta específica abaixo do limite mínimo foi listada.</div>
  </#if>

  <h2>Análise de Impacto dos Desvios</h2>
  <table class="data">
    <thead>
      <tr><th>Indicador</th><th>Valor</th></tr>
    </thead>
    <tbody>
      <tr><td>Dívida Ética</td><td>${ethicsDebtPercent!"N/D"}%</td></tr>
      <tr><td>Dívida Técnica / Processo</td><td>${techDebtPercent!"N/D"}%</td></tr>
      <tr><td>Conformidade Ética (domínio)</td><td>${ethicsScorePercent!"N/D"}%</td></tr>
      <tr><td>Conformidade de Processo (domínio)</td><td>${processScorePercent!"N/D"}%</td></tr>
      <tr><td>Conformidade de Equidade / Fairness (domínio)</td><td>${fairnessScorePercent!"N/D"}%</td></tr>
      <tr><td>Conformidade ESG (domínio)</td><td>${esgScorePercent!"N/D"}%</td></tr>
    </tbody>
  </table>
  <#if impactSummary??>
    <p style="margin-top:10px;">${impactSummary}</p>
  </#if>

  <#if aiUsageDeclared?? && aiUsageDeclared>
    <h2>Riscos de Desenvolvimento Assistido por IA</h2>
    <p style="margin-top:4px;">
      Este projeto declarou utilizar ferramentas de inteligência artificial no processo de
      desenvolvimento<#if aiUsageScopesLabel??> (<strong>${aiUsageScopesLabel}</strong>)</#if>.
      Em escopos que não atingem a faixa mínima, os seguintes riscos específicos devem ser
      considerados na revisão e nas ações corretivas:
    </p>
    <ul class="actions">
      <li>Ausência ou insuficiência de revisão humana sobre artefatos gerados por IA.</li>
      <li>Ausência de testes sobre código produzido com apoio de assistentes baseados em LLMs.</li>
      <li>Risco de exposição de dados sensíveis, credenciais ou informações internas em
          ferramentas externas de IA.</li>
      <li>Perda de rastreabilidade dos artefatos quando não há registro de quais trechos foram
          sugeridos ou produzidos com apoio de IA.</li>
      <li>Uso de código sem verificação adequada de segurança, qualidade ou aderência aos
          critérios do projeto.</li>
      <li>Dependência excessiva de sugestões automatizadas em decisões arquiteturais sem
          justificativa explícita da equipe.</li>
    </ul>
  </#if>

  <h2>Orientações para Revisão e Correção</h2>
  <#if correctiveActions?? && correctiveActions?size gt 0>
    <ul class="actions">
      <#list correctiveActions as action>
      <li>${action}</li>
      </#list>
    </ul>
  <#else>
    <ul class="actions">
      <li>Revisar com a equipe as perguntas listadas como não conformes e as respectivas justificativas.</li>
      <li>Submeter novamente as respostas corrigidas dentro do prazo estabelecido.</li>
      <li>Solicitar o recálculo dos resultados para verificar se a classificação mínima foi atingida.</li>
    </ul>
  </#if>
  <#if reviewDeadlineFormatted??>
    <p style="margin-top:8px;"><strong>Prazo para revisão:</strong> ${reviewDeadlineFormatted}.</p>
  </#if>

  <div class="footer">
    Documento gerado automaticamente pelo ${systemName!"EthicalSoft Compliance"}.
    Este boletim integra a trilha de auditoria do projeto e deve ser arquivado para consulta futura.
    Código de referência: ${documentCode!"N/D"}.
  </div>

</body>
</html>
