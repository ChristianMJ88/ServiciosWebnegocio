package com.techprotech.agenda.modulos.servicios.api;

import com.techprotech.agenda.modulos.servicios.aplicacion.ServicioCatalogoServicios;
import com.techprotech.agenda.modulos.servicios.aplicacion.ServicioPublicoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/publico/servicios")
public class ControladorServiciosPublico {

    private final ServicioCatalogoServicios servicioCatalogoServicios;

    public ControladorServiciosPublico(ServicioCatalogoServicios servicioCatalogoServicios) {
        this.servicioCatalogoServicios = servicioCatalogoServicios;
    }

    @GetMapping
    public List<ServicioPublicoResponse> listar(@RequestParam(required = false) Long sucursalId) {
        return servicioCatalogoServicios.listarPorSucursal(sucursalId);
    }
}

