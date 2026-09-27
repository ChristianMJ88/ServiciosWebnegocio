package com.techprotech.agenda.compartido.seguridad;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class CifradorSecretosAesGcm {
    private static final String PREFIJO = "enc:v1:";
    private static final int LONGITUD_IV = 12;
    private static final int LONGITUD_TAG = 128;

    private CifradorSecretosAesGcm() {}

    public static String encriptar(String valor, String llaveMaestra, String nombreVariable) {
        if (valor == null || valor.isBlank()) return valor;
        validarLlave(llaveMaestra, nombreVariable);
        try {
            byte[] iv = new byte[LONGITUD_IV];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, llave(llaveMaestra), new GCMParameterSpec(LONGITUD_TAG, iv));
            byte[] cifrado = cipher.doFinal(valor.getBytes(StandardCharsets.UTF_8));
            byte[] combinado = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, combinado, 0, iv.length);
            System.arraycopy(cifrado, 0, combinado, iv.length, cifrado.length);
            return PREFIJO + Base64.getUrlEncoder().withoutPadding().encodeToString(combinado);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo cifrar el secreto de integración", ex);
        }
    }

    public static String desencriptar(String valor, String llaveMaestra, String nombreVariable) {
        if (valor == null || valor.isBlank()) return valor;
        if (!valor.startsWith(PREFIJO)) throw new IllegalArgumentException("El secreto de integración no está cifrado");
        validarLlave(llaveMaestra, nombreVariable);
        try {
            byte[] combinado = Base64.getUrlDecoder().decode(valor.substring(PREFIJO.length()));
            if (combinado.length <= LONGITUD_IV) throw new IllegalArgumentException("Secreto de integración inválido");
            byte[] iv = new byte[LONGITUD_IV];
            byte[] cifrado = new byte[combinado.length - LONGITUD_IV];
            System.arraycopy(combinado, 0, iv, 0, LONGITUD_IV);
            System.arraycopy(combinado, LONGITUD_IV, cifrado, 0, cifrado.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, llave(llaveMaestra), new GCMParameterSpec(LONGITUD_TAG, iv));
            return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo descifrar el secreto de integración", ex);
        }
    }

    private static void validarLlave(String llaveMaestra, String nombreVariable) {
        if (llaveMaestra == null || llaveMaestra.isBlank()) throw new IllegalStateException("Falta " + nombreVariable);
    }

    private static SecretKeySpec llave(String valor) throws Exception {
        return new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)), "AES");
    }
}
