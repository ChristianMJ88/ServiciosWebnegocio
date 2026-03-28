package com.techprotech.agenda.compartido.correo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class ProtectorSecretosCorreo {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtectorSecretosCorreo.class);
    private static final String PREFIJO = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final PropiedadesCorreo propiedadesCorreo;
    private final SecureRandom secureRandom = new SecureRandom();

    public ProtectorSecretosCorreo(PropiedadesCorreo propiedadesCorreo) {
        this.propiedadesCorreo = propiedadesCorreo;
    }

    public String desencriptarSiNecesario(String valor) {
        return desencriptarSiNecesario(valor, "smtp_password");
    }

    public String desencriptarSiNecesario(String valor, String etiqueta) {
        if (valor == null || valor.isBlank()) {
            return valor;
        }

        if (!valor.startsWith(PREFIJO)) {
            LOGGER.warn("Se detecto {} en texto plano. Se recomienda migrarlo a formato cifrado.", etiqueta);
            return valor;
        }

        String llave = propiedadesCorreo.llaveCifradoSecretos();
        if (llave == null || llave.isBlank()) {
            throw new IllegalStateException("Falta aplicacion.correo.llave-cifrado-secretos para desencriptar " + etiqueta);
        }

        try {
            byte[] combinado = Base64.getDecoder().decode(valor.substring(PREFIJO.length()));
            byte[] iv = new byte[IV_LENGTH];
            byte[] cifrado = new byte[combinado.length - IV_LENGTH];
            System.arraycopy(combinado, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combinado, IV_LENGTH, cifrado, 0, cifrado.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(derivarLlave(llave), "AES"), new GCMParameterSpec(TAG_LENGTH, iv));
            return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo desencriptar " + etiqueta, ex);
        }
    }

    public String encriptar(String valorPlano) {
        if (valorPlano == null || valorPlano.isBlank()) {
            return valorPlano;
        }

        String llave = propiedadesCorreo.llaveCifradoSecretos();
        if (llave == null || llave.isBlank()) {
            throw new IllegalStateException("Falta aplicacion.correo.llave-cifrado-secretos para cifrar smtp_password");
        }

        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(derivarLlave(llave), "AES"), new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] cifrado = cipher.doFinal(valorPlano.getBytes(StandardCharsets.UTF_8));

            byte[] combinado = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, combinado, 0, iv.length);
            System.arraycopy(cifrado, 0, combinado, iv.length, cifrado.length);
            return PREFIJO + Base64.getEncoder().encodeToString(combinado);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo cifrar smtp_password", ex);
        }
    }

    private byte[] derivarLlave(String llave) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(llave.getBytes(StandardCharsets.UTF_8));
    }
}
