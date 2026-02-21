package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Iteration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IterationRepository extends JpaRepository<Iteration, Long> {
    List<Iteration> findByProjectId(Long projectId);
}
