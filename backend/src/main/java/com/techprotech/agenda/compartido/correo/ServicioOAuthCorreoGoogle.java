package com.techprotech.agenda.compartido.correo;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Service
public class ServicioOAuthCorreoGoogle {

    private static final String AUTORIZACION = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN = "https://oauth2.googleapis.com/token";
    private static final String PERFIL = "https://openidconnect.googleapis.com/v1/userinfo";
    private static final String SCOPES = "openid email profile https://www.googleapis.com/auth/gmail.send";

    private final PropiedadesOAuthCorreoGoogle propiedades;
    private final ProtectorSecretosCorreo protector;
    private final ConfiguracionCorreoEmpresaRepositorio repositorio;
    private final RestClient restClient;

    public ServicioOAuthCorreoGoogle(
            PropiedadesOAuthCorreoGoogle propiedades,
            ProtectorSecretosCorreo protector,
            ConfiguracionCorreoEmpresaRepositorio repositorio,
            RestClient.Builder builder
    ) {
        this.propiedades = propiedades;
        this.protector = protector;
        this.repositorio = repositorio;
        this.restClient = builder.build();
    }

    public InicioOAuthCorreoMicrosoftResponse iniciar(Long empresaId, Long usuarioId) {
        validarConfiguracion();
        String state = protector.encriptar(empresaId + "|" + usuarioId + "|"
                + Instant.now().plusSeconds(600).getEpochSecond() + "|" + UUID.randomUUID());
        String url = UriComponentsBuilder.fromHttpUrl(AUTORIZACION)
                .queryParam("client_id", propiedades.clientId())
                .queryParam("redirect_uri", propiedades.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPES)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("include_granted_scopes", "true")
                .queryParam("state", state).build().encode(StandardCharsets.UTF_8).toUriString();
        return new InicioOAuthCorreoMicrosoftResponse(url);
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public String completar(String code, String state, String error) {
        if (error != null && !error.isBlank()) return resultado("error", "Google no autorizó la conexión");
        try {
            EstadoOAuth estado = validarState(state);
            MultiValueMap<String, String> form = formularioBase();
            form.add("grant_type", "authorization_code");
            form.add("code", code);
            form.add("redirect_uri", propiedades.redirectUri());
            Map<String, Object> token = solicitarToken(form);
            String accessToken = requerido(token, "access_token");
            String refreshToken = requerido(token, "refresh_token");
            Map<String, Object> perfil = restClient.get().uri(PERFIL)
                    .headers(h -> h.setBearerAuth(accessToken)).retrieve().body(Map.class);
            if (perfil == null) throw new IllegalStateException("Google no devolvió el perfil");
            String correo = requerido(perfil, "email");
            String nombre = (String) perfil.getOrDefault("name", correo);

            ConfiguracionCorreoEmpresaEntidad entidad = repositorio.findById(estado.empresaId())
                    .orElseGet(ConfiguracionCorreoEmpresaEntidad::new);
            entidad.setEmpresaId(estado.empresaId());
            entidad.setHabilitado(true);
            entidad.setProveedor(ProveedorCorreo.GMAIL.name());
            entidad.setRemitente(correo);
            entidad.setNombreRemitente(nombre);
            entidad.setGmailUserId(correo);
            entidad.setGmailOauthRefreshToken(protector.encriptar(refreshToken));
            entidad.setGmailOauthScopes((String) token.getOrDefault("scope", SCOPES));
            entidad.setGmailOauthConectadoEn(LocalDateTime.now(ZoneOffset.UTC));
            repositorio.save(entidad);
            return resultado("ok", null);
        } catch (Exception ex) {
            return resultado("error", "No se pudo completar la conexión con Gmail");
        }
    }

    @Transactional
    public String obtenerAccessToken(Long empresaId) {
        validarConfiguracion();
        ConfiguracionCorreoEmpresaEntidad entidad = repositorio.findById(empresaId)
                .orElseThrow(() -> new IllegalStateException("Gmail no está conectado"));
        String refresh = protector.desencriptarSiNecesario(entidad.getGmailOauthRefreshToken(), "gmail_oauth_refresh_token");
        MultiValueMap<String, String> form = formularioBase();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refresh);
        return requerido(solicitarToken(form), "access_token");
    }

    private MultiValueMap<String, String> formularioBase() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", propiedades.clientId());
        form.add("client_secret", propiedades.clientSecret());
        return form;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> solicitarToken(MultiValueMap<String, String> form) {
        Map<String, Object> response = restClient.post().uri(TOKEN)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(Map.class);
        if (response == null) throw new IllegalStateException("Google no devolvió tokens");
        return response;
    }

    private EstadoOAuth validarState(String state) {
        String[] partes = protector.desencriptarSiNecesario(state, "oauth_state").split("\\|", 4);
        if (partes.length != 4 || Long.parseLong(partes[2]) < Instant.now().getEpochSecond()) {
            throw new IllegalArgumentException("El estado OAuth es inválido o expiró");
        }
        return new EstadoOAuth(Long.parseLong(partes[0]), Long.parseLong(partes[1]));
    }

    private String resultado(String estado, String mensaje) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(propiedades.frontendResultadoUrl())
                .queryParam("correoOAuth", estado).queryParam("proveedorCorreo", "gmail");
        if (mensaje != null) builder.queryParam("mensaje", mensaje);
        return builder.build().encode(StandardCharsets.UTF_8).toUriString();
    }

    private String requerido(Map<String, Object> valores, String clave) {
        Object valor = valores.get(clave);
        if (!(valor instanceof String texto) || texto.isBlank()) throw new IllegalStateException("Google no devolvió " + clave);
        return texto;
    }

    private void validarConfiguracion() {
        if (!propiedades.habilitado() || vacio(propiedades.clientId()) || vacio(propiedades.clientSecret())
                || vacio(propiedades.redirectUri()) || vacio(propiedades.frontendResultadoUrl())) {
            throw new IllegalStateException("La conexión Gmail de Fluora no está configurada");
        }
    }

    private boolean vacio(String valor) { return valor == null || valor.isBlank(); }
    private record EstadoOAuth(Long empresaId, Long usuarioId) {}
}
