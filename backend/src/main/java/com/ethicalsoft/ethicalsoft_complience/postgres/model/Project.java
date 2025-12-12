package com.ethicalsoft.ethicalsoft_complience.postgres.model;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.ProjectStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.TimelineStatusEnum;
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
@Table(name = "project")
public class Project {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "project_id")
	private Long id;

	@Column(name = "name", nullable = false)
	private String name;

	@OneToMany(mappedBy = "project")
	private Set<Representative> representatives;

	@Column(name = "type", nullable = false, length = 50)
	@Enumerated(EnumType.STRING)
	private ProjectTypeEnum type;

	@Column(name = "start_date", nullable = false)
	@Temporal(TemporalType.DATE)
	private LocalDate startDate;

	@Column(name = "deadline", nullable = false)
	@Temporal(TemporalType.DATE)
	private LocalDate deadline;

	@Column(name = "status", nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private ProjectStatusEnum status;

	@Column(name = "timeline_status", nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private TimelineStatusEnum timelineStatus;

	@Column(name = "closing_date")
	@Temporal(TemporalType.DATE)
	private LocalDate closingDate;

	@Column(name = "iteration_duration")
	private Integer iterationDuration;

	@Column(name = "iteration_count")
	private Integer iterationCount;

	@OneToMany(mappedBy = "project")
	private Set<Questionnaire> questionnaires;

	@OneToMany(mappedBy = "project")
	private Set<Stage> stages;

	@OneToMany(mappedBy = "project")
	private Set<Iteration> iterations;

	@Column(name = "current_situation", length = 100)
	private String currentSituation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id", nullable = false)
	private User owner;
}
