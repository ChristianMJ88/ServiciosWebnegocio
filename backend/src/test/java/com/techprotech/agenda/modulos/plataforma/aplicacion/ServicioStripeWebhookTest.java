package com.techprotech.agenda.modulos.plataforma.aplicacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.plataforma.infraestructura.stripe.PropiedadesStripePlataforma;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.HexFormat;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class ServicioStripeWebhookTest {
    private final SuscripcionPlataformaRepositorio suscripciones=mock(SuscripcionPlataformaRepositorio.class);
    private final PlanPlataformaRepositorio planes=mock(PlanPlataformaRepositorio.class);
    private final StripeEventoPlataformaRepositorio eventos=mock(StripeEventoPlataformaRepositorio.class);
    private ServicioStripePlataforma servicio;
    private SuscripcionPlataformaEntidad local;

    @BeforeEach void preparar() {
        servicio=new ServicioStripePlataforma(new PropiedadesStripePlataforma(true,"PRUEBA","sk_test_fixture","whsec_fixture",null,null,null,null),
                planes,suscripciones,mock(StripeClientePlataformaRepositorio.class),eventos,mock(EmpresaRepositorio.class),
                mock(UsuarioRepositorio.class),mock(ServicioAuditoriaPlataforma.class),new ObjectMapper(),new ServicioPlanSuscripcionStripe(planes));
        local=new SuscripcionPlataformaEntidad();local.setPlanId(4L);local.setEstado("ACTIVA");
        when(suscripciones.findByStripeSuscripcionId("sub_fixture")).thenReturn(Optional.of(local));
        when(eventos.save(any())).thenAnswer(i->i.getArgument(0));
        PlanPlataformaEntidad plan=new PlanPlataformaEntidad();plan.setDiasGracia(2);
        when(planes.findById(4L)).thenReturn(Optional.of(plan));
    }

    @Test void fallaDeRenovacionIniciaGraciaYLosReintentosNoLaExtienden() throws Exception {
        enviar("evt_1","invoice.payment_failed",0);
        LocalDateTime hasta=local.getGraciaHasta();
        assertThat(hasta).isBetween(LocalDateTime.now(ZoneOffset.UTC).plusDays(2).minusMinutes(1),LocalDateTime.now(ZoneOffset.UTC).plusDays(2));
        enviar("evt_2","invoice.payment_failed",0);
        assertThat(local.getGraciaHasta()).isEqualTo(hasta);
        assertThat(local.getEstado()).isEqualTo("GRACIA");
    }

    @Test void pagoRecuperadoEliminaLaGracia() throws Exception {
        enviar("evt_1","invoice.payment_failed",0);
        enviar("evt_2","invoice.paid",19900);
        assertThat(local.getEstado()).isEqualTo("ACTIVA");
        assertThat(local.getGraciaHasta()).isNull();
    }

    @Test void facturaTardiaNoReactivaSuscripcionCancelada() throws Exception {
        local.setEstado("CANCELADA");
        enviar("evt_1","invoice.paid",19900);
        assertThat(local.getEstado()).isEqualTo("CANCELADA");
    }

    @Test void primerPagoFallidoNoConcedeGraciaNiAcceso() throws Exception {
        local.setEstado("PENDIENTE_PAGO");
        enviar("evt_1","invoice.payment_failed",0);
        assertThat(local.getEstado()).isEqualTo("PENDIENTE_PAGO");
        assertThat(local.getGraciaHasta()).isNull();
    }

    private void enviar(String id,String tipo,long pagado) throws Exception {
        String payload="{\"id\":\""+id+"\",\"object\":\"event\",\"type\":\""+tipo+"\",\"livemode\":false,\"data\":{\"object\":{\"id\":\"in_fixture\",\"subscription\":\"sub_fixture\",\"amount_paid\":"+pagado+"}}}";
        enviarPayload(payload);
    }

    @Test void notificacionAnteriorNoRevierteUnaRecuperacion() throws Exception {
        local.setStripeUltimoEventoCreado(200L);
        enviarPayload("{\"id\":\"evt_old\",\"object\":\"event\",\"created\":100,\"type\":\"invoice.payment_failed\",\"livemode\":false,\"data\":{\"object\":{\"id\":\"in_fixture\",\"subscription\":\"sub_fixture\"}}}");
        assertThat(local.getEstado()).isEqualTo("ACTIVA");
        assertThat(local.getGraciaHasta()).isNull();
        assertThat(local.getStripeUltimoEventoCreado()).isEqualTo(200L);
    }

    @Test void eventoDeSuscripcionNoReactivaUnaCancelada() throws Exception {
        local.setEstado("CANCELADA");
        enviarPayload("{\"id\":\"evt_old\",\"object\":\"event\",\"type\":\"customer.subscription.updated\",\"livemode\":false,\"data\":{\"object\":{\"id\":\"sub_fixture\",\"status\":\"active\"}}}");
        assertThat(local.getEstado()).isEqualTo("CANCELADA");
    }

    @Test void cambioDePlanDesdeStripeActualizaLaSuscripcionDelNegocio() throws Exception {
        PlanPlataformaEntidad nuevo=new PlanPlataformaEntidad();nuevo.setId(9L);nuevo.setMoneda("MXN");
        when(planes.findByStripePrecioMensualIdOrStripePrecioAnualId("price_nuevo","price_nuevo")).thenReturn(Optional.of(nuevo));
        enviarPayload("{\"id\":\"evt_plan\",\"object\":\"event\",\"created\":200,\"type\":\"customer.subscription.updated\",\"livemode\":false,\"data\":{\"object\":{\"id\":\"sub_fixture\",\"status\":\"active\",\"items\":{\"data\":[{\"quantity\":1,\"price\":{\"id\":\"price_nuevo\",\"currency\":\"mxn\",\"unit_amount\":39900,\"recurring\":{\"interval\":\"month\",\"interval_count\":1}}}]}}}}");
        assertThat(local.getPlanId()).isEqualTo(9L);
        assertThat(local.getPrecioAcordado()).isEqualByComparingTo("399.00");
        assertThat(local.getCiclo()).isEqualTo("MENSUAL");
        verify(suscripciones).save(local);
    }

    private void enviarPayload(String payload) throws Exception {
        long instante=Instant.now().getEpochSecond();
        Mac mac=Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("whsec_fixture".getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
        String firma=HexFormat.of().formatHex(mac.doFinal((instante+"."+payload).getBytes(StandardCharsets.UTF_8)));
        servicio.recibirWebhook("t="+instante+",v1="+firma,payload);
    }
}
