package com.techprotech.agenda.compartido.media.aplicacion;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransformadorImagenCloudinaryTest {
    private final TransformadorImagenCloudinary transformador = new TransformadorImagenCloudinary();

    @Test
    void generaRetratoConEncuadreYZoomValidados() {
        String original = "https://res.cloudinary.com/demo/image/upload/v1/Fluora/persona.jpg";

        assertEquals(
                "https://res.cloudinary.com/demo/image/upload/c_fill,g_north,w_480,h_480,z_1.35,q_auto,f_auto/v1/Fluora/persona.jpg",
                transformador.retrato(original, "SUPERIOR", new BigDecimal("1.35"), 480)
        );
    }

    @Test
    void usaMiniaturaFacialParaAcercarElRostro() {
        String original = "https://res.cloudinary.com/demo/image/upload/v1/Fluora/persona.jpg";

        assertEquals(
                "https://res.cloudinary.com/demo/image/upload/c_thumb,g_face,w_360,h_360,z_1.00,q_auto,f_auto/v1/Fluora/persona.jpg",
                transformador.retrato(original, "ROSTRO", BigDecimal.ONE, 360)
        );
    }

    @Test
    void conservaUrlsExternasYNormalizaValoresFueraDeRango() {
        assertEquals("https://cdn.example/persona.jpg",
                transformador.retrato("https://cdn.example/persona.jpg", "CENTRO", BigDecimal.ONE, 320));
        assertEquals(BigDecimal.ONE, transformador.normalizarZoom(new BigDecimal("0.50")));
        assertEquals(new BigDecimal("2.00"), transformador.normalizarZoom(new BigDecimal("3.00")));
        assertEquals("ROSTRO", transformador.normalizarEncuadre("desconocido"));
    }

    @Test
    void generaVariantesResponsivasParaGaleria() {
        String original = "https://res.cloudinary.com/demo/image/upload/v1/Fluora/galeria/trabajo.jpg";

        assertEquals(
                "https://res.cloudinary.com/demo/image/upload/c_limit,w_960,q_auto,f_auto/v1/Fluora/galeria/trabajo.jpg",
                transformador.galeria(original, 960)
        );
        assertEquals("https://cdn.example/trabajo.jpg", transformador.galeria("https://cdn.example/trabajo.jpg", 960));
    }
}
