<html>
  <body>
    <p>Olá ${recipientName!"participante"},</p>
    <p>O questionário <strong>${questionnaireName}</strong> do projeto <strong>${projectName}</strong> está aberto e aguarda a sua resposta.</p>
    <p>Período de aplicação: <strong>${period}</strong></p>
    <#if projectLink??>
      <p>Acesse o projeto e responda o questionário: <a href="${projectLink}">${projectLink}</a></p>
    </#if>
    <p>Caso ainda não tenha feito login, acesse <a href="${frontendBaseUrl!''}/login">${frontendBaseUrl!''}/login</a> e responda o quanto antes.</p>
    <p>Obrigado!</p>
  </body>
</html>
