<html>
    <body>
      <p>Olá ${firstName},</p>
      <p>Você acabou de ser vinculado ao projeto <strong>${projectName}</strong>.</p>
      <p>Resumo rápido:</p>
      <ul>
        <li>Link direto: <a href="${projectLink}">${projectLink}</a> (${environment})</li>
        <li>Período previsto: ${startDateFormatted} até ${deadlineFormatted}</li>
        <li>Papéis atribuídos: <#if roles?has_content>${roles?join(", ")}<#else>não informado</#if></li>
        <li>Situação atual: <#if timelineSummary??>${timelineSummary}<#else>não definido</#if></li>
        <#if activeQuestionnairesMessage??>
            <li>Questionários em andamento: ${activeQuestionnairesMessage}</li>
        <#else>
            <li>Próximo questionário estará disponível em: ${nextQuestionnaireFormatted}</li>
        </#if>
      </ul>
      <p>Para dúvidas sobre o processo, contate ${adminName} (<a href="mailto:${adminEmail}">${adminEmail}</a>) ou suporte em <a href="mailto:${supportEmail}">${supportEmail}</a>.</p>
    </body>
</html>
