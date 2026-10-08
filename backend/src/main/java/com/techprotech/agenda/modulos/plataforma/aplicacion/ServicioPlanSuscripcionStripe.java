package com.techprotech.agenda.modulos.plataforma.aplicacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.PlanPlataformaEntidad;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.SuscripcionPlataformaEntidad;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.PlanPlataformaRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.Locale;

@Service
public class ServicioPlanSuscripcionStripe {
    private final PlanPlataformaRepositorio planes;
    public ServicioPlanSuscripcionStripe(PlanPlataformaRepositorio planes) { this.planes=planes; }

    public void actualizar(SuscripcionPlataformaEntidad local,JsonNode suscripcion) {
        JsonNode items=suscripcion.path("items").path("data");
        if(!items.isArray()||items.size()!=1)throw desconocido();
        JsonNode precio=items.get(0).path("price");
        String id=precio.path("id").asText("");
        if(id.isBlank())throw desconocido();
        PlanPlataformaEntidad plan=planes.findByStripePrecioMensualIdOrStripePrecioAnualId(id,id)
                .orElseGet(()->resolverPrecioAnterior(precio));
        String intervalo=precio.path("recurring").path("interval").asText("");
        String ciclo=switch(intervalo) { case "month"->"MENSUAL"; case "year"->"ANUAL"; default->throw desconocido(); };
        if(precio.path("recurring").path("interval_count").asInt(1)!=1
                ||items.get(0).path("quantity").asInt(1)!=1)throw desconocido();
        String moneda=precio.path("currency").asText("");
        if(!moneda.equalsIgnoreCase(plan.getMoneda()))throw desconocido();
        JsonNode importe=precio.path("unit_amount");
        if(!importe.isIntegralNumber()||importe.asLong()<0)throw desconocido();
        local.setPlanId(plan.getId());
        local.setCiclo(ciclo);
        local.setMoneda(moneda.toUpperCase(Locale.ROOT));
        local.setPrecioAcordado(BigDecimal.valueOf(importe.asLong(),2));
    }

    private PlanPlataformaEntidad resolverPrecioAnterior(JsonNode precio) {
        long id=precio.path("metadata").path("fluora_plan_id").asLong(0);
        PlanPlataformaEntidad plan=planes.findById(id).orElseThrow(this::desconocido);
        String producto=precio.path("product").isObject()?precio.path("product").path("id").asText("")
                :precio.path("product").asText("");
        if(producto.isBlank()||!producto.equals(plan.getStripeProductoId()))throw desconocido();
        return plan;
    }

    private ResponseStatusException desconocido() {
        return new ResponseStatusException(HttpStatus.CONFLICT,"El precio de Stripe no corresponde a un plan de Fluora configurado");
    }
}
