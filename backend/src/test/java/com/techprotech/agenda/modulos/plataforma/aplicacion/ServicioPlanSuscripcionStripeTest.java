package com.techprotech.agenda.modulos.plataforma.aplicacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.PlanPlataformaRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServicioPlanSuscripcionStripeTest {
    private final PlanPlataformaRepositorio planes=mock(PlanPlataformaRepositorio.class);
    private final ServicioPlanSuscripcionStripe servicio=new ServicioPlanSuscripcionStripe(planes);
    private final ObjectMapper json=new ObjectMapper();

    @Test void cambioDePlanUsaElPrecioContratadoYNoElPrecioActualDelCatalogo() throws Exception {
        PlanPlataformaEntidad plan=plan();
        when(planes.findByStripePrecioMensualIdOrStripePrecioAnualId("price_fixture","price_fixture")).thenReturn(Optional.of(plan));
        SuscripcionPlataformaEntidad local=new SuscripcionPlataformaEntidad();local.setPlanId(1L);
        servicio.actualizar(local,json.readTree(suscripcion("year","prod_fixture",7)));
        assertThat(local.getPlanId()).isEqualTo(7L);
        assertThat(local.getCiclo()).isEqualTo("ANUAL");
        assertThat(local.getPrecioAcordado()).isEqualByComparingTo("2599.00");
        assertThat(local.getMoneda()).isEqualTo("MXN");
    }

    @Test void precioAnteriorSeResuelvePorSuProductoYMetadatosDelPlan() throws Exception {
        when(planes.findById(7L)).thenReturn(Optional.of(plan()));
        SuscripcionPlataformaEntidad local=new SuscripcionPlataformaEntidad();
        servicio.actualizar(local,json.readTree(suscripcion("month","prod_fixture",7)));
        assertThat(local.getPlanId()).isEqualTo(7L);
        assertThat(local.getCiclo()).isEqualTo("MENSUAL");
    }

    @Test void productoAjenoNoConcedeCapacidadesAunqueLosMetadatosMencionenUnPlan() {
        when(planes.findById(7L)).thenReturn(Optional.of(plan()));
        SuscripcionPlataformaEntidad local=new SuscripcionPlataformaEntidad();local.setPlanId(1L);
        assertThatThrownBy(()->servicio.actualizar(local,json.readTree(suscripcion("month","prod_ajeno",7))))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(local.getPlanId()).isEqualTo(1L);
    }

    @Test void precioDesconocidoNoModificaLaSuscripcion() {
        SuscripcionPlataformaEntidad local=new SuscripcionPlataformaEntidad();local.setPlanId(1L);
        assertThatThrownBy(()->servicio.actualizar(local,json.readTree(suscripcion("month","prod_fixture",999))))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(local.getPlanId()).isEqualTo(1L);
    }

    private PlanPlataformaEntidad plan() {
        PlanPlataformaEntidad plan=new PlanPlataformaEntidad();plan.setId(7L);plan.setMoneda("MXN");plan.setStripeProductoId("prod_fixture");return plan;
    }
    private String suscripcion(String intervalo,String producto,long plan) {
        return "{\"items\":{\"data\":[{\"quantity\":1,\"price\":{\"id\":\"price_fixture\",\"product\":\""+producto+"\",\"currency\":\"mxn\",\"unit_amount\":259900,\"recurring\":{\"interval\":\""+intervalo+"\",\"interval_count\":1},\"metadata\":{\"fluora_plan_id\":\""+plan+"\"}}}]}}";
    }
}
