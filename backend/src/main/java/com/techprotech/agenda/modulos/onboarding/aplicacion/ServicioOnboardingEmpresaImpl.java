package com.techprotech.agenda.modulos.onboarding.aplicacion;

import com.techprotech.agenda.compartido.correo.BienvenidaEmpresaCorreo;
import com.techprotech.agenda.compartido.correo.ServicioOutboxCorreoBienvenida;
import com.techprotech.agenda.modulos.autenticacion.aplicacion.ServicioRolesEmpresa;
import com.techprotech.agenda.modulos.autenticacion.aplicacion.ServicioAutenticacion;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.PermisoEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.RolEmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.RolEmpresaPermisoEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.RolEmpresaPermisoId;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.RolEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.RolPermisoEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.RolEmpresaPermisoRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.RolEmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.RolPermisoRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.RolRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.autenticacion.social.PerfilRegistroSocial;
import com.techprotech.agenda.modulos.autenticacion.social.ServicioTokenRegistroSocial;
import com.techprotech.agenda.modulos.autenticacion.social.UsuarioIdentidadExternaEntidad;
import com.techprotech.agenda.modulos.autenticacion.social.UsuarioIdentidadExternaRepositorio;
import com.techprotech.agenda.modulos.onboarding.api.dto.RegistrarEmpresaRequest;
import com.techprotech.agenda.modulos.onboarding.api.dto.RegistrarEmpresaResponse;
import com.techprotech.agenda.modulos.onboarding.infraestructura.entidad.EmpresaOnboardingProgresoEntidad;
import com.techprotech.agenda.modulos.onboarding.infraestructura.repositorio.EmpresaOnboardingProgresoRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioConfigEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class ServicioOnboardingEmpresaImpl implements ServicioOnboardingEmpresa {

    private static final List<String> CODIGOS_ROL_BASE = List.of("ADMIN", "RECEPCIONISTA", "CAJERO", "STAFF", "CLIENTE");

    private final EmpresaRepositorio empresaRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final SucursalRepositorio sucursalRepositorio;
    private final EmpresaSitioConfigRepositorio empresaSitioConfigRepositorio;
    private final RolRepositorio rolRepositorio;
    private final RolPermisoRepositorio rolPermisoRepositorio;
    private final RolEmpresaRepositorio rolEmpresaRepositorio;
    private final RolEmpresaPermisoRepositorio rolEmpresaPermisoRepositorio;
    private final ServicioRolesEmpresa servicioRolesEmpresa;
    private final PasswordEncoder passwordEncoder;
    private final ServicioOutboxCorreoBienvenida servicioOutboxCorreoBienvenida;
    private final ServicioTokenRegistroSocial tokenRegistroSocial;
    private final UsuarioIdentidadExternaRepositorio identidadExternaRepositorio;
    private final ServicioVerificacionCorreoOnboarding verificacionCorreo;
    private final ServicioAutenticacion servicioAutenticacion;
    private final EmpresaOnboardingProgresoRepositorio progresoRepositorio;

    public ServicioOnboardingEmpresaImpl(
            EmpresaRepositorio empresaRepositorio,
            UsuarioRepositorio usuarioRepositorio,
            SucursalRepositorio sucursalRepositorio,
            EmpresaSitioConfigRepositorio empresaSitioConfigRepositorio,
            RolRepositorio rolRepositorio,
            RolPermisoRepositorio rolPermisoRepositorio,
            RolEmpresaRepositorio rolEmpresaRepositorio,
            RolEmpresaPermisoRepositorio rolEmpresaPermisoRepositorio,
            ServicioRolesEmpresa servicioRolesEmpresa,
            PasswordEncoder passwordEncoder,
            ServicioOutboxCorreoBienvenida servicioOutboxCorreoBienvenida,
            ServicioTokenRegistroSocial tokenRegistroSocial,
            UsuarioIdentidadExternaRepositorio identidadExternaRepositorio,
            ServicioVerificacionCorreoOnboarding verificacionCorreo,
            ServicioAutenticacion servicioAutenticacion,
            EmpresaOnboardingProgresoRepositorio progresoRepositorio
    ) {
        this.empresaRepositorio = empresaRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.sucursalRepositorio = sucursalRepositorio;
        this.empresaSitioConfigRepositorio = empresaSitioConfigRepositorio;
        this.rolRepositorio = rolRepositorio;
        this.rolPermisoRepositorio = rolPermisoRepositorio;
        this.rolEmpresaRepositorio = rolEmpresaRepositorio;
        this.rolEmpresaPermisoRepositorio = rolEmpresaPermisoRepositorio;
        this.servicioRolesEmpresa = servicioRolesEmpresa;
        this.passwordEncoder = passwordEncoder;
        this.servicioOutboxCorreoBienvenida = servicioOutboxCorreoBienvenida;
        this.tokenRegistroSocial = tokenRegistroSocial;
        this.identidadExternaRepositorio = identidadExternaRepositorio;
        this.verificacionCorreo = verificacionCorreo;
        this.servicioAutenticacion = servicioAutenticacion;
        this.progresoRepositorio = progresoRepositorio;
    }

    @Override
    @Transactional
    public RegistrarEmpresaResponse registrarEmpresa(RegistrarEmpresaRequest request) {
        String slug = resolverSlugDisponible(request.slug(), request.nombreEmpresa());
        PerfilRegistroSocial perfilSocial = resolverPerfilSocial(request.registroSocialToken());
        String correoAdministrador = perfilSocial == null
                ? request.correoAdministrador().trim().toLowerCase(Locale.ROOT)
                : perfilSocial.correo().trim().toLowerCase(Locale.ROOT);

        EmpresaEntidad empresa = new EmpresaEntidad();
        empresa.setNombre(request.nombreEmpresa().trim());
        empresa.setSlug(slug);
        empresa.setZonaHoraria(resolverZonaHoraria(request.zonaHoraria()));
        empresa.setEstado("ACTIVA");
        empresa = empresaRepositorio.save(empresa);

        crearSucursalPrincipal(empresa, request);
        provisionarRolesEmpresa(empresa.getId());
        crearSitioBase(empresa, request, slug, correoAdministrador);
        boolean requiereConfirmacion = perfilSocial == null;
        UsuarioEntidad administrador = crearAdministradorInicial(empresa, correoAdministrador, request, perfilSocial);
        crearProgresoInicial(empresa.getId(), request);
        vincularIdentidadSocial(administrador, perfilSocial);
        if (requiereConfirmacion) verificacionCorreo.enviar(empresa, administrador, request.nombreAdministrador().trim());
        else servicioOutboxCorreoBienvenida.programar(new BienvenidaEmpresaCorreo(empresa.getId(), empresa.getNombre(), request.nombreAdministrador().trim(), correoAdministrador, slug));

        var sesion = servicioAutenticacion.emitirSesionOnboarding(empresa.getId(), administrador.getId());

        return new RegistrarEmpresaResponse(
                empresa.getId(),
                slug,
                empresa.getNombre(),
                correoAdministrador,
                "/e/" + slug,
                "/acceso",
                requiereConfirmacion,
                sesion
        );
    }

    private void crearSucursalPrincipal(EmpresaEntidad empresa, RegistrarEmpresaRequest request) {
        SucursalEntidad sucursal = new SucursalEntidad();
        sucursal.setEmpresaId(empresa.getId());
        sucursal.setNombre("Sucursal principal");
        sucursal.setDireccion("Configuración pendiente");
        sucursal.setTelefono(request.telefonoAdministrador().trim());
        sucursal.setZonaHoraria(empresa.getZonaHoraria());
        sucursal.setActiva(true);
        sucursalRepositorio.save(sucursal);
    }

    private void crearSitioBase(EmpresaEntidad empresa, RegistrarEmpresaRequest request, String slug, String correoAdministrador) {
        EmpresaSitioConfigEntidad sitio = new EmpresaSitioConfigEntidad();
        sitio.setEmpresaId(empresa.getId());
        sitio.setSlug(slug);
        sitio.setNombreComercial(empresa.getNombre());
        sitio.setLogoUrl("/fluora-mark.svg");
        sitio.setDescripcionCorta("Sitio inicial creado desde Fluora para " + request.giro().trim().toLowerCase(Locale.ROOT) + ".");
        sitio.setColorPrimario("#6d4cff");
        sitio.setColorSecundario("#45d4ff");
        sitio.setHeroTitulo("Tu negocio ya tiene una base lista para vender y reservar.");
        sitio.setHeroSubtitulo("Configura servicios, branding y equipo desde Fluora para publicar una experiencia propia por empresa.");
        sitio.setWhatsapp(limpiarTelefono(request.telefonoAdministrador()));
        sitio.setTelefono(request.telefonoAdministrador().trim());
        sitio.setCorreo(correoAdministrador);
        sitio.setDireccion("Configura la dirección principal de tu negocio");
        sitio.setInstagramUrl("");
        sitio.setFacebookUrl("");
        sitio.setTema("fluora-base");
        sitio.setPublicado(false);
        empresaSitioConfigRepositorio.save(sitio);
    }

    private UsuarioEntidad crearAdministradorInicial(EmpresaEntidad empresa, String correoAdministrador, RegistrarEmpresaRequest request, PerfilRegistroSocial perfilSocial) {
        if (usuarioRepositorio.existsByEmpresaIdAndCorreo(empresa.getId(), correoAdministrador)) {
            throw new ResponseStatusException(CONFLICT, "Ya existe un usuario administrador con ese correo en la empresa");
        }

        UsuarioEntidad usuario = new UsuarioEntidad();
        usuario.setEmpresaId(empresa.getId());
        usuario.setCorreo(correoAdministrador);
        if (perfilSocial == null && (request.contrasena() == null || request.contrasena().isBlank())) {
            throw new ResponseStatusException(BAD_REQUEST, "Crea una contraseña para continuar");
        }
        String credencial = perfilSocial == null ? request.contrasena() : UUID.randomUUID().toString();
        usuario.setContrasenaHash(passwordEncoder.encode(credencial));
        usuario.setCorreoVerificadoEn(perfilSocial == null ? null : LocalDateTime.now());
        usuario.setHabilitado(true);
        usuario.setBloqueado(false);
        usuario = usuarioRepositorio.save(usuario);
        servicioRolesEmpresa.asignarRolEmpresa(usuario, empresa.getId(), "ADMIN");
        return usuario;
    }

    private void crearProgresoInicial(Long empresaId, RegistrarEmpresaRequest request) {
        EmpresaOnboardingProgresoEntidad progreso = new EmpresaOnboardingProgresoEntidad();
        progreso.setEmpresaId(empresaId);
        progreso.setCategoria(request.giro().trim());
        progreso.setTamanoEquipo(request.tamanoEquipo().trim());
        progreso.setPasoRecomendado("CONFIGURAR_HORARIO");
        progreso.setCreadoEn(LocalDateTime.now());
        progreso.setActualizadoEn(LocalDateTime.now());
        progresoRepositorio.save(progreso);
    }

    private PerfilRegistroSocial resolverPerfilSocial(String token) {
        if (token == null || token.isBlank()) return null;
        return tokenRegistroSocial.validarToken(token);
    }

    private void vincularIdentidadSocial(UsuarioEntidad usuario, PerfilRegistroSocial perfil) {
        if (perfil == null || identidadExternaRepositorio.existsByUsuarioIdAndProveedor(usuario.getId(), perfil.proveedor())) return;
        UsuarioIdentidadExternaEntidad identidad = new UsuarioIdentidadExternaEntidad();
        identidad.setUsuarioId(usuario.getId());
        identidad.setProveedor(perfil.proveedor());
        identidad.setSubjectProveedor(perfil.subject());
        identidad.setCorreoVerificado(perfil.correo());
        identidad.setCreadoEn(java.time.LocalDateTime.now());
        identidadExternaRepositorio.save(identidad);
    }

    private void provisionarRolesEmpresa(Long empresaId) {
        List<RolEntidad> rolesBase = CODIGOS_ROL_BASE.stream()
                .map(codigo -> rolRepositorio.findByCodigo(codigo)
                        .orElseThrow(() -> new ResponseStatusException(INTERNAL_SERVER_ERROR, "No existe el rol base " + codigo)))
                .toList();

        Map<Long, RolEmpresaEntidad> rolesEmpresaPorRolBaseId = new LinkedHashMap<>();
        for (RolEntidad rolBase : rolesBase) {
            RolEmpresaEntidad rolEmpresa = new RolEmpresaEntidad();
            rolEmpresa.setEmpresaId(empresaId);
            rolEmpresa.setRolBase(rolBase);
            rolEmpresa.setCodigo(rolBase.getCodigo());
            rolEmpresa.setNombre(resolverNombreRol(rolBase.getCodigo()));
            rolEmpresa.setDescripcion("Rol base generado automáticamente durante el onboarding.");
            rolEmpresa.setActivo(true);
            rolEmpresa.setEditable(false);
            rolesEmpresaPorRolBaseId.put(rolBase.getId(), rolEmpresaRepositorio.save(rolEmpresa));
        }

        List<RolPermisoEntidad> permisosBase = rolPermisoRepositorio.findByRol_IdIn(
                rolesBase.stream().map(RolEntidad::getId).toList()
        );

        for (RolPermisoEntidad permisoBase : permisosBase) {
            RolEmpresaEntidad rolEmpresa = rolesEmpresaPorRolBaseId.get(permisoBase.getRol().getId());
            if (rolEmpresa == null) {
                continue;
            }
            PermisoEntidad permiso = permisoBase.getPermiso();
            RolEmpresaPermisoId id = new RolEmpresaPermisoId(rolEmpresa.getId(), permiso.getId());
            rolEmpresaPermisoRepositorio.save(new RolEmpresaPermisoEntidad(id, rolEmpresa, permiso));
        }
    }

    private String resolverSlugDisponible(String slugSolicitado, String nombreEmpresa) {
        String base = slugify(slugSolicitado != null && !slugSolicitado.isBlank() ? slugSolicitado : nombreEmpresa);
        String candidato = base;
        int intento = 2;
        while (empresaRepositorio.existsBySlug(candidato) || empresaSitioConfigRepositorio.existsBySlug(candidato)) {
            candidato = base + "-" + intento;
            intento++;
        }
        return candidato;
    }

    private String resolverNombreRol(String codigoRol) {
        return switch (codigoRol) {
            case "ADMIN" -> "Administrador";
            case "RECEPCIONISTA" -> "Recepción";
            case "CAJERO" -> "Caja";
            case "STAFF" -> "Staff";
            case "CLIENTE" -> "Cliente";
            default -> codigoRol;
        };
    }

    private String resolverZonaHoraria(String zonaHoraria) {
        return (zonaHoraria == null || zonaHoraria.isBlank()) ? "America/Mexico_City" : zonaHoraria.trim();
    }

    private String limpiarTelefono(String telefono) {
        return telefono == null ? "" : telefono.replaceAll("[^0-9+]", "");
    }

    private String slugify(String valor) {
        String ascii = Normalizer.normalize(valor == null ? "" : valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = ascii
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return slug.isBlank() ? "empresa" : slug;
    }
}
