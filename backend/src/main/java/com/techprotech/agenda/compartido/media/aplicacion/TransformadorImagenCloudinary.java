package com.techprotech.agenda.compartido.media.aplicacion;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;

@Component
public class TransformadorImagenCloudinary {
    private static final Map<String, String> GRAVEDADES = Map.of(
            "ROSTRO", "face",
            "AUTOMATICO", "auto",
            "CENTRO", "center",
            "SUPERIOR", "north"
    );

    public String retrato(String url, String encuadre, BigDecimal zoom, int tamano) {
        if (url == null || url.isBlank() || !url.contains("res.cloudinary.com") || !url.contains("/upload/")) {
            return url;
        }
        String gravedad = GRAVEDADES.getOrDefault(normalizarEncuadre(encuadre), "face");
        String modoRecorte = "face".equals(gravedad) ? "thumb" : "fill";
        String zoomSeguro = normalizarZoom(zoom).setScale(2, RoundingMode.HALF_UP).toPlainString();
        String transformacion = "c_" + modoRecorte + ",g_" + gravedad + ",w_" + tamano + ",h_" + tamano
                + ",z_" + zoomSeguro + ",q_auto,f_auto";
        return url.replace("/upload/", "/upload/" + transformacion + "/");
    }

    public String normalizarEncuadre(String encuadre) {
        String valor = encuadre == null ? "ROSTRO" : encuadre.trim().toUpperCase(Locale.ROOT);
        return GRAVEDADES.containsKey(valor) ? valor : "ROSTRO";
    }

    public BigDecimal normalizarZoom(BigDecimal zoom) {
        if (zoom == null) return BigDecimal.ONE;
        if (zoom.compareTo(BigDecimal.ONE) < 0) return BigDecimal.ONE;
        if (zoom.compareTo(new BigDecimal("2.00")) > 0) return new BigDecimal("2.00");
        return zoom;
    }
}
