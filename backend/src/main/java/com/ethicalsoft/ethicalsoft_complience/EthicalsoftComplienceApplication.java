package com.ethicalsoft.ethicalsoft_complience;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = "com.ethicalsoft")
public class EthicalsoftComplienceApplication {

	public static void main( String[] args ) {
		SpringApplication.run( EthicalsoftComplienceApplication.class, args );
	}

}
