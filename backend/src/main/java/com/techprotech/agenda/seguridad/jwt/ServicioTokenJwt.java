package com.techprotech.agenda.seguridad.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Service
public class ServicioTokenJwt {

    private final PropiedadesJwt propiedadesJwt;

    public ServicioTokenJwt(PropiedadesJwt propiedadesJwt) {
        this.propiedadesJwt = propiedadesJwt;
    }

    public String generarTokenAcceso(String sujeto, Long usuarioId, Long empresaId, Collection<String> roles) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(sujeto)
                .issuer(propiedadesJwt.issuer())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(propiedadesJwt.minutosAcceso(), ChronoUnit.MINUTES)))
                .claim("usuarioId", usuarioId)
                .claim("empresaId", empresaId)
                .claim("roles", roles)
                .signWith(claveFirma())
                .compact();
    }

    public String generarTokenActualizacion(String sujeto, Long usuarioId, Long empresaId) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(sujeto)
                .issuer(propiedadesJwt.issuer())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(propiedadesJwt.diasRefresh(), ChronoUnit.DAYS)))
                .claim("usuarioId", usuarioId)
                .claim("empresaId", empresaId)
                .signWith(claveFirma())
                .compact();
    }

    public boolean esValido(String token) {
        try {
            obtenerClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public String obtenerSujeto(String token) {
        return obtenerClaims(token).getSubject();
    }

    public Long obtenerUsuarioId(String token) {
        return obtenerClaims(token).get("usuarioId", Long.class);
    }

    public Long obtenerEmpresaId(String token) {
        return obtenerClaims(token).get("empresaId", Long.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> obtenerRoles(String token) {
        Object roles = obtenerClaims(token).get("roles");
        if (roles instanceof List<?> lista) {
            return lista.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private Claims obtenerClaims(String token) {
        return Jwts.parser()
                .verifyWith(claveFirma())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey claveFirma() {
        String secreto = propiedadesJwt.secreto();
        if (secreto == null || secreto.length() < 32) {
            throw new IllegalStateException("El secreto JWT debe tener al menos 32 caracteres");
        }
        return Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }
}

