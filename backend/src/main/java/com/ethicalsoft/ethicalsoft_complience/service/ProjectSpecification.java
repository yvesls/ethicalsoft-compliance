package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.ProjectSearchRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.ProjectStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.ProjectTypeEnum;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ProjectSpecification {

	public static Specification<Project> findByCriteria(ProjectSearchRequestDTO filters) {

		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (StringUtils.hasText(filters.getName())) {
				predicates.add(cb.like(cb.lower(root.get("name")), "%" + filters.getName().toLowerCase() + "%"));
			}

			if (StringUtils.hasText(filters.getType())) {
				try {
					ProjectTypeEnum typeEnum = ProjectTypeEnum.valueOf(filters.getType().toUpperCase());
					predicates.add(cb.equal(root.get("type"), typeEnum));
				} catch (IllegalArgumentException e) {

				}
			}

			if ( StringUtils.hasText(filters.getStatus())) {
				try {
					ProjectStatusEnum statusEnum = ProjectStatusEnum.valueOf(filters.getStatus().toUpperCase());
					predicates.add(cb.equal(root.get("status"), statusEnum));
				} catch (IllegalArgumentException e) {
				}
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}