package com.ethicalsoft.ethicalsoft_complience.postgres.repository;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Iteration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IterationRepository extends JpaRepository<Iteration, Long> {}