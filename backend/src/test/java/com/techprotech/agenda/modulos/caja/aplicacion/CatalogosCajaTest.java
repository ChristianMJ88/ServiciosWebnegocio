package com.techprotech.agenda.modulos.caja.aplicacion;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogosCajaTest {

    @Test
    void publicaYValidaLosCatalogosOperativos() {
        assertThat(CatalogosCaja.METODOS_PAGO)
                .extracting(CatalogosCaja.Opcion::codigo)
                .containsExactly("EFECTIVO", "TARJETA", "TRANSFERENCIA");
        assertThat(CatalogosCaja.TIPOS_MOVIMIENTO_MANUAL)
                .extracting(CatalogosCaja.Opcion::codigo)
                .contains("GASTO_MENOR", "RETIRO", "INGRESO_EXTRA");
        assertThat(CatalogosCaja.contiene(CatalogosCaja.METODOS_PAGO, "CRIPTOMONEDA")).isFalse();
    }
}
