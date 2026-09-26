package com.techprotech.agenda.modulos.pagos.api;

import com.techprotech.agenda.modulos.pagos.api.dto.CrearOrdenCheckoutRequest;
import com.techprotech.agenda.modulos.pagos.api.dto.OrdenCheckoutResponse;
import com.techprotech.agenda.modulos.pagos.aplicacion.ServicioOrdenCheckout;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/publico/empresas/{slug}/checkout/ordenes")
public class ControladorOrdenCheckoutPublico {
    private final ServicioOrdenCheckout servicio;

    public ControladorOrdenCheckoutPublico(ServicioOrdenCheckout servicio) {
        this.servicio = servicio;
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
}
