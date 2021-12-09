package com.otsi.retail.gateway;

import static com.nimbusds.jose.JWSAlgorithm.RS256;

import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
//import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurerComposite;
//import org.xmlpull.v1.XmlPullParserException;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.ResourceRetriever;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.otsi.retail.gateway.authConfiguration.AuthFilter;

@SpringBootApplication
//@EnableDiscoveryClient
@EnableAutoConfiguration(exclude = { WebMvcAutoConfiguration.class })
@EnableWebFlux
public class SpringCloudGatewayApplication {
	private static final Logger LOGGER = LoggerFactory.getLogger(SpringCloudGatewayApplication.class);
	@Value("${Cognito.aws.region}")
	String REGION;
	@Value("${Cognito.aws.userpool_id}")
	String USERPOOL_ID;
   @Autowired
   private AuthFilter filter;
   
   @Autowired
	RouteDefinitionLocator locator;
   
	@Bean
	public ServerCodecConfigurer serverCodecConfigurer() {
		return ServerCodecConfigurer.create();
	}

	@Bean
	public ConfigurableJWTProcessor getConfigurableJWTProcessor() throws MalformedURLException {
		ResourceRetriever resourceRetriever = new DefaultResourceRetriever(2000, 2000);
		URL jwkURL = new URL(
				String.format("https://cognito-idp.%s.amazonaws.com/%s/.well-known/jwks.json", REGION, USERPOOL_ID));
		JWKSource keySource = new RemoteJWKSet(jwkURL, resourceRetriever);
		ConfigurableJWTProcessor jwtProcessor = new DefaultJWTProcessor();
		JWSKeySelector keySelector = new JWSVerificationKeySelector(RS256, keySource);
		jwtProcessor.setJWSKeySelector(keySelector);
		return jwtProcessor;

	}

	
	/*
	 * @Bean public WebFluxConfigurer corsConfigurer() { return new
	 * WebFluxConfigurerComposite() {
	 * 
	 * @Override public void addCorsMappings(CorsRegistry registry) {
	 * registry.addMapping("/*").allowedOrigins("*") .allowedMethods("*"); } }; }
	 */


	@Bean
	public List<GroupedOpenApi> apis() {
		List<GroupedOpenApi> groups = new ArrayList<>();
		List<RouteDefinition> definitions = locator.getRouteDefinitions().collectList().block();

		definitions.stream().filter(routeDefinition -> routeDefinition.getId().matches(".*-service"))
				.forEach(routeDefinition -> {
					String name = routeDefinition.getId().replaceAll("-service", "");
					groups.add(GroupedOpenApi.builder().pathsToMatch("/" + name + "/**").group(name).build());
				});
		return groups;
	}

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudGatewayApplication.class, args);

	}

}
