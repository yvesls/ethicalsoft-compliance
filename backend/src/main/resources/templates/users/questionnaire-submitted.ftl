<html>
  <body>
    <p>Olá ${recipientName!""},</p>
    <p>${responderName!"Alguém"} respondeu o questionário <strong>${questionnaireName!""}</strong> no projeto <strong>${projectName!""}</strong>.</p>
    <ul>
      <li>Data/hora de envio: ${submittedAtFormatted!""}</li>
      <li>Respondente: ${responderName!""} (${responderEmail!""})</li>
    </ul>
    <p>Para revisar, acesse o projeto em: <a href="${projectLink!""}">${projectLink!""}</a> (${environment!""}).</p>
  </body>
</html>

