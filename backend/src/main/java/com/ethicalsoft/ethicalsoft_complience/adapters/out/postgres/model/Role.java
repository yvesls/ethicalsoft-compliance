package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "role")
public class Role {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "role_id")
	private Long id;

	@Column(name = "name", unique = true, nullable = false)
	private String name;

	@Column(name = "description")
	private String description;

	@ManyToMany(mappedBy = "roles")
	private Set<Representative> representatives;

	@ManyToMany(mappedBy = "roles")
	private Set<Question> questions;
}