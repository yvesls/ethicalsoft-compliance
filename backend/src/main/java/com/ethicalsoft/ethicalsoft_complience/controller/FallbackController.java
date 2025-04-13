package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.exception.ResourceNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Order( Ordered.LOWEST_PRECEDENCE )
public class FallbackController {

	@RequestMapping( "/**" )
	public void handleFallback() {
		throw new ResourceNotFoundException();
	}
}