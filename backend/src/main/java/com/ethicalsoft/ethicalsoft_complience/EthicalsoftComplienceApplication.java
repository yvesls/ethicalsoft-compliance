package com.ethicalsoft.ethicalsoft_complience;

import com.ethicalsoft.ethicalsoft_complience.infra.config.DatabaseInitializerConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@ComponentScan(basePackages = "com.ethicalsoft")
public class EthicalsoftComplienceApplication {

	public static void main( String[] args ) {
		SpringApplication app = new SpringApplication(EthicalsoftComplienceApplication.class);
		app.addListeners(new DatabaseInitializerConfig());
		app.run(args);
	}

}
