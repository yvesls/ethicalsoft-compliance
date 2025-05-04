package com.ethicalsoft.ethicalsoft_complience.infra;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
public class ValidatorConfig {

	@Bean
	@Primary
	LocalValidatorFactoryBean validator( MessageSource messageSource ) {
		var bean = new LocalValidatorFactoryBean();
		bean.setValidationMessageSource( messageSource );
		return bean;
	}

}
