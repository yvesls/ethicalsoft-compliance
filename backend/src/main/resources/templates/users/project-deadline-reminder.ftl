<html>
<body>
  <p>Olá ${recipientName!""},</p>
  <p>O projeto <strong>${projectName}</strong> tem deadline em <strong>${deadlineFormatted}</strong>.</p>
  <p>Faltam ${daysRemaining} dia(s) para o prazo.</p>
  <#if projectLink??>
    <p>Link: <a href="${projectLink}">${projectLink}</a></p>
  </#if>
</body>
</html>

