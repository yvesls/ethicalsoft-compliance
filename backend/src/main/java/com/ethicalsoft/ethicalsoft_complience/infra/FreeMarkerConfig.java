package com.ethicalsoft.ethicalsoft_complience.infra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.util.Properties;

@Configuration
public class FreeMarkerConfig {

	@Bean
	public FreeMarkerConfigurer freemarkerConfigurer() {
		var configurer = new FreeMarkerConfigurer();
		configurer.setTemplateLoaderPath( "classpath:/templates/" );
		configurer.setDefaultEncoding( "UTF-8" );

		Properties settings = new Properties();
		settings.put("default_encoding", "UTF-8");
		settings.put("number_format", "computer");
		settings.put("template_exception_handler", "rethrow");

		configurer.setFreemarkerSettings(settings);
		return configurer;
	}
}