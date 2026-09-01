package com.techprotech.agenda.modulos.autenticacion.social;

import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class ServicioAutenticacionSocialGoogle {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioAutenticacionSocialGoogle.class);
    private static final String AUTORIZACION = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN = "https://oauth2.googleapis.com/token";
    private static final String PERFIL = "https://openidconnect.googleapis.com/v1/userinfo";
    private static final String SCOPES = "openid email profile";

    private final PropiedadesAutenticacionSocial propiedades;
    private final ServicioTokenRegistroSocial tokens;
    private final ServicioAccesoSocial accesoSocial;
    private final RestClient restClient;

    public ServicioAutenticacionSocialGoogle(
            PropiedadesAutenticacionSocial propiedades,
            ServicioTokenRegistroSocial tokens,
            ServicioAccesoSocial accesoSocial,
            RestClient.Builder builder
    ) {
        this.propiedades = propiedades;
        this.tokens = tokens;
        this.accesoSocial = accesoSocial;
        this.restClient = builder.build();
    }

    public boolean estaHabilitado() {
        return propiedades.googleHabilitado()
                && !vacio(propiedades.googleClientId())
                && !vacio(propiedades.googleClientSecret())
                && !vacio(propiedades.googleRedirectUri());
    }

    public String construirUrlInicio() {
        return construirUrlInicio("REGISTRO");
    }

    public String construirUrlInicio(String destino) {
        validarConfiguracion();
        String state = tokens.crearEstado("GOOGLE", destino);
        return UriComponentsBuilder.fromHttpUrl(AUTORIZACION)
                .queryParam("client_id", propiedades.googleClientId())
                .queryParam("redirect_uri", propiedades.googleRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPES)
                .queryParam("prompt", "select_account")
                .queryParam("state", state)
                .build().encode(StandardCharsets.UTF_8).toUriString();
    }

    @SuppressWarnings("unchecked")
    public String completar(String code, String state, String error) {
        String destino = "REGISTRO";
        try {
            destino = tokens.validarEstado(state, "GOOGLE");
            if (error != null && !error.isBlank()) {
                return "ACCESO".equals(destino)
                        ? resultadoAcceso(null, "Google no autorizó el acceso")
                        : resultadoRegistro(null, "Google no autorizó el registro");
            }
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", propiedades.googleClientId());
            form.add("client_secret", propiedades.googleClientSecret());
            form.add("grant_type", "authorization_code");
            form.add("code", code);
            form.add("redirect_uri", propiedades.googleRedirectUri());
            Map<String, Object> token = restClient.post().uri(TOKEN)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form).retrieve().body(Map.class);
            String accessToken = requerido(token, "access_token");
            Map<String, Object> perfil = restClient.get().uri(PERFIL)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve().body(Map.class);
            if (perfil == null || !Boolean.TRUE.equals(perfil.get("email_verified"))) {
                throw new IllegalStateException("Google no confirmó el correo");
            }
            PerfilRegistroSocial registro = new PerfilRegistroSocial(
                    "GOOGLE",
                    requerido(perfil, "sub"),
                    requerido(perfil, "email").trim().toLowerCase(),
                    String.valueOf(perfil.getOrDefault("name", perfil.get("email")))
            );
            if ("ACCESO".equals(destino)) return resultadoAcceso(accesoSocial.crearCodigo(registro), null);
            return resultadoRegistro(tokens.crearToken(registro), null);
        } catch (Exception ex) {
            LOGGER.warn("No se pudo completar el registro social con Google: {}: {}",
                    ex.getClass().getSimpleName(), ex.getMessage());
            return "ACCESO".equals(destino)
                    ? resultadoAcceso(null, "No se pudo validar tu cuenta de Google")
                    : resultadoRegistro(null, "No se pudo validar tu cuenta de Google");
        }
    }

    @SuppressWarnings("unchecked")
    public PerfilRegistroSocial validarTokenRegistro(String token) {
        return tokens.validarToken(token);
    }

    private String resultadoRegistro(String token, String error) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(propiedades.frontendRegistroUrl());
        if (token != null) builder.queryParam("registroSocial", token);
        if (error != null) builder.queryParam("errorSocial", error);
        return builder.build().encode(StandardCharsets.UTF_8).toUriString();
    }

    private String resultadoAcceso(String codigo, String error) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(propiedades.frontendAccesoUrl());
        if (codigo != null) builder.queryParam("inicioSocial", codigo);
        if (error != null) builder.queryParam("errorSocial", error);
        return builder.build().encode(StandardCharsets.UTF_8).toUriString();
    }

    private String requerido(Map<String, Object> datos, String clave) {
        if (datos == null || datos.get(clave) == null || String.valueOf(datos.get(clave)).isBlank()) {
            throw new IllegalStateException("Falta " + clave);
        }
        return String.valueOf(datos.get(clave));
    }

    private void validarConfiguracion() {
        if (!estaHabilitado() || vacio(propiedades.frontendRegistroUrl())) {
            throw new IllegalStateException("El registro con Google no está configurado");
        }
    }

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
