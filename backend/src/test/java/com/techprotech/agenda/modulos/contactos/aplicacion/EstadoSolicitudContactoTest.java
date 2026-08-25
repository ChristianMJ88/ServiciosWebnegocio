package com.techprotech.agenda.modulos.contactos.aplicacion;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoSolicitudContactoTest {

    @Test
    void concentraLosEstadosAceptadosPorElBackend() {
        assertThat(EstadoSolicitudContacto.values())
                .extracting(Enum::name)
                .containsExactly("NUEVO", "EN_PROCESO", "ATENDIDO", "CERRADO");
        assertThat(EstadoSolicitudContacto.esValido("EN_PROCESO")).isTrue();
        assertThat(EstadoSolicitudContacto.esValido("DESCONOCIDO")).isFalse();
    }
}
