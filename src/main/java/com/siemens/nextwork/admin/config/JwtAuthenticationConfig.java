package com.siemens.nextwork.admin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@EnableAutoConfiguration(exclude = { UserDetailsServiceAutoConfiguration.class })
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class JwtAuthenticationConfig extends WebSecurityConfigurerAdapter {

	@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
	public String jwkSetUri;

	public static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationConfig.class);

	@Override
	public void configure(HttpSecurity http) throws Exception {

		http.authorizeRequests()
		.antMatchers("/v3/api-docs", "/configuration/**", "/swagger-resources/**", "/swagger-ui/**",
				"/swagger-ui.html", "/webjars/**", "/lib/**", "/fonts/**", "/*", "/actuator/health/**",
				"/actuator/info/**")
		.permitAll();
		http.oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt).authorizeRequests().anyRequest().authenticated()
		.and().exceptionHandling().and().oauth2ResourceServer().jwt();

	}
	
	@Override
	public void configure(final WebSecurity web) throws Exception {
		web.ignoring().antMatchers(HttpMethod.POST, "/api/v1/accessRequest");
	}

	@Bean
	@Profile("!Test")
	NimbusJwtDecoder jwtDecoder() {
		return NimbusJwtDecoder.withJwkSetUri(this.jwkSetUri).jwsAlgorithm(SignatureAlgorithm.RS384)
				// .restOperations(rest)
				.build();
	}

}