package com.techprotech.agenda.modulos.envios.api;

import com.techprotech.agenda.modulos.envios.api.dto.GeocodigoResponse;
import com.techprotech.agenda.modulos.envios.infraestructura.envia.ClienteGeocodigosEnvia;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@Validated
@RestController
@RequestMapping("/api/v1/publico/envios/geocodigos")
public class ControladorGeocodigosPublicos {
    private final ClienteGeocodigosEnvia cliente;

    public ControladorGeocodigosPublicos(ClienteGeocodigosEnvia cliente) { this.cliente = cliente; }

    @GetMapping("/{pais}/{codigoPostal}")
    public GeocodigoResponse validar(
            @PathVariable @Pattern(regexp = "[A-Za-z]{2}") String pais,
            @PathVariable @Pattern(regexp = "[0-9A-Za-z -]{3,12}") String codigoPostal) {
        return cliente.validar(pais.toUpperCase(Locale.ROOT), codigoPostal.trim().toUpperCase(Locale.ROOT));
    }
}
