package com.techprotech.agenda.compartido.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class ConfiguracionCors {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${aplicacion.cors.origenes-permitidos}") String origenesPermitidos,
            org.springframework.beans.factory.ObjectProvider<com.techprotech.agenda.modulos.dominios.aplicacion.ServicioConsultaDominios> dominios
    ) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOriginPatterns(Arrays.stream(origenesPermitidos.split(",")).map(String::trim).toList());
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("*"));
        configuracion.setExposedHeaders(List.of("Authorization"));
        configuracion.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuracion);
        return request -> {
            CorsConfiguration base = source.getCorsConfiguration(request);
            String origen = request.getHeader("Origin");
            String ruta = request.getRequestURI().substring(request.getContextPath().length());
            // Custom sites use the same-origin proxy, which preserves Origin on POST.
            // Their dynamic allowance is public/customer registration only, never tenant administration.
            boolean publica = ruta.startsWith("/api/v1/publico/") || ruta.equals("/api/v1/auth/registrar-cliente");
            if (base == null || origen == null || base.checkOrigin(origen) != null || !publica) return base;
            try {
                java.net.URI uri = java.net.URI.create(origen);
                if (uri.getHost() == null || !origen.equals("https://" + uri.getHost())) return base;
                var servicio = dominios.getIfAvailable();
                if (servicio == null) return base;
                servicio.empresaActiva(uri.getHost());
                CorsConfiguration permitida = new CorsConfiguration(base);
                permitida.addAllowedOrigin(origen);
                permitida.setAllowCredentials(false);
                permitida.setExposedHeaders(List.of());
                return permitida;
            } catch (IllegalArgumentException | org.springframework.web.server.ResponseStatusException | org.springframework.dao.DataAccessException ignored) {
                return base;
            }
        };
    }
}
