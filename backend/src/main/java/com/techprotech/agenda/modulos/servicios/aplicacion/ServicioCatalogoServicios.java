package com.techprotech.agenda.modulos.servicios.aplicacion;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ServicioCatalogoServicios {

    public List<ServicioPublicoResponse> listarPorSucursal(Long sucursalId) {
        Long sucursal = sucursalId != null ? sucursalId : 1L;
        return List.of(
                new ServicioPublicoResponse(1L, sucursal, "Manicura", "Servicio base de ejemplo", 60, 0, 10, new BigDecimal("250.00"), "MXN"),
                new ServicioPublicoResponse(2L, sucursal, "Pedicura", "Servicio base de ejemplo", 75, 0, 15, new BigDecimal("320.00"), "MXN")
        );
    }
}

