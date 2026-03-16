<html>
<body>
  <p>Olá ${recipientName!"Participante"},</p>
  <p>O projeto <strong>${projectName!""}</strong> foi <strong>encerrado com sucesso</strong> e o <strong>ISEP Consolidado foi calculado</strong>.</p>
  <ul>
    <li>Projeto: ${projectName!""}</li>
    <li>ISEP Consolidado: <strong>${isepPercent!""}%</strong></li>
    <li>Classificação (Faixa): <strong>${band!""}</strong></li>
    <li>Questionários processados: ${totalQuestionnaires!""}</li>
    <li>Calculado em: ${calculatedAtFormatted!""}</li>
    <li>Encerrado por: ${closedBy!""}</li>
  </ul>
  <p>
    O Índice de Saúde Ética do Projeto (ISEP) consolidado representa a conformidade ética ponderada de todos os questionários ao longo do ciclo de vida do projeto.
    Acesse o painel de gestão para visualizar os detalhes estatísticos completos (evolução, heatmap, distribuição por faixa).
  </p>
  <#if projectLink??>
    <p>Acesse o projeto: <a href="${projectLink}">${projectLink}</a></p>
  </#if>
</body>
</html>

