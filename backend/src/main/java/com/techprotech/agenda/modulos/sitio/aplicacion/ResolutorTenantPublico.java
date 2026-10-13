package com.techprotech.agenda.modulos.sitio.aplicacion;

import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioSucursalRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

/** El slug define el negocio; los identificadores del cliente nunca amplían ese ámbito. */
@Service
public class ResolutorTenantPublico {
    private final EmpresaSitioConfigRepositorio sitios;
    private final SucursalRepositorio sucursales;
    private final ServicioRepositorio servicios;
    private final ServicioSucursalRepositorio asignaciones;
    private final UsuarioRepositorio usuarios;

    public ResolutorTenantPublico(EmpresaSitioConfigRepositorio sitios, SucursalRepositorio sucursales,
            ServicioRepositorio servicios, ServicioSucursalRepositorio asignaciones, UsuarioRepositorio usuarios) {
        this.sitios=sitios; this.sucursales=sucursales; this.servicios=servicios;
        this.asignaciones=asignaciones; this.usuarios=usuarios;
    }
    public Long resolver(String slug, Long empresaSolicitada) {
        if(slug==null || slug.isBlank()) throw new ResponseStatusException(BAD_REQUEST,"El slug del negocio es obligatorio");
        var sitio=sitios.findBySlug(slug.trim().toLowerCase(java.util.Locale.ROOT))
                .filter(com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioConfigEntidad::isPublicado).orElseThrow(() -> noExiste());
        if(empresaSolicitada!=null && !sitio.getEmpresaId().equals(empresaSolicitada)) throw noExiste();
        return sitio.getEmpresaId();
    }
    public void validarSucursal(Long empresaId, Long sucursalId) {
        if(sucursalId==null) throw new ResponseStatusException(BAD_REQUEST,"La sucursal es obligatoria");
        sucursales.findByIdAndEmpresaIdAndActivaTrue(sucursalId,empresaId).orElseThrow(() -> noExiste());
    }
    public void validarServicio(Long empresaId,Long sucursalId,Long servicioId,Long prestadorId) {
        validarSucursal(empresaId,sucursalId);
        servicios.findByIdAndEmpresaIdAndActivoTrue(servicioId,empresaId).orElseThrow(() -> noExiste());
        if(!asignaciones.existsByEmpresaIdAndIdServicioIdAndIdSucursalIdAndActivoTrue(empresaId,servicioId,sucursalId)) throw noExiste();
        if(prestadorId!=null) usuarios.findByIdAndEmpresaId(prestadorId,empresaId).orElseThrow(() -> noExiste());
    }
    private static ResponseStatusException noExiste() { return new ResponseStatusException(NOT_FOUND,"El recurso no está disponible en este negocio"); }
}
