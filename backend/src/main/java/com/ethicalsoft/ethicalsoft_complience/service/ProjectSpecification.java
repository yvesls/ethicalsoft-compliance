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
		// Usa uma lambda (root, query, criteriaBuilder)
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			// 1. Filtro por NOME (LIKE %...%)
			if (StringUtils.hasText(filters.getName())) {
				predicates.add(cb.like(cb.lower(root.get("name")), "%" + filters.getName().toLowerCase() + "%"));
			}

			// 2. Filtro por CÓDIGO (Assumindo que 'code' exista no modelo)
			// Se 'code' não existir no seu modelo Project, remova este bloco.
            /*
            if (StringUtils.hasText(filters.getCode())) {
                predicates.add(cb.like(cb.lower(root.get("code")), "%" + filters.getCode().toLowerCase() + "%"));
            }
            */

			// 3. Filtro por TIPO (Enum)
			if (StringUtils.hasText(filters.getType())) {
				try {
					ProjectTypeEnum typeEnum = ProjectTypeEnum.valueOf(filters.getType().toUpperCase());
					predicates.add(cb.equal(root.get("type"), typeEnum));
				} catch (IllegalArgumentException e) {
					// Ignora tipo inválido
				}
			}

			// 4. Filtro por STATUS (Enum)
			if ( StringUtils.hasText(filters.getStatus())) {
				try {
					ProjectStatusEnum statusEnum = ProjectStatusEnum.valueOf(filters.getStatus().toUpperCase());
					predicates.add(cb.equal(root.get("status"), statusEnum));
				} catch (IllegalArgumentException e) {
					// Ignora status inválido
				}
			}

			// Combina todos os predicados com "AND"
			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}