package com.techprotech.agenda.modulos.autenticacion.aplicacion;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServicioProteccionAccesoTest {
    @Test
    void limpiaBloqueoTrasAccesoValido() {
        ServicioProteccionAcceso servicio = new ServicioProteccionAcceso(null);
        UsuarioEntidad usuario = new UsuarioEntidad();
        usuario.setIntentosLoginFallidos(3);
        usuario.setLoginBloqueadoHasta(java.time.LocalDateTime.now().plusMinutes(10));
        servicio.limpiar(usuario);
        assertEquals(0, usuario.getIntentosLoginFallidos());
        assertNull(usuario.getLoginBloqueadoHasta());
    }
}
