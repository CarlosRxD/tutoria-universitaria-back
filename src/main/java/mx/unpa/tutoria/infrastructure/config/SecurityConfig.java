package mx.unpa.tutoria.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer; // 👈 NUEVO IMPORT
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Endpoints móviles
                        .requestMatchers("/api/movil/**").permitAll()
                        .requestMatchers("/api/web/**").permitAll()
                        .requestMatchers("/api/docentes/**").permitAll()
                        .requestMatchers("/api/alumnos/**").permitAll()
                        .requestMatchers("/api/asignaciones/**").permitAll()
                        .requestMatchers("/api/sincronizacion/**").permitAll()
                        .requestMatchers("/api/periodos/**").permitAll()
                        .requestMatchers("/api/archivos/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}