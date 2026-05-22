<html>
<body>
  <p>Olá ${recipientName!"Participante"},</p>
  <p>
    Foi emitido um <strong>Boletim de Não Conformidade Ética</strong> referente ao questionário
    <strong>${questionnaireName!""}</strong> do projeto <strong>${projectName!""}</strong>.
  </p>
  <ul>
    <li>Projeto: ${projectName!""}</li>
    <li>Questionário: ${questionnaireName!""}</li>
    <li>ISEP do questionário: <strong>${isepPercent!""}%</strong> (Faixa ${band!""})</li>
    <li>Faixa mínima aceitável: <strong>${minimumBand!"B"}</strong></li>
    <li>Emitido por: ${emittedBy!""}</li>
    <li>Emitido em: ${emittedAtFormatted!""}</li>
  </ul>
  <p>
    O boletim completo está <strong>anexo a este e-mail em formato PDF</strong>. Ele detalha as
    perguntas e os membros em não conformidade e apresenta orientações para a revisão e a correção
    das respostas.
  </p>
  <p>
    Conforme as regras do processo, a iteração não poderá ser concluída enquanto as não
    conformidades não forem revisadas. Solicitamos que as respostas sejam revisadas e submetidas
    novamente dentro do prazo indicado no boletim.
  </p>
  <#if projectLink??>
    <p>Acesse o projeto: <a href="${projectLink}">${projectLink}</a></p>
  </#if>
</body>
</html>
