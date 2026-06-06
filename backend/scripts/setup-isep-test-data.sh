#!/usr/bin/env bash
# =============================================================================
# setup-isep-test-data.sh
#
# Prepara dados de teste para o fluxo completo de cálculo de ISEP.
# Idempotente — pode ser executado múltiplas vezes com segurança.
#
# Cobre dois cenários:
#   - Projeto CASCATA   (id=7, questionário id=13, Iniciação, stage 17)
#       Representantes: 13 (roles: GP=2, AQ=4) | 14 (roles: Cliente=3)
#       Perguntas: 24 (roles: 2,3,4) | 25 (roles: 2,4)
#
#   - Projeto ITERATIVO (id=2, questionário id=4, Sprint 2)
#       Representantes: 3 (roles: Dev=1, Cliente=3) | 4 (roles: GP=2, AQ=4)
#       Pergunta: 7 (stageIds: 3,6,4,5 – roles: 2,4 → apenas rep 4 responde)
#
# Após executar este script:
#   1. Suba a aplicação (scheduler agora roda a cada 1 min).
#   2. Aguarde até 1 minuto — o ISEP dos questionários 4 e 13 será calculado.
#   3. Aguarde mais 30s  — o ISEP consolidado dos projetos 2 e 7 será calculado.
#   4. Consulte os resultados com os curls do test-isep-curls.sh.
# =============================================================================

PG_USER="yves"
PG_PASS="aeiou123"
PG_DB="ethicalsoft_db"
PG_HOST="localhost"
PG_PORT="5432"

MONGO_USER="ethicalsoft"
MONGO_PASS="aeiou123"
MONGO_DB="ethicalsoft_db"
MONGO_AUTH="admin"

echo "============================================================"
echo " ETHICALSOFT – Setup de Dados para Teste ISEP"
echo "============================================================"

# ---------------------------------------------------------------------------
# 1. PostgreSQL – Ajustar datas dos questionários e projetos
# ---------------------------------------------------------------------------
echo ""
echo "[1/3] PostgreSQL: ajustando datas e status..."

PGPASSWORD="$PG_PASS" psql -U "$PG_USER" -d "$PG_DB" -h "$PG_HOST" -p "$PG_PORT" -c "
-- CASCATA: Projeto 7 – Questionário 13 (Iniciação, stage 17)
UPDATE questionnaire
SET    application_end_date   = CURRENT_DATE - 1,
       application_start_date = CURRENT_DATE - 8,
       status                 = 'EM_ANDAMENTO'
WHERE  questionnaire_id = 13;

-- ITERATIVO: Projeto 2 – Questionário 4 (Sprint 2)
UPDATE questionnaire
SET    application_end_date   = CURRENT_DATE - 1,
       application_start_date = CURRENT_DATE - 8,
       status                 = 'EM_ANDAMENTO'
WHERE  questionnaire_id = 4;

-- Deadline dos projetos no passado para o scheduler de projeto os encontrar
UPDATE project
SET    deadline = CURRENT_DATE - 1
WHERE  project_id IN (2, 7);

-- Limpar resultados anteriores (para re-testar)
DELETE FROM member_stage_compliance_result
WHERE result_id IN (SELECT result_id FROM questionnaire_result WHERE questionnaire_id IN (4, 13));
DELETE FROM member_compliance_result
WHERE result_id IN (SELECT result_id FROM questionnaire_result WHERE questionnaire_id IN (4, 13));
DELETE FROM questionnaire_result WHERE questionnaire_id IN (4, 13);
DELETE FROM project_isep_result WHERE project_id IN (2, 7);

SELECT questionnaire_id, name, status, application_start_date, application_end_date
FROM   questionnaire
WHERE  questionnaire_id IN (4, 13);
" 2>&1

echo "[1/3] ✅  PostgreSQL ajustado."

# ---------------------------------------------------------------------------
# 2. MongoDB – CASCATA: completar resposta do rep 13 no questionário 13
# ---------------------------------------------------------------------------
echo ""
echo "[2/3] MongoDB: CASCATA – completando resposta do rep 13 (q13)..."

mongosh --quiet \
  --username  "$MONGO_USER" \
  --password  "$MONGO_PASS" \
  --authenticationDatabase "$MONGO_AUTH" \
  "$MONGO_DB" \
  --eval '
var existing = db.questionnaire_responses.findOne({questionnaireId: 13, representativeId: NumberLong("13")});
if (existing) {
  db.questionnaire_responses.updateOne(
    {_id: existing._id},
    {$set: {
      status: "COMPLETED",
      submissionDate: new Date(),
      answers: [
        {questionId: NumberLong("24"), questionText: "O Termo de Abertura do Projeto (TAP) foi aprovado?",
         stageIds: [17], roleIds: [NumberLong("2"), NumberLong("3"), NumberLong("4")],
         response: true, justification: {descricao: "Aprovado em reuniao de kickoff."}, attachments: []},
        {questionId: NumberLong("25"), questionText: "Os stakeholders iniciais foram identificados?",
         stageIds: [17], roleIds: [NumberLong("2"), NumberLong("4")],
         response: true, justification: {descricao: "Matriz de stakeholders elaborada."}, attachments: []}
      ]
    }}
  );
  print("Rep 13 -> COMPLETED (2 respostas: q24=SIM, q25=SIM)");
} else {
  print("ATENCAO: rep 13 nao encontrado no questionnaire 13 – verifique o banco.");
}

var rep14 = db.questionnaire_responses.findOne({questionnaireId: 13, representativeId: NumberLong("14")});
if (rep14 && rep14.status === "COMPLETED") {
  print("Rep 14 -> ja COMPLETED (ok).");
} else {
  print("ATENCAO: rep 14 nao esta COMPLETED. Status: " + (rep14 ? rep14.status : "nao encontrado"));
}
' 2>&1

echo "[2/3] ✅  MongoDB CASCATA ajustado."

# ---------------------------------------------------------------------------
# 3. MongoDB – ITERATIVO: criar respostas COMPLETED para reps 3 e 4 (questionnaire 4)
# ---------------------------------------------------------------------------
echo ""
echo "[3/3] MongoDB: ITERATIVO – criando/atualizando respostas para reps 3 e 4 (q4)..."

mongosh --quiet \
  --username  "$MONGO_USER" \
  --password  "$MONGO_PASS" \
  --authenticationDatabase "$MONGO_AUTH" \
  "$MONGO_DB" \
  --eval '
// Limpar docs orfaos (sem representativeId) do questionnaire 4
var deleted = db.questionnaire_responses.deleteMany({questionnaireId: 4, representativeId: {$exists: false}});
if (deleted.deletedCount > 0) print("Removidos " + deleted.deletedCount + " docs orfaos.");

// Rep 4 (GP=2, AQ=4) – pergunta 7 tem roles 2 e 4 -> responde
var rep4 = db.questionnaire_responses.findOne({questionnaireId: 4, representativeId: NumberLong("4")});
if (!rep4) {
  db.questionnaire_responses.insertOne({
    projectId: NumberLong("2"), questionnaireId: 4,
    representativeId: NumberLong("4"), status: "COMPLETED", submissionDate: new Date(),
    answers: [{
      questionId: NumberLong("7"),
      questionText: "A Definition of Done foi alcancada para todas as historias?",
      stageIds: [3, 6, 4, 5], roleIds: [NumberLong("2"), NumberLong("4")],
      response: true, justification: {descricao: "Todos os criterios de aceite verificados."}, attachments: []
    }],
    _class: "com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse"
  });
  print("Rep 4 -> COMPLETED criado (1 resposta: q7=SIM)");
} else {
  db.questionnaire_responses.updateOne({_id: rep4._id}, {$set: {
    status: "COMPLETED", submissionDate: new Date(),
    answers: [{
      questionId: NumberLong("7"),
      questionText: "A Definition of Done foi alcancada para todas as historias?",
      stageIds: [3, 6, 4, 5], roleIds: [NumberLong("2"), NumberLong("4")],
      response: true, justification: {descricao: "Todos os criterios de aceite verificados."}, attachments: []
    }]
  }});
  print("Rep 4 -> COMPLETED atualizado (1 resposta: q7=SIM)");
}

// Rep 3 (Dev=1, Cliente=3) – pergunta 7 nao tem roles 1/3 -> sem perguntas aplicaveis
// Mas DEVE ter COMPLETED para o isFullyCompleted retornar true para este questionario.
var rep3 = db.questionnaire_responses.findOne({questionnaireId: 4, representativeId: NumberLong("3")});
if (!rep3) {
  db.questionnaire_responses.insertOne({
    projectId: NumberLong("2"), questionnaireId: 4,
    representativeId: NumberLong("3"), status: "COMPLETED", submissionDate: new Date(),
    answers: [],
    _class: "com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse"
  });
  print("Rep 3 -> COMPLETED criado (sem perguntas para seus roles neste questionario)");
} else {
  db.questionnaire_responses.updateOne({_id: rep3._id}, {$set: {status: "COMPLETED", submissionDate: new Date()}});
  print("Rep 3 -> COMPLETED atualizado");
}
' 2>&1

echo "[3/3] ✅  MongoDB ITERATIVO ajustado."

# ---------------------------------------------------------------------------
# Resumo final
# ---------------------------------------------------------------------------
echo ""
echo "============================================================"
echo " ESTADO FINAL"
echo "============================================================"
echo ""
echo "--- Questionários ---"
PGPASSWORD="$PG_PASS" psql -U "$PG_USER" -d "$PG_DB" -h "$PG_HOST" -p "$PG_PORT" \
  -c "SELECT q.questionnaire_id, q.project_id, q.name, q.status,
             q.application_start_date, q.application_end_date
      FROM questionnaire q WHERE q.questionnaire_id IN (4, 13);" 2>&1

echo ""
echo "--- Projetos ---"
PGPASSWORD="$PG_PASS" psql -U "$PG_USER" -d "$PG_DB" -h "$PG_HOST" -p "$PG_PORT" \
  -c "SELECT project_id, name, type, status, timeline_status, deadline
      FROM project WHERE project_id IN (2, 7);" 2>&1

echo ""
echo "--- Respostas MongoDB ---"
mongosh --quiet \
  --username "$MONGO_USER" --password "$MONGO_PASS" \
  --authenticationDatabase "$MONGO_AUTH" "$MONGO_DB" \
  --eval '
print("Q4 (ITERATIVO, p2):");
db.questionnaire_responses.find({questionnaireId: 4}).forEach(r =>
  print("  repId=" + r.representativeId + " status=" + r.status + " answers=" + r.answers.length));
print("Q13 (CASCATA, p7):");
db.questionnaire_responses.find({questionnaireId: 13}).forEach(r =>
  print("  repId=" + r.representativeId + " status=" + r.status + " answers=" + r.answers.length));
' 2>&1

echo ""
echo "============================================================"
echo " ✅  Tudo pronto!"
echo ""
echo " 1. Suba a aplicação (scheduler roda a cada 1 min)."
echo " 2. Em ~1 min: ISEP dos questionários 4 e 13 calculado."
echo " 3. Em ~1m30s: ISEP consolidado dos projetos 2 e 7 calculado."
echo " 4. Execute: bash scripts/test-isep-curls.sh <TOKEN>"
echo "============================================================"

