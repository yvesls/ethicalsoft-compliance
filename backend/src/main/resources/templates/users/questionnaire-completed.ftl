<html>
<body>
  <p>Olá ${recipientName!""},</p>
  <p>O questionário <strong>${questionnaireName}</strong> do projeto <strong>${projectName}</strong> foi finalizado/aprovado.</p>
  <ul>
    <li>Status: ${status}</li>
    <li>Finalizado por: ${approvedBy!""}</li>
    <li>Data/hora: ${finishedAtFormatted!""}</li>
  </ul>
  <#if projectLink??>
    <p>Link do projeto: <a href="${projectLink}">${projectLink}</a></p>
  </#if>
</body>
</html>

