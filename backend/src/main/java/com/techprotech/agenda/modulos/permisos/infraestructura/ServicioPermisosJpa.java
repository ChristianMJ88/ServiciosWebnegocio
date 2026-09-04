package com.techprotech.agenda.modulos.permisos.infraestructura;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.PermisoRepositorio;
import com.techprotech.agenda.modulos.permisos.aplicacion.ServicioPermisos;
import com.techprotech.agenda.modulos.permisos.dominio.Permiso;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.springframework.http.HttpStatus.NOT_FOUND;
@Service
public class ServicioPermisosJpa implements ServicioPermisos {
    private final PermisoRepositorio permisos; private final EmpresaRepositorio empresas;
    public ServicioPermisosJpa(PermisoRepositorio permisos, EmpresaRepositorio empresas) { this.permisos = permisos; this.empresas = empresas; }
    @Override @Transactional(readOnly = true) public List<Permiso> listarCatalogo(Long empresaId) {
        if (!empresas.existsById(empresaId)) throw new ResponseStatusException(NOT_FOUND, "El negocio no existe");
        return permisos.findAllByOrderByNombreAsc().stream().map(p -> new Permiso(p.getId(), p.getCodigo(), p.getNombre(), p.getDescripcion())).toList();
    }
}
