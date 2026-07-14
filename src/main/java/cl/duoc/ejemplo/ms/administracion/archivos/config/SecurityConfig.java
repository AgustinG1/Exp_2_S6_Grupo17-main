package cl.duoc.ejemplo.ms.administracion.archivos.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http.cors(Customizer.withDefaults()).authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/api/guias/descargar").hasAnyAuthority("ROLE_DOWNLOAD", "ROLE_ADMIN")
						.requestMatchers("/api/guias/**").hasAuthority("ROLE_ADMIN")
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
		return http.build();
	}

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(new AzureAdRoleConverter());
		return converter;
	}

	static class AzureAdRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
		@Override
		public Collection<GrantedAuthority> convert(Jwt source) {
			// Azure AD B2C usually puts roles in a claim called "extension_Role" or "roles"
			Object rolesObj = source.getClaims().get("extension_Role");
			if (rolesObj == null) {
				rolesObj = source.getClaims().get("roles");
			}
			
			if (rolesObj instanceof List<?>) {
				List<String> roles = (List<String>) rolesObj;
				return roles.stream()
						.map(role -> new SimpleGrantedAuthority(role)) // ROLE_ADMIN, ROLE_DOWNLOAD
						.collect(Collectors.toList());
			} else if (rolesObj instanceof String) {
				return List.of(new SimpleGrantedAuthority((String) rolesObj));
			}
			
			return List.of();
		}
	}
}
