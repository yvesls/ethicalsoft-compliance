<#--
  Template de PDF: Certificado de Ciência Ética e Conformidade Declarada (BR07 / UC08).
  Processado via FreeMarkerTemplateUtils; o HTML resultante deve ser convertido em PDF.
  Atende tanto o certificado de iteração/etapa quanto o consolidado, via flag 'consolidado'.
  Variáveis esperadas: ver docs/pdf-templates-boletim-certificado.md
-->
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8"/>
<title>${documentTitle!"Certificado de Ciência Ética e Conformidade Declarada"}</title>
<style>
  @page { size: A4 landscape; margin: 1.4cm; }
  * { box-sizing: border-box; }
  body { font-family: "Helvetica Neue", Arial, sans-serif; color: #1f2933; font-size: 12px; }
  .frame { border: 3px solid #1e7d34; padding: 26px 40px; height: 100%; }
  .frame-inner { border: 1px solid #b7d9bf; padding: 24px 34px; height: 100%; }
  .cert-header { text-align: center; border-bottom: 1px solid #cbd2d9; padding-bottom: 12px; }
  .cert-header .system { font-size: 11px; letter-spacing: 0.18em; text-transform: uppercase; color: #52606d; }
  .cert-header h1 { font-size: 30px; margin: 8px 0 2px; color: #1e7d34; letter-spacing: 0.04em; }
  .cert-header .scope { font-size: 11px; color: #52606d; text-transform: uppercase; letter-spacing: 0.08em; }
  .declaration { text-align: center; font-size: 14px; line-height: 1.7; margin: 22px 8% 18px; color: #323f4b; }
  .declaration strong { color: #1f2933; }
  .project-name { display: block; font-size: 19px; font-weight: bold; color: #1e7d34; margin: 6px 0; }
  .results { display: table; width: 100%; margin: 8px 0 14px; }
  .results .cell { display: table-cell; width: 25%; padding: 8px; text-align: center; border-right: 1px solid #e1e5ea; }
  .results .cell:last-child { border-right: none; }
  .results .cell .value { font-size: 22px; font-weight: bold; }
  .results .cell .caption { font-size: 9px; text-transform: uppercase; color: #9aa5b1; letter-spacing: 0.05em; }
  .band { display: inline-block; padding: 2px 10px; border-radius: 3px; font-weight: bold; color: #fff; }
  .band-A { background: #1e7d34; } .band-B { background: #5a9e1f; } .band-C { background: #c98a00; }
  .band-D { background: #d9601a; } .band-E { background: #b3261e; }
  table.scores { width: 70%; margin: 0 auto; border-collapse: collapse; font-size: 10px; }
  table.scores td { padding: 3px 8px; border: 1px solid #e1e5ea; }
  table.scores td.k { color: #52606d; background: #f0f4f8; }
  table.iter { width: 100%; border-collapse: collapse; margin-top: 6px; font-size: 10px; }
  table.iter th { background: #f0f4f8; padding: 5px 8px; border: 1px solid #cbd2d9; text-align: left; color: #52606d; }
  table.iter td { padding: 5px 8px; border: 1px solid #e1e5ea; }
  .section-label { font-size: 10px; text-transform: uppercase; letter-spacing: 0.08em; color: #9aa5b1;
                   text-align: center; margin: 14px 0 4px; }
  .cert-footer { display: table; width: 100%; margin-top: 26px; }
  .cert-footer .col { display: table-cell; width: 50%; vertical-align: bottom; font-size: 10px; color: #52606d; }
  .cert-footer .col.right { text-align: right; }
  .sign-line { border-top: 1px solid #1f2933; width: 70%; margin-top: 30px; padding-top: 4px; }
  .auth-code { font-family: "Courier New", monospace; font-size: 13px; font-weight: bold; color: #1f2933;
               background: #f0f4f8; padding: 4px 10px; border: 1px dashed #9aa5b1; display: inline-block; }
</style>
</head>
<body>
<div class="frame">
<div class="frame-inner">

  <div class="cert-header">
    <div class="system">${systemName!"EthicalSoft Compliance"}</div>
    <h1>${documentTitle!"Certificado de Ciência Ética e Conformidade Declarada"}</h1>
    <#if consolidado?? && consolidado>
      <div class="scope">Certificado Consolidado do Projeto</div>
    <#else>
      <div class="scope">${scopeLabel!"Iteração"} Avaliada: ${scopeName!"N/D"}</div>
    </#if>
  </div>

  <div class="declaration">
    Certifica-se que o projeto
    <span class="project-name">${projectName!"N/D"}<#if projectCode??> (${projectCode})</#if></span>
    <#if consolidado?? && consolidado>
      concluiu seu ciclo de avaliação ética e <strong>atingiu ou superou a faixa mínima aceitável</strong>
      de conformidade ética em todas as iterações ou etapas consideradas,
    <#else>
      teve a ${scopeLabel!"iteração"} <strong>${scopeName!"N/D"}</strong> avaliada e
      <strong>atingiu ou superou a faixa mínima aceitável</strong> de conformidade ética,
    </#if>
    obtendo o Índice de Saúde Ética do Projeto (ISEP) registrado abaixo.
  </div>

  <div class="results">
    <div class="cell">
      <div class="value">${isepPercent!"--"}%</div>
      <div class="caption">ISEP obtido</div>
    </div>
    <div class="cell">
      <div class="value"><span class="band band-${band!"A"}">${band!"A"}</span></div>
      <div class="caption">${bandLabel!"Faixa de conformidade"}</div>
    </div>
    <div class="cell">
      <div class="value">${minimumBand!"--"}</div>
      <div class="caption">Faixa mínima exigida</div>
    </div>
    <div class="cell">
      <div class="value">${teamStandardDeviationPercent!"--"}%</div>
      <div class="caption">Desvio padrão da equipe</div>
    </div>
  </div>

  <#if consolidado?? && consolidado>
    <div class="section-label">Iterações ou Etapas Concluídas e Aprovadas</div>
    <#if iterations?? && iterations?size gt 0>
      <table class="iter">
        <thead><tr><th>Iteração / Etapa</th><th>ISEP</th><th>Faixa</th></tr></thead>
        <tbody>
          <#list iterations as it>
          <tr>
            <td>${it.name!"N/D"}</td>
            <td>${it.isepPercent!"--"}%</td>
            <td><span class="band band-${it.band!"A"}">${it.band!"A"}</span> ${it.bandLabel!""}</td>
          </tr>
          </#list>
        </tbody>
      </table>
      <p style="font-size:10px; color:#52606d; text-align:center; margin-top:6px;">
        Questionários concluídos: <strong>${approvedQuestionnaires!"--"}</strong> de
        <strong>${totalQuestionnaires!"--"}</strong>.
      </p>
    </#if>
  <#else>
    <div class="section-label">Scores por Domínio de Governança</div>
    <table class="scores">
      <tr>
        <td class="k">Ética</td><td>${ethicsScorePercent!"N/D"}%</td>
        <td class="k">Processo</td><td>${processScorePercent!"N/D"}%</td>
      </tr>
      <tr>
        <td class="k">Equidade / Fairness</td><td>${fairnessScorePercent!"N/D"}%</td>
        <td class="k">ESG</td><td>${esgScorePercent!"N/D"}%</td>
      </tr>
    </table>
  </#if>

  <div style="margin: 18px 6% 6px; padding: 10px 14px; border: 1px solid #cbd2d9;
              background: #f7f9fb; font-size: 9.5px; color: #52606d; line-height: 1.5; text-align: justify;">
    <strong>Declaração de limites deste certificado.</strong>
    Este documento registra o resultado consolidado da autoavaliação ética realizada na
    plataforma ${systemName!"EthicalSoft Compliance"}, com base nas respostas, justificativas
    e evidências disponíveis no momento da emissão. O documento <strong>não substitui</strong>
    auditoria externa, certificação normativa independente, avaliação jurídica ou revisão
    ética institucional, e tampouco constitui parecer regulatório. A responsabilidade pelas
    informações declaradas é do projeto e dos seus participantes.
  </div>

  <div class="cert-footer">
    <div class="col">
      <div>Período avaliado: <strong>${periodFormatted!"N/D"}</strong></div>
      <div>ISEP calculado em: <strong>${calculatedAtFormatted!"N/D"}</strong></div>
      <div>Tipo de projeto: <strong>${projectType!"N/D"}</strong></div>
      <div style="margin-top:10px;">
        Código de autenticidade:<br/>
        <span class="auth-code">${certificateCode!"N/D"}</span>
      </div>
      <#if validationUrl??>
        <div style="margin-top:4px;">Validação por terceiros: ${validationUrl}</div>
      </#if>
    </div>
    <div class="col right">
      <div class="sign-line" style="margin-left:auto;">
        ${issuedBy!"Responsável pela emissão"}<br/>
        <span style="font-size:9px; color:#9aa5b1;">Analista de Qualidade (ADMIN)</span>
      </div>
      <div style="margin-top:8px;">Emitido em ${issuedAtFormatted!"N/D"}</div>
    </div>
  </div>

</div>
</div>
</body>
</html>
