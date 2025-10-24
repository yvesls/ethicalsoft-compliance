package com.ethicalsoft.ethicalsoft_complience.model;

import com.ethicalsoft.ethicalsoft_complience.model.enums.ProjectStatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "project")
public class Project {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "project_id")
	private Long id;

	@Column(name = "name", nullable = false)
	private String name;

	@OneToMany(mappedBy = "project")
	private List<Representative> representatives;

	@Column(name = "type", nullable = false, length = 50)
	private String type;

	@Column(name = "start_date", nullable = false)
	@Temporal(TemporalType.DATE)
	private LocalDate startDate;

	@Column(name = "deadline", nullable = false)
	@Temporal(TemporalType.DATE)
	private LocalDate deadline;

	@Column(name = "status", nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private ProjectStatusEnum status;

	@Column(name = "closing_date")
	@Temporal(TemporalType.DATE)
	private LocalDate closingDate;

	@Column(name = "iteration_duration")
	private Integer iterationDuration;

	@Column(name = "iteration_count")
	private Integer iterationCount;

	@ManyToOne
	@JoinColumn(name = "template_id")
	private Template template;

	@OneToMany(mappedBy = "project")
	private Set<Questionnaire> questionnaires;

	@OneToMany(mappedBy = "project")
	private Set<Stage> stages;

	@OneToMany(mappedBy = "project")
	private Set<Iteration> iterations;
}
