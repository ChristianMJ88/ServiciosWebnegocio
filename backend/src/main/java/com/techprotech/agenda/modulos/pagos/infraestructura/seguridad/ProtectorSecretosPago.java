package com.techprotech.agenda.modulos.pagos.infraestructura.seguridad;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class ProtectorSecretosPago {
    private static final String PREFIJO = "enc:v1:";
    private static final int LONGITUD_IV = 12;
    private static final int LONGITUD_TAG = 128;

    private final PropiedadesSeguridadPagos propiedades;
    private final SecureRandom secureRandom = new SecureRandom();

    public ProtectorSecretosPago(PropiedadesSeguridadPagos propiedades) {
        this.propiedades = propiedades;
    }

    public String encriptar(String valor) {
        if (valor == null || valor.isBlank()) return valor;
        validarLlave();
        try {
            byte[] iv = new byte[LONGITUD_IV];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(llave(), "AES"), new GCMParameterSpec(LONGITUD_TAG, iv));
            byte[] cifrado = cipher.doFinal(valor.getBytes(StandardCharsets.UTF_8));
            byte[] combinado = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, combinado, 0, iv.length);
            System.arraycopy(cifrado, 0, combinado, iv.length, cifrado.length);
            return PREFIJO + Base64.getUrlEncoder().withoutPadding().encodeToString(combinado);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo cifrar el secreto de pagos", ex);
        }
    }

    public String desencriptar(String valor) {
        if (valor == null || valor.isBlank()) return valor;
        if (!valor.startsWith(PREFIJO)) throw new IllegalArgumentException("El secreto de pagos no está cifrado");
        validarLlave();
        try {
            byte[] combinado = Base64.getUrlDecoder().decode(valor.substring(PREFIJO.length()));
            if (combinado.length <= LONGITUD_IV) throw new IllegalArgumentException("Secreto de pagos inválido");
            byte[] iv = new byte[LONGITUD_IV];
            byte[] cifrado = new byte[combinado.length - LONGITUD_IV];
            System.arraycopy(combinado, 0, iv, 0, LONGITUD_IV);
            System.arraycopy(combinado, LONGITUD_IV, cifrado, 0, cifrado.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(llave(), "AES"), new GCMParameterSpec(LONGITUD_TAG, iv));
            return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo descifrar el secreto de pagos", ex);
        }
    }

    private void validarLlave() {
        if (propiedades.llaveCifradoSecretos() == null || propiedades.llaveCifradoSecretos().isBlank()) {
            throw new IllegalStateException("Falta PAGOS_LLAVE_CIFRADO_SECRETOS");
        }
    }

    private byte[] llave() throws Exception {
        return MessageDigest.getInstance("SHA-256")
                .digest(propiedades.llaveCifradoSecretos().getBytes(StandardCharsets.UTF_8));
    }
}
