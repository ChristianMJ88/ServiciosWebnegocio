package com.techprotech.agenda.seguridad.config;

import com.techprotech.agenda.seguridad.jwt.FiltroAutenticacionJwt;
import com.techprotech.agenda.seguridad.ratelimit.FiltroLimiteSolicitudes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Configuration
@EnableMethodSecurity
public class ConfiguracionSeguridad {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            FiltroAutenticacionJwt filtroAutenticacionJwt,
            FiltroLimiteSolicitudes filtroLimiteSolicitudes
    ) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(UNAUTHORIZED))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/v1/publico/**").permitAll()
                        .requestMatchers(
                                "/api/v1/auth/iniciar-sesion",
                                "/api/v1/auth/app-login",
                                "/api/v1/auth/registrar-cliente",
                                "/api/v1/auth/refrescar-token",
                                "/api/v1/auth/cerrar-sesion"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/onboarding/empresas").permitAll()
                        .requestMatchers("/api/v1/auth/social/**").permitAll()
                        .requestMatchers("/api/v1/admin/correo/oauth/microsoft/callback").permitAll()
                        .requestMatchers("/api/v1/admin/correo/oauth/google/callback").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(filtroAutenticacionJwt, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(filtroLimiteSolicitudes, FiltroAutenticacionJwt.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public FilterRegistrationBean<FiltroLimiteSolicitudes> desactivarRegistroServletLimiteSolicitudes(
            FiltroLimiteSolicitudes filtro
    ) {
        FilterRegistrationBean<FiltroLimiteSolicitudes> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }
}
