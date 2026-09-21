package com.techprotech.agenda.modulos.sitio.aplicacion;

import com.techprotech.agenda.compartido.media.aplicacion.TransformadorImagenCloudinary;
import com.techprotech.agenda.modulos.prestadores.infraestructura.entidad.PrestadorServicioEntidad;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServicioEquipoSitio {

    private final SucursalRepositorio sucursalRepositorio;
    private final PrestadorServicioRepositorio prestadorServicioRepositorio;
    private final TransformadorImagenCloudinary transformadorImagen;

    public ServicioEquipoSitio(
            SucursalRepositorio sucursalRepositorio,
            PrestadorServicioRepositorio prestadorServicioRepositorio,
            TransformadorImagenCloudinary transformadorImagen
    ) {
        this.sucursalRepositorio = sucursalRepositorio;
        this.prestadorServicioRepositorio = prestadorServicioRepositorio;
        this.transformadorImagen = transformadorImagen;
    }

    @Transactional(readOnly = true)
    public List<IntegranteEquipoSitioPublicoResponse> obtenerParaSitioPublico(Long empresaId) {
        List<SucursalEntidad> sucursales = sucursalRepositorio.findByEmpresaIdAndActivaTrue(empresaId);
        if (sucursales.isEmpty()) return List.of();

        Map<Long, SucursalEntidad> sucursalesPorId = sucursales.stream()
                .collect(Collectors.toMap(SucursalEntidad::getId, Function.identity()));

        return prestadorServicioRepositorio.findBySucursalIdInOrderByNombreMostrarAsc(sucursalesPorId.keySet()).stream()
                .filter(PrestadorServicioEntidad::isActivo)
                .map(prestador -> new IntegranteEquipoSitioPublicoResponse(
                        prestador.getUsuarioId(),
                        prestador.getNombreMostrar(),
                        prestador.getBiografia(),
                        transformadorImagen.retrato(prestador.getFotoUrl(), prestador.getFotoEncuadre(), prestador.getFotoZoom(), 480),
                        sucursalesPorId.get(prestador.getSucursalId()).getNombre()
                ))
                .toList();
    }
}
