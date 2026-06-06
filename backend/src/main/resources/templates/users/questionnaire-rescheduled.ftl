<html>
<body>
  <p>Olá ${recipientName!"Administrador"},</p>
  <p>O questionário <strong>${questionnaireName!""}</strong> do projeto <strong>${projectName!""}</strong> foi <strong>reagendado</strong>.</p>
  <ul>
    <li>Questionário: ${questionnaireName!""}</li>
    <li>Projeto: ${projectName!""}</li>
    <li>Período anterior: ${oldStartDate!""} até ${oldEndDate!""}</li>
    <li>Novo período: ${newStartDate!""} até ${newEndDate!""}</li>
  </ul>
  <#if deadlineWarning?has_content>
  <p style="color: #c0392b; font-weight: bold;">
    ⚠️ Atenção: ${deadlineWarning}
  </p>
  </#if>
  <p>
    Caso as novas datas afetem a sua participação, entre em contato com o administrador do projeto.
  </p>
  <#if projectLink??>
    <p>Acesse o projeto: <a href="${projectLink}">${projectLink}</a></p>
  </#if>
</body>
</html>

