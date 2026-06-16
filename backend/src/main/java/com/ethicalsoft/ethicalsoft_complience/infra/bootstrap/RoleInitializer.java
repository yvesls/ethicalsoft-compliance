package com.ethicalsoft.ethicalsoft_complience.infra.bootstrap;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Role;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class RoleInitializer {

    private final RoleRepository repository;

    @PostConstruct
    public void seedRoles() {
        try {
            int inserted = 0;
            int updated = 0;
            for (Map.Entry<String, String> entry : canonicalRoles()) {
                String name = entry.getKey();
                String description = entry.getValue();

                Optional<Role> existing = repository.findByName(name);
                if (existing.isEmpty()) {
                    Role role = new Role();
                    role.setName(name);
                    role.setDescription(description);
                    repository.save(role);
                    inserted++;
                } else {
                    Role role = existing.get();
                    if (description != null && !description.equals(role.getDescription())) {
                        role.setDescription(description);
                        repository.save(role);
                        updated++;
                    }
                }
            }
            if (inserted > 0 || updated > 0) {
                log.info("[role-init] Papéis garantidos: {} inseridos, {} atualizados.", inserted, updated);
            }
        } catch (RuntimeException e) {
            log.warn("[role-init] Não foi possível inicializar os papéis. A aplicação continuará. Erro: {}",
                    e.getMessage());
        }
    }

    private List<Map.Entry<String, String>> canonicalRoles() {
        return List.of(
                Map.entry("Desenvolvedor",
                        "Responsável por implementar as funcionalidades, corrigir bugs, escrever testes e manter a qualidade técnica do código."),
                Map.entry("Gerente de Projeto",
                        "Responsável por planejar e coordenar o projeto, gerir prazos, riscos, escopo, comunicação e alinhamento entre as partes."),
                Map.entry("Cliente",
                        "Representa a área solicitante; valida requisitos, aprova entregas e fornece feedback sobre o produto/serviço."),
                Map.entry("Analista de Qualidade",
                        "Responsável por validar a qualidade do software, definir e executar testes, registrar evidências e garantir conformidade com critérios de aceite."),
                Map.entry("Analista de Requisitos",
                        "Responsável por levantar, detalhar e validar requisitos, documentar regras de negócio e garantir entendimento comum entre stakeholders."),
                Map.entry("Designer",
                        "Responsável por conceber a experiência do usuário e a interface, garantindo usabilidade, consistência visual e aderência à identidade do produto."),
                Map.entry("Responsável do Negócio",
                        "Responsável por definir prioridades do negócio, aprovar requisitos críticos e garantir alinhamento com os objetivos estratégicos."),
                Map.entry("Suporte",
                        "Responsável por atender usuários, registrar incidentes, orientar uso correto e retroalimentar melhorias com base nas ocorrências."),
                Map.entry("Stakeholder",
                        "Parte interessada que influencia ou é impactada pelo projeto; fornece expectativas, valida decisões e acompanha resultados."),
                Map.entry("Líder de Equipe",
                        "Responsável por coordenar a equipe técnica, distribuir tarefas, garantir aderência a padrões e facilitar a comunicação entre membros."),
                Map.entry("Arquiteto de Software",
                        "Responsável por definir a arquitetura técnica, padrões de design, decisões estruturais e garantir escalabilidade e manutenibilidade.")
        );
    }
}
