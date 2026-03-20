package com.techprotech.agenda.modulos.sucursales.aplicacion;

import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServicioSucursales {

    private final SucursalRepositorio sucursalRepositorio;

    public ServicioSucursales(SucursalRepositorio sucursalRepositorio) {
        this.sucursalRepositorio = sucursalRepositorio;
    }

    public List<SucursalPublicaResponse> listarPublicas(Long empresaId) {
        Long empresa = empresaId != null ? empresaId : 1L;
        return sucursalRepositorio.findByEmpresaIdAndActivaTrue(empresa)
                .stream()
                .map(sucursal -> new SucursalPublicaResponse(
                        sucursal.getId(),
                        sucursal.getEmpresaId(),
                        sucursal.getNombre(),
                        sucursal.getDireccion(),
                        sucursal.getTelefono(),
                        sucursal.getZonaHoraria()
                ))
                .toList();
    }
}
