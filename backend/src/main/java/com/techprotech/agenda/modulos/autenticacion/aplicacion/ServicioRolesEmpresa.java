package com.techprotech.agenda.modulos.autenticacion.aplicacion;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.RolEmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioInternoPerfilEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioInternoSucursalEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioPermisoEmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioRolEmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioRolEmpresaId;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioInternoPerfilRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioInternoSucursalRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.RolEmpresaPermisoRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.RolEmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioPermisoEmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRolEmpresaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServicioRolesEmpresa {

    private final RolEmpresaRepositorio rolEmpresaRepositorio;
    private final RolEmpresaPermisoRepositorio rolEmpresaPermisoRepositorio;
    private final UsuarioRolEmpresaRepositorio usuarioRolEmpresaRepositorio;
    private final UsuarioInternoPerfilRepositorio usuarioInternoPerfilRepositorio;
    private final UsuarioInternoSucursalRepositorio usuarioInternoSucursalRepositorio;
    private final UsuarioPermisoEmpresaRepositorio usuarioPermisoEmpresaRepositorio;

    public ServicioRolesEmpresa(
            RolEmpresaRepositorio rolEmpresaRepositorio,
            RolEmpresaPermisoRepositorio rolEmpresaPermisoRepositorio,
            UsuarioRolEmpresaRepositorio usuarioRolEmpresaRepositorio,
            UsuarioInternoPerfilRepositorio usuarioInternoPerfilRepositorio,
            UsuarioInternoSucursalRepositorio usuarioInternoSucursalRepositorio,
            UsuarioPermisoEmpresaRepositorio usuarioPermisoEmpresaRepositorio
    ) {
        this.rolEmpresaRepositorio = rolEmpresaRepositorio;
        this.rolEmpresaPermisoRepositorio = rolEmpresaPermisoRepositorio;
        this.usuarioRolEmpresaRepositorio = usuarioRolEmpresaRepositorio;
        this.usuarioInternoPerfilRepositorio = usuarioInternoPerfilRepositorio;
        this.usuarioInternoSucursalRepositorio = usuarioInternoSucursalRepositorio;
        this.usuarioPermisoEmpresaRepositorio = usuarioPermisoEmpresaRepositorio;
    }

    @Transactional(readOnly = true)
    public List<String> obtenerCodigosRolUsuario(Long empresaId, Long usuarioId) {
        return usuarioRolEmpresaRepositorio.findByUsuario_IdAndRolEmpresa_EmpresaId(usuarioId, empresaId).stream()
                .map(UsuarioRolEmpresaEntidad::getRolEmpresa)
                .filter(RolEmpresaEntidad::isActivo)
                .map(RolEmpresaEntidad::getCodigo)
                .distinct()
                .sorted()
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> obtenerPermisosUsuario(Long empresaId, Long usuarioId) {
        LinkedHashSet<String> permisos = new LinkedHashSet<>(rolEmpresaPermisoRepositorio.findCodigosPermisosByUsuarioIdAndEmpresaId(usuarioId, empresaId));
        permisos.addAll(obtenerPermisosDirectosUsuario(empresaId, usuarioId));
        return permisos.stream().sorted().toList();
    }

    @Transactional(readOnly = true)
    public List<String> obtenerPermisosDirectosUsuario(Long empresaId, Long usuarioId) {
        return usuarioPermisoEmpresaRepositorio.findByUsuario_Id(usuarioId).stream()
                .filter(asignacion -> asignacion.getUsuario().getEmpresaId().equals(empresaId))
                .map(UsuarioPermisoEmpresaEntidad::getPermiso)
                .map(permiso -> permiso.getCodigo())
                .distinct()
                .sorted()
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, List<String>> obtenerPermisosDirectosUsuarios(Long empresaId, Collection<Long> usuarioIds) {
        if (usuarioIds == null || usuarioIds.isEmpty()) {
            return Map.of();
        }

        return usuarioPermisoEmpresaRepositorio.findByUsuario_IdIn(usuarioIds).stream()
                .filter(asignacion -> asignacion.getUsuario().getEmpresaId().equals(empresaId))
                .collect(Collectors.groupingBy(
                        asignacion -> asignacion.getUsuario().getId(),
                        Collectors.mapping(
                                asignacion -> asignacion.getPermiso().getCodigo(),
                                Collectors.collectingAndThen(Collectors.toList(), permisos -> permisos.stream().distinct().sorted().toList())
                        )
                ));
    }

    @Transactional(readOnly = true)
    public List<Long> obtenerSucursalesPermitidas(Long empresaId, Long usuarioId) {
        List<Long> sucursalesExplicitas = usuarioInternoSucursalRepositorio.findByUsuario_Id(usuarioId).stream()
                .map(UsuarioInternoSucursalEntidad::getSucursal)
                .filter(sucursal -> sucursal.getEmpresaId().equals(empresaId))
                .map(sucursal -> sucursal.getId())
                .distinct()
                .sorted()
                .toList();
        if (!sucursalesExplicitas.isEmpty()) {
            return sucursalesExplicitas;
        }

        return usuarioInternoPerfilRepositorio.findById(usuarioId)
                .map(UsuarioInternoPerfilEntidad::getSucursalId)
                .filter(java.util.Objects::nonNull)
                .map(List::of)
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public boolean usuarioTieneRol(Long empresaId, Long usuarioId, String codigoRol) {
        String rolNormalizado = codigoRol != null ? codigoRol.trim().toUpperCase() : "";
        return obtenerCodigosRolUsuario(empresaId, usuarioId).stream().anyMatch(rolNormalizado::equals);
    }

    @Transactional(readOnly = true)
    public List<RolEmpresaEntidad> listarRolesEmpresa(Long empresaId) {
        return rolEmpresaRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId);
    }

    @Transactional(readOnly = true)
    public RolEmpresaEntidad obtenerRolEmpresa(Long empresaId, String codigoRol) {
        String rolNormalizado = codigoRol != null ? codigoRol.trim().toUpperCase() : "";
        return rolEmpresaRepositorio.findByEmpresaIdAndCodigo(empresaId, rolNormalizado)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No existe el rol de empresa " + rolNormalizado));
    }

    @Transactional(readOnly = true)
    public Map<Long, List<String>> obtenerPermisosPorRolEmpresa(Collection<Long> rolEmpresaIds) {
        if (rolEmpresaIds == null || rolEmpresaIds.isEmpty()) {
            return Map.of();
        }

        return rolEmpresaPermisoRepositorio.findByRolEmpresa_IdIn(rolEmpresaIds).stream()
                .collect(Collectors.groupingBy(
                        rolPermiso -> rolPermiso.getRolEmpresa().getId(),
                        Collectors.mapping(rolPermiso -> rolPermiso.getPermiso().getCodigo(), Collectors.collectingAndThen(Collectors.toList(), permisos -> permisos.stream().distinct().sorted().toList()))
                ));
    }

    @Transactional(readOnly = true)
    public List<UsuarioRolEmpresaEntidad> listarAsignacionesUsuario(Long empresaId, Long usuarioId) {
        return usuarioRolEmpresaRepositorio.findByUsuario_IdAndRolEmpresa_EmpresaId(usuarioId, empresaId);
    }

    @Transactional(readOnly = true)
    public List<UsuarioRolEmpresaEntidad> listarAsignacionesUsuarios(Long empresaId, Collection<Long> usuarioIds) {
        if (usuarioIds == null || usuarioIds.isEmpty()) {
            return List.of();
        }
        return usuarioRolEmpresaRepositorio.findByUsuario_IdInAndRolEmpresa_EmpresaId(usuarioIds, empresaId);
    }

    @Transactional
    public void asignarRolEmpresa(UsuarioEntidad usuario, Long empresaId, String codigoRol) {
        RolEmpresaEntidad rolEmpresa = obtenerRolEmpresa(empresaId, codigoRol);
        UsuarioRolEmpresaId id = new UsuarioRolEmpresaId(usuario.getId(), rolEmpresa.getId());
        if (!usuarioRolEmpresaRepositorio.existsByUsuario_IdAndRolEmpresa_Id(usuario.getId(), rolEmpresa.getId())) {
            usuarioRolEmpresaRepositorio.save(new UsuarioRolEmpresaEntidad(id, usuario, rolEmpresa));
        }
    }

    @Transactional
    public void reasignarRolesPorCodigo(UsuarioEntidad usuario, Long empresaId, Collection<String> codigosAReemplazar, String nuevoCodigoRol) {
        List<Long> rolEmpresaIds = listarRolesEmpresa(empresaId).stream()
                .filter(rolEmpresa -> codigosAReemplazar.contains(rolEmpresa.getCodigo()))
                .map(RolEmpresaEntidad::getId)
                .toList();

        if (!rolEmpresaIds.isEmpty()) {
            usuarioRolEmpresaRepositorio.deleteByUsuario_IdAndRolEmpresa_EmpresaIdAndRolEmpresa_IdIn(usuario.getId(), empresaId, rolEmpresaIds);
        }

        asignarRolEmpresa(usuario, empresaId, nuevoCodigoRol);
    }
}
