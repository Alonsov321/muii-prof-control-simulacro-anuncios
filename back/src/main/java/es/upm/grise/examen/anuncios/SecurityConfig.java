package es.upm.grise.examen.anuncios;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class SecurityConfig {

    /**
     * Reglas de acceso.
     * - GET /api/whoami: público (comprobación del despliegue).
     * - GET /api/mensajes: cualquier usuario autenticado.
     * - POST /api/mensajes: solo usuarios del grupo "admin" de Cognito.
     *
     * Si todavía no se ha ejecutado cognito.sh no hay emisor de tokens. En ese caso la aplicación
     * arranca igualmente, sin validar tokens: los endpoints protegidos responden 401.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri) throws Exception {
        http
            // API sin sesión y con tokens: no hay cookies que proteger contra CSRF.
            .csrf(csrf -> csrf.disable())
            // La consola H2 se dibuja con marcos del mismo origen.
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/whoami").permitAll()
                // Consola H2: tiene su propio inicio de sesión. Solo es accesible por túnel SSH.
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/mensajes").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) ->
                    escribirError(response, HttpStatus.UNAUTHORIZED, "Unauthorized"))
                .accessDeniedHandler((request, response, e) ->
                    escribirError(response, HttpStatus.FORBIDDEN, "Forbidden"))
            );

        if (!issuerUri.isBlank()) {
            http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        }
        return http.build();
    }

    /**
     * Cada grupo del claim "cognito:groups" pasa a ser un rol: el grupo "admin" da ROLE_ADMIN.
     * Los grupos viajan dentro del token, así que un token emitido antes de asignar el usuario
     * al grupo no los lleva y hay que pedir uno nuevo.
     */
    static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<GrantedAuthority> autoridades = new ArrayList<>();
            List<String> grupos = jwt.getClaimAsStringList("cognito:groups");
            if (grupos != null) {
                for (String grupo : grupos) {
                    autoridades.add(new SimpleGrantedAuthority("ROLE_" + grupo.toUpperCase()));
                }
            }
            return autoridades;
        });
        return converter;
    }

    /**
     * Cognito firma el ID token y el access token con las mismas claves y el mismo emisor.
     * El claim 'token_use' los distingue. Solo se aceptan access tokens.
     */
    @Bean
    public OAuth2TokenValidator<Jwt> tokenUseValidator() {
        return new JwtClaimValidator<String>("token_use", "access"::equals);
    }

    private void escribirError(HttpServletResponse response, HttpStatus status, String error) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + error + "\"}");
    }
}
