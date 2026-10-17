package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.compartido.correo.ServicioOutboxCorreoCitas;
import com.techprotech.agenda.compartido.whatsapp.ServicioOutboxWhatsappCitas;
import com.techprotech.agenda.modulos.citas.dominio.eventos.*;
import com.techprotech.agenda.modulos.clientes.infraestructura.entidad.ClienteEntidad;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacionesEventosCitaTest {
    @Mock ServicioOutboxCorreoCitas correo;
    @Mock ServicioOutboxWhatsappCitas whatsapp;
    @Mock ClienteRepositorio clientes;
    @InjectMocks NotificacionesEventosCita notificaciones;

    @ParameterizedTest @CsvSource({"true,2220000000,true", "false,2220000000,false", "true,'',false", "true,,false"})
    void noProgramaWhatsappSinConsentimientoOTelefonoDelClienteDelTenant(boolean acepta,String telefono,boolean esperado) {
        var cliente=new ClienteEntidad(); cliente.setAceptaWhatsapp(acepta); cliente.setTelefono(telefono);
        when(clientes.findByClienteIdAndEmpresaId(20L,7L)).thenReturn(Optional.of(cliente));
        var datos=datos(OrigenEventoCita.ADMIN);
        notificaciones.finalizada(new CitaFinalizada(datos));
        verify(correo).programarGraciasVisita(7L,1L,datos.inicio());
        if(esperado) verify(whatsapp).programarGraciasVisita(7L,1L,telefono,datos.inicio());
        else verifyNoInteractions(whatsapp);
        verify(clientes,never()).findById(any());
    }

    @Test void cancelacionDelClienteConservaSoloElEventoDeCorreoDelCliente() {
        var datos=datos(OrigenEventoCita.CLIENTE);
        notificaciones.cancelada(new CitaCancelada(datos));
        verify(correo).programarCanceladaCliente(7L,1L,datos.inicio());
        verifyNoMoreInteractions(correo); verifyNoInteractions(whatsapp,clientes);
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void registroDevuelveElAcuseRealSinInventarHabilitacionNiRomperLaAgrupacion(boolean programado) {
        var datos=datos(OrigenEventoCita.CLIENTE);
        var registro=new NotificacionesRegistroCita(correo,whatsapp);
        when(correo.programarRegistrada(7L,1L,datos.inicio())).thenReturn(programado);
        assertThat(registro.registrada(7L,1L,datos.inicio())).isEqualTo(programado);
        registro.solicitarConfirmacion(7L,1L,"2220000000",datos.inicio());
        registro.solicitarConfirmacion(7L,1L,"2220000000",datos.inicio(),1);
        registro.solicitarConfirmacion(7L,1L,"2220000000",datos.inicio(),3);
        verify(whatsapp).programarConfirmacion(7L,1L,"2220000000",datos.inicio());
        verify(whatsapp).programarConfirmacion(7L,1L,"2220000000",datos.inicio(),1);
        verify(whatsapp).programarConfirmacion(7L,1L,"2220000000",datos.inicio(),3);
        verifyNoMoreInteractions(whatsapp);
    }

    static DatosEventoCita datos(OrigenEventoCita origen) {
        return new DatosEventoCita(7L,3L,1L,20L,origen,11L,Instant.parse("2026-10-16T18:00:00Z"),
                LocalDateTime.parse("2026-10-16T10:00"),LocalDateTime.parse("2026-10-16T11:00"));
    }
}
