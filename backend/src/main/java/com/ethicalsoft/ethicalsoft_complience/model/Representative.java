package com.ethicalsoft.ethicalsoft_complience.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "representative")
public class Representative {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "representative_id")
	private Long id;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@ManyToMany
	@JoinTable(name = "representative_roles",
			joinColumns = @JoinColumn(name = "representative_id"),
			inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new HashSet<>();

	@Column(name = "creation_date")
	private LocalDate creationDate;

	@Column(name = "update_date")
	private LocalDate updateDate;

	@Column(name = "deletion_date")
	private LocalDate deletionDate;

	@Column(name = "weight", nullable = false, precision = 5, scale = 2)
	private BigDecimal weight;
}
