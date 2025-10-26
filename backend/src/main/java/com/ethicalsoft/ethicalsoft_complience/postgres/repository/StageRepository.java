package com.ethicalsoft.ethicalsoft_complience.postgres.repository;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.Stage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StageRepository extends JpaRepository<Stage, Integer> {
	List<Stage> findAllByProjectId( Long projectId );
}