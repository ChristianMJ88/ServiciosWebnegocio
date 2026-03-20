package com.techprotech.agenda.seguridad.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    private final ServicioTokenJwt servicioTokenJwt;

    public FiltroAutenticacionJwt(ServicioTokenJwt servicioTokenJwt) {
        this.servicioTokenJwt = servicioTokenJwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolverToken(request);
        if (StringUtils.hasText(token) && servicioTokenJwt.esValido(token)) {
            var roles = servicioTokenJwt.obtenerRoles(token);
            var authorities = servicioTokenJwt.obtenerRoles(token)
                    .stream()
                    .map(rol -> rol.startsWith("ROLE_") ? rol : "ROLE_" + rol)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());

            UsuarioAutenticado usuarioAutenticado = new UsuarioAutenticado(
                    servicioTokenJwt.obtenerSujeto(token),
                    servicioTokenJwt.obtenerUsuarioId(token),
                    servicioTokenJwt.obtenerEmpresaId(token),
                    roles
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
