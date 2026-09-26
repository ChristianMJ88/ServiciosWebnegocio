package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.pagos.api.dto.ConexionProveedorPagoResponse;
import com.techprotech.agenda.modulos.pagos.api.dto.InicioOAuthPagoResponse;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.EmpresaProveedorPagoEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.ProveedorPagoEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago.PropiedadesMercadoPago;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.EmpresaProveedorPagoRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.ProveedorPagoRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.seguridad.ProtectorSecretosPago;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ServicioOAuthMercadoPago {
    private static final String CODIGO_PROVEEDOR = "MERCADO_PAGO";
    private static final String AMBIENTE_PRODUCCION = "PRODUCCION";
    private static final String AMBIENTE_PRUEBA = "PRUEBA";
    private static final String AUTORIZACION = "https://auth.mercadopago.com/authorization";
    private static final String TOKEN = "https://api.mercadopago.com/oauth/token";
    private static final String PERFIL = "https://api.mercadopago.com/users/me";

    private final PropiedadesMercadoPago propiedades;
    private final ProtectorSecretosPago protector;
    private final ProveedorPagoRepositorio proveedores;
    private final EmpresaProveedorPagoRepositorio conexiones;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public ServicioOAuthMercadoPago(
            PropiedadesMercadoPago propiedades,
            ProtectorSecretosPago protector,
            ProveedorPagoRepositorio proveedores,
            EmpresaProveedorPagoRepositorio conexiones,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.propiedades = propiedades;
        this.protector = protector;
        this.proveedores = proveedores;
        this.conexiones = conexiones;
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public InicioOAuthPagoResponse iniciar(Long empresaId, Long usuarioId) {
        validarConfiguracionOAuth();
        String verifier = generarVerifier();
        String state = protector.encriptar(empresaId + "|" + usuarioId + "|"
                + Instant.now().plusSeconds(600).getEpochSecond() + "|" + verifier + "|" + UUID.randomUUID());
        String url = UriComponentsBuilder.fromHttpUrl(AUTORIZACION)
                .queryParam("response_type", "code")
                .queryParam("client_id", propiedades.clientId())
                .queryParam("redirect_uri", propiedades.redirectUri())
                .queryParam("state", state)
                .queryParam("scope", "read write offline_access")
                .queryParam("code_challenge", desafio(verifier))
                .queryParam("code_challenge_method", "S256")
                .build().encode(StandardCharsets.UTF_8).toUriString();
        return new InicioOAuthPagoResponse(url);
    }

    @Transactional
    public String completar(String code, String state, String error) {
        if (!vacio(error)) return resultado("error", "Mercado Pago no autorizó la conexión");
        try {
            validarConfiguracionOAuth();
            EstadoOAuth estado = validarState(state);
            JsonNode token = solicitarToken(code, estado.codeVerifier());
            String accessToken = requerido(token, "access_token");
            String refreshToken = requerido(token, "refresh_token");
            String cuentaId = texto(token, "user_id");
            if (vacio(cuentaId)) throw new IllegalStateException("Mercado Pago no devolvió user_id");

            JsonNode perfil = consultarPerfil(accessToken);
            ProveedorPagoEntidad proveedor = proveedor();
            EmpresaProveedorPagoEntidad conexion = conexiones
                    .findByEmpresaIdAndProveedorPagoIdAndAmbiente(estado.empresaId(), proveedor.getId(), AMBIENTE_PRODUCCION)
                    .orElseGet(EmpresaProveedorPagoEntidad::new);
            LocalDateTime ahora = LocalDateTime.now(ZoneOffset.UTC);
            conexion.setEmpresaId(estado.empresaId());
            conexion.setProveedorPagoId(proveedor.getId());
            conexion.setEstado("CONECTADO");
            conexion.setAmbiente(AMBIENTE_PRODUCCION);
            conexion.setCuentaExternaId(cuentaId);
            conexion.setCorreoCuenta(texto(perfil, "email"));
            conexion.setLlavePublica(texto(token, "public_key"));
            conexion.setTokenAccesoCifrado(protector.encriptar(accessToken));
            conexion.setTokenActualizacionCifrado(protector.encriptar(refreshToken));
            conexion.setTokenExpiraEn(ahora.plusSeconds(token.path("expires_in").asLong(15_552_000L)));
            conexion.setConectadoEn(ahora);
            conexion.setDesconectadoEn(null);
            conexion.setUltimaSincronizacionEn(ahora);
            conexiones.save(conexion);
            return resultado("ok", null);
        } catch (Exception ex) {
            return resultado("error", "No se pudo completar la conexión con Mercado Pago");
        }
    }

    @Transactional(readOnly = true)
    public ConexionProveedorPagoResponse estado(Long empresaId) {
        ProveedorPagoEntidad proveedor = proveedor();
        String ambiente = ambienteActivo();
        return conexiones.findByEmpresaIdAndProveedorPagoIdAndAmbiente(empresaId, proveedor.getId(), ambiente)
                .map(this::respuesta)
                .orElseGet(() -> new ConexionProveedorPagoResponse(CODIGO_PROVEEDOR, "NO_CONECTADO", ambiente,
                        null, null, null, null));
    }

    @Transactional
    public ConexionProveedorPagoResponse conectarPruebas(Long empresaId) {
        validarConfiguracionPruebas();
        ProveedorPagoEntidad proveedor = proveedor();
        EmpresaProveedorPagoEntidad conexion = conexiones
                .findByEmpresaIdAndProveedorPagoIdAndAmbiente(empresaId, proveedor.getId(), AMBIENTE_PRUEBA)
                .orElseGet(EmpresaProveedorPagoEntidad::new);
        LocalDateTime ahora = LocalDateTime.now(ZoneOffset.UTC);
        conexion.setEmpresaId(empresaId);
        conexion.setProveedorPagoId(proveedor.getId());
        conexion.setEstado("CONECTADO");
        conexion.setAmbiente(AMBIENTE_PRUEBA);
        conexion.setCuentaExternaId(null);
        conexion.setCorreoCuenta(null);
        conexion.setLlavePublica(propiedades.publicKeyPrueba());
        conexion.setTokenAccesoCifrado(protector.encriptar(propiedades.accessTokenPrueba()));
        conexion.setTokenActualizacionCifrado(null);
        conexion.setTokenExpiraEn(null);
        conexion.setConectadoEn(ahora);
        conexion.setDesconectadoEn(null);
        conexion.setUltimaSincronizacionEn(ahora);
        return respuesta(conexiones.save(conexion));
    }

    @Transactional
    public ConexionProveedorPagoResponse desconectar(Long empresaId) {
        ProveedorPagoEntidad proveedor = proveedor();
        String ambiente = ambienteActivo();
        EmpresaProveedorPagoEntidad conexion = conexiones
                .findByEmpresaIdAndProveedorPagoIdAndAmbiente(empresaId, proveedor.getId(), ambiente)
                .orElseThrow(() -> new IllegalStateException("Mercado Pago no está conectado"));
        conexion.setEstado("DESCONECTADO");
        conexion.setTokenAccesoCifrado(null);
        conexion.setTokenActualizacionCifrado(null);
        conexion.setTokenExpiraEn(null);
        conexion.setDesconectadoEn(LocalDateTime.now(ZoneOffset.UTC));
        return respuesta(conexiones.save(conexion));
    }

    @Transactional
    public String obtenerAccessToken(Long empresaId) {
        if (!propiedades.habilitado()) throw new IllegalStateException("Mercado Pago no está habilitado");
        ProveedorPagoEntidad proveedor = proveedor();
        String ambiente = ambienteActivo();
        EmpresaProveedorPagoEntidad conexion = conexiones
                .findByEmpresaIdAndProveedorPagoIdAndAmbiente(empresaId, proveedor.getId(), ambiente)
                .filter(entidad -> "CONECTADO".equals(entidad.getEstado()))
                .orElseThrow(() -> new IllegalStateException("Mercado Pago no está conectado para la empresa"));
        if (AMBIENTE_PRUEBA.equals(ambiente)) {
            return protector.desencriptar(conexion.getTokenAccesoCifrado());
        }
        validarConfiguracionOAuth();
        LocalDateTime margenRenovacion = LocalDateTime.now(ZoneOffset.UTC).plusDays(1);
        if (conexion.getTokenExpiraEn() != null && conexion.getTokenExpiraEn().isAfter(margenRenovacion)) {
            return protector.desencriptar(conexion.getTokenAccesoCifrado());
        }

        Map<String, String> body = new LinkedHashMap<>();
        body.put("client_id", propiedades.clientId());
        body.put("client_secret", propiedades.clientSecret());
        body.put("grant_type", "refresh_token");
        body.put("refresh_token", protector.desencriptar(conexion.getTokenActualizacionCifrado()));
        JsonNode token = solicitarToken(body);
        String accessToken = requerido(token, "access_token");
        String refreshToken = requerido(token, "refresh_token");
        LocalDateTime ahora = LocalDateTime.now(ZoneOffset.UTC);
        conexion.setTokenAccesoCifrado(protector.encriptar(accessToken));
        conexion.setTokenActualizacionCifrado(protector.encriptar(refreshToken));
        conexion.setTokenExpiraEn(ahora.plusSeconds(token.path("expires_in").asLong(15_552_000L)));
        conexion.setUltimaSincronizacionEn(ahora);
        conexiones.save(conexion);
        return accessToken;
    }

    private JsonNode solicitarToken(String code, String verifier) {
        if (vacio(code)) throw new IllegalArgumentException("Falta el código OAuth");
        Map<String, String> body = new LinkedHashMap<>();
        body.put("client_id", propiedades.clientId());
        body.put("client_secret", propiedades.clientSecret());
        body.put("grant_type", "authorization_code");
        body.put("code", code);
        body.put("redirect_uri", propiedades.redirectUri());
        body.put("code_verifier", verifier);
        return solicitarToken(body);
    }

    private JsonNode solicitarToken(Map<String, String> body) {
        JsonNode response = restClient.post().uri(TOKEN).contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().body(JsonNode.class);
        if (response == null) throw new IllegalStateException("Mercado Pago no devolvió tokens");
        return response;
    }

    private JsonNode consultarPerfil(String accessToken) {
        try {
            JsonNode perfil = restClient.get().uri(PERFIL).headers(h -> h.setBearerAuth(accessToken))
                    .retrieve().body(JsonNode.class);
            return perfil == null ? objectMapper.createObjectNode() : perfil;
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private EstadoOAuth validarState(String state) {
        String[] partes = protector.desencriptar(state).split("\\|", 5);
        if (partes.length != 5 || Long.parseLong(partes[2]) < Instant.now().getEpochSecond()) {
            throw new IllegalArgumentException("El estado OAuth es inválido o expiró");
        }
        return new EstadoOAuth(Long.parseLong(partes[0]), Long.parseLong(partes[1]), partes[3]);
    }

    private ProveedorPagoEntidad proveedor() {
        return proveedores.findByCodigoAndActivoTrue(CODIGO_PROVEEDOR)
                .orElseThrow(() -> new IllegalStateException("Mercado Pago no está disponible"));
    }

    private ConexionProveedorPagoResponse respuesta(EmpresaProveedorPagoEntidad entidad) {
        return new ConexionProveedorPagoResponse(CODIGO_PROVEEDOR, entidad.getEstado(), entidad.getAmbiente(),
                entidad.getCuentaExternaId(), entidad.getCorreoCuenta(), entidad.getConectadoEn(), entidad.getTokenExpiraEn());
    }

    private String resultado(String estado, String mensaje) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(propiedades.frontendResultadoUrl())
                .queryParam("mercadoPagoOAuth", estado);
        if (mensaje != null) builder.queryParam("mensaje", mensaje);
        return builder.build().encode(StandardCharsets.UTF_8).toUriString();
    }

    private String generarVerifier() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String desafio(String verifier) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar PKCE", ex);
        }
    }

    private String requerido(JsonNode nodo, String campo) {
        String valor = texto(nodo, campo);
        if (vacio(valor)) throw new IllegalStateException("Mercado Pago no devolvió " + campo);
        return valor;
    }

    private String texto(JsonNode nodo, String campo) {
        if (nodo == null || nodo.path(campo).isMissingNode() || nodo.path(campo).isNull()) return null;
        String valor = nodo.path(campo).asText();
        return vacio(valor) ? null : valor;
    }

    private void validarConfiguracionOAuth() {
        if (!propiedades.habilitado() || vacio(propiedades.clientId()) || vacio(propiedades.clientSecret())
                || vacio(propiedades.redirectUri()) || vacio(propiedades.frontendResultadoUrl())) {
            throw new IllegalStateException("Mercado Pago no está configurado en Fluora");
        }
    }

    private void validarConfiguracionPruebas() {
        if (!propiedades.habilitado() || !AMBIENTE_PRUEBA.equals(ambienteActivo())
                || vacio(propiedades.publicKeyPrueba()) || vacio(propiedades.accessTokenPrueba())) {
            throw new IllegalStateException("Las credenciales de prueba de Mercado Pago no están configuradas");
        }
    }

    private String ambienteActivo() {
        return AMBIENTE_PRUEBA.equalsIgnoreCase(propiedades.ambiente()) ? AMBIENTE_PRUEBA : AMBIENTE_PRODUCCION;
    }

    private boolean vacio(String valor) { return valor == null || valor.isBlank(); }
    private record EstadoOAuth(Long empresaId, Long usuarioId, String codeVerifier) {}
}
