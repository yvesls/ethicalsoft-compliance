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
@Table(name = "template")
public class Template {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "template_id")
	private Integer id;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "creation_date")
	@Temporal(TemporalType.DATE)
	private LocalDate creationDate;

	@ManyToOne
	@JoinColumn(name = "project_id")
	private Project project;

	@OneToMany(mappedBy = "template")
	private Set<Project> projects;
}
