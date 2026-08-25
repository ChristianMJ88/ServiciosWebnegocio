package com.techprotech.agenda.modulos.disponibilidad.aplicacion;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogosDisponibilidadTest {

    @Test
    void publicaYReconoceLosValoresPermitidos() {
        assertThat(CatalogosDisponibilidad.TIPOS_SUJETO)
                .extracting(CatalogosDisponibilidad.Opcion::valor)
                .containsExactly("SUCURSAL", "PRESTADOR");
        assertThat(CatalogosDisponibilidad.TIPOS_BLOQUEO)
                .extracting(CatalogosDisponibilidad.Opcion::valor)
                .contains("BLOQUEO", "HORARIO_ESPECIAL");
        assertThat(CatalogosDisponibilidad.contiene(CatalogosDisponibilidad.TIPOS_BLOQUEO, "DESCONOCIDO"))
                .isFalse();
    }
}
