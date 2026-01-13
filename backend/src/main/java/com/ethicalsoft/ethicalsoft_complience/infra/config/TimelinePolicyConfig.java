package com.ethicalsoft.ethicalsoft_complience.infra.config;

import com.ethicalsoft.ethicalsoft_complience.domain.service.ProjectTimelineStatusPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimelinePolicyConfig {

    @Bean
    public ProjectTimelineStatusPolicy projectTimelineStatusPolicy() {
        return new ProjectTimelineStatusPolicy(Clock.systemDefaultZone());
    }
}

