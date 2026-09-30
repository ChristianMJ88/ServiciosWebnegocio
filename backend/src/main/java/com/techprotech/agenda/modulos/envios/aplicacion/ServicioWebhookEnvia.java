package com.techprotech.agenda.modulos.envios.aplicacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.envios.infraestructura.entidad.EmpresaEnvioConfigEntidad;
import com.techprotech.agenda.modulos.envios.infraestructura.entidad.EventoWebhookEnvioEntidad;
import com.techprotech.agenda.modulos.envios.infraestructura.repositorio.EmpresaEnvioConfigRepositorio;
import com.techprotech.agenda.modulos.envios.infraestructura.repositorio.EventoWebhookEnvioRepositorio;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class ServicioWebhookEnvia {
    private static final String PROVEEDOR = "ENVIA";
    private static final long VENTANA_FIRMA_MILISEGUNDOS = 5 * 60 * 1000L;

    private final EmpresaEnvioConfigRepositorio configuraciones;
    private final EventoWebhookEnvioRepositorio eventos;
    private final ResolutorCredencialEnvio credenciales;
    private final ObjectMapper objectMapper;

    public ServicioWebhookEnvia(EmpresaEnvioConfigRepositorio configuraciones,
                                EventoWebhookEnvioRepositorio eventos,
                                ResolutorCredencialEnvio credenciales,
                                ObjectMapper objectMapper) {
        this.configuraciones = configuraciones;
        this.eventos = eventos;
        this.credenciales = credenciales;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void recibir(String token, String entregaId, String eventoHeader, String timestamp,
                        String firma, String payload) {
        EmpresaEnvioConfigEntidad config = configuraciones
                .findByWebhookTokenHashAndProveedorAndHabilitadoTrue(hash(token), PROVEEDOR)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (firma != null && !firma.isBlank()) validarFirma(config, eventoHeader, timestamp, firma, payload);

        try {
            JsonNode cuerpo = objectMapper.readTree(payload);
            String tipo = texto(cuerpo, "type");
            if (tipo == null) tipo = eventoHeader;
            if (tipo == null || tipo.isBlank()) tipo = "desconocido";
            String eventoId = entregaId;
            if (eventoId == null || eventoId.isBlank()) {
                eventoId = hash(tipo + ":" + texto(cuerpo, "created_at") + ":" + payload);
            }
            if (eventos.existsByEmpresaIdAndProveedorAndEventoExternoId(config.getEmpresaId(), PROVEEDOR, eventoId)) return;

            EventoWebhookEnvioEntidad evento = new EventoWebhookEnvioEntidad();
            evento.setEmpresaId(config.getEmpresaId());
            evento.setProveedor(PROVEEDOR);
            evento.setEventoExternoId(eventoId);
            evento.setTipo(tipo);
            evento.setEstadoProcesamiento("RECIBIDO");
            evento.setPayload(payload);
            eventos.saveAndFlush(evento);
        } catch (DataIntegrityViolationException ignored) {
            // Envia puede reintentar la misma entrega; el índice único la vuelve idempotente.
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook de Envia.com inválido", ex);
        }
    }

    private void validarFirma(EmpresaEnvioConfigEntidad config, String evento, String timestamp,
                              String firma, String payload) {
        try {
            if (evento == null || timestamp == null) throw new IllegalArgumentException("Encabezados de firma incompletos");
            long instante = Long.parseLong(timestamp);
            if (instante < 10_000_000_000L) instante *= 1000;
            if (Math.abs(Instant.now().toEpochMilli() - instante) > VENTANA_FIRMA_MILISEGUNDOS) {
                throw new IllegalArgumentException("Firma expirada");
            }
            String token = credenciales.resolver(config);
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(token.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] esperado = hmac.doFinal((timestamp + "." + evento + "." + payload)
                    .getBytes(StandardCharsets.UTF_8));
            String valor = firma.startsWith("v1=") ? firma.substring(3) : firma;
            byte[] recibido = HexFormat.of().parseHex(valor);
            if (!MessageDigest.isEqual(esperado, recibido)) throw new IllegalArgumentException("Firma inválida");
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Firma de Envia.com inválida", ex);
        }
    }

    private String texto(JsonNode nodo, String campo) {
        JsonNode valor = nodo.path(campo);
        return valor.isMissingNode() || valor.isNull() ? null : valor.asText();
    }

    private String hash(String valor) {
        if (valor == null || valor.isBlank()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
