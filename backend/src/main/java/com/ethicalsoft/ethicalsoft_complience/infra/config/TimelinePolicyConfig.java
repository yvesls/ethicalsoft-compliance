package com.ethicalsoft.ethicalsoft_complience.infra.config;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.IterationRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.StageRepository;
import com.ethicalsoft.ethicalsoft_complience.domain.service.ProjectTimelineStatusPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimelinePolicyConfig {

    @Bean
    public ProjectTimelineStatusPolicy projectTimelineStatusPolicy(StageRepository stageRepository,
                                                                   IterationRepository iterationRepository,
                                                                   QuestionnaireRepository questionnaireRepository) {
        return new ProjectTimelineStatusPolicy(Clock.systemDefaultZone(), stageRepository, iterationRepository, questionnaireRepository);
    }
}
