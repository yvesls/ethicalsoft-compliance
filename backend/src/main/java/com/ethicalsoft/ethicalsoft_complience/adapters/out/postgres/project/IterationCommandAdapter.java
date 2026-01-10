package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.project;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Iteration;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.IterationRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.project.IterationCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class IterationCommandAdapter implements IterationCommandPort {

    private final IterationRepository iterationRepository;

    @Override
    public List<Iteration> saveAll(List<Iteration> iterations) {
        return iterationRepository.saveAll(iterations);
    }
}

