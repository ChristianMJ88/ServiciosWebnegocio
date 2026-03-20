package com.techprotech.agenda.modulos.servicios.aplicacion;

import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServicioCatalogoServicios {

    private final ServicioRepositorio servicioRepositorio;

    public ServicioCatalogoServicios(ServicioRepositorio servicioRepositorio) {
        this.servicioRepositorio = servicioRepositorio;
    }

    public List<ServicioPublicoResponse> listarPorSucursal(Long sucursalId) {
        Long sucursal = sucursalId != null ? sucursalId : 1L;
        return servicioRepositorio.findBySucursalIdAndActivoTrue(sucursal)
                .stream()
                .map(servicio -> new ServicioPublicoResponse(
                        servicio.getId(),
                        servicio.getSucursalId(),
                        servicio.getNombre(),
                        servicio.getDescripcion(),
                        servicio.getDuracionMinutos(),
                        servicio.getBufferAntesMinutos(),
                        servicio.getBufferDespuesMinutos(),
                        servicio.getPrecio(),
                        servicio.getMoneda()
                ))
                .toList();
    }
}
