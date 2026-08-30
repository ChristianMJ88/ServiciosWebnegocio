package com.techprotech.agenda.compartido.correo;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class ServicioOAuthCorreoMicrosoft {

    private static final String LOGIN_BASE_URL = "https://login.microsoftonline.com";
    private static final String GRAPH_BASE_URL = "https://graph.microsoft.com/v1.0";
    private static final String SCOPES = "openid profile email offline_access User.Read Mail.Send";
    private static final String CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    private final PropiedadesOAuthCorreoMicrosoft propiedades;
    private final ProtectorSecretosCorreo protectorSecretos;
    private final ConfiguracionCorreoEmpresaRepositorio repositorio;
    private final RestClient restClient;

    public ServicioOAuthCorreoMicrosoft(
            PropiedadesOAuthCorreoMicrosoft propiedades,
            ProtectorSecretosCorreo protectorSecretos,
            ConfiguracionCorreoEmpresaRepositorio repositorio,
            RestClient.Builder restClientBuilder
    ) {
        this.propiedades = propiedades;
        this.protectorSecretos = protectorSecretos;
        this.repositorio = repositorio;
        this.restClient = restClientBuilder.build();
    }

    public InicioOAuthCorreoMicrosoftResponse iniciar(Long empresaId, Long usuarioId) {
        validarConfiguracion();
        long expira = Instant.now().plusSeconds(600).getEpochSecond();
        String statePlano = empresaId + "|" + usuarioId + "|" + expira + "|" + UUID.randomUUID();
        String state = protectorSecretos.encriptar(statePlano);
        String url = UriComponentsBuilder.fromHttpUrl(LOGIN_BASE_URL + "/organizations/oauth2/v2.0/authorize")
                .queryParam("client_id", propiedades.clientId())
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", propiedades.redirectUri())
                .queryParam("response_mode", "query")
                .queryParam("scope", SCOPES)
                .queryParam("state", state)
                .build().encode(StandardCharsets.UTF_8).toUriString();
        return new InicioOAuthCorreoMicrosoftResponse(url);
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public String completar(String code, String state, String error) {
        if (error != null && !error.isBlank()) {
            return urlResultado("error", "Microsoft no autorizó la conexión");
        }
        try {
            EstadoOAuth estado = validarState(state);
            String tokenEndpoint = LOGIN_BASE_URL + "/organizations/oauth2/v2.0/token";
            MultiValueMap<String, String> form = formularioBase(tokenEndpoint);
            form.add("grant_type", "authorization_code");
            form.add("code", code);
            form.add("redirect_uri", propiedades.redirectUri());
            form.add("scope", SCOPES);

            Map<String, Object> token = solicitarToken(tokenEndpoint, form);
            String accessToken = requerido(token, "access_token");
            String refreshToken = requerido(token, "refresh_token");
            Map<String, Object> perfil = restClient.get()
                    .uri(GRAPH_BASE_URL + "/me?$select=id,displayName,mail,userPrincipalName")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve().body(Map.class);
            if (perfil == null) {
                throw new IllegalStateException("Microsoft Graph no devolvió el perfil del usuario");
            }

            String tenantId = extraerTenantId(token.get("id_token"));
            String correo = primeroNoVacio((String) perfil.get("mail"), (String) perfil.get("userPrincipalName"));
            String nombre = primeroNoVacio((String) perfil.get("displayName"), correo);
            String userId = requerido(perfil, "id");

            ConfiguracionCorreoEmpresaEntidad entidad = repositorio.findById(estado.empresaId())
                    .orElseGet(ConfiguracionCorreoEmpresaEntidad::new);
            entidad.setEmpresaId(estado.empresaId());
            entidad.setHabilitado(true);
            entidad.setProveedor(ProveedorCorreo.GRAPH.name());
            entidad.setRemitente(correo);
            entidad.setNombreRemitente(nombre);
            entidad.setGraphTenantId(tenantId);
            entidad.setGraphClientId(propiedades.clientId());
            entidad.setGraphUserId(userId);
            entidad.setGraphOauthRefreshToken(protectorSecretos.encriptar(refreshToken));
            entidad.setGraphOauthScopes((String) token.getOrDefault("scope", SCOPES));
            entidad.setGraphOauthConectadoEn(LocalDateTime.now(ZoneOffset.UTC));
            repositorio.save(entidad);
            return urlResultado("ok", null);
        } catch (Exception ex) {
            return urlResultado("error", "No se pudo completar la conexión con Microsoft 365");
        }
    }

    @Transactional
    public String obtenerAccessToken(ConfiguracionCorreoResolvida configuracion) {
        validarConfiguracion();
        String tenantId = configuracion.graphTenantId();
        String tokenEndpoint = LOGIN_BASE_URL + "/" + tenantId + "/oauth2/v2.0/token";
        MultiValueMap<String, String> form = formularioBase(tokenEndpoint);
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", configuracion.graphOauthRefreshToken());
        form.add("scope", SCOPES);
        Map<String, Object> token = solicitarToken(tokenEndpoint, form);
        String nuevoRefreshToken = (String) token.get("refresh_token");
        if (nuevoRefreshToken != null && !nuevoRefreshToken.isBlank() && configuracion.empresaId() != null) {
            repositorio.findById(configuracion.empresaId()).ifPresent(entidad -> {
                entidad.setGraphOauthRefreshToken(protectorSecretos.encriptar(nuevoRefreshToken));
                repositorio.save(entidad);
            });
        }
        return requerido(token, "access_token");
    }

    private MultiValueMap<String, String> formularioBase(String tokenEndpoint) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", propiedades.clientId());
        form.add("client_assertion_type", CLIENT_ASSERTION_TYPE);
        form.add("client_assertion", construirClientAssertion(tokenEndpoint));
        return form;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> solicitarToken(String tokenEndpoint, MultiValueMap<String, String> form) {
        Map<String, Object> response = restClient.post().uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form).retrieve().body(Map.class);
        if (response == null) {
            throw new IllegalStateException("Microsoft no devolvió tokens");
        }
        return response;
    }

    private String construirClientAssertion(String tokenEndpoint) {
        Instant ahora = Instant.now();
        return Jwts.builder().header()
                .add("typ", "JWT").add("alg", "RS256")
                .add("x5t", thumbprintBase64Url(propiedades.certificateThumbprint())).and()
                .issuer(propiedades.clientId()).subject(propiedades.clientId())
                .audience().add(tokenEndpoint).and().id(UUID.randomUUID().toString())
                .issuedAt(Date.from(ahora)).notBefore(Date.from(ahora.minusSeconds(30)))
                .expiration(Date.from(ahora.plusSeconds(600)))
                .signWith(cargarLlavePrivada(), SignatureAlgorithm.RS256).compact();
    }

    private EstadoOAuth validarState(String state) {
        String plano = protectorSecretos.desencriptarSiNecesario(state, "oauth_state");
        String[] partes = plano.split("\\|", 4);
        if (partes.length != 4 || Long.parseLong(partes[2]) < Instant.now().getEpochSecond()) {
            throw new IllegalArgumentException("El estado OAuth es inválido o expiró");
        }
        return new EstadoOAuth(Long.parseLong(partes[0]), Long.parseLong(partes[1]));
    }

    private String extraerTenantId(Object idToken) {
        if (!(idToken instanceof String jwt) || jwt.isBlank()) {
            throw new IllegalStateException("Microsoft no devolvió id_token");
        }
        String[] partes = jwt.split("\\.");
        if (partes.length < 2) {
            throw new IllegalStateException("id_token inválido");
        }
        try {
            String json = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\\"tid\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(json);
            if (!matcher.find()) {
                throw new IllegalStateException("id_token sin tenant");
            }
            return matcher.group(1);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("No se pudo leer el tenant de Microsoft", ex);
        }
    }

    private RSAPrivateKey cargarLlavePrivada() {
        try {
            String contenido = propiedades.privateKeyPem()
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
            PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(contenido))
            );
            return (RSAPrivateKey) key;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo cargar la credencial privada de Microsoft", ex);
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

    private void validarConfiguracion() {
        if (!propiedades.habilitado() || esVacio(propiedades.clientId())
                || esVacio(propiedades.certificateThumbprint()) || esVacio(propiedades.privateKeyPem())) {
            throw new IllegalStateException("La conexión Microsoft 365 de Fluora no está configurada");
        }
    }

    private String urlResultado(String estado, String mensaje) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(propiedades.frontendResultadoUrl())
                .queryParam("correoOAuth", estado);
        if (mensaje != null) {
            builder.queryParam("mensaje", mensaje);
        }
        return builder.build().encode(StandardCharsets.UTF_8).toUriString();
    }

    private String requerido(Map<String, Object> valores, String clave) {
        Object valor = valores.get(clave);
        if (!(valor instanceof String texto) || texto.isBlank()) {
            throw new IllegalStateException("Microsoft no devolvió " + clave);
        }
        return texto;
    }

    private String primeroNoVacio(String primero, String segundo) {
        return !esVacio(primero) ? primero : segundo;
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private record EstadoOAuth(Long empresaId, Long usuarioId) {}
}

