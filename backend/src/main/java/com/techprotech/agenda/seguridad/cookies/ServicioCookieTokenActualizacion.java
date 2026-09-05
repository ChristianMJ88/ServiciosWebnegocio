package com.techprotech.agenda.seguridad.cookies;

import com.techprotech.agenda.modulos.autenticacion.api.dto.RefrescarTokenRequest;
import com.techprotech.agenda.modulos.autenticacion.api.dto.RespuestaAccesoApp;
import com.techprotech.agenda.modulos.autenticacion.api.dto.RespuestaTokenJwt;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class ServicioCookieTokenActualizacion {

    public static final String NOMBRE_COOKIE = "fluora_refresh";
    private static final String RUTA_COOKIE = "/api/v1/auth";

    private final boolean segura;
    private final String sameSite;
    private final Duration vigencia;

    public ServicioCookieTokenActualizacion(
            @Value("${aplicacion.cookie-sesion.secure:false}") boolean segura,
            @Value("${aplicacion.cookie-sesion.same-site:Lax}") String sameSite,
            @Value("${aplicacion.jwt.dias-refresh:7}") long diasRefresh
    ) {
        this.segura = segura;
        this.sameSite = sameSite;
        this.vigencia = Duration.ofDays(diasRefresh);
    }

    public void agregar(HttpServletResponse response, RespuestaTokenJwt sesion) {
        if (sesion != null && StringUtils.hasText(sesion.tokenActualizacion())) {
            response.addHeader(HttpHeaders.SET_COOKIE, construir(sesion.tokenActualizacion(), vigencia).toString());
        }
    }

    public void agregar(HttpServletResponse response, RespuestaAccesoApp acceso) {
        if (acceso != null) {
            agregar(response, acceso.sesion());
        }
    }

    public void eliminar(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, construir("", Duration.ZERO).toString());
    }

    public RefrescarTokenRequest resolver(String tokenCookie) {
        if (!StringUtils.hasText(tokenCookie)) {
            throw new ResponseStatusException(UNAUTHORIZED, "No hay una sesión renovable");
        }
        return new RefrescarTokenRequest(tokenCookie);
    }

    private ResponseCookie construir(String valor, Duration maxAge) {
        return ResponseCookie.from(NOMBRE_COOKIE, valor)
                .httpOnly(true)
                .secure(segura)
                .sameSite(sameSite)
                .path(RUTA_COOKIE)
                .maxAge(maxAge)
                .build();
    }
}
