package com.ethicalsoft.ethicalsoft_complience.infra.bootstrap;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.ProjectTemplate;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.dto.*;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.ProjectTemplateRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.RoleSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.StageSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TemplateVisibilityEnum;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class BaseQuestionnaireTemplateInitializer {

    private static final String CASCATA_BASE_ID = "680205b7036c4f4afca55001";
    private static final String ITERATIVO_BASE_ID = "680205b7036c4f4afca55002";

    private static final long DESENVOLVEDOR = 1L;
    private static final long GERENTE_PROJETO = 2L;
    private static final long CLIENTE = 3L;
    private static final long ANALISTA_QUALIDADE = 4L;
    private static final long ANALISTA_REQUISITOS = 5L;
    private static final long DESIGNER = 6L;
    private static final long RESPONSAVEL_NEGOCIO = 7L;
    private static final long SUPORTE = 8L;
    private static final long STAKEHOLDER = 9L;
    private static final long LIDER_EQUIPE = 10L;
    private static final long ARQUITETO_SOFTWARE = 11L;

    private final ProjectTemplateRepository repository;

    @PostConstruct
    public void seed() {
        insertIfMissing(buildCascataBase());
        insertIfMissing(buildIterativoBase());
    }

    private void insertIfMissing(ProjectTemplate t) {
        if (!repository.existsById(t.getId())) {
            repository.save(t);
            log.info("[template-init] Template base '{}' inserido.", t.getName());
        }
    }

    private ProjectTemplate buildCascataBase() {
        ProjectTemplate t = new ProjectTemplate();
        t.setId(CASCATA_BASE_ID);
        t.setName("Template Base Governança Ética - Cascata");
        t.setType(ProjectTypeEnum.CASCATA);
        t.setDescription("Template completo de maturidade ética para projetos em cascata (5 etapas SISP). "
                + "Cobre todas as roles e fases: Iniciação, Requisitos, Projeto, Desenvolvimento e Testes.");
        t.setVisibility(TemplateVisibilityEnum.PUBLIC);
        t.setStages(List.of(
                stage("Iniciação", "2.00", 0),
                stage("Requisitos", "3.00", 1),
                stage("Projeto", "2.00", 2),
                stage("Desenvolvimento", "2.50", 3),
                stage("Testes", "1.50", 4)
        ));
        t.setQuestionnaires(List.of(
                cascataQuestionnaire("Iniciação", buildIniciacao(null)),
                cascataQuestionnaire("Requisitos", buildRequisitos(null)),
                cascataQuestionnaire("Projeto", buildProjeto(null)),
                cascataQuestionnaire("Desenvolvimento", buildDesenvolvimento(null)),
                cascataQuestionnaire("Testes", buildTestes(null))
        ));
        t.setIterations(List.of());
        t.setRepresentatives(buildBaseRepresentatives());
        return t;
    }


    private TemplateQuestionnaireDTO cascataQuestionnaire(String stageName, List<TemplateQuestionDTO> questions) {
        TemplateQuestionnaireDTO q = new TemplateQuestionnaireDTO();
        q.setName(stageName);
        q.setStageName(stageName);
        q.setQuestions(questions);
        return q;
    }

    private ProjectTemplate buildIterativoBase() {
        ProjectTemplate t = new ProjectTemplate();
        t.setId(ITERATIVO_BASE_ID);
        t.setName("Template Base Governança Ética - Iterativo");
        t.setType(ProjectTypeEnum.ITERATIVO);
        t.setDescription("Template completo de maturidade ética para projetos iterativos (4 Sprints). "
                + "Cada sprint cobre as 5 etapas com perguntas distribuídas por role.");
        t.setVisibility(TemplateVisibilityEnum.PUBLIC);
        t.setDefaultIterationCount(4);
        t.setDefaultIterationDuration(14);

        List<TemplateStageDTO> stages = List.of(
                stage("Iniciação", "2.00", 0),
                stage("Requisitos", "3.00", 1),
                stage("Projeto", "2.00", 2),
                stage("Desenvolvimento", "2.50", 3),
                stage("Testes", "1.50", 4)
        );
        t.setStages(stages);

        List<StageSummaryResponseDTO> allStages = List.of(
                stageSummary(0, "Iniciação"),
                stageSummary(1, "Requisitos"),
                stageSummary(2, "Projeto"),
                stageSummary(3, "Desenvolvimento"),
                stageSummary(4, "Testes")
        );

        t.setQuestionnaires(List.of(
                iterativeQuestionnaire("Sprint 1", buildAllQuestions(allStages)),
                iterativeQuestionnaire("Sprint 2", buildAllQuestions(allStages)),
                iterativeQuestionnaire("Sprint 3", buildAllQuestions(allStages)),
                iterativeQuestionnaire("Sprint 4", buildAllQuestions(allStages))
        ));

        t.setIterations(List.of(
                iteration("Sprint 1", "1.00"),
                iteration("Sprint 2", "1.00"),
                iteration("Sprint 3", "1.50"),
                iteration("Sprint 4", "2.00")
        ));
        t.setRepresentatives(buildBaseRepresentatives());
        return t;
    }

    private TemplateQuestionnaireDTO iterativeQuestionnaire(String sprintName, List<TemplateQuestionDTO> questions) {
        TemplateQuestionnaireDTO q = new TemplateQuestionnaireDTO();
        q.setName(sprintName);
        q.setIterationRefName(sprintName);
        q.setQuestions(questions);
        return q;
    }

    private List<TemplateQuestionDTO> buildAllQuestions(List<StageSummaryResponseDTO> allStages) {
        List<TemplateQuestionDTO> all = new ArrayList<>();
        List<StageSummaryResponseDTO> inicStages = List.of(stageSummary(0, "Iniciação"));
        List<StageSummaryResponseDTO> reqStages = List.of(stageSummary(1, "Requisitos"));
        List<StageSummaryResponseDTO> projStages = List.of(stageSummary(2, "Projeto"));
        List<StageSummaryResponseDTO> devStages = List.of(stageSummary(3, "Desenvolvimento"));
        List<StageSummaryResponseDTO> testStages = List.of(stageSummary(4, "Testes"));

        all.addAll(buildIniciacao(inicStages));
        all.addAll(buildRequisitos(reqStages));
        all.addAll(buildProjeto(projStages));
        all.addAll(buildDesenvolvimento(devStages));
        all.addAll(buildTestes(testStages));
        return all;
    }

    private List<TemplateQuestionDTO> buildIniciacao(List<StageSummaryResponseDTO> stgs) {
        List<TemplateQuestionDTO> qs = new ArrayList<>();
        // -- CLIENTE --
        qs.add(q("Voce entendeu os objetivos do software?", Set.of(r(CLIENTE), r(STAKEHOLDER)), "Iniciação", stgs));
        qs.add(q("Voce esta ciente das politicas eticas que a equipe de desenvolvimento seguira durante todo o projeto?", Set.of(r(CLIENTE), r(STAKEHOLDER)), "Iniciação", stgs));
        qs.add(q("Voce esta ciente das possiveis implicacoes eticas do software na seguranca e privacidade dos usuarios?", Set.of(r(CLIENTE), r(STAKEHOLDER)), "Iniciação", stgs));
        qs.add(q("Voce esta disposto a fornecer feedback para a equipe de desenvolvimento durante todo o processo?", Set.of(r(CLIENTE)), "Iniciação", stgs));
        qs.add(q("Voce tem alguma preocupacao com relacao ao desenvolvimento etico do software?", Set.of(r(CLIENTE), r(STAKEHOLDER)), "Iniciação", stgs));
        qs.add(q("Voce esta ciente de quaisquer requisitos legais ou regulatorios relacionados ao desenvolvimento do software?", Set.of(r(CLIENTE), r(RESPONSAVEL_NEGOCIO)), "Iniciação", stgs));
        qs.add(q("Voce esta disposto a trabalhar em colaboracao com a equipe de desenvolvimento para garantir o desenvolvimento etico do software?", Set.of(r(CLIENTE)), "Iniciação", stgs));
        qs.add(q("Foi determinado quem toma as decisoes em nome da sua empresa?", Set.of(r(CLIENTE), r(RESPONSAVEL_NEGOCIO)), "Iniciação", stgs));
        qs.add(q("Foi determinado quem aprova a flexibilidade orcamentaria para o gerenciamento do projeto?", Set.of(r(CLIENTE), r(RESPONSAVEL_NEGOCIO)), "Iniciação", stgs));
        qs.add(q("Foi definido quem e a autoridade que trata de anormalidades no escopo do trabalho?", Set.of(r(CLIENTE), r(GERENTE_PROJETO)), "Iniciação", stgs));
        // -- GERENTE DE PROJETO --
        qs.add(q("Voce definiu politicas eticas claras que devem ser seguidas durante o desenvolvimento do software?", Set.of(r(GERENTE_PROJETO)), "Iniciação", stgs));
        qs.add(q("Voce identificou possiveis conflitos eticos que possam surgir durante o desenvolvimento do software?", Set.of(r(GERENTE_PROJETO)), "Iniciação", stgs));
        qs.add(q("Voce forneceu orientacoes claras para a equipe de desenvolvimento sobre como lidar com possiveis conflitos eticos?", Set.of(r(GERENTE_PROJETO)), "Iniciação", stgs));
        qs.add(q("Voce esta disposto a apoiar a equipe de desenvolvimento na resolucao de possiveis conflitos eticos?", Set.of(r(GERENTE_PROJETO)), "Iniciação", stgs));
        qs.add(q("Voce esta disposto a fornecer recursos adicionais para garantir que o software seja desenvolvido de maneira etica?", Set.of(r(GERENTE_PROJETO), r(RESPONSAVEL_NEGOCIO)), "Iniciação", stgs));
        qs.add(q("Voce esta comprometido em garantir que o software seja desenvolvido de maneira etica?", Set.of(r(GERENTE_PROJETO)), "Iniciação", stgs));
        qs.add(q("Voce identificou possiveis implicacoes eticas do software na seguranca e privacidade dos usuarios?", Set.of(r(GERENTE_PROJETO)), "Iniciação", stgs));
        qs.add(q("Voce esta disposto a trabalhar com a equipe de desenvolvimento para garantir que o software seja seguro e proteja a privacidade dos usuarios?", Set.of(r(GERENTE_PROJETO)), "Iniciação", stgs));
        // -- LIDER DE EQUIPE --
        qs.add(q("Voce definiu politicas eticas claras que devem ser seguidas durante o desenvolvimento do software?", Set.of(r(LIDER_EQUIPE)), "Iniciação", stgs));
        qs.add(q("Voce compartilhou essas politicas eticas com sua equipe de desenvolvimento?", Set.of(r(LIDER_EQUIPE)), "Iniciação", stgs));
        qs.add(q("Voce esta garantindo que a equipe esteja ciente dos possiveis conflitos eticos que possam surgir durante o desenvolvimento do software?", Set.of(r(LIDER_EQUIPE)), "Iniciação", stgs));
        qs.add(q("Voce esta ciente sobre a necessidade de incentivar sua equipe a levantar questoes eticas e a discutir possiveis solucoes para conflitos eticos?", Set.of(r(LIDER_EQUIPE)), "Iniciação", stgs));
        qs.add(q("Voce esta disposto a apoiar a equipe de desenvolvimento na resolucao de possiveis conflitos eticos?", Set.of(r(LIDER_EQUIPE)), "Iniciação", stgs));
        // -- ARQUITETO DE SOFTWARE --
        qs.add(q("Voce esta ciente sobre a necessidade de incentivar sua equipe a levantar questoes eticas e a discutir possiveis solucoes para conflitos eticos?", Set.of(r(ARQUITETO_SOFTWARE)), "Iniciação", stgs));
        qs.add(q("Voce entendeu as politicas eticas estabelecidas pelo administrador?", Set.of(r(ARQUITETO_SOFTWARE)), "Iniciação", stgs));
        qs.add(q("Voce esta disposto a apoiar a equipe de desenvolvimento na resolucao de possiveis conflitos eticos relacionados ao design do software?", Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER)), "Iniciação", stgs));
        qs.add(q("Voce compartilhou as politicas eticas com sua equipe de desenvolvimento?", Set.of(r(ARQUITETO_SOFTWARE)), "Iniciação", stgs));
        qs.add(q("Voce esta comprometido em seguir as politicas eticas estabelecidas e em desenvolver o software de maneira etica?", Set.of(r(ARQUITETO_SOFTWARE)), "Iniciação", stgs));
        qs.add(q("Voce esta disposto a reportar possiveis problemas eticos identificados durante o desenvolvimento do software?", Set.of(r(ARQUITETO_SOFTWARE)), "Iniciação", stgs));
        qs.add(q("Voce esta trabalhando com a equipe de desenvolvimento para garantir que as questoes de usabilidade e acessibilidade sejam consideradas durante o design do software?", Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER)), "Iniciação", stgs));
        // -- DESENVOLVEDOR --
        qs.add(q("Voce entendeu as politicas eticas estabelecidas pelo administrador?", Set.of(r(DESENVOLVEDOR)), "Iniciação", stgs));
        qs.add(q("Voce entendeu os objetivos do software?", Set.of(r(DESENVOLVEDOR)), "Iniciação", stgs));
        qs.add(q("Orientacoes claras sobre como lidar com possiveis conflitos eticos foram fornecidas?", Set.of(r(DESENVOLVEDOR)), "Iniciação", stgs));
        qs.add(q("Voce tem as habilidades tecnicas e o conhecimento adequado para implementar o software de maneira etica?", Set.of(r(DESENVOLVEDOR)), "Iniciação", stgs));
        qs.add(q("O codigo fonte do software esta livre de quaisquer tipos de vies ou discriminacao?", Set.of(r(DESENVOLVEDOR)), "Iniciação", stgs));
        qs.add(q("As possiveis implicacoes eticas do software na seguranca e privacidade dos usuarios foram consideradas?", Set.of(r(DESENVOLVEDOR)), "Iniciação", stgs));
        qs.add(q("Voce esta comprometido em seguir as politicas eticas estabelecidas e em desenvolver o software de maneira etica?", Set.of(r(DESENVOLVEDOR)), "Iniciação", stgs));
        qs.add(q("Voce esta disposto a reportar possiveis problemas eticos identificados durante o desenvolvimento do software?", Set.of(r(DESENVOLVEDOR)), "Iniciação", stgs));
        // -- ANALISTA DE QUALIDADE --
        qs.add(q("Voce entendeu as politicas eticas estabelecidas pelo administrador?", Set.of(r(ANALISTA_QUALIDADE)), "Iniciação", stgs));
        qs.add(q("Voce entendeu os objetivos eticos do software?", Set.of(r(ANALISTA_QUALIDADE)), "Iniciação", stgs));
        qs.add(q("Foram fornecidas orientacoes claras sobre como lidar com possiveis conflitos eticos durante o processo de desenvolvimento?", Set.of(r(ANALISTA_QUALIDADE)), "Iniciação", stgs));
        qs.add(q("Voce tem as habilidades tecnicas e o conhecimento adequado para avaliar se o software esta sendo desenvolvido de maneira etica?", Set.of(r(ANALISTA_QUALIDADE)), "Iniciação", stgs));
        qs.add(q("Voce esta ciente das possiveis implicacoes eticas do software na seguranca e privacidade dos usuarios?", Set.of(r(ANALISTA_QUALIDADE)), "Iniciação", stgs));
        qs.add(q("O plano de qualidade inclui criterios eticos para avaliar o software?", Set.of(r(ANALISTA_QUALIDADE)), "Iniciação", stgs));
        qs.add(q("Voce esta disposto a reportar possiveis problemas eticos identificados durante o desenvolvimento do software?", Set.of(r(ANALISTA_QUALIDADE)), "Iniciação", stgs));
        qs.add(q("Voce esta comprometido em garantir que o software seja desenvolvido de maneira etica?", Set.of(r(ANALISTA_QUALIDADE)), "Iniciação", stgs));
        qs.add(q("Existem criterios para aprovacao de iniciacao de produto, que devem ser particularmente exigentes para areas nas quais a organizacao ou desenvolvedor nao e especialista?", Set.of(r(ANALISTA_QUALIDADE)), "Iniciação", stgs));
        qs.add(q("Foi determinado quem aprova a flexibilidade orcamentaria para o gerenciamento projeto?", Set.of(r(ANALISTA_QUALIDADE)), "Iniciação", stgs));
        qs.add(q("Os riscos eticos do projeto foram documentados e comunicados a todos os envolvidos?", Set.of(r(GERENTE_PROJETO), r(ANALISTA_QUALIDADE), r(RESPONSAVEL_NEGOCIO)), "Iniciação", stgs));
        qs.add(q("Existe um canal definido para reportar preocupacoes eticas de forma anonima e segura?", Set.of(r(GERENTE_PROJETO), r(LIDER_EQUIPE), r(SUPORTE)), "Iniciação", stgs));
        qs.add(q("Os valores eticos do projeto estao explicitamente documentados e acessiveis a todos?", Set.of(r(GERENTE_PROJETO), r(LIDER_EQUIPE), r(CLIENTE)), "Iniciação", stgs));
        return qs;
    }

    private List<TemplateQuestionDTO> buildRequisitos(List<StageSummaryResponseDTO> stgs) {
        List<TemplateQuestionDTO> qs = new ArrayList<>();
        // -- CLIENTE --
        qs.add(q("Voce esta considerando as implicacoes eticas dos requisitos que esta solicitando?", Set.of(r(CLIENTE)), "Requisitos", stgs));
        qs.add(q("Voce esta disposto a ajustar os requisitos caso haja preocupacoes eticas levantadas pela equipe de desenvolvimento?", Set.of(r(CLIENTE)), "Requisitos", stgs));
        qs.add(q("Voce esta garantindo que o software a ser desenvolvido respeite a privacidade dos usuarios finais e seus dados?", Set.of(r(CLIENTE)), "Requisitos", stgs));
        qs.add(q("Voce esta considerando a inclusao de recursos de acessibilidade no software para atender as necessidades de usuarios com deficiencias?", Set.of(r(CLIENTE)), "Requisitos", stgs));
        qs.add(q("Voce esta considerando as implicacoes de longo prazo do software para as pessoas e a sociedade como um todo?", Set.of(r(CLIENTE), r(STAKEHOLDER)), "Requisitos", stgs));
        qs.add(q("Voce esta garantindo que os requisitos eticos estejam sendo incorporados ao processo de desenvolvimento de software?", Set.of(r(CLIENTE)), "Requisitos", stgs));
        qs.add(q("Voce esta disposto a trabalhar com a equipe de desenvolvimento para encontrar solucoes que atendam aos requisitos eticos e aos objetivos do projeto?", Set.of(r(CLIENTE)), "Requisitos", stgs));
        qs.add(q("Voce esta aberto a receber feedback sobre as implicacoes eticas dos requisitos que esta solicitando?", Set.of(r(CLIENTE)), "Requisitos", stgs));
        // -- GERENTE DE PROJETO --
        qs.add(q("Voce esta assegurando que os requisitos estejam em conformidade com as politicas da organizacao?", Set.of(r(GERENTE_PROJETO)), "Requisitos", stgs));
        qs.add(q("Foi determinado quem define os requisitos?", Set.of(r(GERENTE_PROJETO)), "Requisitos", stgs));
        qs.add(q("Voce esta considerando a viabilidade e os recursos necessarios para implementar os requisitos solicitados?", Set.of(r(GERENTE_PROJETO)), "Requisitos", stgs));
        qs.add(q("Voce esta garantindo que os requisitos possam ser medidos e testados para verificacao de conformidade?", Set.of(r(GERENTE_PROJETO)), "Requisitos", stgs));
        qs.add(q("Voce esta considerando a seguranca cibernetica como parte dos requisitos do software a ser desenvolvido?", Set.of(r(GERENTE_PROJETO)), "Requisitos", stgs));
        qs.add(q("Voce esta assegurando que as especificacoes de requisitos sejam claras e precisas para evitar mal-entendidos?", Set.of(r(GERENTE_PROJETO)), "Requisitos", stgs));
        qs.add(q("Voce esta disposto a trabalhar com a equipe de desenvolvimento para encontrar solucoes que atendam aos requisitos e aos objetivos do projeto?", Set.of(r(GERENTE_PROJETO)), "Requisitos", stgs));
        qs.add(q("Voce esta disposto a reavaliar os requisitos se as preocupacoes eticas ou outras preocupacoes forem levantadas pela equipe de desenvolvimento?", Set.of(r(GERENTE_PROJETO)), "Requisitos", stgs));
        qs.add(q("Voce esta garantindo que os requisitos sejam atualizados e modificados, se necessario, a medida que o projeto evolui?", Set.of(r(GERENTE_PROJETO)), "Requisitos", stgs));
        // -- LIDER DE EQUIPE --
        qs.add(q("O escopo do projeto esta claro e completo?", Set.of(r(LIDER_EQUIPE)), "Requisitos", stgs));
        qs.add(q("Os requisitos sao claramente definidos e verificaveis?", Set.of(r(LIDER_EQUIPE), r(ANALISTA_REQUISITOS)), "Requisitos", stgs));
        qs.add(q("O prazo para a entrega dos requisitos e razoavel e viavel?", Set.of(r(LIDER_EQUIPE)), "Requisitos", stgs));
        qs.add(q("O orcamento para a implementacao dos requisitos e adequado e realista?", Set.of(r(LIDER_EQUIPE)), "Requisitos", stgs));
        qs.add(q("Os requisitos estao alinhados com as necessidades do cliente?", Set.of(r(LIDER_EQUIPE)), "Requisitos", stgs));
        qs.add(q("O cliente forneceu feedback sobre os requisitos propostos?", Set.of(r(LIDER_EQUIPE)), "Requisitos", stgs));
        qs.add(q("Existem requisitos de seguranca, privacidade e protecao de dados que precisam ser considerados?", Set.of(r(LIDER_EQUIPE)), "Requisitos", stgs));
        qs.add(q("O cliente esta disposto a trabalhar em conjunto para a definicao e revisao dos requisitos?", Set.of(r(LIDER_EQUIPE)), "Requisitos", stgs));
        qs.add(q("Voce esta garantindo que o software a ser desenvolvido respeite a privacidade dos usuarios finais e seus dados?", Set.of(r(LIDER_EQUIPE)), "Requisitos", stgs));
        qs.add(q("A documentacao dos requisitos e completa e precisa?", Set.of(r(LIDER_EQUIPE), r(ANALISTA_REQUISITOS)), "Requisitos", stgs));
        // -- ARQUITETO DE SOFTWARE --
        qs.add(q("Os requisitos foram entendidos e documentados de maneira clara?", Set.of(r(ARQUITETO_SOFTWARE)), "Requisitos", stgs));
        qs.add(q("O design proposto e coerente com os requisitos do cliente?", Set.of(r(ARQUITETO_SOFTWARE)), "Requisitos", stgs));
        qs.add(q("O design considera aspectos de usabilidade e experiencia do usuario?", Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER)), "Requisitos", stgs));
        qs.add(q("O design proposto e viavel em termos de implementacao tecnica?", Set.of(r(ARQUITETO_SOFTWARE)), "Requisitos", stgs));
        qs.add(q("O design considera aspectos de escalabilidade e manutenibilidade?", Set.of(r(ARQUITETO_SOFTWARE)), "Requisitos", stgs));
        qs.add(q("As expectativas do cliente em relacao ao design foram claramente comunicadas e estao sendo atendidas?", Set.of(r(ARQUITETO_SOFTWARE)), "Requisitos", stgs));
        qs.add(q("O designer esta disposto a trabalhar em conjunto com outras partes interessadas para a definicao e revisao dos requisitos?", Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER)), "Requisitos", stgs));
        qs.add(q("Voce esta considerando a inclusao de recursos de acessibilidade no software para atender as necessidades de usuarios com deficiencias?", Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER)), "Requisitos", stgs));
        // -- DESENVOLVEDOR --
        qs.add(q("Os requisitos sao claros e compreensiveis?", Set.of(r(DESENVOLVEDOR)), "Requisitos", stgs));
        qs.add(q("A documentacao dos requisitos e completa e precisa?", Set.of(r(DESENVOLVEDOR)), "Requisitos", stgs));
        qs.add(q("Os requisitos sao viaveis tecnicamente?", Set.of(r(DESENVOLVEDOR)), "Requisitos", stgs));
        qs.add(q("Os requisitos atendem as necessidades do usuario?", Set.of(r(DESENVOLVEDOR)), "Requisitos", stgs));
        qs.add(q("Existe um processo de revisao e aprovacao dos requisitos?", Set.of(r(DESENVOLVEDOR)), "Requisitos", stgs));
        qs.add(q("Ha um processo formal de mudanca de requisitos em vigor?", Set.of(r(DESENVOLVEDOR)), "Requisitos", stgs));
        qs.add(q("O prazo para a conclusao dos requisitos e realista?", Set.of(r(DESENVOLVEDOR)), "Requisitos", stgs));
        qs.add(q("Os requisitos estao alinhados com as expectativas do cliente?", Set.of(r(DESENVOLVEDOR)), "Requisitos", stgs));
        qs.add(q("Voce esta garantindo que o software a ser desenvolvido respeite a privacidade dos usuarios finais e seus dados?", Set.of(r(DESENVOLVEDOR)), "Requisitos", stgs));
        // -- ANALISTA DE QUALIDADE --
        qs.add(q("Os requisitos do projeto sao claros e precisos?", Set.of(r(ANALISTA_QUALIDADE)), "Requisitos", stgs));
        qs.add(q("Os requisitos estao documentados?", Set.of(r(ANALISTA_QUALIDADE)), "Requisitos", stgs));
        qs.add(q("Os requisitos sao verificaveis e mensuraveis?", Set.of(r(ANALISTA_QUALIDADE), r(ANALISTA_REQUISITOS)), "Requisitos", stgs));
        qs.add(q("As necessidades dos usuarios foram levantadas e consideradas nos requisitos?", Set.of(r(ANALISTA_QUALIDADE)), "Requisitos", stgs));
        qs.add(q("Os requisitos estao alinhados com as expectativas do cliente?", Set.of(r(ANALISTA_QUALIDADE)), "Requisitos", stgs));
        qs.add(q("Os requisitos sao factiveis dentro do cronograma e do orcamento estabelecidos?", Set.of(r(ANALISTA_QUALIDADE)), "Requisitos", stgs));
        qs.add(q("Os requisitos foram validados com todas as partes interessadas?", Set.of(r(ANALISTA_QUALIDADE)), "Requisitos", stgs));
        qs.add(q("Existem mecanismos para determinar o caminho a seguir quando se descobre que um ou mais requisitos funcionais nao podem ser alcancados?", Set.of(r(ANALISTA_QUALIDADE)), "Requisitos", stgs));
        qs.add(q("Existem mecanismos para determinar o caminho da acao quando o cliente insiste?", Set.of(r(ANALISTA_QUALIDADE)), "Requisitos", stgs));
        qs.add(q("Existe um procedimento para definir o curso de acao se, durante as fases de planejamento dos requisitos, for descoberto que o atendimento a demanda do cliente levara a consequencias negativas e possivelmente resultados contraditorios e ilegais?", Set.of(r(ANALISTA_QUALIDADE)), "Requisitos", stgs));
        qs.add(q("O processo de requisitos esta de acordo com as politicas e normas da organizacao?", Set.of(r(ANALISTA_QUALIDADE)), "Requisitos", stgs));
        qs.add(q("Existe um mecanismo para priorizar os requisitos?", Set.of(r(ANALISTA_QUALIDADE), r(ANALISTA_REQUISITOS)), "Requisitos", stgs));
        qs.add(q("O prazo para a conclusao dos requisitos e realista?", Set.of(r(ANALISTA_QUALIDADE)), "Requisitos", stgs));
        qs.add(q("O cliente forneceu feedback sobre os requisitos propostos?", Set.of(r(ANALISTA_QUALIDADE)), "Requisitos", stgs));
        qs.add(q("Os requisitos estao alinhados com as necessidades do cliente?", Set.of(r(ANALISTA_QUALIDADE)), "Requisitos", stgs));
        qs.add(q("O cliente esta disposto a trabalhar em conjunto para a definicao e revisao dos requisitos?", Set.of(r(ANALISTA_QUALIDADE)), "Requisitos", stgs));
        qs.add(q("O orcamento para a implementacao dos requisitos e adequado e realista?", Set.of(r(ANALISTA_QUALIDADE)), "Requisitos", stgs));
        qs.add(q("A documentacao dos requisitos e completa e precisa?", Set.of(r(ANALISTA_QUALIDADE)), "Requisitos", stgs));

        qs.add(q("Foram identificados requisitos que possam impactar negativamente grupos vulneraveis ou minorias?", Set.of(r(ANALISTA_REQUISITOS), r(CLIENTE), r(STAKEHOLDER)), "Requisitos", stgs));
        qs.add(q("Os requisitos contemplam conformidade com legislacao de protecao de dados (ex: LGPD, GDPR)?", Set.of(r(GERENTE_PROJETO), r(ANALISTA_REQUISITOS), r(RESPONSAVEL_NEGOCIO)), "Requisitos", stgs));
        return qs;
    }

    private List<TemplateQuestionDTO> buildProjeto(List<StageSummaryResponseDTO> stgs) {
        List<TemplateQuestionDTO> qs = new ArrayList<>();
        // -- CLIENTE --
        qs.add(q("Os requisitos do software foram claramente entendidos?", Set.of(r(CLIENTE)), "Projeto", stgs));
        qs.add(q("Os requisitos estao claramente descritos na documentacao?", Set.of(r(CLIENTE)), "Projeto", stgs));
        qs.add(q("Foram definidos prazos realistas para o projeto?", Set.of(r(CLIENTE)), "Projeto", stgs));
        qs.add(q("A equipe de desenvolvimento tem conhecimento tecnico suficiente para o projeto?", Set.of(r(CLIENTE)), "Projeto", stgs));
        qs.add(q("O projeto esta alinhado com as expectativas do cliente?", Set.of(r(CLIENTE), r(STAKEHOLDER)), "Projeto", stgs));
        qs.add(q("As condicoes sao definidas para determinar o nivel de comprometimento com o desenvolvimento de uma abordagem de reuso?", Set.of(r(CLIENTE)), "Projeto", stgs));
        // -- GERENTE DE PROJETO --
        qs.add(q("O orcamento do projeto foi definido?", Set.of(r(GERENTE_PROJETO)), "Projeto", stgs));
        qs.add(q("A equipe de desenvolvimento esta disponivel para o projeto?", Set.of(r(GERENTE_PROJETO)), "Projeto", stgs));
        qs.add(q("O cronograma do projeto e factivel?", Set.of(r(GERENTE_PROJETO)), "Projeto", stgs));
        qs.add(q("O escopo do projeto esta claramente definido?", Set.of(r(GERENTE_PROJETO)), "Projeto", stgs));
        qs.add(q("Os recursos necessarios para o projeto foram identificados?", Set.of(r(GERENTE_PROJETO)), "Projeto", stgs));
        qs.add(q("As condicoes sao definidas para determinar o nivel de comprometimento com o desenvolvimento de uma abordagem de reuso?", Set.of(r(GERENTE_PROJETO)), "Projeto", stgs));
        qs.add(q("As condicoes sao definidas para determinar o nivel de comprometimento com o desenvolvimento de todas as metodologias de padroes?", Set.of(r(GERENTE_PROJETO)), "Projeto", stgs));
        qs.add(q("Alguem que nao seja especialista pode orientar os diagramas?", Set.of(r(GERENTE_PROJETO)), "Projeto", stgs));
        qs.add(q("Voce acha que deveria usar mais diagramas de projeto?", Set.of(r(GERENTE_PROJETO)), "Projeto", stgs));
        qs.add(q("Os diagramas sao suficientemente claros?", Set.of(r(GERENTE_PROJETO)), "Projeto", stgs));
        qs.add(q("O nivel de detalhamento e suficiente?", Set.of(r(GERENTE_PROJETO)), "Projeto", stgs));
        qs.add(q("Seria facil se orientar nos diagramas de projeto?", Set.of(r(GERENTE_PROJETO)), "Projeto", stgs));
        // -- LIDER DE EQUIPE --
        qs.add(q("Os membros da equipe possuem as habilidades necessarias para o projeto?", Set.of(r(LIDER_EQUIPE)), "Projeto", stgs));
        qs.add(q("A equipe tem conhecimento suficiente da tecnologia a ser utilizada no projeto?", Set.of(r(LIDER_EQUIPE)), "Projeto", stgs));
        qs.add(q("As tarefas foram distribuidas de forma equilibrada na equipe?", Set.of(r(LIDER_EQUIPE)), "Projeto", stgs));
        qs.add(q("O plano de comunicacao da equipe foi definido?", Set.of(r(LIDER_EQUIPE)), "Projeto", stgs));
        qs.add(q("O lider da equipe tem autoridade suficiente para tomar decisoes?", Set.of(r(LIDER_EQUIPE)), "Projeto", stgs));
        qs.add(q("As condicoes sao definidas para determinar o nivel de comprometimento com o desenvolvimento de uma abordagem de reuso?", Set.of(r(LIDER_EQUIPE)), "Projeto", stgs));
        qs.add(q("As condicoes sao definidas para determinar o nivel de comprometimento com o desenvolvimento de todas as metodologias de padroes?", Set.of(r(LIDER_EQUIPE)), "Projeto", stgs));
        qs.add(q("O nivel de detalhamento e suficiente?", Set.of(r(LIDER_EQUIPE)), "Projeto", stgs));
        qs.add(q("Os diagramas sao suficientemente claros?", Set.of(r(LIDER_EQUIPE)), "Projeto", stgs));
        // -- ARQUITETO DE SOFTWARE --
        qs.add(q("O design proposto atende as expectativas do cliente?", Set.of(r(ARQUITETO_SOFTWARE)), "Projeto", stgs));
        qs.add(q("O design e viavel do ponto de vista tecnico?", Set.of(r(ARQUITETO_SOFTWARE)), "Projeto", stgs));
        qs.add(q("O design atende aos requisitos do projeto?", Set.of(r(ARQUITETO_SOFTWARE)), "Projeto", stgs));
        qs.add(q("O design segue as boas praticas de usabilidade e acessibilidade?", Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER)), "Projeto", stgs));
        qs.add(q("O design esta de acordo com as diretrizes de marca da empresa?", Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER)), "Projeto", stgs));
        qs.add(q("O design leva em consideracao possiveis problemas eticos, como vieses ou discriminacao?", Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER)), "Projeto", stgs));
        qs.add(q("O nivel de detalhamento e suficiente?", Set.of(r(ARQUITETO_SOFTWARE)), "Projeto", stgs));
        qs.add(q("O design inclui opcoes de personalizacao para atender a diferentes perfis de usuarios?", Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER)), "Projeto", stgs));
        qs.add(q("O design inclui opcoes de feedback para que os usuarios possam fornecer suas opinioes e sugestoes?", Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER)), "Projeto", stgs));
        // -- DESENVOLVEDOR --
        qs.add(q("Os requisitos do projeto sao claros e compreensiveis?", Set.of(r(DESENVOLVEDOR)), "Projeto", stgs));
        qs.add(q("As tecnologias utilizadas sao apropriadas para o projeto?", Set.of(r(DESENVOLVEDOR)), "Projeto", stgs));
        qs.add(q("As funcionalidades do software sao desenvolvidas seguindo as especificacoes dos requisitos?", Set.of(r(DESENVOLVEDOR)), "Projeto", stgs));
        qs.add(q("O codigo produzido segue as boas praticas de programacao?", Set.of(r(DESENVOLVEDOR)), "Projeto", stgs));
        qs.add(q("O software inclui medidas de seguranca para proteger os dados do usuario?", Set.of(r(DESENVOLVEDOR)), "Projeto", stgs));
        qs.add(q("As condicoes sao definidas para determinar o nivel de comprometimento com o desenvolvimento de uma abordagem de reuso?", Set.of(r(DESENVOLVEDOR)), "Projeto", stgs));
        qs.add(q("O software e projetado para ser escalavel e extensivel, permitindo a adicao de novas funcionalidades no futuro?", Set.of(r(DESENVOLVEDOR)), "Projeto", stgs));
        qs.add(q("O software e projetado para ser de facil manutencao e solucao de problemas?", Set.of(r(DESENVOLVEDOR)), "Projeto", stgs));
        qs.add(q("O nivel de detalhamento e suficiente?", Set.of(r(DESENVOLVEDOR)), "Projeto", stgs));
        qs.add(q("Os diagramas sao suficientemente claros?", Set.of(r(DESENVOLVEDOR)), "Projeto", stgs));
        qs.add(q("O desenvolvimento inclui testes unitarios e de integracao para garantir a qualidade do codigo?", Set.of(r(DESENVOLVEDOR)), "Projeto", stgs));
        // -- ANALISTA DE QUALIDADE --
        qs.add(q("Os requisitos do projeto foram corretamente interpretados e documentados?", Set.of(r(ANALISTA_QUALIDADE)), "Projeto", stgs));
        qs.add(q("Os testes de software sao realizados de acordo com as especificacoes dos requisitos?", Set.of(r(ANALISTA_QUALIDADE)), "Projeto", stgs));
        qs.add(q("Os testes sao realizados em diferentes cenarios e condicoes para garantir a robustez do software?", Set.of(r(ANALISTA_QUALIDADE)), "Projeto", stgs));
        qs.add(q("Os resultados dos testes sao documentados e compartilhados com a equipe para acompanhamento e correcao de problemas?", Set.of(r(ANALISTA_QUALIDADE)), "Projeto", stgs));
        qs.add(q("O software e testado em diferentes plataformas e dispositivos para garantir sua compatibilidade?", Set.of(r(ANALISTA_QUALIDADE)), "Projeto", stgs));
        qs.add(q("O software e testado em diferentes cenarios de uso, considerando diferentes perfis de usuario?", Set.of(r(ANALISTA_QUALIDADE)), "Projeto", stgs));
        qs.add(q("O nivel de detalhamento e suficiente?", Set.of(r(ANALISTA_QUALIDADE)), "Projeto", stgs));
        qs.add(q("As condicoes sao definidas para determinar o nivel de comprometimento com o desenvolvimento de uma abordagem de reuso?", Set.of(r(ANALISTA_QUALIDADE)), "Projeto", stgs));
        qs.add(q("Os diagramas sao suficientemente claros?", Set.of(r(ANALISTA_QUALIDADE)), "Projeto", stgs));
        qs.add(q("As metricas de qualidade, como taxa de defeitos e satisfacao do usuario, sao monitoradas e avaliadas regularmente?", Set.of(r(ANALISTA_QUALIDADE)), "Projeto", stgs));
        qs.add(q("Sao realizados testes de seguranca para detectar possiveis vulnerabilidades e prevenir ataques ciberneticos?", Set.of(r(ANALISTA_QUALIDADE)), "Projeto", stgs));

        qs.add(q("As decisoes arquiteturais consideram o impacto etico de longo prazo sobre usuarios e sociedade?", Set.of(r(ARQUITETO_SOFTWARE), r(GERENTE_PROJETO)), "Projeto", stgs));
        qs.add(q("Existe rastreabilidade entre decisoes de projeto e requisitos eticos documentados?", Set.of(r(ANALISTA_QUALIDADE), r(ANALISTA_REQUISITOS)), "Projeto", stgs));
        return qs;
    }

    private List<TemplateQuestionDTO> buildDesenvolvimento(List<StageSummaryResponseDTO> stgs) {
        List<TemplateQuestionDTO> qs = new ArrayList<>();
        // -- CLIENTE --
        qs.add(q("Os requisitos estao sendo atendidos?", Set.of(r(CLIENTE)), "Desenvolvimento", stgs));
        qs.add(q("O projeto esta dentro do orcamento planejado?", Set.of(r(CLIENTE)), "Desenvolvimento", stgs));
        qs.add(q("O projeto esta dentro do cronograma planejado?", Set.of(r(CLIENTE)), "Desenvolvimento", stgs));
        qs.add(q("As funcionalidades estao sendo desenvolvidas de acordo com o que foi especificado?", Set.of(r(CLIENTE)), "Desenvolvimento", stgs));
        qs.add(q("As expectativas estao sendo atendidas em relacao a qualidade do produto?", Set.of(r(CLIENTE), r(STAKEHOLDER)), "Desenvolvimento", stgs));
        // -- GERENTE DE PROJETO --
        qs.add(q("O projeto esta seguindo o cronograma estabelecido?", Set.of(r(GERENTE_PROJETO)), "Desenvolvimento", stgs));
        qs.add(q("As tarefas dos membros da equipe estao sendo bem definidas?", Set.of(r(GERENTE_PROJETO)), "Desenvolvimento", stgs));
        qs.add(q("Os recursos necessarios para o desenvolvimento estao sendo providenciados?", Set.of(r(GERENTE_PROJETO)), "Desenvolvimento", stgs));
        qs.add(q("O projeto esta respeitando as normas e politicas de seguranca da empresa?", Set.of(r(GERENTE_PROJETO)), "Desenvolvimento", stgs));
        qs.add(q("As etapas de testes estao sendo bem documentadas?", Set.of(r(GERENTE_PROJETO)), "Desenvolvimento", stgs));
        qs.add(q("O progresso do projeto esta sendo relatado de forma clara e concisa?", Set.of(r(GERENTE_PROJETO)), "Desenvolvimento", stgs));
        qs.add(q("As alteracoes de escopo estao sendo gerenciadas adequadamente?", Set.of(r(GERENTE_PROJETO)), "Desenvolvimento", stgs));
        qs.add(q("Existem condicoes para integrar os recursos avancados do gerenciamento de codigo? Horarios? Parametros ambientais?", Set.of(r(GERENTE_PROJETO)), "Desenvolvimento", stgs));
        qs.add(q("Existem condicoes para a integracao de desenvolvedores jovens e menos experientes?", Set.of(r(GERENTE_PROJETO), r(LIDER_EQUIPE)), "Desenvolvimento", stgs));
        qs.add(q("A comunicacao entre os membros da equipe esta sendo eficaz?", Set.of(r(GERENTE_PROJETO), r(LIDER_EQUIPE)), "Desenvolvimento", stgs));
        // -- LIDER DE EQUIPE --
        qs.add(q("O cronograma de desenvolvimento esta sendo cumprido?", Set.of(r(LIDER_EQUIPE)), "Desenvolvimento", stgs));
        qs.add(q("A equipe tem recursos suficientes para desenvolver o projeto dentro do prazo e orcamento?", Set.of(r(LIDER_EQUIPE)), "Desenvolvimento", stgs));
        qs.add(q("O progresso do projeto esta sendo monitorado regularmente e comunicado a equipe e partes interessadas?", Set.of(r(LIDER_EQUIPE)), "Desenvolvimento", stgs));
        qs.add(q("A equipe esta seguindo as melhores praticas de desenvolvimento de software?", Set.of(r(LIDER_EQUIPE)), "Desenvolvimento", stgs));
        qs.add(q("A equipe esta aderindo as normas e padroes definidos para o projeto?", Set.of(r(LIDER_EQUIPE)), "Desenvolvimento", stgs));
        qs.add(q("As condicoes foram definidas para selecionar o idioma de codificacao?", Set.of(r(LIDER_EQUIPE)), "Desenvolvimento", stgs));
        qs.add(q("As mudancas de requisitos sao gerenciadas e controladas adequadamente?", Set.of(r(LIDER_EQUIPE)), "Desenvolvimento", stgs));
        qs.add(q("As falhas e defeitos sao registrados e tratados corretamente?", Set.of(r(LIDER_EQUIPE)), "Desenvolvimento", stgs));
        qs.add(q("As expectativas dos stakeholders em relacao a qualidade do produto estao sendo atendidas?", Set.of(r(LIDER_EQUIPE)), "Desenvolvimento", stgs));
        // -- ARQUITETO DE SOFTWARE --
        qs.add(q("As interfaces de usuario estao sendo desenvolvidas com base nas especificacoes de design?", Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER)), "Desenvolvimento", stgs));
        qs.add(q("As telas de interface estao sendo testadas e revisadas conforme o feedback dos usuarios e a conformidade com os padroes de design?", Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER)), "Desenvolvimento", stgs));
        qs.add(q("As animacoes e efeitos visuais estao sendo integrados de acordo com as diretrizes de design?", Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER)), "Desenvolvimento", stgs));
        qs.add(q("As fontes, cores e icones estao sendo utilizados de acordo com o manual de identidade visual da empresa?", Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER)), "Desenvolvimento", stgs));
        qs.add(q("As interfaces estao sendo desenvolvidas para serem responsivas e adaptaveis a diferentes tamanhos de tela?", Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER)), "Desenvolvimento", stgs));
        qs.add(q("O design da interface esta sendo desenvolvido de forma a facilitar a usabilidade e a acessibilidade para pessoas com deficiencia?", Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER)), "Desenvolvimento", stgs));
        qs.add(q("O design da interface esta sendo desenvolvido de forma a promover a coerencia e a consistencia em todas as telas?", Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER)), "Desenvolvimento", stgs));
        qs.add(q("As animacoes e transicoes de interface estao sendo desenvolvidas de forma a proporcionar uma experiencia de usuario agradavel?", Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER)), "Desenvolvimento", stgs));
        // -- DESENVOLVEDOR --
        qs.add(q("Os requisitos estao claramente definidos e documentados?", Set.of(r(DESENVOLVEDOR)), "Desenvolvimento", stgs));
        qs.add(q("Os casos de teste foram definidos e documentados para cada requisito?", Set.of(r(DESENVOLVEDOR)), "Desenvolvimento", stgs));
        qs.add(q("A arquitetura do software foi definida e documentada?", Set.of(r(DESENVOLVEDOR)), "Desenvolvimento", stgs));
        qs.add(q("Os padroes de codificacao foram definidos e documentados?", Set.of(r(DESENVOLVEDOR)), "Desenvolvimento", stgs));
        qs.add(q("Foi definido um processo de revisao de codigo?", Set.of(r(DESENVOLVEDOR)), "Desenvolvimento", stgs));
        qs.add(q("O codigo e compativel com os diagramas de projeto?", Set.of(r(DESENVOLVEDOR)), "Desenvolvimento", stgs));
        qs.add(q("Os comentarios tem clareza suficiente?", Set.of(r(DESENVOLVEDOR)), "Desenvolvimento", stgs));
        qs.add(q("E facil descrever para outra pessoa o codigo que voce escreveu?", Set.of(r(DESENVOLVEDOR)), "Desenvolvimento", stgs));
        qs.add(q("O controle de versao esta sendo utilizado adequadamente?", Set.of(r(DESENVOLVEDOR)), "Desenvolvimento", stgs));
        qs.add(q("O ambiente de desenvolvimento esta configurado corretamente?", Set.of(r(DESENVOLVEDOR)), "Desenvolvimento", stgs));
        qs.add(q("Foram definidos e documentados criterios de aceitacao para a entrega do software?", Set.of(r(DESENVOLVEDOR)), "Desenvolvimento", stgs));
        // -- ANALISTA DE QUALIDADE --
        qs.add(q("O codigo fonte esta sendo versionado adequadamente?", Set.of(r(ANALISTA_QUALIDADE)), "Desenvolvimento", stgs));
        qs.add(q("Os testes unitarios estao sendo executados e cobrindo a maioria das funcionalidades do software?", Set.of(r(ANALISTA_QUALIDADE)), "Desenvolvimento", stgs));
        qs.add(q("As correcoes de bugs estao sendo registradas e tratadas de maneira apropriada?", Set.of(r(ANALISTA_QUALIDADE)), "Desenvolvimento", stgs));
        qs.add(q("Os requisitos de desempenho estao sendo atendidos de acordo com as expectativas?", Set.of(r(ANALISTA_QUALIDADE)), "Desenvolvimento", stgs));
        qs.add(q("As metricas de qualidade do codigo (ex: analise estatica) estao sendo monitoradas e utilizadas para orientar as decisoes do time?", Set.of(r(ANALISTA_QUALIDADE)), "Desenvolvimento", stgs));
        qs.add(q("O processo de deploy esta sendo automatizado e documentado adequadamente?", Set.of(r(ANALISTA_QUALIDADE)), "Desenvolvimento", stgs));

        qs.add(q("Existe revisao de codigo com foco em identificar vieses algoritmicos ou praticas discriminatorias?", Set.of(r(DESENVOLVEDOR), r(ANALISTA_QUALIDADE)), "Desenvolvimento", stgs));
        qs.add(q("Os dados utilizados no desenvolvimento respeitam as politicas de privacidade e consentimento?", Set.of(r(DESENVOLVEDOR), r(GERENTE_PROJETO)), "Desenvolvimento", stgs));
        qs.add(q("As decisoes tecnicas de trade-off estao sendo documentadas com justificativa etica?", Set.of(r(DESENVOLVEDOR), r(ARQUITETO_SOFTWARE), r(LIDER_EQUIPE)), "Desenvolvimento", stgs));
        return qs;
    }

    private List<TemplateQuestionDTO> buildTestes(List<StageSummaryResponseDTO> stgs) {
        List<TemplateQuestionDTO> qs = new ArrayList<>();
        // -- CLIENTE --
        qs.add(q("Os requisitos estabelecidos foram atendidos corretamente?", Set.of(r(CLIENTE)), "Testes", stgs));
        qs.add(q("O software apresenta erros ou comportamentos inesperados?", Set.of(r(CLIENTE)), "Testes", stgs));
        qs.add(q("As funcionalidades foram testadas de maneira abrangente?", Set.of(r(CLIENTE)), "Testes", stgs));
        qs.add(q("A interface do software esta intuitiva e facil de usar?", Set.of(r(CLIENTE)), "Testes", stgs));
        qs.add(q("O desempenho do software atende as expectativas estabelecidas?", Set.of(r(CLIENTE)), "Testes", stgs));
        qs.add(q("O nivel de erros permitidos foi determinado antes do produto ir para teste de aceitacao?", Set.of(r(CLIENTE)), "Testes", stgs));
        // -- GERENTE DE PROJETO --
        qs.add(q("O sistema foi testado em diferentes ambientes?", Set.of(r(GERENTE_PROJETO)), "Testes", stgs));
        qs.add(q("Foi verificado se o sistema e compativel com diferentes dispositivos?", Set.of(r(GERENTE_PROJETO)), "Testes", stgs));
        qs.add(q("Foram realizados testes de seguranca no sistema?", Set.of(r(GERENTE_PROJETO)), "Testes", stgs));
        qs.add(q("O sistema foi testado quanto a sua capacidade de lidar com altas cargas de trafego?", Set.of(r(GERENTE_PROJETO)), "Testes", stgs));
        qs.add(q("Foram realizados testes de backup e recuperacao de dados?", Set.of(r(GERENTE_PROJETO)), "Testes", stgs));
        qs.add(q("O nivel de erros permitidos foi determinado antes do produto ir para teste de aceitacao?", Set.of(r(GERENTE_PROJETO)), "Testes", stgs));
        qs.add(q("Existe um mecanismo que distingue entre um produto de qualidade versus produto correto?", Set.of(r(GERENTE_PROJETO)), "Testes", stgs));
        // -- LIDER DE EQUIPE --
        qs.add(q("Todos os requisitos foram implementados corretamente?", Set.of(r(LIDER_EQUIPE)), "Testes", stgs));
        qs.add(q("As funcionalidades estao atendendo as expectativas?", Set.of(r(LIDER_EQUIPE)), "Testes", stgs));
        qs.add(q("Os resultados dos testes unitarios estao satisfatorios?", Set.of(r(LIDER_EQUIPE)), "Testes", stgs));
        qs.add(q("Todos os testes foram concluidos com sucesso?", Set.of(r(LIDER_EQUIPE)), "Testes", stgs));
        qs.add(q("Foram encontrados bugs ou problemas?", Set.of(r(LIDER_EQUIPE)), "Testes", stgs));
        qs.add(q("O sistema esta respondendo adequadamente as solicitacoes de entrada?", Set.of(r(LIDER_EQUIPE)), "Testes", stgs));
        qs.add(q("Os usuarios estao conseguindo navegar facilmente na interface?", Set.of(r(LIDER_EQUIPE)), "Testes", stgs));
        qs.add(q("O sistema esta compativel com diferentes dispositivos e navegadores?", Set.of(r(LIDER_EQUIPE)), "Testes", stgs));
        // -- ARQUITETO DE SOFTWARE --
        qs.add(q("Todos os requisitos de design foram implementados no produto?", Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER)), "Testes", stgs));
        qs.add(q("A interface do usuario esta intuitiva e facil de usar?", Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER)), "Testes", stgs));
        qs.add(q("O design e consistente em todas as telas e funcionalidades do produto?", Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER)), "Testes", stgs));
        qs.add(q("As cores, fontes e elementos visuais estao em conformidade com a identidade visual da empresa?", Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER)), "Testes", stgs));
        qs.add(q("Todas as funcionalidades foram testadas para garantir que estejam funcionando corretamente?", Set.of(r(ARQUITETO_SOFTWARE)), "Testes", stgs));
        qs.add(q("O design esta em conformidade com as melhores praticas de usabilidade e acessibilidade?", Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER)), "Testes", stgs));
        qs.add(q("O design esta adequado para todas as resolucoes de tela e dispositivos?", Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER)), "Testes", stgs));
        qs.add(q("Todas as informacoes e feedbacks foram considerados e implementados no design do produto?", Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER)), "Testes", stgs));
        // -- DESENVOLVEDOR --
        qs.add(q("Voce entende as especificacoes do teste?", Set.of(r(DESENVOLVEDOR)), "Testes", stgs));
        qs.add(q("Voce implementou todos os requisitos de teste?", Set.of(r(DESENVOLVEDOR)), "Testes", stgs));
        qs.add(q("Voce atualizou o codigo-fonte para corrigir erros encontrados nos testes?", Set.of(r(DESENVOLVEDOR)), "Testes", stgs));
        qs.add(q("Voce documentou os resultados dos testes de unidade?", Set.of(r(DESENVOLVEDOR)), "Testes", stgs));
        qs.add(q("Voce implementou testes de integracao para garantir que o software funcione em conjunto?", Set.of(r(DESENVOLVEDOR)), "Testes", stgs));
        qs.add(q("Voce executou testes de estresse para garantir que o software possa lidar com cargas de trabalho pesadas?", Set.of(r(DESENVOLVEDOR)), "Testes", stgs));
        qs.add(q("Voce trabalhou com outros desenvolvedores para garantir que todos os modulos do software foram testados?", Set.of(r(DESENVOLVEDOR)), "Testes", stgs));
        qs.add(q("Voce realizou testes de regressao para garantir que correcoes e atualizacoes nao afetaram outras areas do software?", Set.of(r(DESENVOLVEDOR)), "Testes", stgs));
        // -- ANALISTA DE QUALIDADE --
        qs.add(q("Todas as funcionalidades especificadas nos requisitos foram testadas?", Set.of(r(ANALISTA_QUALIDADE)), "Testes", stgs));
        qs.add(q("Todas as funcionalidades testadas passaram nos testes unitarios?", Set.of(r(ANALISTA_QUALIDADE)), "Testes", stgs));
        qs.add(q("Foram realizados testes de integracao entre as diferentes partes do sistema?", Set.of(r(ANALISTA_QUALIDADE)), "Testes", stgs));
        qs.add(q("As metricas para determinar o nivel de treinamento exigido do testador estao definidas?", Set.of(r(ANALISTA_QUALIDADE)), "Testes", stgs));
        qs.add(q("Foram realizados testes de performance do sistema?", Set.of(r(ANALISTA_QUALIDADE)), "Testes", stgs));
        qs.add(q("Existe um mecanismo que determina se os testes necessarios foram realizados?", Set.of(r(ANALISTA_QUALIDADE)), "Testes", stgs));
        qs.add(q("Existe um conjunto de testes obrigatorios?", Set.of(r(ANALISTA_QUALIDADE)), "Testes", stgs));
        qs.add(q("Foram identificados e corrigidos todos os bugs e erros encontrados durante os testes?", Set.of(r(ANALISTA_QUALIDADE)), "Testes", stgs));
        qs.add(q("Foram realizados testes de seguranca para identificar possiveis vulnerabilidades no sistema?", Set.of(r(ANALISTA_QUALIDADE)), "Testes", stgs));
        qs.add(q("Todas as funcionalidades do sistema estao em conformidade com as normas e padroes estabelecidos?", Set.of(r(ANALISTA_QUALIDADE)), "Testes", stgs));
        qs.add(q("O nivel de erros permitidos foi determinado antes do produto ir para teste de aceitacao?", Set.of(r(ANALISTA_QUALIDADE)), "Testes", stgs));
        qs.add(q("Existe um mecanismo que distingue entre um produto de qualidade versus produto correto?", Set.of(r(ANALISTA_QUALIDADE)), "Testes", stgs));
        qs.add(q("Foram realizados testes de usabilidade para avaliar a experiencia do usuario com o sistema?", Set.of(r(ANALISTA_QUALIDADE)), "Testes", stgs));

        qs.add(q("Foram realizados testes especificos para detectar vieses nos resultados do software?", Set.of(r(ANALISTA_QUALIDADE), r(DESENVOLVEDOR)), "Testes", stgs));
        qs.add(q("Os cenarios de teste incluem perfis de usuarios vulneraveis ou com necessidades especiais?", Set.of(r(ANALISTA_QUALIDADE), r(DESIGNER)), "Testes", stgs));
        qs.add(q("Os resultados dos testes eticos sao compartilhados com os stakeholders para validacao?", Set.of(r(ANALISTA_QUALIDADE), r(GERENTE_PROJETO), r(STAKEHOLDER)), "Testes", stgs));
        return qs;
    }

    private List<TemplateRepresentativeDTO> buildBaseRepresentatives() {
        return List.of(
                rep("clayton.fraga@ufes.br", "Clayton", "Fraga", "1.00",
                        Set.of(r(CLIENTE), r(STAKEHOLDER))),
                rep("yves.silva@edu.ufes.br", "Gabriel", "Nama", "2.00",
                        Set.of(r(GERENTE_PROJETO), r(RESPONSAVEL_NEGOCIO))),
                rep("paula@retre.com", "Paula", "Ribeira", "1.50",
                        Set.of(r(LIDER_EQUIPE), r(ANALISTA_REQUISITOS))),
                rep("camila@retre.com", "Camila", "Benzeno", "1.00",
                        Set.of(r(ARQUITETO_SOFTWARE), r(DESIGNER))),
                rep("tomas@retre.com", "Tomas", "Tancredo", "1.00",
                        Set.of(r(DESENVOLVEDOR))),
                rep("jose@retre.com", "Jose", "Camargo", "1.50",
                        Set.of(r(ANALISTA_QUALIDADE), r(SUPORTE)))
        );
    }

    private TemplateStageDTO stage(String name, String weight, int seq) {
        TemplateStageDTO s = new TemplateStageDTO();
        s.setName(name);
        s.setWeight(new BigDecimal(weight));
        s.setSequence(seq);
        return s;
    }

    private TemplateIterationDTO iteration(String name, String weight) {
        TemplateIterationDTO i = new TemplateIterationDTO();
        i.setName(name);
        i.setWeight(new BigDecimal(weight));
        return i;
    }

    private TemplateQuestionDTO q(String value, Set<RoleSummaryResponseDTO> roles,
                                   String stageName, List<StageSummaryResponseDTO> stages) {
        TemplateQuestionDTO dto = new TemplateQuestionDTO();
        dto.setValue(value);
        dto.setRoles(roles);
        dto.setStageName(stageName);
        dto.setStages(stages);
        return dto;
    }

    private RoleSummaryResponseDTO r(long id) {
        return roleName(id);
    }

    private RoleSummaryResponseDTO roleName(long id) {
        String name = switch ((int) id) {
            case 1 -> "Desenvolvedor";
            case 2 -> "Gerente de Projeto";
            case 3 -> "Cliente";
            case 4 -> "Analista de Qualidade";
            case 5 -> "Analista de Requisitos";
            case 6 -> "Designer";
            case 7 -> "Responsável do Negócio";
            case 8 -> "Suporte";
            case 9 -> "Stakeholder";
            case 10 -> "Líder de Equipe";
            case 11 -> "Arquiteto de Software";
            default -> "Desconhecido";
        };
        return new RoleSummaryResponseDTO(id, name);
    }

    private TemplateRepresentativeDTO rep(String email, String firstName, String lastName,
                                           String weight, Set<RoleSummaryResponseDTO> roles) {
        TemplateRepresentativeDTO r = new TemplateRepresentativeDTO();
        r.setEmail(email);
        r.setFirstName(firstName);
        r.setLastName(lastName);
        r.setWeight(new BigDecimal(weight));
        r.setRoles(roles);
        return r;
    }

    private StageSummaryResponseDTO stageSummary(int id, String name) {
        return new StageSummaryResponseDTO(id, name);
    }
}

