package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.project;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Stage;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.StageRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.project.StageCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StageCommandAdapter implements StageCommandPort {

    private final StageRepository stageRepository;

    @Override
    public List<Stage> saveAll(List<Stage> stages) {
        return stageRepository.saveAll(stages);
    }
}

