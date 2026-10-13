package com.techprotech.agenda.seguridad.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;

@Component
public class FiltroLimiteSolicitudes extends OncePerRequestFilter {

    private static final List<Regla> REGLAS = List.of(
            new Regla("login", List.of("/api/v1/auth/iniciar-sesion", "/api/v1/auth/app-login"), 10, 60),
            new Regla("recuperacion", List.of("/api/v1/publico/recuperacion-contrasena/solicitar"), 5, 900),
            new Regla("registro", List.of("/api/v1/auth/registrar-cliente", "/api/v1/onboarding/empresas"), 5, 3600),
            new Regla("contacto", List.of("/api/v1/publico/contactos"), 10, 3600),
            new Regla("direccion", List.of("/api/v1/publico/direcciones/validar"), 30, 3600),
            new Regla("token", List.of("/api/v1/auth/refrescar-token"), 60, 60),
            new Regla("acceso-social", List.of("/api/v1/auth/social/intercambiar-acceso"), 10, 60),
            new Regla("confirmacion", List.of(
                    "/api/v1/publico/recuperacion-contrasena/confirmar",
                    "/api/v1/publico/invitaciones/usuario/aceptar"
            ), 10, 900)
    );

    private final LimiteVentanaFija limite = new LimiteVentanaFija(Clock.systemUTC());
    private final boolean habilitado;
    private final Regla disponibilidad;

    public FiltroLimiteSolicitudes(
            @Value("${aplicacion.limite-solicitudes.habilitado:true}") boolean habilitado
    ) {
        this(habilitado, 60, 60);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public FiltroLimiteSolicitudes(
            @Value("${aplicacion.limite-solicitudes.habilitado:true}") boolean habilitado,
            @Value("${aplicacion.limite-solicitudes.disponibilidad-maximo:60}") int maximo,
            @Value("${aplicacion.limite-solicitudes.disponibilidad-ventana-segundos:60}") long ventana) {
        this.habilitado = habilitado;
        this.disponibilidad = new Regla("disponibilidad", List.of("/api/v1/publico/disponibilidad/dias"), maximo, ventana);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !habilitado || !("POST".equalsIgnoreCase(request.getMethod())
                || "GET".equalsIgnoreCase(request.getMethod()) && disponibilidad.rutas().contains(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Regla regla = buscarRegla(request.getRequestURI());
        if (regla == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String origen = request.getRemoteAddr() == null ? "desconocido" : request.getRemoteAddr();
        LimiteVentanaFija.Resultado resultado = limite.consumir(
                regla.nombre() + ':' + origen,
                regla.maximo(),
                regla.ventanaSegundos()
        );
        if (resultado.permitido()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(resultado.reintentarEnSegundos()));
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"mensaje\":\"Demasiadas solicitudes. Intenta nuevamente más tarde.\"}");
    }

    private Regla buscarRegla(String ruta) {
        if (disponibilidad.rutas().contains(ruta)) return disponibilidad;
        return REGLAS.stream()
                .filter(regla -> regla.rutas().contains(ruta))
                .findFirst()
                .orElse(null);
    }

    private record Regla(String nombre, List<String> rutas, int maximo, long ventanaSegundos) {}
}
