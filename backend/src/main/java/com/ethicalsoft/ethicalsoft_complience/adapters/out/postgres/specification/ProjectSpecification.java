package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.specification;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.ProjectSearchRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.UserRoleEnum;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ProjectSpecification {

	public static Specification<Project> findByCriteria(ProjectSearchRequestDTO filters, User currentUser) {

		return (root, query, cb) -> {
			query.distinct(true);
			List<Predicate> predicates = new ArrayList<>();

			if (StringUtils.hasText(filters.getName())) {
				predicates.add(cb.like(cb.lower(root.get("name")), "%" + filters.getName().toLowerCase() + "%"));
			}

			if (StringUtils.hasText(filters.getType())) {
                ProjectTypeEnum typeEnum = ProjectTypeEnum.valueOf(filters.getType().toUpperCase());
                predicates.add(cb.equal(root.get("type"), typeEnum));
			}

			if (StringUtils.hasText(filters.getStatus())) {
                ProjectStatusEnum statusEnum = ProjectStatusEnum.valueOf(filters.getStatus().toUpperCase());
                predicates.add(cb.equal(root.get("status"), statusEnum));
			}

			if (currentUser != null) {
				if (UserRoleEnum.ADMIN.equals(currentUser.getRole())) {
					predicates.add(cb.equal(root.get("owner").get("id"), currentUser.getId()));
				} else {
					Join<Object, Object> representativeJoin = root.join("representatives", JoinType.LEFT);
					predicates.add(cb.equal(representativeJoin.get("user").get("id"), currentUser.getId()));
				}
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}