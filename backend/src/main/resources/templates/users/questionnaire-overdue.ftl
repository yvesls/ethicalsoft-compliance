<html>
<body>
  <p>Olá ${recipientName!"Administrador"},</p>
  <p>O questionário <strong>${questionnaireName!""}</strong> do projeto <strong>${projectName!""}</strong> foi marcado como <strong>ATRASADO</strong>.</p>
  <ul>
    <li>Questionário: ${questionnaireName!""}</li>
    <li>Projeto: ${projectName!""}</li>
    <li>Data de expiração: ${expiredAtFormatted!""}</li>
    <li>Respondentes pendentes: ${pendingCount!""}</li>
    <li>Total de respondentes: ${totalCount!""}</li>
  </ul>
  <p>
    Nem todos os membros vinculados responderam dentro do prazo estabelecido.<br/>
    O status do projeto foi atualizado para <strong>ATRASADO</strong>.
  </p>
  <p>
    Você pode encerrar este questionário manualmente, mesmo com respostas pendentes, acessando o painel de gestão do projeto.
  </p>
  <#if projectLink??>
    <p>Acesse o projeto: <a href="${projectLink}">${projectLink}</a></p>
  </#if>
</body>
</html>

