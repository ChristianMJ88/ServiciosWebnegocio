package com.techprotech.agenda.modulos.sucursales.api;

import com.techprotech.agenda.modulos.sucursales.aplicacion.ServicioSucursales;
import com.techprotech.agenda.modulos.sucursales.aplicacion.SucursalPublicaResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/publico/sucursales")
public class ControladorSucursalesPublico {

    private final ServicioSucursales servicioSucursales;

    public ControladorSucursalesPublico(ServicioSucursales servicioSucursales) {
        this.servicioSucursales = servicioSucursales;
    }

    @GetMapping
    public List<SucursalPublicaResponse> listar(@RequestParam(required = false) Long empresaId) {
        return servicioSucursales.listarPublicas(empresaId);
    }
}

