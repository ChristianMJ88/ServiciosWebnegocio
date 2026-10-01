package com.techprotech.agenda.modulos.pagos.api;

import com.techprotech.agenda.modulos.pagos.api.dto.CrearOrdenCheckoutRequest;
import com.techprotech.agenda.modulos.pagos.api.dto.CrearPagoCheckoutRequest;
import com.techprotech.agenda.modulos.pagos.api.dto.CrearPagoAlternativoCheckoutRequest;
import com.techprotech.agenda.modulos.pagos.api.dto.OrdenCheckoutResponse;
import com.techprotech.agenda.modulos.pagos.api.dto.PagoCheckoutResponse;
import com.techprotech.agenda.modulos.pagos.aplicacion.ServicioOrdenCheckout;
import com.techprotech.agenda.modulos.pagos.aplicacion.ServicioPagoCheckout;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/publico/empresas/{slug}/checkout/ordenes")
public class ControladorOrdenCheckoutPublico {
    private final ServicioOrdenCheckout servicio;
    private final ServicioPagoCheckout pagos;

    public ControladorOrdenCheckoutPublico(ServicioOrdenCheckout servicio, ServicioPagoCheckout pagos) {
        this.servicio = servicio;
        this.pagos = pagos;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrdenCheckoutResponse crear(@PathVariable String slug,
                                       @RequestHeader("Idempotency-Key") String idempotencia,
                                       @Valid @RequestBody CrearOrdenCheckoutRequest request) {
        return servicio.crear(slug, idempotencia, request);
    }

    @GetMapping("/{ordenId}")
    public OrdenCheckoutResponse obtener(@PathVariable String slug, @PathVariable String ordenId) {
        return servicio.obtener(slug, ordenId);
    }

    @PostMapping("/{ordenId}/pagos/tarjeta")
    public PagoCheckoutResponse pagarConTarjeta(@PathVariable String slug, @PathVariable String ordenId,
                                                @RequestHeader("Idempotency-Key") String idempotencia,
                                                @Valid @RequestBody CrearPagoCheckoutRequest request) {
        return pagos.pagarConTarjeta(slug, ordenId, idempotencia, request);
    }

    @PostMapping("/{ordenId}/pagos/alternativo")
    public PagoCheckoutResponse pagarConMetodoAlternativo(@PathVariable String slug, @PathVariable String ordenId,
                                                           @RequestHeader("Idempotency-Key") String idempotencia,
                                                           @Valid @RequestBody CrearPagoAlternativoCheckoutRequest request) {
        return pagos.pagarConMetodoAlternativo(slug, ordenId, idempotencia, request);
    }

    @GetMapping("/{ordenId}/pago")
    public PagoCheckoutResponse consultarPago(@PathVariable String slug, @PathVariable String ordenId) {
        return pagos.consultar(slug, ordenId);
    }
}
