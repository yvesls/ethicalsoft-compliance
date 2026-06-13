<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8"/>
  <title>Boletim de Não Conformidade Ética</title>
</head>
<body style="font-family: Arial, sans-serif; color:#1f2933; max-width:640px; margin:0 auto; padding:24px;">
  <h2 style="color:#b3261e; margin-bottom:8px;">Boletim de Não Conformidade Ética emitido</h2>
  <p>Olá, <strong>${recipientName!"participante"}</strong>.</p>

  <p>
    Foi emitido um Boletim de Não Conformidade Ética relativo ao questionário
    <strong>${questionnaireName!"N/D"}</strong> do projeto <strong>${projectName!"N/D"}</strong>.
  </p>

  <table style="border-collapse:collapse; margin: 14px 0;">
    <tr>
      <td style="padding:4px 12px; color:#52606d;">ISEP do escopo</td>
      <td style="padding:4px 12px; font-weight:bold;">${isepPercent!"--"}%</td>
    </tr>
    <tr>
      <td style="padding:4px 12px; color:#52606d;">Faixa obtida</td>
      <td style="padding:4px 12px; font-weight:bold; color:#b3261e;">${band!"E"}</td>
    </tr>
    <tr>
      <td style="padding:4px 12px; color:#52606d;">Faixa mínima exigida</td>
      <td style="padding:4px 12px; font-weight:bold;">${minimumBand!"B"}</td>
    </tr>
  </table>

  <p>
    O documento integral, em PDF, está anexado a esta mensagem. O sistema
    <strong>recomenda</strong> a revisão das perguntas não conformes, a reavaliação
    das justificativas e o registro das ações corretivas antes do encerramento da etapa
    ou iteração.
  </p>

  <p style="color:#9aa5b1; font-size:12px;">
    Emitido por ${emittedBy!"Analista de Qualidade"} em ${emittedAtFormatted!"N/D"}.
  </p>
</body>
</html>
