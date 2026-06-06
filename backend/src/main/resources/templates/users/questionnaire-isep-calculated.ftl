<html>
<body>
  <p>Olá ${recipientName!"Administrador"},</p>
  <p>O questionário <strong>${questionnaireName!""}</strong> do projeto <strong>${projectName!""}</strong> foi concluído e o <strong>ISEP foi calculado com sucesso</strong>.</p>
  <ul>
    <li>Questionário: ${questionnaireName!""}</li>
    <li>Projeto: ${projectName!""}</li>
    <li>ISEP calculado: <strong>${isepPercent!""}%</strong></li>
    <li>Classificação (Faixa): <strong>${band!""}</strong></li>
    <li>Calculado em: ${calculatedAtFormatted!""}</li>
    <li>Encerrado por: ${closedBy!""}</li>
  </ul>
  <#if ethicsDebtPercent?? || techDebtPercent??>
  <p><strong>Indicadores de Dívida (Governança):</strong></p>
  <ul>
    <#if ethicsDebtPercent??>
      <li>Dívida Ética: <strong>${ethicsDebtPercent}%</strong></li>
    </#if>
    <#if techDebtPercent??>
      <li>Dívida Técnica/Processo: <strong>${techDebtPercent}%</strong></li>
    </#if>
  </ul>
  </#if>
  <p>
    O Índice de Saúde Ética do Projeto (ISEP) representa a conformidade ética ponderada dos membros para este questionário.
    Acesse o painel de gestão para visualizar os detalhes estatísticos (gráficos, heatmap, desvio padrão).
  </p>
  <#if projectLink??>
    <p>Acesse o projeto: <a href="${projectLink}">${projectLink}</a></p>
  </#if>
</body>
</html>

