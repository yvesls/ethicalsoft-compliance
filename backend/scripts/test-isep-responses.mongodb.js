
function upsertResponse(filter, doc) {
  var existing = db.questionnaire_responses.findOne(filter);
  if (existing) {
    db.questionnaire_responses.updateOne({ _id: existing._id }, { $set: doc });
    print("Updated: proj=" + doc.projectId + " q=" + doc.questionnaireId + " rep=" + doc.representativeId);
  } else {
    db.questionnaire_responses.insertOne(Object.assign({}, filter, doc, {
      _class: "com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse"
    }));
    print("Inserted: proj=" + doc.projectId + " q=" + doc.questionnaireId + " rep=" + doc.representativeId);
  }
}

upsertResponse(
  { projectId: NumberLong("7"), questionnaireId: 13, representativeId: NumberLong("13") },
  {
    projectId: NumberLong("7"), questionnaireId: 13, representativeId: NumberLong("13"),
    stageId: 17, status: "COMPLETED", submissionDate: new Date(),
    answers: [
      {
        questionId: NumberLong("24"), stageIds: [17], roleIds: [NumberLong("2"), NumberLong("3"), NumberLong("4")],
        response: true,
        justification: { descricao: "TAP aprovado em reunião de kickoff com todos os patrocinadores. Documentação arquivada no sistema de gestão." },
        attachments: []
      },
      {
        questionId: NumberLong("25"), stageIds: [17], roleIds: [NumberLong("2"), NumberLong("4")],
        response: true,
        justification: { descricao: "Matriz de stakeholders elaborada com mapeamento de interesse e influência. Revisada pela equipe de governança." },
        attachments: []
      },
      {
        questionId: NumberLong("1"), stageIds: [17], roleIds: [NumberLong("2"), NumberLong("4")],
        response: true,
        justification: { descricao: "Processos de decisão bem documentados, com rastreabilidade e justificativas claras para cada escolha de responsabilidade ética." },
        attachments: []
      },
      {
        questionId: NumberLong("3"), stageIds: [17], roleIds: [NumberLong("2"), NumberLong("4")],
        response: true,
        justification: { descricao: "LGPD aplicada desde o escopo inicial. Avaliação de impacto de privacidade (DPIA) iniciada com consultor especializado." },
        attachments: []
      }
    ]
  }
);

upsertResponse(
  { projectId: NumberLong("7"), questionnaireId: 13, representativeId: NumberLong("14") },
  {
    projectId: NumberLong("7"), questionnaireId: 13, representativeId: NumberLong("14"),
    stageId: 17, status: "COMPLETED", submissionDate: new Date(),
    answers: [
      {
        questionId: NumberLong("24"), stageIds: [17], roleIds: [NumberLong("2"), NumberLong("3"), NumberLong("4")],
        response: true,
        justification: { descricao: "Aprovado e assinado. Ficamos satisfeitos com o processo de governança inicial e a transparência da equipe." },
        attachments: []
      }
    ]
  }
);

upsertResponse(
  { projectId: NumberLong("2"), questionnaireId: 3, representativeId: NumberLong("3") },
  {
    projectId: NumberLong("2"), questionnaireId: 3, representativeId: NumberLong("3"),
    status: "COMPLETED", submissionDate: new Date(),
    answers: [
      {
        questionId: NumberLong("1"), stageIds: [3,4,5,6], roleIds: [NumberLong("1"), NumberLong("3")],
        response: false,
        justification: { descricao: "O prazo apertado forçou a equipe a tomar decisões rápidas sem documentar o raciocínio ético. Pressão do cliente para entrega acelerada foi determinante." },
        attachments: []
      },
      {
        questionId: NumberLong("3"), stageIds: [3,4], roleIds: [NumberLong("1"), NumberLong("3")],
        response: false,
        justification: { descricao: "Privacidade foi postergada por conta do prazo e custo. O orcamento reduzido não permitiu contratar especialista em LGPD neste sprint." },
        attachments: []
      },
      {
        questionId: NumberLong("11"), stageIds: [5,6], roleIds: [NumberLong("1")],
        response: false,
        justification: { descricao: "Testes cortados por conta do deadline iminente. A urgencia de entrega pelo cliente não permitiu ciclo completo de QA. Divida tecnica gerada." },
        attachments: []
      },
      {
        questionId: NumberLong("6"), stageIds: [3,4,5,6], roleIds: [NumberLong("1")],
        response: false,
        justification: { descricao: "Documentação negligenciada por pressao de prazo e meta de velocidade. Equipe focada exclusivamente na entrega." },
        attachments: []
      }
    ]
  }
);

upsertResponse(
  { projectId: NumberLong("2"), questionnaireId: 3, representativeId: NumberLong("4") },
  {
    projectId: NumberLong("2"), questionnaireId: 3, representativeId: NumberLong("4"),
    status: "COMPLETED", submissionDate: new Date(),
    answers: [
      {
        questionId: NumberLong("7"), stageIds: [3,4,5,6], roleIds: [NumberLong("2"), NumberLong("4")],
        response: true,
        justification: { descricao: "DoD alcancado, mas criterios flexibilizados por conta do prazo agressivo definido pela diretoria. Acordo registrado no backlog." },
        attachments: []
      },
      {
        questionId: NumberLong("10"), stageIds: [3,4], roleIds: [NumberLong("2"), NumberLong("4")],
        response: false,
        justification: { descricao: "Pressao de custo levou a escolhas tecnicas sem consulta ampla. Budget limitado impôs solucao de menor custo sem avaliar riscos de seguranca." },
        attachments: []
      },
      {
        questionId: NumberLong("15"), stageIds: [3,4,5,6], roleIds: [NumberLong("2"), NumberLong("4")],
        response: true,
        justification: { descricao: "Medidas basicas de segurança implementadas dentro do orcamento. Controles avancados planejados para proximo sprint por razoes de custo." },
        attachments: []
      }
    ]
  }
);

upsertResponse(
  { projectId: NumberLong("2"), questionnaireId: 4, representativeId: NumberLong("3") },
  {
    projectId: NumberLong("2"), questionnaireId: 4, representativeId: NumberLong("3"),
    status: "COMPLETED", submissionDate: new Date(),
    answers: [
      {
        questionId: NumberLong("1"), stageIds: [3,4,5,6], roleIds: [NumberLong("1"), NumberLong("3")],
        response: true,
        justification: { descricao: "Apos retrospectiva do Sprint 1, a equipe adotou checklist etico. Melhora significativa no processo de responsabilidade." },
        attachments: []
      },
      {
        questionId: NumberLong("3"), stageIds: [3,4], roleIds: [NumberLong("1"), NumberLong("3")],
        response: false,
        justification: { descricao: "Ainda pendente consultor LGPD. O prazo do contrato de privacidade atrasou. Custo ainda em negociacao com fornecedor." },
        attachments: []
      },
      {
        questionId: NumberLong("11"), stageIds: [5,6], roleIds: [NumberLong("1")],
        response: true,
        justification: { descricao: "Testes automatizados implementados. Pressao de prazo foi negociada com cliente e escopo ajustado para permitir qualidade adequada." },
        attachments: []
      },
      {
        questionId: NumberLong("6"), stageIds: [3,4,5,6], roleIds: [NumberLong("1")],
        response: true,
        justification: { descricao: "Documentacao retomada. ADRs adotados para rastrear decisoes tecnicas e eticas. Rastreabilidade garantida." },
        attachments: []
      }
    ]
  }
);

upsertResponse(
  { projectId: NumberLong("2"), questionnaireId: 4, representativeId: NumberLong("4") },
  {
    projectId: NumberLong("2"), questionnaireId: 4, representativeId: NumberLong("4"),
    status: "COMPLETED", submissionDate: new Date(),
    answers: [
      {
        questionId: NumberLong("7"), stageIds: [3,4,5,6], roleIds: [NumberLong("2"), NumberLong("4")],
        response: true,
        justification: { descricao: "DoD alcancado sem flexibilizacao. Metas ajustadas para ser realistas dado o time e orcamento disponiveis." },
        attachments: []
      },
      {
        questionId: NumberLong("10"), stageIds: [3,4], roleIds: [NumberLong("2"), NumberLong("4")],
        response: true,
        justification: { descricao: "Reuniao de arquitetura realizada com toda a equipe. Trade-offs documentados e aprovados pelo cliente antes da implementacao." },
        attachments: []
      },
      {
        questionId: NumberLong("15"), stageIds: [3,4,5,6], roleIds: [NumberLong("2"), NumberLong("4")],
        response: true,
        justification: { descricao: "Controles de seguranca implementados. Auditoria de acesso configurada. Custo de seguranca aprovado na revisao de budget." },
        attachments: []
      }
    ]
  }
);

upsertResponse(
  { projectId: NumberLong("2"), questionnaireId: 5, representativeId: NumberLong("3") },
  {
    projectId: NumberLong("2"), questionnaireId: 5, representativeId: NumberLong("3"),
    status: "COMPLETED", submissionDate: new Date(),
    answers: [
      {
        questionId: NumberLong("1"), stageIds: [3,4,5,6], roleIds: [NumberLong("1"), NumberLong("3")],
        response: true,
        justification: { descricao: "Cultura etica consolidada na equipe. Checklist etico integrado ao Definition of Ready. Decisoes rastraveis no sistema de governanca." },
        attachments: []
      },
      {
        questionId: NumberLong("3"), stageIds: [3,4], roleIds: [NumberLong("1"), NumberLong("3")],
        response: true,
        justification: { descricao: "LGPD plenamente implementada. Consultor de privacidade integrado ao time. Avaliacao de impacto concluida e aprovada pelo DPO." },
        attachments: []
      },
      {
        questionId: NumberLong("11"), stageIds: [5,6], roleIds: [NumberLong("1")],
        response: true,
        justification: { descricao: "Cobertura de testes acima de 85%. Pipeline CI/CD com gates de qualidade. Nenhum defeito critico em producao." },
        attachments: []
      },
      {
        questionId: NumberLong("20"), stageIds: [5,6], roleIds: [NumberLong("1")],
        response: true,
        justification: { descricao: "Auditoria de acessibilidade WCAG 2.1 AA realizada. Ajustes apos feedback de usuarios com deficiencia visual. Inclusao garantida." },
        attachments: []
      }
    ]
  }
);

upsertResponse(
  { projectId: NumberLong("2"), questionnaireId: 5, representativeId: NumberLong("4") },
  {
    projectId: NumberLong("2"), questionnaireId: 5, representativeId: NumberLong("4"),
    status: "COMPLETED", submissionDate: new Date(),
    answers: [
      {
        questionId: NumberLong("7"), stageIds: [3,4,5,6], roleIds: [NumberLong("2"), NumberLong("4")],
        response: true,
        justification: { descricao: "100% das historias com DoD atendido. Time amadureceu e estimativas alinhadas com capacidade real. Entrega sustentavel e etica." },
        attachments: []
      },
      {
        questionId: NumberLong("10"), stageIds: [3,4], roleIds: [NumberLong("2"), NumberLong("4")],
        response: true,
        justification: { descricao: "RFC process adotado para mudancas arquiteturais. Stakeholders consultados antes de cada decisao relevante. Transparencia plena." },
        attachments: []
      },
      {
        questionId: NumberLong("15"), stageIds: [3,4,5,6], roleIds: [NumberLong("2"), NumberLong("4")],
        response: true,
        justification: { descricao: "Pentest realizado por empresa especializada. Vulnerabilidades corrigidas. Conformidade com controles de seguranca verificada." },
        attachments: []
      },
      {
        questionId: NumberLong("21"), stageIds: [3,4,5,6], roleIds: [NumberLong("2"), NumberLong("4")],
        response: true,
        justification: { descricao: "Analise de fairness realizada. Dataset validado por especialista em etica em IA. Ausencia de vies comprovada por especialista externo." },
        attachments: []
      }
    ]
  }
);

print("\n✅  Seed de respostas MongoDB concluido.");
print("Projetos: CASCATA (7) e ITERATIVO (2).");
print("Questionarios: 3 (Sprint1-alto_risco), 4 (Sprint2-medio), 5 (Sprint3-baixo), 13 (Iniciacao-exemplar).");
