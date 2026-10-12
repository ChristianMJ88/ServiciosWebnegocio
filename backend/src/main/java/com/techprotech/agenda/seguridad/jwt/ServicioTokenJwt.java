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

    public String generarTokenAcceso(
            String sujeto,
            Long usuarioId,
            Long empresaId,
            String empresaSlug,
            Collection<String> roles,
            Collection<String> permisos,
            Collection<Long> sucursalesPermitidas
    ) {
        return generarTokenAcceso(sujeto, usuarioId, empresaId, empresaSlug, roles, permisos, sucursalesPermitidas, 0);
    }

    public String generarTokenAcceso(String sujeto, Long usuarioId, Long empresaId, String empresaSlug,
            Collection<String> roles, Collection<String> permisos, Collection<Long> sucursalesPermitidas, long versionSesion) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(sujeto)
                .issuer(propiedadesJwt.issuer())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(propiedadesJwt.minutosAcceso(), ChronoUnit.MINUTES)))
                .claim("usuarioId", usuarioId)
                .claim("empresaId", empresaId)
                .claim("empresaSlug", empresaSlug)
                .claim("versionSesion", versionSesion)
                .claim("ambito", "TENANT")
                .claim("roles", roles)
                .claim("permisos", permisos)
                .claim("sucursales", sucursalesPermitidas)
                .signWith(claveFirma())
                .compact();
    }

    public String generarTokenPlataforma(String sujeto, Long usuarioId,
                                          Collection<String> roles, Collection<String> permisos) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(sujeto)
                .issuer(propiedadesJwt.issuer())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(propiedadesJwt.minutosAcceso(), ChronoUnit.MINUTES)))
                .claim("usuarioId", usuarioId)
                .claim("ambito", "PLATAFORMA")
                .claim("roles", roles)
                .claim("permisos", permisos)
                .claim("sucursales", List.of())
                .signWith(claveFirma())
                .compact();
    }

    public String generarTokenActualizacion(String sujeto, Long usuarioId, Long empresaId) {
        return generarTokenActualizacion(sujeto, usuarioId, empresaId, 0);
    }

    public String generarTokenActualizacion(String sujeto, Long usuarioId, Long empresaId, long versionSesion) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(sujeto)
                .issuer(propiedadesJwt.issuer())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(propiedadesJwt.diasRefresh(), ChronoUnit.DAYS)))
                .claim("usuarioId", usuarioId)
                .claim("empresaId", empresaId)
                .claim("versionSesion", versionSesion)
                .signWith(claveFirma())
                .compact();
    }

    public long obtenerVersionSesion(String token) {
        Number version = obtenerClaims(token).get("versionSesion", Number.class);
        return version == null ? 0 : version.longValue();
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

    public String obtenerEmpresaSlug(String token) {
        return obtenerClaims(token).get("empresaSlug", String.class);
    }

    public String obtenerAmbito(String token) {
        String ambito = obtenerClaims(token).get("ambito", String.class);
        return ambito == null ? "TENANT" : ambito;
    }

    @SuppressWarnings("unchecked")
    public List<String> obtenerRoles(String token) {
        Object roles = obtenerClaims(token).get("roles");
        if (roles instanceof List<?> lista) {
            return lista.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public List<String> obtenerPermisos(String token) {
        Object permisos = obtenerClaims(token).get("permisos");
        if (permisos instanceof List<?> lista) {
            return lista.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    public List<Long> obtenerSucursalesPermitidas(String token) {
        Object sucursales = obtenerClaims(token).get("sucursales");
        if (sucursales instanceof List<?> lista) {
            return lista.stream()
                    .map(valor -> {
                        if (valor instanceof Number numero) {
                            return numero.longValue();
                        }
                        return Long.valueOf(String.valueOf(valor));
                    })
                    .toList();
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
