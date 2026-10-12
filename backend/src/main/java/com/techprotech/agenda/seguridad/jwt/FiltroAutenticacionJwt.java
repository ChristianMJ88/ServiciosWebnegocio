package com.techprotech.agenda.seguridad.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
public class FiltroAutenticacionJwt extends OncePerRequestFilter {
    private static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();

    private final ServicioTokenJwt servicioTokenJwt;
    private final UsuarioRepositorio usuarios;

    public FiltroAutenticacionJwt(ServicioTokenJwt servicioTokenJwt, UsuarioRepositorio usuarios) {
        this.servicioTokenJwt = servicioTokenJwt;
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolverToken(request);
        if (StringUtils.hasText(token) && servicioTokenJwt.esValido(token)) {
            if ("TENANT".equals(servicioTokenJwt.obtenerAmbito(token))) {
                var usuario = usuarios.findByIdAndEmpresaId(servicioTokenJwt.obtenerUsuarioId(token), servicioTokenJwt.obtenerEmpresaId(token)).orElse(null);
                String motivo = EstadoSesionUsuario.motivoNoDisponible(usuario, servicioTokenJwt.obtenerVersionSesion(token));
                if (motivo != null) {
                    SecurityContextHolder.clearContext();
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    JSON.writeValue(response.getWriter(), new com.techprotech.agenda.compartido.exception.ErrorApi(
                            "SESION_NO_DISPONIBLE", motivo, java.time.OffsetDateTime.now()));
                    return;
                }
            }
            var roles = servicioTokenJwt.obtenerRoles(token);
            var permisos = servicioTokenJwt.obtenerPermisos(token);
            var sucursalesPermitidas = servicioTokenJwt.obtenerSucursalesPermitidas(token);
            var authorities = permisos.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());
            String ambito = servicioTokenJwt.obtenerAmbito(token);
            authorities.add(new SimpleGrantedAuthority("AMBITO_" + ambito));
            authorities.addAll(servicioTokenJwt.obtenerRoles(token)
                    .stream()
                    .map(rol -> rol.startsWith("ROLE_") ? rol : "ROLE_" + rol)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet()));

            UsuarioAutenticado usuarioAutenticado = new UsuarioAutenticado(
                    servicioTokenJwt.obtenerSujeto(token),
                    servicioTokenJwt.obtenerUsuarioId(token),
                    servicioTokenJwt.obtenerEmpresaId(token),
                    ambito,
                    roles,
                    permisos,
                    sucursalesPermitidas
            );

            var autenticacion = new UsernamePasswordAuthenticationToken(
                    usuarioAutenticado,
                    null,
                    authorities
            );
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
        }

        filterChain.doFilter(request, response);
    }

    private String resolverToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
