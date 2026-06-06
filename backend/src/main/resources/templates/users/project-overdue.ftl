<html>
<body>
  <p>Olá ${recipientName!"Administrador"},</p>
  <p>O projeto <strong>${projectName!""}</strong> está <strong>ATRASADO</strong> — a data de encerramento prevista foi <strong>${deadlineFormatted!""}</strong> e ainda existem questionários não concluídos.</p>
  <ul>
    <li>Projeto: ${projectName!""}</li>
    <li>Prazo original: ${deadlineFormatted!""}</li>
    <li>Questionários pendentes: <strong>${pendingQuestionnaires!""}</strong></li>
    <li>Total de questionários: ${totalQuestionnaires!""}</li>
  </ul>
  <p>
    O projeto só poderá ser encerrado e ter seu ISEP consolidado calculado quando <strong>todos</strong> os questionários forem respondidos por 100% dos representantes.
    Você pode encerrar o projeto manualmente, mesmo com questionários pendentes, acessando o painel de gestão.
  </p>
  <#if projectLink??>
    <p>Acesse o projeto: <a href="${projectLink}">${projectLink}</a></p>
  </#if>
</body>
</html>

