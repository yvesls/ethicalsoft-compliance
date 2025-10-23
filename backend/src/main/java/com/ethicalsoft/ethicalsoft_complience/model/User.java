package com.ethicalsoft.ethicalsoft_complience.model;

import com.ethicalsoft.ethicalsoft_complience.model.enums.UserRoleEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table( name = "user_account" )
public class User implements UserDetails {

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
	private List<Representative> representatives;

	@Id
	@GeneratedValue( strategy = GenerationType.IDENTITY )
	@Column( name = "user_id" )
	private Long id;

	@Column( name = "first_name", nullable = false )
	private String firstName;

	@Column( name = "email", unique = true, nullable = false )
	private String email;

	@Column( name = "last_name", nullable = false )
	private String lastName;

	@Column( name = "password", nullable = false )
	private String password;

	@Column( name = "accepted_terms" )
	private boolean acceptedTerms;

	@Column( name = "first_access" )
	private boolean firstAccess;

	@Enumerated(EnumType.STRING)
	@Column(name = "role")
	private UserRoleEnum role;

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		if ( this.role == UserRoleEnum.ADMIN ) return List.of( new SimpleGrantedAuthority( UserRoleEnum.ADMIN.name() ), new SimpleGrantedAuthority( UserRoleEnum.USER.name() ) );
		else return List.of( new SimpleGrantedAuthority( UserRoleEnum.USER.name() ) );
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return email;
	}

}

