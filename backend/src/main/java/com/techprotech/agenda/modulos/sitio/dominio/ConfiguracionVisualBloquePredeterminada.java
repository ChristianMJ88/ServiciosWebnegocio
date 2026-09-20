package com.techprotech.agenda.modulos.sitio.dominio;

public record ConfiguracionVisualBloquePredeterminada(
        SuperficieBloqueSitio superficie,
        PresentacionBloqueSitio presentacion,
        AnchoBloqueSitio ancho,
        AlineacionBloqueSitio alineacion,
        SeparadorBloqueSitio separador,
        EspaciadoBloqueSitio espaciado,
        String etiqueta,
        String titulo,
        String descripcion
) {
    public static ConfiguracionVisualBloquePredeterminada para(TipoBloqueSitio tipo) {
        return switch (tipo) {
            case BENEFICIOS -> new ConfiguracionVisualBloquePredeterminada(
                    SuperficieBloqueSitio.MARCA, PresentacionBloqueSitio.ABIERTA, AnchoBloqueSitio.AMPLIO,
                    AlineacionBloqueSitio.CENTRO, SeparadorBloqueSitio.DEGRADADO, EspaciadoBloqueSitio.NORMAL,
                    "Por qué elegirnos", "Una experiencia pensada para ti", null
            );
            case EQUIPO -> new ConfiguracionVisualBloquePredeterminada(
                    SuperficieBloqueSitio.BASE, PresentacionBloqueSitio.PANEL, AnchoBloqueSitio.AMPLIO,
                    AlineacionBloqueSitio.CENTRO, SeparadorBloqueSitio.LINEA, EspaciadoBloqueSitio.NORMAL,
                    "Nuestro equipo", "Conoce a quienes te atienden",
                    "Profesionales del negocio disponibles para acompañarte en tu próxima cita."
            );
            case SERVICIOS_DESTACADOS -> new ConfiguracionVisualBloquePredeterminada(
                    SuperficieBloqueSitio.SUAVE, PresentacionBloqueSitio.PANEL, AnchoBloqueSitio.AMPLIO,
                    AlineacionBloqueSitio.IZQUIERDA, SeparadorBloqueSitio.DEGRADADO, EspaciadoBloqueSitio.NORMAL,
                    null, null, null);
            case GALERIA -> new ConfiguracionVisualBloquePredeterminada(
                    SuperficieBloqueSitio.BASE, PresentacionBloqueSitio.PANEL, AnchoBloqueSitio.AMPLIO,
                    AlineacionBloqueSitio.IZQUIERDA, SeparadorBloqueSitio.LINEA, EspaciadoBloqueSitio.NORMAL,
                    "Galería", null, null);
            case PROMOCIONES -> new ConfiguracionVisualBloquePredeterminada(
                    SuperficieBloqueSitio.SUAVE, PresentacionBloqueSitio.BANDA, AnchoBloqueSitio.AMPLIO,
                    AlineacionBloqueSitio.IZQUIERDA, SeparadorBloqueSitio.DEGRADADO, EspaciadoBloqueSitio.NORMAL,
                    "Promociones", "Una oportunidad especial para reservar",
                    "Ofertas vigentes seleccionadas para nuestros clientes.");
            case CONTACTO -> new ConfiguracionVisualBloquePredeterminada(
                    SuperficieBloqueSitio.SUAVE, PresentacionBloqueSitio.PANEL, AnchoBloqueSitio.NORMAL,
                    AlineacionBloqueSitio.IZQUIERDA, SeparadorBloqueSitio.LINEA, EspaciadoBloqueSitio.NORMAL,
                    null, null, null);
            default -> new ConfiguracionVisualBloquePredeterminada(
                    SuperficieBloqueSitio.BASE, PresentacionBloqueSitio.ABIERTA, AnchoBloqueSitio.NORMAL,
                    AlineacionBloqueSitio.IZQUIERDA, SeparadorBloqueSitio.NINGUNO, EspaciadoBloqueSitio.NORMAL,
                    null, null, null);
        };
    }
}
