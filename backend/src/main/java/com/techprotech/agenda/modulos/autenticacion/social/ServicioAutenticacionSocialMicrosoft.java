package com.techprotech.agenda.modulos.autenticacion.social;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class ServicioAutenticacionSocialMicrosoft {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioAutenticacionSocialMicrosoft.class);
    private static final String LOGIN = "https://login.microsoftonline.com/common/oauth2/v2.0";
    private static final String PERFIL = "https://graph.microsoft.com/v1.0/me?$select=id,displayName,mail,userPrincipalName";
    private static final String SCOPES = "openid profile email User.Read";
    private static final String ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    private final PropiedadesAutenticacionSocial propiedades;
    private final ServicioTokenRegistroSocial tokens;
    private final ServicioAccesoSocial accesoSocial;
    private final RestClient restClient;

    public ServicioAutenticacionSocialMicrosoft(
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
        return propiedades.microsoftHabilitado()
                && !vacio(propiedades.microsoftClientId())
                && !vacio(propiedades.microsoftCertificateThumbprint())
                && !vacio(propiedades.microsoftPrivateKeyPem())
                && !vacio(propiedades.microsoftRedirectUri())
                && !vacio(propiedades.frontendRegistroUrl());
    }

    public String construirUrlInicio(String destino) {
        validarConfiguracion();
        return UriComponentsBuilder.fromHttpUrl(LOGIN + "/authorize")
                .queryParam("client_id", propiedades.microsoftClientId())
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", propiedades.microsoftRedirectUri())
                .queryParam("response_mode", "query")
                .queryParam("scope", SCOPES)
                .queryParam("prompt", "select_account")
                .queryParam("state", tokens.crearEstado("MICROSOFT", destino))
                .build().encode(StandardCharsets.UTF_8).toUriString();
    }

    @SuppressWarnings("unchecked")
    public String completar(String code, String state, String error) {
        String destino = "REGISTRO";
        try {
            destino = tokens.validarEstado(state, "MICROSOFT");
            if (error != null && !error.isBlank()) {
                return "ACCESO".equals(destino)
                        ? resultadoAcceso(null, "Microsoft no autorizó el acceso")
                        : resultadoRegistro(null, "Microsoft no autorizó el registro");
            }
            String tokenEndpoint = LOGIN + "/token";
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", propiedades.microsoftClientId());
            form.add("client_assertion_type", ASSERTION_TYPE);
            form.add("client_assertion", crearAsercion(tokenEndpoint));
            form.add("grant_type", "authorization_code");
            form.add("code", code);
            form.add("redirect_uri", propiedades.microsoftRedirectUri());
            form.add("scope", SCOPES);
            Map<String, Object> respuesta = restClient.post().uri(tokenEndpoint)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form).retrieve().body(Map.class);
            String accessToken = requerido(respuesta, "access_token");
            Map<String, Object> perfil = restClient.get().uri(PERFIL)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve().body(Map.class);
            String correo = primeroNoVacio(texto(perfil, "mail"), texto(perfil, "userPrincipalName"));
            if (vacio(correo) || !correo.contains("@")) throw new IllegalStateException("Microsoft no devolvió un correo válido");
            PerfilRegistroSocial registro = new PerfilRegistroSocial(
                    "MICROSOFT",
                    requerido(perfil, "id"),
                    correo.trim().toLowerCase(),
                    primeroNoVacio(texto(perfil, "displayName"), correo)
            );
            if ("ACCESO".equals(destino)) {
                return resultadoAcceso(accesoSocial.crearCodigo(registro), null);
            }
            return resultadoRegistro(tokens.crearToken(registro), null);
        } catch (Exception ex) {
            LOGGER.warn("No se pudo completar el registro social con Microsoft: {}: {}",
                    ex.getClass().getSimpleName(), ex.getMessage());
            return "ACCESO".equals(destino)
                    ? resultadoAcceso(null, "No se pudo validar tu cuenta de Microsoft")
                    : resultadoRegistro(null, "No se pudo validar tu cuenta de Microsoft");
        }
    }

    private String crearAsercion(String audience) {
        Instant ahora = Instant.now();
        return Jwts.builder().header()
                .add("typ", "JWT").add("alg", "RS256")
                .add("x5t", thumbprintBase64Url(propiedades.microsoftCertificateThumbprint())).and()
                .issuer(propiedades.microsoftClientId()).subject(propiedades.microsoftClientId())
                .audience().add(audience).and().id(UUID.randomUUID().toString())
                .issuedAt(Date.from(ahora)).notBefore(Date.from(ahora.minusSeconds(30)))
                .expiration(Date.from(ahora.plusSeconds(600)))
                .signWith(cargarLlavePrivada(), SignatureAlgorithm.RS256).compact();
    }

    private RSAPrivateKey cargarLlavePrivada() {
        try {
            String contenido = propiedades.microsoftPrivateKeyPem()
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
            PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(contenido)));
            return (RSAPrivateKey) key;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo cargar el certificado de identidad Microsoft", ex);
        }
    }

    private String thumbprintBase64Url(String hex) {
        String limpio = hex.replace(":", "").replace(" ", "");
        byte[] bytes = new byte[limpio.length() / 2];
        for (int i = 0; i < limpio.length(); i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(limpio.substring(i, i + 2), 16);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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

    private String requerido(Map<String, Object> valores, String clave) {
        String valor = texto(valores, clave);
        if (vacio(valor)) throw new IllegalStateException("Microsoft no devolvió " + clave);
        return valor;
    }

    private String texto(Map<String, Object> valores, String clave) {
        if (valores == null || valores.get(clave) == null) return null;
        return String.valueOf(valores.get(clave));
    }

    private String primeroNoVacio(String primero, String segundo) { return !vacio(primero) ? primero : segundo; }
    private boolean vacio(String valor) { return valor == null || valor.isBlank(); }
    private void validarConfiguracion() { if (!estaHabilitado()) throw new IllegalStateException("El registro con Microsoft no está configurado"); }
}
