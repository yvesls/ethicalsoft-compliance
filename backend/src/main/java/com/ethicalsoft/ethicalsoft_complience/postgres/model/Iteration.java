package com.ethicalsoft.ethicalsoft_complience.postgres.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "iteration")
public class Iteration {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "iteration_id")
	private Long id;

	@ManyToOne
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "weight", nullable = false, precision = 5, scale = 2)
	private BigDecimal weight;

	@Column(name = "application_start_date")
	@Temporal(TemporalType.DATE)
	private LocalDate applicationStartDate;

	@Column(name = "application_end_date")
	@Temporal(TemporalType.DATE)
	private LocalDate applicationEndDate;

	@OneToMany(mappedBy = "iterationRef")
	private Set<Questionnaire> questionnaires;
}