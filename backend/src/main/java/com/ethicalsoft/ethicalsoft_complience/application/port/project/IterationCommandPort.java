package com.ethicalsoft.ethicalsoft_complience.application.port.project;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Iteration;

import java.util.List;

public interface IterationCommandPort {
    List<Iteration> saveAll(List<Iteration> iterations);
}

