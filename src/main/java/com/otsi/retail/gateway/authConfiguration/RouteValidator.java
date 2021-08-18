package com.otsi.retail.gateway.authConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class RouteValidator {

	public static final List<String> nonSecureRoutes=Arrays.asList(
            "/auth/login",
            "/auth/signup",
            "/auth/confirmEmail",
            "/v3/api-docs"
            
    );	
	
	 public Predicate<ServerHttpRequest> isSecured =
	            request -> nonSecureRoutes
	                    .stream()
	                    .noneMatch(uri -> request.getURI().getPath().contains(uri));
}
