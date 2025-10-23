package com.ethicalsoft.ethicalsoft_complience.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "questionnaire")
public class Questionnaire {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "questionnaire_id")
	private Integer id;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "iteration", length = 50)
	private String iteration;

	@Column(name = "weight", nullable = false)
	private Integer weight;

	@Column(name = "application_start_date")
	@Temporal(TemporalType.DATE)
	private LocalDate applicationStartDate;

	@Column(name = "application_end_date")
	@Temporal(TemporalType.DATE)
	private LocalDate applicationEndDate;

	@ManyToOne
	@JoinColumn(name = "project_id")
	private Project project;

	@OneToMany(mappedBy = "questionnaire")
	private Set<Question> questions;

	@ManyToOne
	@JoinColumn(name = "stage_id")
	private Stage stage;

	@ManyToOne
	@JoinColumn(name = "iteration_id")
	private Iteration iterationRef;
}