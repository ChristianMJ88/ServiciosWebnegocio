package com.techprotech.agenda.modulos.sucursales.aplicacion;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServicioSucursales {

    public List<SucursalPublicaResponse> listarPublicas(Long empresaId) {
        return List.of(
                new SucursalPublicaResponse(
                        1L,
                        empresaId != null ? empresaId : 1L,
                        "Sucursal Centro",
                        "Por definir",
                        "5550000000",
                        "America/Mexico_City"
                )
        );
    }
}

