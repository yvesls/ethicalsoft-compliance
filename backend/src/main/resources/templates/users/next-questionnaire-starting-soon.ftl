<html>
<body>
  <p>Olá ${recipientName!"Administrador"},</p>
  <p>O próximo questionário do projeto <strong>${projectName!""}</strong> será iniciado em breve.</p>
  <ul>
    <li>Próximo questionário: <strong>${nextQuestionnaireName!""}</strong></li>
    <li>Início previsto: ${nextStartDate!""}</li>
    <li>Término previsto: ${nextEndDate!""}</li>
    <li>Dias até o início: ${daysUntilStart!""}</li>
    <li>Questionário encerrado: ${closedQuestionnaireName!""}</li>
  </ul>
  <p>
    Caso deseje <strong>adiantar o início</strong> deste questionário, acesse o painel do projeto e utilize a opção de reagendamento.
  </p>
  <#if projectLink??>
    <p>Acesse o projeto: <a href="${projectLink}">${projectLink}</a></p>
  </#if>
</body>
</html>

