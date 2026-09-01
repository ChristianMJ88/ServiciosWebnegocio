package com.techprotech.agenda.modulos.autenticacion.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.compartido.correo.ProtectorSecretosCorreo;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class ServicioTokenRegistroSocial {
    private final ProtectorSecretosCorreo protector;
    private final ObjectMapper objectMapper;

    public ServicioTokenRegistroSocial(ProtectorSecretosCorreo protector, ObjectMapper objectMapper) {
        this.protector = protector;
        this.objectMapper = objectMapper;
    }

    public String crearEstado(String proveedor) {
        return crearEstado(proveedor, "REGISTRO");
    }

    public String crearEstado(String proveedor, String destino) {
        return cifrar(Map.of(
                "proveedor", proveedor,
                "destino", destino,
                "exp", Instant.now().plusSeconds(600).getEpochSecond(),
                "nonce", UUID.randomUUID().toString()
        ));
    }

    public String validarEstado(String state, String proveedor) {
        Map<String, Object> datos = descifrar(state, "oauth_state_social");
        validarExpiracion(datos);
        if (!proveedor.equals(String.valueOf(datos.get("proveedor")))) {
            throw new IllegalArgumentException("El proveedor OAuth no coincide");
        }
        return requerido(datos, "destino");
    }

    public String crearToken(PerfilRegistroSocial perfil) {
        return cifrar(Map.of(
                "proveedor", perfil.proveedor(),
                "subject", perfil.subject(),
                "correo", perfil.correo(),
                "nombre", perfil.nombre(),
                "exp", Instant.now().plusSeconds(600).getEpochSecond()
        ));
    }

    public PerfilRegistroSocial validarToken(String token) {
        Map<String, Object> datos = descifrar(token, "registro_social");
        validarExpiracion(datos);
        return new PerfilRegistroSocial(
                requerido(datos, "proveedor"),
                requerido(datos, "subject"),
                requerido(datos, "correo"),
                requerido(datos, "nombre")
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> descifrar(String valor, String contexto) {
        try {
            if (valor == null || valor.isBlank()) throw new IllegalArgumentException("Falta el valor OAuth");
            String protegido = new String(Base64.getUrlDecoder().decode(valor), StandardCharsets.UTF_8);
            return objectMapper.readValue(protector.desencriptarSiNecesario(protegido, contexto), Map.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("El token social no es válido o expiró", ex);
        }
    }

    private String cifrar(Map<String, Object> datos) {
        try {
            String protegido = protector.encriptar(objectMapper.writeValueAsString(datos));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(protegido.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo proteger el flujo social", ex);
        }
    }

    private void validarExpiracion(Map<String, Object> datos) {
        Object exp = datos.get("exp");
        if (!(exp instanceof Number numero) || numero.longValue() < Instant.now().getEpochSecond()) {
            throw new IllegalArgumentException("El flujo social expiró");
        }
    }

    private String requerido(Map<String, Object> datos, String clave) {
        Object valor = datos.get(clave);
        if (valor == null || String.valueOf(valor).isBlank()) throw new IllegalStateException("Falta " + clave);
        return String.valueOf(valor);
    }
}
