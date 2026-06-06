BASE="http://localhost:8080/api"
TOKEN="${1:-SEU_TOKEN_JWT_AQUI}"
AUTH="Authorization: Bearer $TOKEN"

echo ""
echo "══════════════════════════════════════════════════════════════"
echo " 1. DASHBOARD DE QUESTIONÁRIO – Sub-índices de Governança"
echo "══════════════════════════════════════════════════════════════"

echo ""
echo "=== [CASCATA] Dashboard Questionário 13 – Projeto 7 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/7/questionnaires/13/dashboard" \
  | jq '{
      questionnaireId,
      band,
      isepPercent,
      "governance_domains": {
        ethicsScorePercent,
        processScorePercent,
        fairnessScorePercent,
        esgScorePercent
      },
      "debt_indicators": {
        ethicsDebtPercent,
        techDebtPercent
      }
    }'

echo ""
echo "=== [ITERATIVO] Dashboard Questionário 3 – Sprint 1 (dívida ALTA) ==="
curl -s -H "$AUTH" \
  "$BASE/projects/2/questionnaires/3/dashboard" \
  | jq '{questionnaireId, band, isepPercent, ethicsDebtPercent, techDebtPercent}'

echo ""
echo "=== [ITERATIVO] Dashboard Questionário 4 – Sprint 2 (dívida MÉDIA) ==="
curl -s -H "$AUTH" \
  "$BASE/projects/2/questionnaires/4/dashboard" \
  | jq '{questionnaireId, band, isepPercent, ethicsDebtPercent, techDebtPercent}'

echo ""
echo "=== [ITERATIVO] Dashboard Questionário 5 – Sprint 3 (dívida BAIXA) ==="
curl -s -H "$AUTH" \
  "$BASE/projects/2/questionnaires/5/dashboard" \
  | jq '{questionnaireId, band, isepPercent, ethicsDebtPercent, techDebtPercent}'

echo ""
echo "══════════════════════════════════════════════════════════════"
echo " 2. DASHBOARD DO PROJETO – ISEP Consolidado + Debt Evolution"
echo "══════════════════════════════════════════════════════════════"

echo ""
echo "=== [CASCATA] Dashboard Consolidado – Projeto 7 ==="
curl -s -H "$AUTH" "$BASE/projects/7/dashboard" \
  | jq '{
      projectId, projectName, projectBand, projectIsepPercent,
      "governance": {ethicsScorePercent, processScorePercent, fairnessScorePercent, esgScorePercent},
      "debt": {ethicsDebtPercent, techDebtPercent},
      "history": [.isepHistory[] | {questionnaireId, band, isepPercent, ethicsDebtPercent, techDebtPercent}]
    }'

echo ""
echo "=== [ITERATIVO] Dashboard Consolidado – Projeto 2 (Debt Evolution) ==="
curl -s -H "$AUTH" "$BASE/projects/2/dashboard" \
  | jq '{
      projectId, projectName, projectBand, projectIsepPercent,
      "governance": {ethicsScorePercent, processScorePercent, fairnessScorePercent, esgScorePercent},
      "debt": {ethicsDebtPercent, techDebtPercent},
      "history": [.isepHistory[] | {questionnaireId, band, isepPercent, ethicsDebtPercent, techDebtPercent}]
    }'

echo ""
echo "══════════════════════════════════════════════════════════════"
echo " 3. HEATMAP – Conformidade por Papel × Etapa"
echo "══════════════════════════════════════════════════════════════"

echo ""
echo "=== [CASCATA] Heatmap – Q13 Projeto 7 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/7/questionnaires/13/dashboard/role-stage" | jq '.'

echo ""
echo "=== [ITERATIVO] Heatmap – Q3 Projeto 2 (Sprint 1, dívida alta) ==="
curl -s -H "$AUTH" \
  "$BASE/projects/2/questionnaires/3/dashboard/role-stage" | jq '.'

echo ""
echo "══════════════════════════════════════════════════════════════"
echo " 4. NUVEM DE PALAVRAS – Global + Por Categoria de Governança"
echo "══════════════════════════════════════════════════════════════"

echo ""
echo "=== [ITERATIVO] Word Cloud – Q3 Sprint 1 (esperar insights ETHICS/PROCESS) ==="
curl -s -H "$AUTH" \
  "$BASE/projects/2/questionnaires/3/dashboard/word-cloud" \
  | jq '{
      questionnaireId,
      totalJustifications,
      "top5_words": (.topWords[:5]),
      "category_summary": (
        if .categoryWordFrequency then
          .categoryWordFrequency | to_entries | map({(.key): (.value | to_entries[:3] | map("\(.key)=\(.value)") | join(", "))}) | add
        else "sem metadados disponíveis" end
      ),
      topThemeInsights
    }'

echo ""
echo "=== [CASCATA] Word Cloud – Q13 Projeto 7 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/7/questionnaires/13/dashboard/word-cloud" \
  | jq '{totalJustifications, "top10": (.topWords[:10]), topThemeInsights}'

echo ""
echo "══════════════════════════════════════════════════════════════"
echo " 5. PAINEL INDIVIDUAL"
echo "══════════════════════════════════════════════════════════════"

echo ""
echo "=== [CASCATA] Individual – Rep 13 / Q13 Projeto 7 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/7/questionnaires/13/dashboard/individual?representativeId=13" | jq '.'

echo ""
echo "=== [ITERATIVO] Individual – Rep 4 / Q4 Projeto 2 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/2/questionnaires/4/dashboard/individual?representativeId=4" | jq '.'

echo ""
echo "══════════════════════════════════════════════════════════════"
echo " 6. EXPORTAÇÃO JSON – Todos os Questionários do Projeto"
echo "══════════════════════════════════════════════════════════════"

echo ""
echo "=== [ITERATIVO] Export JSON – Projeto 2 (com sub-índices) ==="
curl -s -H "$AUTH" \
  "$BASE/projects/2/dashboard/export?anonymize=true" \
  | jq '.[] | {questionnaireId, band, isepPercent, ethicsScorePercent, processScorePercent, ethicsDebtPercent, techDebtPercent}'

echo ""
echo "=== [CASCATA] Export JSON – Projeto 7 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/7/dashboard/export?anonymize=true" \
  | jq '.[] | {questionnaireId, band, isepPercent, ethicsScorePercent, ethicsDebtPercent}'

echo ""
echo "══════════════════════════════════════════════════════════════"
echo " 7. EXPORTAÇÃO CSV"
echo "══════════════════════════════════════════════════════════════"

echo ""
echo "=== [ITERATIVO] Export CSV – Projeto 2 (primeiras linhas) ==="
curl -s -H "$AUTH" \
  "$BASE/projects/2/dashboard/export/csv?anonymize=true" | head -5

echo ""
echo "=== [CASCATA] Export CSV – Projeto 7 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/7/dashboard/export/csv?anonymize=true" | head -5

echo ""
echo "══════════════════════════════════════════════════════════════"
echo " 8. STATUS DOS QUESTIONÁRIOS"
echo "══════════════════════════════════════════════════════════════"

echo ""
echo "=== Questionários do Projeto 7 (CASCATA) ==="
curl -s -H "$AUTH" \
  "$BASE/projects/7/questionnaires?page=0&size=20" \
  | jq '.content[]? | {id, name, status}'

echo ""
echo "=== Questionários do Projeto 2 (ITERATIVO) ==="
curl -s -H "$AUTH" \
  "$BASE/projects/2/questionnaires?page=0&size=20" \
  | jq '.content[]? | {id, name, status}'

echo ""
echo "══════════════════════════════════════════════════════════════"
echo " 9. FORCE CLOSE (descomente para executar)"
echo "══════════════════════════════════════════════════════════════"

echo "# Para forçar cálculo ISEP questionário:"
echo "# curl -s -X POST -H \"$AUTH\" \"$BASE/projects/7/questionnaires/13/force-close\" | jq ."
echo ""
echo "# Para encerrar projeto manualmente:"
echo "# curl -s -X POST -H \"$AUTH\" \"$BASE/projects/7/close\" | jq ."

echo ""
echo "✅  Todos os curls executados."

echo "=== [CASCATA] ISEP do Questionário 13 (Projeto 7 – Iniciação) ==="
curl -s -H "$AUTH" \
  "$BASE/projects/7/questionnaires/13/isep" | jq .

echo ""
echo "=== [ITERATIVO] ISEP do Questionário 4 (Projeto 2 – Sprint 2) ==="
curl -s -H "$AUTH" \
  "$BASE/projects/2/questionnaires/4/isep" | jq .

echo ""
echo "=== [CASCATA] ISEP Consolidado do Projeto 7 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/7/isep" | jq .

echo ""
echo "=== [ITERATIVO] ISEP Consolidado do Projeto 2 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/2/isep" | jq .

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

echo ""
echo "=== [CASCATA] Dashboard de Gestão – Projeto 7 / Questionário 13 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/7/dashboard/management?questionnaireId=13" | jq .

echo ""
echo "=== [ITERATIVO] Dashboard de Gestão – Projeto 2 / Questionário 4 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/2/dashboard/management?questionnaireId=4" | jq .

echo ""
echo "=== [CASCATA] Heatmap – Projeto 7 / Questionário 13 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/7/questionnaires/13/heatmap" | jq .

echo ""
echo "=== [ITERATIVO] Heatmap – Projeto 2 / Questionário 4 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/2/questionnaires/4/heatmap" | jq .

echo ""
echo "=== [CASCATA] Word Cloud – Projeto 7 / Questionário 13 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/7/questionnaires/13/word-cloud" | jq .

echo ""
echo "=== [CASCATA] Export CSV – Projeto 7 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/7/isep/export/csv" | head -20

echo ""
echo "=== [ITERATIVO] Export CSV – Projeto 2 ==="
curl -s -H "$AUTH" \
  "$BASE/projects/2/isep/export/csv" | head -20

echo ""
echo "=== Force Close Questionnaire 13 (requer admin do projeto) ==="
echo "AVISO: Só execute se quiser forçar o fechamento sem 100% das respostas."
echo "Descomente a linha abaixo para executar:"

echo ""
echo "=== Lista de Questionários do Projeto 7 (CASCATA) ==="
curl -s -H "$AUTH" \
  "$BASE/projects/7/questionnaires?page=0&size=10" | jq '.content[] | {id, name, status, respondedRespondents, totalRespondents}'

echo ""
echo "=== Lista de Questionários do Projeto 2 (ITERATIVO) ==="
curl -s -H "$AUTH" \
  "$BASE/projects/2/questionnaires?page=0&size=10" | jq '.content[] | {id, name, status, respondedRespondents, totalRespondents}'

