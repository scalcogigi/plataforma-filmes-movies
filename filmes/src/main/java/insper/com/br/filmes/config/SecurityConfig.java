package insper.com.br.filmes.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers(
                                        HttpMethod.OPTIONS,
                                        "/**"
                                ).permitAll()
                                .requestMatchers("/error").permitAll()
                                .anyRequest().authenticated()
                )
                .oauth2ResourceServer(resourceServer ->
                        resourceServer.jwt(
                                Customizer.withDefaults()
                        )
                )
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${auth0.issuer}")
            String issuer,

            @Value("${auth0.audience}")
            String audience,

            @Value("${auth0.jwk-set-uri}")
            String jwkSetUri
    ) {
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder
                        .withJwkSetUri(jwkSetUri)
                        .build();

        var issuerValidator =
                JwtValidators.createDefaultWithIssuer(
                        issuer
                );

        var audienceValidator =
                new AudienceValidator(audience);

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<Jwt>(
                        issuerValidator,
                        audienceValidator
                )
        );

        return decoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origin}")
            String frontendOrigin
    ) {
        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of(frontendOrigin)
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type"
                )
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}