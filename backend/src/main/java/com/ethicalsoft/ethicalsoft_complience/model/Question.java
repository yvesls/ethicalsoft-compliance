package com.ethicalsoft.ethicalsoft_complience.model;

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
@Table( name = "question" )
public class Question {
	@Id
	@GeneratedValue( strategy = GenerationType.IDENTITY )
	@Column( name = "question_id" )
	private Integer id;

	@Column( name = "text", nullable = false )
	private String value;

	@ManyToOne
	@JoinColumn( name = "questionnaire_id" )
	private Questionnaire questionnaire;

	@ManyToMany
	@JoinTable( name = "question_role", joinColumns = @JoinColumn( name = "question_id" ), inverseJoinColumns = @JoinColumn( name = "role_id" ) )
	private Set<Role> roles;
}