package com.ethicalsoft.ethicalsoft_complience.application.port.project;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Stage;

import java.util.List;

public interface StageCommandPort {
    List<Stage> saveAll(List<Stage> stages);
}

