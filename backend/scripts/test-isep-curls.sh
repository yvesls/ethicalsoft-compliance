#!/usr/bin/env bash
# =============================================================================
# test-isep-curls.sh
#
# CURLs para testar manualmente os endpoints do ISEP após rodar o scheduler.
# Pré-requisito: aplicação rodando em localhost:8080
#
# Obtenha o token fazendo login primeiro (ajuste e-mail/senha conforme necessário):
#   TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
#     -H 'Content-Type: application/json' \
#     -d '{"email":"yves.silva@edu.ufes.br","password":"SuaSenhaAqui"}' \
#     | jq -r '.token')
# =============================================================================

BASE="http://localhost:8080/api"
TOKEN="${1:-SEU_TOKEN_JWT_AQUI}"
AUTH="Authorization: Bearer $TOKEN"

# ─────────────────────────────────────────────────────────────────────────────
# 1. QUESTIONNAIRE ISEP RESULT
#    GET /api/projects/{projectId}/questionnaires/{questionnaireId}/isep
# ─────────────────────────────────────────────────────────────────────────────

echo "=== [CASCATA] ISEP do Questionário 13 (Projeto 7 – Iniciação) ==="
curl -s -H "$AUTH" \
  "$BASE/projects/7/questionnaires/13/isep" | jq .

echo ""
echo "=== [ITERATIVO] ISEP do Questionário 4 (Projeto 2 – Sprint 2) ==="
curl -s -H "$AUTH" \
  "$BASE/projects/2/questionnaires/4/isep" | jq .

# ─────────────────────────────────────────────────────────────────────────────
# 2. PROJECT ISEP RESULT (ISEP consolidado do projeto)
#    GET /api/projects/{projectId}/isep
# ─────────────────────────────────────────────────────────────────────────────

echo ""
echo "=== [CASCATA] ISEP Consolidado do Projeto 7 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/7/isep" | jq .

echo ""
echo "=== [ITERATIVO] ISEP Consolidado do Projeto 2 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/2/isep" | jq .

# ─────────────────────────────────────────────────────────────────────────────
# 3. DASHBOARD INDIVIDUAL (Visão 1)
#    GET /api/projects/{projectId}/dashboard/individual?representativeId=X
# ─────────────────────────────────────────────────────────────────────────────

echo ""
echo "=== [CASCATA] Dashboard Individual – Rep 13 no Projeto 7 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/7/dashboard/individual?representativeId=13" | jq .

echo ""
echo "=== [CASCATA] Dashboard Individual – Rep 14 no Projeto 7 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/7/dashboard/individual?representativeId=14" | jq .

echo ""
echo "=== [ITERATIVO] Dashboard Individual – Rep 4 no Projeto 2 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/2/dashboard/individual?representativeId=4" | jq .

# ─────────────────────────────────────────────────────────────────────────────
# 4. DASHBOARD DE GESTÃO / PAINEL DO PROJETO (Visão 2)
#    GET /api/projects/{projectId}/dashboard/management?questionnaireId=X
# ─────────────────────────────────────────────────────────────────────────────

echo ""
echo "=== [CASCATA] Dashboard de Gestão – Projeto 7 / Questionário 13 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/7/dashboard/management?questionnaireId=13" | jq .

echo ""
echo "=== [ITERATIVO] Dashboard de Gestão – Projeto 2 / Questionário 4 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/2/dashboard/management?questionnaireId=4" | jq .

# ─────────────────────────────────────────────────────────────────────────────
# 5. HEATMAP DE RISCO (Gráfico 3 – Encargo x Etapa)
#    GET /api/projects/{projectId}/questionnaires/{questionnaireId}/heatmap
# ─────────────────────────────────────────────────────────────────────────────

echo ""
echo "=== [CASCATA] Heatmap – Projeto 7 / Questionário 13 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/7/questionnaires/13/heatmap" | jq .

echo ""
echo "=== [ITERATIVO] Heatmap – Projeto 2 / Questionário 4 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/2/questionnaires/4/heatmap" | jq .

# ─────────────────────────────────────────────────────────────────────────────
# 6. NUVEM DE PALAVRAS (Widget 5 – Justificativas)
#    GET /api/projects/{projectId}/questionnaires/{questionnaireId}/word-cloud
# ─────────────────────────────────────────────────────────────────────────────

echo ""
echo "=== [CASCATA] Word Cloud – Projeto 7 / Questionário 13 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/7/questionnaires/13/word-cloud" | jq .

# ─────────────────────────────────────────────────────────────────────────────
# 7. EXPORTAÇÃO CSV
#    GET /api/projects/{projectId}/isep/export/csv
# ─────────────────────────────────────────────────────────────────────────────

echo ""
echo "=== [CASCATA] Export CSV – Projeto 7 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/7/isep/export/csv" | head -20

echo ""
echo "=== [ITERATIVO] Export CSV – Projeto 2 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/2/isep/export/csv" | head -20

# ─────────────────────────────────────────────────────────────────────────────
# 8. FORCE CLOSE QUESTIONNAIRE (Fechar manualmente – sem precisar 100% respostas)
#    POST /api/projects/{projectId}/questionnaires/{questionnaireId}/force-close
# ─────────────────────────────────────────────────────────────────────────────

echo ""
echo "=== Force Close Questionnaire 13 (requer admin do projeto) ==="
echo "AVISO: Só execute se quiser forçar o fechamento sem 100% das respostas."
echo "Descomente a linha abaixo para executar:"
# curl -s -X POST -H "$AUTH" \
#   "$BASE/projects/7/questionnaires/13/force-close" | jq .

# ─────────────────────────────────────────────────────────────────────────────
# 9. VERIFICAR STATUS DOS QUESTIONÁRIOS DO PROJETO
#    GET /api/projects/{projectId}/questionnaires
# ─────────────────────────────────────────────────────────────────────────────

echo ""
echo "=== Lista de Questionários do Projeto 7 (CASCATA) ==="
curl -s -H "$AUTH" \
  "$BASE/projects/7/questionnaires?page=0&size=10" | jq '.content[] | {id, name, status, respondedRespondents, totalRespondents}'

echo ""
echo "=== Lista de Questionários do Projeto 2 (ITERATIVO) ==="
curl -s -H "$AUTH" \
  "$BASE/projects/2/questionnaires?page=0&size=10" | jq '.content[] | {id, name, status, respondedRespondents, totalRespondents}'

