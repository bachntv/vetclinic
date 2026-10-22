package com.bach.vetclinic.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import com.bach.vetclinic.repository.UserRepository;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecretKey jwtSecretKey(@Value("${app.jwt.secret}") String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey, UserRepository users, com.bach.vetclinic.repository.RevokedTokenRepository revokedTokens) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<org.springframework.security.oauth2.jwt.Jwt> account = jwt -> {
            boolean valid = !revokedTokens.existsById(CredentialStamp.of(jwt.getTokenValue())) && users.findByEmailIgnoreCase(jwt.getSubject()).filter(u -> u.isEnabled()
                    && (u.getVet() == null || u.getVet().isActive())
                    && u.getRole().name().equals(jwt.getClaimAsString("roles"))
                    && CredentialStamp.of(u.getPasswordHash()).equals(jwt.getClaimAsString("credentials"))).isPresent();
            return valid ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Tài khoản hoặc mật khẩu đã thay đổi", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer("vetclinic"), account));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/health", "/api/auth/login").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/vet/**").hasAnyRole("VET", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/vaccinations/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/owners/**").hasAnyRole("ADMIN", "VET")
                        .requestMatchers(HttpMethod.POST, "/api/owners", "/api/pets", "/api/appointments", "/api/visits").hasAnyRole("ADMIN", "VET", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.POST, "/api/owners/*/activate", "/api/pets/*/activate").hasAnyRole("ADMIN", "VET", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.DELETE, "/api/pets/**").hasAnyRole("ADMIN", "VET", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.PUT, "/api/appointments/*", "/api/owners/**", "/api/pets/*").hasAnyRole("ADMIN", "VET", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.POST, "/api/vaccinations").hasAnyRole("ADMIN", "VET")
                        .requestMatchers(HttpMethod.PUT, "/api/vaccinations/**").hasAnyRole("ADMIN", "VET")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
