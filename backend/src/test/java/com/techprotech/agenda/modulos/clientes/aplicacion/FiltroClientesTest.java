package com.techprotech.agenda.modulos.clientes.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class FiltroClientesTest {
    @Test void validaPeriodoYOrdenEnBackend() {
        assertThrows(ResponseStatusException.class, () -> new FiltroClientes(null,null,null,
                LocalDate.of(2026,10,10),LocalDate.of(2026,10,1),null,null));
        assertThrows(ResponseStatusException.class, () -> FiltroClientes.opcion(FiltroClientes.Orden.class,"SQL_INVENTADO"));
    }
    @Test void conservaFiltrosIndependientesYDefaults() {
        var filtro = new FiltroClientes(" Ana "," ANa "," 222 ",null,null,null,null);
        assertEquals("ana",filtro.nombre()); assertEquals("222",filtro.telefono());
        assertEquals(FiltroClientes.Orden.NOMBRE_ASC,filtro.orden());
        assertEquals(FiltroClientes.Citas.TODOS,filtro.citas());
    }
}
