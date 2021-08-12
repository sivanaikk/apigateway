package com.otsi.retail.gateway.authConfiguration;

import java.text.ParseException;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;

import reactor.core.publisher.Mono;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

	@Value("${Cognito.aws.idTokenPoolUrl}")
	private String ID_TOKEN_URL;

	@Autowired
	private RouteValidator routeValidator;

	@Autowired
	private ConfigurableJWTProcessor configurableJWTProcessor;

	private static final Log logger = LogFactory.getLog(AuthFilter.class);

	public AuthFilter() {
		super(Config.class);

	}

	@Override
	public GatewayFilter apply(Config config) {

		return (exchange, chain) -> {

			ServerHttpRequest request = exchange.getRequest();
			JWTClaimsSet claims = null;
			System.out.println(request.getPath());
			if (routeValidator.isSecured.test(request)) {
				if (isAuthHeaderMissing(request)) {
					logger.error("Authorization header is missing in request");
					return this.onError(exchange, "Authorization header is missing in request",
							HttpStatus.UNAUTHORIZED);
				}

				final String token = this.getAuthHeader(request);

				try {
					claims = getCliamsFromToken(token);
				} catch (ParseException | BadJOSEException | JOSEException e) {
					logger.error("Error occurs while getting cliams from token ==>" + e.getMessage());
					return this.onError(exchange, e.getMessage(), HttpStatus.UNAUTHORIZED);
				}
				if (isTokenInvalid(claims)) {
					logger.error("#####Token is expired#####");
					return this.onError(exchange, "JWT Token is Expired", HttpStatus.UNAUTHORIZED);
				}
				if (verifyIfIdToken(claims)) {
					logger.error("JWT Token is not an ID Token");

					return this.onError(exchange, "JWT Token is not an ID Token", HttpStatus.UNAUTHORIZED);
				}
				if (validateIssuer(claims)) {
					logger.error("Issuer  does not match cognito idp ");

					return this.onError(exchange, "Issuer  does not match cognito idp ", HttpStatus.UNAUTHORIZED);
				}

				this.populateRequestWithHeaders(exchange, token, claims);

			}
			return chain.filter(exchange);

		};
	}

	
	private void populateRequestWithHeaders(ServerWebExchange exchange, String token, JWTClaimsSet claims) {

		exchange.getRequest().mutate().headers(header->{
			header.add("Username", String.valueOf(claims.getClaim("cognito:username")));
			header.add("Roles", String.valueOf(claims.getClaim("cognito:groups")));
		}).build();	
			
//			        .header("userName", String.valueOf(claims.getClaim("cognito:username")))
//				.header("roles", String.valueOf(claims.getClaim("cognito:groups")))
//				.build();
		System.out.println("-------------------->>>>>"+exchange.getRequest().getHeaders().get("Username"));
	}

	private Mono<Void> onError(ServerWebExchange exchange, String error, HttpStatus httpStatus) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(httpStatus);
		return response.setComplete();
	}

	private JWTClaimsSet getCliamsFromToken(String token) throws ParseException, BadJOSEException, JOSEException {
		return configurableJWTProcessor.process(getBearerToken(token), null);

	}

	private String getBearerToken(String token) {
		return token.startsWith("Bearer ") ? token.substring("Bearer ".length()) : token;

	}

	private boolean isTokenInvalid(JWTClaimsSet claims) {
		return claims.getExpirationTime().before(new Date());
	}

	private String getAuthHeader(ServerHttpRequest request) {
		return request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
	}

	private boolean isAuthHeaderMissing(ServerHttpRequest request) {
		return !request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION);
	}

	private boolean verifyIfIdToken(JWTClaimsSet claims) {

		return !claims.getIssuer().equals(ID_TOKEN_URL);

	}

	private boolean validateIssuer(JWTClaimsSet claims) {

		return !claims.getIssuer().equals(ID_TOKEN_URL);

	}

	public static class Config {

	}

}
