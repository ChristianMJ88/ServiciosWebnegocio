package com.techprotech.agenda.modulos.plataforma.aplicacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.plataforma.infraestructura.stripe.*;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.Optional;
import java.time.LocalDateTime;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class ServicioStripeConciliacionTest {
    final ObjectMapper mapper=new ObjectMapper();
    final SuscripcionPlataformaRepositorio repositorio=mock(SuscripcionPlataformaRepositorio.class);
    final ServicioAvisosSuscripcion avisos=mock(ServicioAvisosSuscripcion.class);
    SuscripcionPlataformaEntidad local;
    ServicioStripePlataforma servicio;
    ObjectNode sub;
    ObjectNode invoice;
    @BeforeEach void preparar() {
        local=new SuscripcionPlataformaEntidad();local.setId(1L);local.setEmpresaId(2L);local.setPlanId(3L);local.setEstado("ACTIVA");local.setStripeSuscripcionId("sub_test");local.setStripeClienteId("cus_test");
        when(repositorio.findById(1L)).thenReturn(Optional.of(local));when(repositorio.findByStripeSuscripcionId("sub_test")).thenReturn(Optional.of(local));
        servicio=new ServicioStripePlataforma(new PropiedadesStripePlataforma(true,"PRUEBA","rk_test_fixture",null,null,null,null,null),mock(PlanPlataformaRepositorio.class),repositorio,mock(StripeClientePlataformaRepositorio.class),mock(StripeEventoPlataformaRepositorio.class),mock(EmpresaRepositorio.class),mock(UsuarioRepositorio.class),mock(ServicioAuditoriaPlataforma.class),mapper,mock(ServicioPlanSuscripcionStripe.class),avisos);
        sub=mapper.createObjectNode().put("id","sub_test").put("customer","cus_test").put("livemode",false).put("status","active").put("latest_invoice","in_test").put("current_period_end",System.currentTimeMillis()/1000+86400);
        invoice=mapper.createObjectNode().put("id","in_test").put("customer","cus_test").put("livemode",false).put("status","paid").put("subscription","sub_test").put("amount_paid",19900);
    }
    @Test void recuperaRenovacionPagadaSinWebhook() {
        local.setEstado("GRACIA");local.setGraciaHasta(LocalDateTime.now().plusDays(1));
        servicio.conciliarSuscripcion(1L,new ConsultaSuscripcionStripe.Resultado(sub,invoice));
        assertThat(local.getEstado()).isEqualTo("ACTIVA");assertThat(local.getGraciaHasta()).isNull();assertThat(local.getPeriodoActualHasta()).isNotNull();
        verify(avisos).programar(eq(local),eq("PAGO"),eq("in_test"),eq(new java.math.BigDecimal("199.00")));
    }
    @Test void noAmpliaAccesoPorUnaFacturaSinPagar() {
        invoice.put("status","open");servicio.conciliarSuscripcion(1L,new ConsultaSuscripcionStripe.Resultado(sub,invoice));
        verify(repositorio,never()).save(any());verifyNoInteractions(avisos);
    }
    @Test void noAmpliaAccesoSiNoPuedeConsultarLaFactura() {
        servicio.conciliarSuscripcion(1L,new ConsultaSuscripcionStripe.Resultado(sub,null));verify(repositorio,never()).save(any());
    }
    @Test void rechazaConsultaDeOtroCliente() {
        sub.put("customer","cus_other");assertThatThrownBy(()->servicio.conciliarSuscripcion(1L,new ConsultaSuscripcionStripe.Resultado(sub,invoice))).isInstanceOf(IllegalArgumentException.class);
        verify(repositorio,never()).save(any());
    }
    @Test void rechazaDatosDeOtroAmbiente() {
        sub.put("livemode",true);assertThatThrownBy(()->servicio.conciliarSuscripcion(1L,new ConsultaSuscripcionStripe.Resultado(sub,invoice))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void reintentoNoExtiendeGraciaExistente() {
        local.setEstado("GRACIA");var fecha=LocalDateTime.now().minusDays(1);local.setGraciaHasta(fecha);sub.put("status","past_due");
        servicio.conciliarSuscripcion(1L,new ConsultaSuscripcionStripe.Resultado(sub,null));assertThat(local.getGraciaHasta()).isEqualTo(fecha);assertThat(local.getEstado()).isEqualTo("GRACIA");
    }
    @Test void cuentaCanceladaPuedeContratarNuevamente() {
        local.setEstado("CANCELADA");when(repositorio.findFirstByEmpresaIdOrderByCreadaEnDesc(2L)).thenReturn(Optional.of(local));
        when(repositorio.findByEmpresaIdAndEstadoIn(eq(2L),anyList())).thenReturn(java.util.List.of());
        assertThat(servicio.estadoSuscripcion(2L).puedeContratar()).isTrue();
    }
    @Test void deudaStripeDebeRegularizarseSinCrearOtraSuscripcion() {
        local.setEstado("GRACIA");when(repositorio.findFirstByEmpresaIdOrderByCreadaEnDesc(2L)).thenReturn(Optional.of(local));
        when(repositorio.findByEmpresaIdAndEstadoIn(eq(2L),anyList())).thenReturn(java.util.List.of(local));
        assertThat(servicio.estadoSuscripcion(2L).puedeContratar()).isFalse();
    }
    @Test void noOfreceUnaSegundaPruebaSiElNegocioYaUtilizoLaPrimera() {
        when(repositorio.findFirstByEmpresaIdOrderByCreadaEnDesc(2L)).thenReturn(Optional.of(local));
        when(repositorio.existsByEmpresaIdAndPruebaHastaIsNotNull(2L)).thenReturn(true);
        assertThat(servicio.estadoSuscripcion(2L).pruebaDisponible()).isFalse();
    }

}
