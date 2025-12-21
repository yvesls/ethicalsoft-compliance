package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TimelineStatusEnum;
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
@Table(name = "stage")
public class Stage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "stage_id")
	private Integer id;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "weight", nullable = false, precision = 5, scale = 2)
	private BigDecimal weight;

	@Column(name = "sequence", nullable = false)
	private int sequence;

	@ManyToOne
	@JoinColumn(name = "project_id")
	private Project project;

	@OneToMany(mappedBy = "stage")
	private Set<Questionnaire> questionnaires;

	@Column(name = "application_start_date")
	@Temporal(TemporalType.DATE)
	private LocalDate applicationStartDate;

	@Column(name = "application_end_date")
	@Temporal(TemporalType.DATE)
	private LocalDate applicationEndDate;

	@Column(name = "status", nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private TimelineStatusEnum status = TimelineStatusEnum.PENDENTE;
}