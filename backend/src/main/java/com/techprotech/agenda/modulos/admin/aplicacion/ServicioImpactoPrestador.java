package com.techprotech.agenda.modulos.admin.aplicacion;

import com.techprotech.agenda.modulos.admin.api.dto.ImpactoPrestadorResponse;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServicioImpactoPrestador {
    private final UsuarioRepositorio usuarios;
    private final PrestadorServicioRepositorio prestadores;
    private final CitaRepositorio citas;
    private final com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio sucursales;
    public ServicioImpactoPrestador(UsuarioRepositorio usuarios, PrestadorServicioRepositorio prestadores, CitaRepositorio citas, com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio sucursales) {
        this.usuarios = usuarios; this.prestadores = prestadores; this.citas = citas; this.sucursales = sucursales;
    }
    @Transactional(readOnly = true)
    public ImpactoPrestadorResponse consultar(Long empresaId, Long id) {
        usuarios.findByIdAndEmpresaId(id, empresaId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        var prestador = prestadores.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        var sucursal = sucursales.findByIdAndEmpresaId(prestador.getSucursalId(), empresaId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        return new ImpactoPrestadorResponse(id, citas.contarCitasFuturasPorEstado(empresaId, id, LocalDateTime.now(java.time.ZoneId.of(sucursal.getZonaHoraria()))).stream()
                .map(c -> new ImpactoPrestadorResponse.ConteoEstado(c.getEstado(), c.getCantidad())).toList());
    }
}
