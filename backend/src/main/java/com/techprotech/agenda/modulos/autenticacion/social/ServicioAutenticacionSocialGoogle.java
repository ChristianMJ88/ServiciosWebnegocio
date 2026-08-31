package com.techprotech.agenda.modulos.autenticacion.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.compartido.correo.ProtectorSecretosCorreo;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class ServicioAutenticacionSocialGoogle {
    private static final String AUTORIZACION = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN = "https://oauth2.googleapis.com/token";
    private static final String PERFIL = "https://openidconnect.googleapis.com/v1/userinfo";
    private static final String SCOPES = "openid email profile";

    private final PropiedadesAutenticacionSocial propiedades;
    private final ProtectorSecretosCorreo protector;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public ServicioAutenticacionSocialGoogle(
            PropiedadesAutenticacionSocial propiedades,
            ProtectorSecretosCorreo protector,
            ObjectMapper objectMapper,
            RestClient.Builder builder
    ) {
        this.propiedades = propiedades;
        this.protector = protector;
        this.objectMapper = objectMapper;
        this.restClient = builder.build();
    }

    public boolean estaHabilitado() {
        return propiedades.googleHabilitado()
                && !vacio(propiedades.googleClientId())
                && !vacio(propiedades.googleClientSecret())
                && !vacio(propiedades.googleRedirectUri());
    }

    public String construirUrlInicio() {
        validarConfiguracion();
        String state = cifrar(Map.of(
                "exp", Instant.now().plusSeconds(600).getEpochSecond(),
                "nonce", UUID.randomUUID().toString()
        ));
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
        if (error != null && !error.isBlank()) {
            return resultadoFrontend(null, "Google no autorizó el registro");
        }
        try {
            validarState(state);
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
            Map<String, Object> registro = Map.of(
                    "proveedor", "GOOGLE",
                    "subject", requerido(perfil, "sub"),
                    "correo", requerido(perfil, "email").trim().toLowerCase(),
                    "nombre", String.valueOf(perfil.getOrDefault("name", perfil.get("email"))),
                    "exp", Instant.now().plusSeconds(600).getEpochSecond()
            );
            return resultadoFrontend(cifrar(registro), null);
        } catch (Exception ex) {
            return resultadoFrontend(null, "No se pudo validar tu cuenta de Google");
        }
    }

    @SuppressWarnings("unchecked")
    public PerfilRegistroSocial validarTokenRegistro(String token) {
        try {
            Map<String, Object> datos = objectMapper.readValue(
                    protector.desencriptarSiNecesario(token, "registro_social"), Map.class);
            long exp = ((Number) datos.get("exp")).longValue();
            if (exp < Instant.now().getEpochSecond()) {
                throw new IllegalArgumentException("El registro social expiró");
            }
            return new PerfilRegistroSocial(
                    requerido(datos, "proveedor"),
                    requerido(datos, "subject"),
                    requerido(datos, "correo"),
                    requerido(datos, "nombre")
            );
        } catch (Exception ex) {
            throw new IllegalArgumentException("El registro social no es válido o expiró", ex);
        }
    }

    private void validarState(String state) throws Exception {
        Map<?, ?> datos = objectMapper.readValue(protector.desencriptarSiNecesario(state, "oauth_state_social"), Map.class);
        if (((Number) datos.get("exp")).longValue() < Instant.now().getEpochSecond()) {
            throw new IllegalArgumentException("El estado OAuth expiró");
        }
    }

    private String resultadoFrontend(String token, String error) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(propiedades.frontendRegistroUrl());
        if (token != null) builder.queryParam("registroSocial", token);
        if (error != null) builder.queryParam("errorSocial", error);
        return builder.build().encode(StandardCharsets.UTF_8).toUriString();
    }

    private String cifrar(Map<String, Object> datos) {
        try {
            return protector.encriptar(objectMapper.writeValueAsString(datos));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo proteger el registro social", ex);
        }
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
