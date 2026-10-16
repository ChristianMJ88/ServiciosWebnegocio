package com.techprotech.agenda.modulos.plataforma.aplicacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.compartido.correo.*;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.PlanPlataformaRepositorio;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ServicioAvisosSuscripcionTest {
    private final BandejaSalidaNotificacionRepositorio outbox=mock(BandejaSalidaNotificacionRepositorio.class);
    private final EmpresaRepositorio empresas=mock(EmpresaRepositorio.class);
    private final PlanPlataformaRepositorio planes=mock(PlanPlataformaRepositorio.class);
    private final ObjectMapper mapper=new ObjectMapper();
    private final ServicioAvisosSuscripcion servicio=new ServicioAvisosSuscripcion(outbox,empresas,planes,mapper, java.time.Clock.systemUTC());
    private SuscripcionPlataformaEntidad s;
    @BeforeEach void preparar(){
        s=new SuscripcionPlataformaEntidad();s.setId(8L);s.setEmpresaId(3L);s.setPlanId(4L);s.setCorreoFacturacion("billing@example.test");s.setMoneda("MXN");
        var e=new EmpresaEntidad();e.setNombre("Negocio de prueba");e.setZonaHoraria("America/Mexico_City");
        var p=new PlanPlataformaEntidad();p.setNombre("Plan configurado");
        when(empresas.findById(3L)).thenReturn(Optional.of(e));when(planes.findById(4L)).thenReturn(Optional.of(p));
    }
    @Test void pagoSeProgramaConTenantDestinatarioEImporteConfirmado() throws Exception {
        servicio.programar(s,"PAGO","in_fixture",new BigDecimal("499.00"));
        var c=ArgumentCaptor.forClass(BandejaSalidaNotificacionEntidad.class);verify(outbox).save(c.capture());
        var salida=c.getValue();assertThat(salida.getEmpresaId()).isEqualTo(3L);assertThat(salida.getEstado()).isEqualTo("PENDIENTE");
        var p=mapper.readValue(salida.getPayloadJson(),SuscripcionCorreoPayload.class);
        assertThat(p.destinatario()).isEqualTo("billing@example.test");assertThat(p.importe()).isEqualByComparingTo("499.00");
        assertThat(p.plan()).isEqualTo("Plan configurado");assertThat(p.referencia()).isEqualTo("in_fixture");
    }
    @Test void facturaRepetidaNoGeneraOtroCorreo(){
        when(outbox.existsByClaveDeduplicacion("suscripcion:8:PAGO:in_fixture")).thenReturn(true);
        servicio.programar(s,"PAGO","in_fixture",BigDecimal.TEN);verify(outbox,never()).save(any());
    }
    @Test void noInventaUnDestinatarioCuandoNoHayCorreo(){
        s.setCorreoFacturacion(null);servicio.programar(s,"PRUEBA","inicio",null);verifyNoInteractions(outbox);
    }
    @Test void muestraVencimientoEnLaZonaHorariaDelNegocio() throws Exception {
        s.setEstado("PRUEBA");s.setPruebaHasta(LocalDateTime.of(2026,10,8,2,0));
        servicio.programar(s,"VENCIMIENTO","fin",null);
        var c=ArgumentCaptor.forClass(BandejaSalidaNotificacionEntidad.class);verify(outbox).save(c.capture());
        var p=mapper.readValue(c.getValue().getPayloadJson(),SuscripcionCorreoPayload.class);
        assertThat(p.fecha()).contains("07 oct 2026 20:00");assertThat(p.importe()).isNull();
    }
}
