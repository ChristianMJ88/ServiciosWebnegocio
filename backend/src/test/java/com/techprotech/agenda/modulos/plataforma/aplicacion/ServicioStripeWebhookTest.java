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
    private final ServicioAvisosSuscripcion avisos=mock(ServicioAvisosSuscripcion.class);
    private SuscripcionPlataformaEntidad local;

    @BeforeEach void preparar() {
        servicio=new ServicioStripePlataforma(new PropiedadesStripePlataforma(true,"PRUEBA","sk_test_fixture","whsec_fixture",null,null,null,null),
                planes,suscripciones,mock(StripeClientePlataformaRepositorio.class),eventos,mock(EmpresaRepositorio.class),
                mock(UsuarioRepositorio.class),mock(ServicioAuditoriaPlataforma.class),new ObjectMapper(),new ServicioPlanSuscripcionStripe(planes),avisos);
        local=new SuscripcionPlataformaEntidad();local.setPlanId(4L);local.setEstado("ACTIVA");
        when(suscripciones.findByStripeSuscripcionId("sub_fixture")).thenReturn(Optional.of(local));
        when(eventos.save(any())).thenAnswer(i->i.getArgument(0));
        PlanPlataformaEntidad plan=new PlanPlataformaEntidad();plan.setDiasGracia(2);
        when(planes.findById(4L)).thenReturn(Optional.of(plan));
    }

    @Test void facturaSinCobroNoEnviaAvisoDePagoAunqueLaSuscripcionSigaActiva() throws Exception {
        enviar("evt_zero","invoice.paid",0);
        assertThat(local.getEstado()).isEqualTo("ACTIVA");
        verifyNoInteractions(avisos);
    }

    @Test void primerPagoAnteriorALaVinculacionActivaYNotificaElTenantCorrecto() throws Exception {
        prepararFacturaInicial();
        enviarFacturaInicial("1","cus_fixture");
        assertThat(local.getEstado()).isEqualTo("ACTIVA");
        assertThat(local.getStripeSuscripcionId()).isEqualTo("sub_fixture");
        verify(avisos).programar(local,"PAGO","in_first",java.math.BigDecimal.valueOf(19900,2));
    }

    @Test void metadataDeOtroTenantNoVinculaNiActivaLaSuscripcion() throws Exception {
        prepararFacturaInicial();
        enviarFacturaInicial("2","cus_fixture");
        assertThat(local.getEstado()).isEqualTo("PENDIENTE_PAGO");
        assertThat(local.getStripeSuscripcionId()).isNull();
        verifyNoInteractions(avisos);verify(suscripciones,never()).save(any());
    }

    @Test void clienteStripeDiferenteNoVinculaNiActivaLaSuscripcion() throws Exception {
        prepararFacturaInicial();
        enviarFacturaInicial("1","cus_other");
        assertThat(local.getEstado()).isEqualTo("PENDIENTE_PAGO");
        assertThat(local.getStripeSuscripcionId()).isNull();
        verifyNoInteractions(avisos);verify(suscripciones,never()).save(any());
    }

    private void prepararFacturaInicial() {
        local.setId(8L);local.setEmpresaId(1L);local.setEstado("PENDIENTE_PAGO");local.setStripeClienteId("cus_fixture");
        when(suscripciones.findByStripeSuscripcionId("sub_fixture")).thenReturn(Optional.empty());
        when(suscripciones.findById(8L)).thenReturn(Optional.of(local));
    }

    private void enviarFacturaInicial(String empresa,String cliente) throws Exception {
        enviarPayload("""
                {"id":"evt_first","type":"invoice.paid","livemode":false,"created":200,
                "data":{"object":{"id":"in_first","customer":"%s","amount_paid":19900,
                "parent":{"subscription_details":{"subscription":"sub_fixture",
                "metadata":{"fluora_suscripcion_id":"8","fluora_empresa_id":"%s"}}}}}}
                """.formatted(cliente,empresa));
    }

    @Test void facturaSinCobroNoActivaSuscripcionPendiente() throws Exception {
        local.setEstado("PENDIENTE_PAGO");
        enviar("evt_zero","invoice.paid",0);
        assertThat(local.getEstado()).isEqualTo("PENDIENTE_PAGO");
        verifyNoInteractions(avisos);
    }

    @Test void pagoPositivoGeneraAvisoConElImporteReal() throws Exception {
        enviar("evt_positive","invoice.paid",49900);
        verify(avisos).programar(eq(local),eq("PAGO"),any(),eq(java.math.BigDecimal.valueOf(49900,2)));
    }

    @Test void fallaDeRenovacionIniciaGraciaYLosReintentosNoLaExtienden() throws Exception {
        enviar("evt_1","invoice.payment_failed",0);
        LocalDateTime hasta=local.getGraciaHasta();
        assertThat(hasta).isBetween(LocalDateTime.now(ZoneOffset.UTC).plusDays(2).minusMinutes(1),LocalDateTime.now(ZoneOffset.UTC).plusDays(2));
        enviar("evt_2","invoice.payment_failed",0);
        assertThat(local.getGraciaHasta()).isEqualTo(hasta);
        assertThat(local.getEstado()).isEqualTo("GRACIA");
        verify(avisos,times(2)).programar(eq(local),eq("FALLO"),eq("in_fixture"),isNull());
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
        verify(avisos).programar(local,"PLAN","200:9",null);
        verify(suscripciones).save(local);
    }

    @Test void suscripcionActivaExponePeriodoVigenteSinPresentarLaPruebaHistorica() {
        LocalDateTime historica=LocalDateTime.of(2026,10,7,12,0);
        LocalDateTime periodo=historica.plusMonths(1);
        local.setPruebaHasta(historica);local.setPeriodoActualHasta(periodo);
        when(suscripciones.findFirstByEmpresaIdOrderByCreadaEnDesc(1L)).thenReturn(Optional.of(local));
        var respuesta=servicio.estadoSuscripcion(1L);
        assertThat(respuesta.pruebaHasta()).isNull();
        assertThat(respuesta.periodoActualHasta()).isEqualTo(periodo);
        assertThat(local.getPruebaHasta()).isEqualTo(historica);
    }

    private void enviarPayload(String payload) throws Exception {
        long instante=Instant.now().getEpochSecond();
        Mac mac=Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("whsec_fixture".getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
        String firma=HexFormat.of().formatHex(mac.doFinal((instante+"."+payload).getBytes(StandardCharsets.UTF_8)));
        servicio.recibirWebhook("t="+instante+",v1="+firma,payload);
    }

    @Test void completarCheckoutVinculaStripeSinConcederAccesoAntesDelPago() throws Exception {
        local.setEstado("PENDIENTE_PAGO");
        when(suscripciones.findByStripeCheckoutSesionId("cs_fixture")).thenReturn(Optional.of(local));
        enviarPayload("""
                {"id":"evt_checkout","object":"event","type":"checkout.session.completed","livemode":false,
                 "data":{"object":{"id":"cs_fixture","subscription":"sub_fixture","customer":"cus_fixture"}}}
                """);
        assertThat(local.getEstado()).isEqualTo("PENDIENTE_PAGO");
        assertThat(local.getStripeSuscripcionId()).isEqualTo("sub_fixture");
        assertThat(local.getStripeClienteId()).isEqualTo("cus_fixture");
        enviar("evt_paid","invoice.paid",19900);
        assertThat(local.getEstado()).isEqualTo("ACTIVA");
    }

    @Test void facturaDeRenovacionResuelveSuscripcionEnElFormatoActualDeStripe() throws Exception {
        local.setEstado("GRACIA");local.setGraciaHasta(LocalDateTime.now(ZoneOffset.UTC).plusDays(1));
        enviarPayload("""
                {"id":"evt_renewal","object":"event","type":"invoice.paid","livemode":false,
                 "data":{"object":{"id":"in_renewal","amount_paid":19900,
                 "parent":{"subscription_details":{"subscription":"sub_fixture"}}}}}
                """);
        assertThat(local.getEstado()).isEqualTo("ACTIVA");
        assertThat(local.getGraciaHasta()).isNull();
        assertThat(local.getStripeUltimaFacturaId()).isEqualTo("in_renewal");
    }

    @Test void cancelacionProgramadaConservaEstadoActivoYFechaDeCorte() throws Exception {
        PlanPlataformaEntidad plan=new PlanPlataformaEntidad();plan.setId(4L);plan.setMoneda("MXN");
        when(planes.findByStripePrecioMensualIdOrStripePrecioAnualId("price_fixture","price_fixture"))
                .thenReturn(Optional.of(plan));
        enviarPayload("""
                {"id":"evt_cancel_scheduled","object":"event","type":"customer.subscription.updated","livemode":false,
                 "data":{"object":{"id":"sub_fixture","status":"active","cancel_at_period_end":true,
                 "items":{"data":[{"current_period_end":2000000000,"quantity":1,"price":{
                 "id":"price_fixture","currency":"mxn","unit_amount":19900,
                 "recurring":{"interval":"month","interval_count":1}}}]}}}}
                """);
        assertThat(local.getEstado()).isEqualTo("ACTIVA");
        assertThat(local.isCancelarAlFinalPeriodo()).isTrue();
        assertThat(local.getPeriodoActualHasta()).isEqualTo(LocalDateTime.ofInstant(Instant.ofEpochSecond(2000000000),ZoneOffset.UTC));
    }

    @Test void cancelacionDefinitivaImpideQueUnaFacturaPosteriorReactiveElPlan() throws Exception {
        enviarPayload("""
                {"id":"evt_deleted","object":"event","type":"customer.subscription.deleted","livemode":false,
                 "data":{"object":{"id":"sub_fixture","status":"canceled","customer":"cus_fixture"}}}
                """);
        assertThat(local.getEstado()).isEqualTo("CANCELADA");
        assertThat(local.getCanceladaEn()).isNotNull();
        enviar("evt_late_paid","invoice.paid",19900);
        assertThat(local.getEstado()).isEqualTo("CANCELADA");
    }

    @Test void eventoDuplicadoNoVuelveAProcesarUnaFactura() throws Exception {
        when(eventos.existsByStripeEventoId("evt_duplicate")).thenReturn(true);
        enviar("evt_duplicate","invoice.payment_failed",0);
        assertThat(local.getEstado()).isEqualTo("ACTIVA");
        verify(suscripciones,never()).save(any());
        verify(eventos,never()).save(any());
    }
}
