package com.techprotech.agenda.modulos.admin.aplicacion;

import com.techprotech.agenda.compartido.correo.ConfiguracionCorreoEmpresaEntidad;
import com.techprotech.agenda.compartido.correo.ConfiguracionCorreoEmpresaRepositorio;
import com.techprotech.agenda.compartido.correo.BandejaSalidaNotificacionEntidad;
import com.techprotech.agenda.compartido.correo.BandejaSalidaNotificacionRepositorio;
import com.techprotech.agenda.compartido.correo.ProveedorCorreo;
import com.techprotech.agenda.compartido.correo.ProtectorSecretosCorreo;
import com.techprotech.agenda.compartido.whatsapp.ClienteWhatsappTwilio;
import com.techprotech.agenda.compartido.whatsapp.ConfiguracionWhatsappEmpresaEntidad;
import com.techprotech.agenda.compartido.whatsapp.ConfiguracionWhatsappEmpresaRepositorio;
import com.techprotech.agenda.compartido.whatsapp.ConfiguracionWhatsappResolvida;
import com.techprotech.agenda.compartido.whatsapp.PlantillaWhatsappEmpresaEntidad;
import com.techprotech.agenda.compartido.whatsapp.PlantillaWhatsappEmpresaRepositorio;
import com.techprotech.agenda.compartido.whatsapp.ResultadoEnvioWhatsapp;
import com.techprotech.agenda.compartido.whatsapp.ServicioConfiguracionWhatsappEmpresa;
import com.techprotech.agenda.compartido.whatsapp.ServicioOutboxWhatsappCitas;
import com.techprotech.agenda.compartido.correo.ServicioOutboxCorreoCitas;
import com.techprotech.agenda.modulos.admin.api.dto.ConfiguracionCorreoAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.ConfiguracionCorreoAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ConfiguracionSitioAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.ConfiguracionSitioAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ConfiguracionWhatsappAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.ConfiguracionWhatsappAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.CatalogoSugeridoAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.DetectarChannelSenderWhatsappResponse;
import com.techprotech.agenda.modulos.admin.api.dto.EnviarMensajeWhatsappAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.GrupoServicioAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.GrupoServicioAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ImportarCatalogoSugeridoRequest;
import com.techprotech.agenda.modulos.admin.api.dto.ImportarCatalogoSugeridoResponse;
import com.techprotech.agenda.modulos.admin.api.dto.LogMensajeWhatsappAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.MensajeWhatsappAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.MigracionSecretosCorreoResponse;
import com.techprotech.agenda.modulos.admin.api.dto.AsociarChannelSenderWhatsappRequest;
import com.techprotech.agenda.modulos.admin.api.dto.AsociarChannelSenderWhatsappResponse;
import com.techprotech.agenda.modulos.admin.api.dto.AuditoriaConfiguracionAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.AuditoriaRolInternoAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.PermisoAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.PlantillaRolInternoAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.PlantillaWhatsappAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.PlantillaWhatsappEmpresaAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.PlantillaWhatsappEmpresaAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.PrestadorAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.PrestadorAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ProvisionarSubcuentaWhatsappRequest;
import com.techprotech.agenda.modulos.admin.api.dto.ProvisionarMessagingServiceWhatsappRequest;
import com.techprotech.agenda.modulos.admin.api.dto.ProvisionarMessagingServiceWhatsappResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ProvisionarSubcuentaWhatsappResponse;
import com.techprotech.agenda.modulos.admin.api.dto.PruebaPlantillaWhatsappRequest;
import com.techprotech.agenda.modulos.admin.api.dto.PruebaPlantillaWhatsappResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ReportePrestadorAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ReporteServicioAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.PeriodoReporteAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.RolInternoAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.RolInternoAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ServicioAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.ServicioAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.SubgrupoServicioAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.SubgrupoServicioAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.SucursalAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.SucursalAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.UsuarioInternoAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.UsuarioInternoAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ResumenAdminResponse;
import com.techprotech.agenda.modulos.admin.infraestructura.entidad.AuditoriaRolEmpresaEntidad;
import com.techprotech.agenda.modulos.admin.infraestructura.entidad.AuditoriaConfiguracionEmpresaEntidad;
import com.techprotech.agenda.modulos.admin.infraestructura.repositorio.AuditoriaConfiguracionEmpresaRepositorio;
import com.techprotech.agenda.modulos.admin.infraestructura.repositorio.AuditoriaRolEmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.aplicacion.ServicioRolesEmpresa;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.PermisoRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.RolEmpresaPermisoRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.RolEmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioInternoPerfilRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioInternoSucursalRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioPermisoEmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRolEmpresaRepositorio;
import com.techprotech.agenda.modulos.citas.api.dto.CitaClienteResponse;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.HistorialEstadoCitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.HistorialEstadoCitaRepositorio;
import com.techprotech.agenda.modulos.prestadores.infraestructura.entidad.PrestadorServicioEntidad;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.AsignacionServicioPrestadorEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.AsignacionServicioPrestadorId;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.GrupoServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioSucursalEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioSucursalId;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.SubgrupoServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.AsignacionServicioPrestadorRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.GrupoServicioRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioSucursalRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.SubgrupoServicioRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioConfigEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import com.techprotech.agenda.modulos.whatsapp.infraestructura.entidad.MensajeWhatsappEntidad;
import com.techprotech.agenda.modulos.whatsapp.infraestructura.repositorio.MensajeWhatsappRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.net.IDN;
import java.net.URI;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Comparator;
import java.util.Collections;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class ServicioAdminCitas {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioAdminCitas.class);
    private static final Set<String> PERMISOS_ACCESO_INTERNO = Set.of(
            "PANEL_ADMIN_ACCESO",
            "RECEPCION_ACCESO",
            "CAJA_ACCESO",
            "STAFF_PANEL_ACCESO"
    );
    private static final List<CatalogoSugeridoDef> CATALOGOS_SUGERIDOS = List.of(
            new CatalogoSugeridoDef(
                    "nails-studio",
                    "Nails Studio",
                    "Manicura, pedicura, gel y extensiones para arrancar rápido el catálogo.",
                    List.of(
                            new GrupoSugeridoDef(
                                    "Uñas",
                                    "Servicios principales para manos y pies.",
                                    "/fluora-mark.svg",
                                    "spa",
                                    10,
                                    List.of(
                                            new SubgrupoSugeridoDef(
                                                    "Manicura",
                                                    "Clásica, gel y soft gel.",
                                                    10,
                                                    List.of(
                                                            new ServicioSugeridoDef("Manicura tradicional", "Limpieza, forma y esmaltado clásico.", 60, 0, 10, new BigDecimal("220"), "MXN", 10, false, null, null),
                                                            new ServicioSugeridoDef("Manicura gel", "Preparación y aplicación de gel semipermanente.", 75, 0, 10, new BigDecimal("290"), "MXN", 20, false, null, null),
                                                            new ServicioSugeridoDef("Soft gel", "Aplicación de tips soft gel con acabado natural.", 100, 0, 15, new BigDecimal("450"), "MXN", 30, true, "FIJO", new BigDecimal("120"))
                                                    )
                                            ),
                                            new SubgrupoSugeridoDef(
                                                    "Pedicura",
                                                    "Cuidado integral para pies.",
                                                    20,
                                                    List.of(
                                                            new ServicioSugeridoDef("Pedicura spa", "Limpieza, exfoliación y esmaltado.", 75, 0, 10, new BigDecimal("320"), "MXN", 10, false, null, null),
                                                            new ServicioSugeridoDef("Pedicura gel", "Pedicura con acabado de larga duración.", 90, 0, 10, new BigDecimal("390"), "MXN", 20, false, null, null)
                                                    )
                                            ),
                                            new SubgrupoSugeridoDef(
                                                    "Extensiones",
                                                    "Sets completos y mantenimientos.",
                                                    30,
                                                    List.of(
                                                            new ServicioSugeridoDef("Uñas acrílicas set completo", "Construcción de set completo con forma básica.", 120, 0, 15, new BigDecimal("580"), "MXN", 10, true, "FIJO", new BigDecimal("150")),
                                                            new ServicioSugeridoDef("Retoque acrílico", "Mantenimiento y relleno de crecimiento.", 90, 0, 10, new BigDecimal("420"), "MXN", 20, false, null, null)
                                                    )
                                            )
                                    )
                            )
                    )
            ),
            new CatalogoSugeridoDef(
                    "makeup-brows",
                    "Makeup + Brows",
                    "Maquillaje social, cejas y pestañas para estudios de belleza integral.",
                    List.of(
                            new GrupoSugeridoDef(
                                    "Maquillaje",
                                    "Looks para eventos y sesiones.",
                                    "/fluora-logo.svg",
                                    "palette",
                                    10,
                                    List.of(
                                            new SubgrupoSugeridoDef(
                                                    "Social",
                                                    "Maquillaje para eventos y ocasiones especiales.",
                                                    10,
                                                    List.of(
                                                            new ServicioSugeridoDef("Maquillaje social", "Preparación de piel y look completo.", 90, 0, 15, new BigDecimal("950"), "MXN", 10, true, "FIJO", new BigDecimal("250")),
                                                            new ServicioSugeridoDef("Maquillaje express", "Look ligero para compromisos rápidos.", 45, 0, 10, new BigDecimal("550"), "MXN", 20, false, null, null)
                                                    )
                                            )
                                    )
                            ),
                            new GrupoSugeridoDef(
                                    "Cejas y pestañas",
                                    "Servicios para mirada y definición.",
                                    "/fluora-mark.svg",
                                    "visibility",
                                    20,
                                    List.of(
                                            new SubgrupoSugeridoDef(
                                                    "Cejas",
                                                    "Diseño y acabado.",
                                                    10,
                                                    List.of(
                                                            new ServicioSugeridoDef("Diseño de ceja", "Mapeo y depilación con acabado natural.", 35, 0, 5, new BigDecimal("180"), "MXN", 10, false, null, null),
                                                            new ServicioSugeridoDef("Laminado de ceja", "Peinado, fijación y acabado definido.", 60, 0, 10, new BigDecimal("420"), "MXN", 20, false, null, null)
                                                    )
                                            ),
                                            new SubgrupoSugeridoDef(
                                                    "Pestañas",
                                                    "Lifting y tintes.",
                                                    20,
                                                    List.of(
                                                            new ServicioSugeridoDef("Lifting de pestañas", "Curvatura y nutrición de pestaña natural.", 70, 0, 10, new BigDecimal("480"), "MXN", 10, false, null, null),
                                                            new ServicioSugeridoDef("Tinte de pestañas", "Intensidad natural en pestaña.", 30, 0, 5, new BigDecimal("160"), "MXN", 20, false, null, null)
                                                    )
                                            )
                                    )
                            )
                    )
            )
    );

    private final CitaRepositorio citaRepositorio;
    private final HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio;
    private final SucursalRepositorio sucursalRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final ServicioSucursalRepositorio servicioSucursalRepositorio;
    private final GrupoServicioRepositorio grupoServicioRepositorio;
    private final SubgrupoServicioRepositorio subgrupoServicioRepositorio;
    private final PrestadorServicioRepositorio prestadorServicioRepositorio;
    private final AsignacionServicioPrestadorRepositorio asignacionServicioPrestadorRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final UsuarioInternoPerfilRepositorio usuarioInternoPerfilRepositorio;
    private final UsuarioInternoSucursalRepositorio usuarioInternoSucursalRepositorio;
    private final UsuarioPermisoEmpresaRepositorio usuarioPermisoEmpresaRepositorio;
    private final UsuarioRolEmpresaRepositorio usuarioRolEmpresaRepositorio;
    private final RolEmpresaRepositorio rolEmpresaRepositorio;
    private final RolEmpresaPermisoRepositorio rolEmpresaPermisoRepositorio;
    private final PermisoRepositorio permisoRepositorio;
    private final ServicioRolesEmpresa servicioRolesEmpresa;
    private final PasswordEncoder passwordEncoder;
    private final EmpresaRepositorio empresaRepositorio;
    private final EmpresaSitioConfigRepositorio empresaSitioConfigRepositorio;
    private final ConfiguracionCorreoEmpresaRepositorio configuracionCorreoEmpresaRepositorio;
    private final ConfiguracionWhatsappEmpresaRepositorio configuracionWhatsappEmpresaRepositorio;
    private final PlantillaWhatsappEmpresaRepositorio plantillaWhatsappEmpresaRepositorio;
    private final ServicioConfiguracionWhatsappEmpresa servicioConfiguracionWhatsappEmpresa;
    private final ClienteWhatsappTwilio clienteWhatsappTwilio;
    private final ServicioOutboxWhatsappCitas servicioOutboxWhatsappCitas;
    private final ServicioOutboxCorreoCitas servicioOutboxCorreoCitas;
    private final BandejaSalidaNotificacionRepositorio bandejaSalidaNotificacionRepositorio;
    private final MensajeWhatsappRepositorio mensajeWhatsappRepositorio;
    private final AuditoriaConfiguracionEmpresaRepositorio auditoriaConfiguracionEmpresaRepositorio;
    private final AuditoriaRolEmpresaRepositorio auditoriaRolEmpresaRepositorio;
    private final ObjectMapper objectMapper;
    private final ProtectorSecretosCorreo protectorSecretosCorreo;

    public ServicioAdminCitas(
            CitaRepositorio citaRepositorio,
            HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio,
            SucursalRepositorio sucursalRepositorio,
            ServicioRepositorio servicioRepositorio,
            ServicioSucursalRepositorio servicioSucursalRepositorio,
            GrupoServicioRepositorio grupoServicioRepositorio,
            SubgrupoServicioRepositorio subgrupoServicioRepositorio,
            PrestadorServicioRepositorio prestadorServicioRepositorio,
            AsignacionServicioPrestadorRepositorio asignacionServicioPrestadorRepositorio,
            ClienteRepositorio clienteRepositorio,
            UsuarioRepositorio usuarioRepositorio,
            UsuarioInternoPerfilRepositorio usuarioInternoPerfilRepositorio,
            UsuarioInternoSucursalRepositorio usuarioInternoSucursalRepositorio,
            UsuarioPermisoEmpresaRepositorio usuarioPermisoEmpresaRepositorio,
            UsuarioRolEmpresaRepositorio usuarioRolEmpresaRepositorio,
            RolEmpresaRepositorio rolEmpresaRepositorio,
            RolEmpresaPermisoRepositorio rolEmpresaPermisoRepositorio,
            PermisoRepositorio permisoRepositorio,
            ServicioRolesEmpresa servicioRolesEmpresa,
            PasswordEncoder passwordEncoder,
            EmpresaRepositorio empresaRepositorio,
            EmpresaSitioConfigRepositorio empresaSitioConfigRepositorio,
            ConfiguracionCorreoEmpresaRepositorio configuracionCorreoEmpresaRepositorio,
            ConfiguracionWhatsappEmpresaRepositorio configuracionWhatsappEmpresaRepositorio,
            PlantillaWhatsappEmpresaRepositorio plantillaWhatsappEmpresaRepositorio,
            ServicioConfiguracionWhatsappEmpresa servicioConfiguracionWhatsappEmpresa,
            ClienteWhatsappTwilio clienteWhatsappTwilio,
            ServicioOutboxWhatsappCitas servicioOutboxWhatsappCitas,
            ServicioOutboxCorreoCitas servicioOutboxCorreoCitas,
            BandejaSalidaNotificacionRepositorio bandejaSalidaNotificacionRepositorio,
            MensajeWhatsappRepositorio mensajeWhatsappRepositorio,
            AuditoriaConfiguracionEmpresaRepositorio auditoriaConfiguracionEmpresaRepositorio,
            AuditoriaRolEmpresaRepositorio auditoriaRolEmpresaRepositorio,
            ObjectMapper objectMapper,
            ProtectorSecretosCorreo protectorSecretosCorreo
    ) {
        this.citaRepositorio = citaRepositorio;
        this.historialEstadoCitaRepositorio = historialEstadoCitaRepositorio;
        this.sucursalRepositorio = sucursalRepositorio;
        this.servicioRepositorio = servicioRepositorio;
        this.servicioSucursalRepositorio = servicioSucursalRepositorio;
        this.grupoServicioRepositorio = grupoServicioRepositorio;
        this.subgrupoServicioRepositorio = subgrupoServicioRepositorio;
        this.prestadorServicioRepositorio = prestadorServicioRepositorio;
        this.asignacionServicioPrestadorRepositorio = asignacionServicioPrestadorRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.usuarioInternoPerfilRepositorio = usuarioInternoPerfilRepositorio;
        this.usuarioInternoSucursalRepositorio = usuarioInternoSucursalRepositorio;
        this.usuarioPermisoEmpresaRepositorio = usuarioPermisoEmpresaRepositorio;
        this.usuarioRolEmpresaRepositorio = usuarioRolEmpresaRepositorio;
        this.rolEmpresaRepositorio = rolEmpresaRepositorio;
        this.rolEmpresaPermisoRepositorio = rolEmpresaPermisoRepositorio;
        this.permisoRepositorio = permisoRepositorio;
        this.servicioRolesEmpresa = servicioRolesEmpresa;
        this.passwordEncoder = passwordEncoder;
        this.empresaRepositorio = empresaRepositorio;
        this.empresaSitioConfigRepositorio = empresaSitioConfigRepositorio;
        this.configuracionCorreoEmpresaRepositorio = configuracionCorreoEmpresaRepositorio;
        this.configuracionWhatsappEmpresaRepositorio = configuracionWhatsappEmpresaRepositorio;
        this.plantillaWhatsappEmpresaRepositorio = plantillaWhatsappEmpresaRepositorio;
        this.servicioConfiguracionWhatsappEmpresa = servicioConfiguracionWhatsappEmpresa;
        this.clienteWhatsappTwilio = clienteWhatsappTwilio;
        this.servicioOutboxWhatsappCitas = servicioOutboxWhatsappCitas;
        this.servicioOutboxCorreoCitas = servicioOutboxCorreoCitas;
        this.bandejaSalidaNotificacionRepositorio = bandejaSalidaNotificacionRepositorio;
        this.mensajeWhatsappRepositorio = mensajeWhatsappRepositorio;
        this.auditoriaConfiguracionEmpresaRepositorio = auditoriaConfiguracionEmpresaRepositorio;
        this.auditoriaRolEmpresaRepositorio = auditoriaRolEmpresaRepositorio;
        this.objectMapper = objectMapper;
        this.protectorSecretosCorreo = protectorSecretosCorreo;
    }

    @Transactional(readOnly = true)
    public ResumenAdminResponse resumen(Long empresaId, LocalDate desde, LocalDate hasta) {
        List<CitaEntidad> citas = citasReporte(empresaId, desde, hasta);
        LocalDate hoy = LocalDate.now();
        long total = citas.size();
        return new ResumenAdminResponse(
                total,
                contarEstado(citas, "PENDIENTE"),
                contarEstado(citas, "CONFIRMADA"),
                contarEstado(citas, "FINALIZADA"),
                contarEstado(citas, "CANCELADA"),
                contarEstado(citas, "NO_ASISTIO"),
                citas.stream().filter(cita -> cita.getInicio().toLocalDate().isEqual(hoy)).count(),
                sumarIngresos(citas, Set.of("PENDIENTE", "CONFIRMADA", "FINALIZADA")),
                sumarIngresos(citas, Set.of("FINALIZADA"))
        );
    }

    @Transactional(readOnly = true)
    public List<ReporteServicioAdminResponse> reporteServicios(Long empresaId, LocalDate desde, LocalDate hasta) {
        Map<Long, ServicioEntidad> servicios = servicioRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .collect(Collectors.toMap(ServicioEntidad::getId, servicio -> servicio));
        return citasReporte(empresaId, desde, hasta).stream()
                .collect(Collectors.groupingBy(CitaEntidad::getServicioId))
                .entrySet()
                .stream()
                .map(entry -> {
                    ServicioEntidad servicio = servicios.get(entry.getKey());
                    List<CitaEntidad> citasServicio = entry.getValue();
                    return new ReporteServicioAdminResponse(
                            entry.getKey(),
                            servicio != null ? servicio.getNombre() : "Servicio",
                            citasServicio.size(),
                            contarEstado(citasServicio, "PENDIENTE"),
                            contarEstado(citasServicio, "CONFIRMADA"),
                            contarEstado(citasServicio, "FINALIZADA"),
                            sumarIngresos(citasServicio, Set.of("PENDIENTE", "CONFIRMADA", "FINALIZADA")),
                            sumarIngresos(citasServicio, Set.of("FINALIZADA"))
                    );
                })
                .sorted(Comparator.comparing(ReporteServicioAdminResponse::totalCitas).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReportePrestadorAdminResponse> reportePrestadores(Long empresaId, LocalDate desde, LocalDate hasta) {
        List<CitaEntidad> citas = citasReporte(empresaId, desde, hasta);
        Set<Long> prestadorIds = citas.stream()
                .map(CitaEntidad::getPrestadorId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, PrestadorServicioEntidad> prestadores = prestadorServicioRepositorio.findAllById(prestadorIds).stream()
                .collect(Collectors.toMap(PrestadorServicioEntidad::getUsuarioId, prestador -> prestador));

        return citas.stream()
                .filter(cita -> cita.getPrestadorId() != null)
                .collect(Collectors.groupingBy(CitaEntidad::getPrestadorId, LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> {
                    PrestadorServicioEntidad prestador = prestadores.get(entry.getKey());
                    List<CitaEntidad> citasPrestador = entry.getValue();
                    BigDecimal ingresosFinalizados = sumarIngresos(citasPrestador, Set.of("FINALIZADA"));
                    long finalizadas = contarEstado(citasPrestador, "FINALIZADA");
                    return new ReportePrestadorAdminResponse(
                            entry.getKey(),
                            prestador != null ? prestador.getNombreMostrar() : "Prestador",
                            citasPrestador.size(),
                            contarEstado(citasPrestador, "PENDIENTE"),
                            contarEstado(citasPrestador, "CONFIRMADA"),
                            finalizadas,
                            contarEstado(citasPrestador, "CANCELADA"),
                            contarEstado(citasPrestador, "NO_ASISTIO"),
                            sumarIngresos(citasPrestador, Set.of("PENDIENTE", "CONFIRMADA", "FINALIZADA")),
                            ingresosFinalizados,
                            calcularPromedio(ingresosFinalizados, finalizadas)
                    );
                })
                .sorted(
                        Comparator.comparing(ReportePrestadorAdminResponse::ingresosFinalizados).reversed()
                                .thenComparing(ReportePrestadorAdminResponse::totalCitas, Comparator.reverseOrder())
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PeriodoReporteAdminResponse> periodosReporte() {
        LocalDate hoy = LocalDate.now();
        return List.of(
                new PeriodoReporteAdminResponse(
                        "MES_ACTUAL",
                        "Este mes",
                        hoy.withDayOfMonth(1),
                        hoy.withDayOfMonth(hoy.lengthOfMonth()),
                        true
                ),
                new PeriodoReporteAdminResponse("ULTIMOS_30_DIAS", "Últimos 30 días", hoy.minusDays(29), hoy, false),
                new PeriodoReporteAdminResponse("ULTIMOS_90_DIAS", "Últimos 90 días", hoy.minusDays(89), hoy, false),
                new PeriodoReporteAdminResponse(
                        "ANIO_ACTUAL",
                        "Este año",
                        hoy.withDayOfYear(1),
                        hoy.withDayOfYear(hoy.lengthOfYear()),
                        false
                ),
                new PeriodoReporteAdminResponse("TODO", "Todo el historial", null, null, false)
        );
    }

    private List<CitaEntidad> citasReporte(Long empresaId, LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null) {
            return citaRepositorio.findByEmpresaIdOrderByInicioDesc(empresaId);
        }
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new ResponseStatusException(BAD_REQUEST, "El periodo del reporte no es válido");
        }
        return citaRepositorio.findByEmpresaIdAndInicioBetweenOrderByInicioAsc(
                empresaId,
                desde.atStartOfDay(),
                hasta.plusDays(1).atStartOfDay()
        );
    }

    @Transactional(readOnly = true)
    public ConfiguracionCorreoAdminResponse obtenerConfiguracionCorreo(Long empresaId) {
        ConfiguracionCorreoEmpresaEntidad configuracion = configuracionCorreoEmpresaRepositorio.findById(empresaId)
                .orElseGet(() -> {
                    ConfiguracionCorreoEmpresaEntidad nueva = new ConfiguracionCorreoEmpresaEntidad();
                    nueva.setEmpresaId(empresaId);
                    nueva.setHabilitado(false);
                    return nueva;
                });

        return mapearConfiguracionCorreo(configuracion);
    }

    @Transactional(readOnly = true)
    public ConfiguracionSitioAdminResponse obtenerConfiguracionSitio(Long empresaId) {
        EmpresaEntidad empresa = empresaRepositorio.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La empresa indicada no existe"));
        EmpresaSitioConfigEntidad configuracion = empresaSitioConfigRepositorio.findById(empresaId)
                .orElseGet(() -> construirConfiguracionSitioBase(empresa));
        return mapearConfiguracionSitio(configuracion);
    }

    @Transactional
    public ConfiguracionSitioAdminResponse actualizarConfiguracionSitio(Long empresaId, Long usuarioActorId, ConfiguracionSitioAdminRequest request) {
        EmpresaEntidad empresa = empresaRepositorio.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La empresa indicada no existe"));
        EmpresaSitioConfigEntidad configuracion = empresaSitioConfigRepositorio.findById(empresaId)
                .orElseGet(() -> construirConfiguracionSitioBase(empresa));
        String snapshotAntes = snapshotConfiguracionSitio(configuracion);

        String slugNormalizado = normalizarSlugPublico(request.slug());
        validarSlugSitioDisponible(empresaId, slugNormalizado);

        String dominioPrincipal = normalizarDominioPrincipal(request.dominioPrincipal());
        validarDominioPrincipalDisponible(empresaId, dominioPrincipal);

        configuracion.setEmpresaId(empresaId);
        configuracion.setSlug(slugNormalizado);
        configuracion.setNombreComercial(request.nombreComercial().trim());
        configuracion.setDominioPrincipal(dominioPrincipal);
        configuracion.setLogoUrl(normalizarOpcional(request.logoUrl()));
        configuracion.setDescripcionCorta(normalizarOpcional(request.descripcionCorta()));
        configuracion.setColorPrimario(normalizarColorHex(request.colorPrimario()));
        configuracion.setColorSecundario(normalizarColorHex(request.colorSecundario()));
        configuracion.setFuenteTitulos(normalizarFuente(request.fuenteTitulos(), "JAKARTA"));
        configuracion.setFuenteCuerpo(normalizarFuente(request.fuenteCuerpo(), "INTER"));
        configuracion.setHeroTitulo(normalizarOpcional(request.heroTitulo()));
        configuracion.setHeroSubtitulo(normalizarOpcional(request.heroSubtitulo()));
        configuracion.setHeroImagenUrl(normalizarOpcional(request.heroImagenUrl()));
        configuracion.setWhatsapp(normalizarOpcional(request.whatsapp()));
        configuracion.setTelefono(normalizarOpcional(request.telefono()));
        configuracion.setCorreo(normalizarOpcional(request.correo()));
        configuracion.setDireccion(normalizarOpcional(request.direccion()));
        configuracion.setInstagramUrl(normalizarOpcional(request.instagramUrl()));
        configuracion.setFacebookUrl(normalizarOpcional(request.facebookUrl()));
        configuracion.setTema(normalizarTema(request.tema()));
        configuracion.setPublicado(request.publicado());

        EmpresaSitioConfigEntidad guardada = empresaSitioConfigRepositorio.save(configuracion);
        registrarAuditoriaConfiguracion(
                empresaId,
                usuarioActorId,
                "SITIO",
                "CONFIGURACION_SITIO_ACTUALIZADA",
                "Se actualizó la configuración del sitio público",
                snapshotAntes,
                snapshotConfiguracionSitio(guardada)
        );
        return mapearConfiguracionSitio(guardada);
    }

    @Transactional
    public ConfiguracionCorreoAdminResponse actualizarConfiguracionCorreo(Long empresaId, Long usuarioActorId, ConfiguracionCorreoAdminRequest request) {
        ConfiguracionCorreoEmpresaEntidad configuracion = configuracionCorreoEmpresaRepositorio.findById(empresaId)
                .orElseGet(() -> {
                    ConfiguracionCorreoEmpresaEntidad nueva = new ConfiguracionCorreoEmpresaEntidad();
                    nueva.setEmpresaId(empresaId);
                    return nueva;
        });
        String snapshotAntes = snapshotConfiguracionCorreo(configuracion);

        ProveedorCorreo proveedor = ProveedorCorreo.desdeValor(request.proveedor(), ProveedorCorreo.SMTP);
        configuracion.setHabilitado(request.habilitado());
        configuracion.setProveedor(proveedor.name());
        configuracion.setRemitente(normalizarOpcional(request.remitente()));
        configuracion.setNombreRemitente(normalizarOpcional(request.nombreRemitente()));
        configuracion.setResponderA(normalizarOpcional(request.responderA()));
        configuracion.setSmtpHost(normalizarOpcional(request.smtpHost()));
        configuracion.setSmtpPort(request.smtpPort());
        configuracion.setSmtpUsername(normalizarOpcional(request.smtpUsername()));
        configuracion.setSmtpAuth(request.smtpAuth());
        configuracion.setSmtpStartTls(request.smtpStartTls());
        configuracion.setGraphTenantId(normalizarOpcional(request.graphTenantId()));
        configuracion.setGraphClientId(normalizarOpcional(request.graphClientId()));
        configuracion.setGraphUserId(normalizarOpcional(request.graphUserId()));
        configuracion.setGraphCertificateThumbprint(normalizarOpcional(request.graphCertificateThumbprint()));
        if (proveedor != ProveedorCorreo.GRAPH) {
            configuracion.setGraphOauthRefreshToken(null);
            configuracion.setGraphOauthScopes(null);
            configuracion.setGraphOauthConectadoEn(null);
        }

        if (request.smtpPassword() != null) {
            String passwordNormalizado = normalizarOpcional(request.smtpPassword());
            configuracion.setSmtpPassword(
                    passwordNormalizado != null ? protectorSecretosCorreo.encriptar(passwordNormalizado) : null
            );
        }

        if (request.graphClientSecret() != null) {
            String secretoNormalizado = normalizarOpcional(request.graphClientSecret());
            configuracion.setGraphClientSecret(
                    secretoNormalizado != null ? protectorSecretosCorreo.encriptar(secretoNormalizado) : null
            );
        }

        if (request.graphPrivateKeyPem() != null) {
            String llavePrivadaNormalizada = normalizarOpcional(request.graphPrivateKeyPem());
            configuracion.setGraphPrivateKeyPem(
                    llavePrivadaNormalizada != null ? protectorSecretosCorreo.encriptar(llavePrivadaNormalizada) : null
            );
        }

        ConfiguracionCorreoEmpresaEntidad guardada = configuracionCorreoEmpresaRepositorio.save(configuracion);
        registrarAuditoriaConfiguracion(
                empresaId,
                usuarioActorId,
                "CORREO",
                "CONFIGURACION_CORREO_ACTUALIZADA",
                "Se actualizó la configuración de correo transaccional",
                snapshotAntes,
                snapshotConfiguracionCorreo(guardada)
        );
        return mapearConfiguracionCorreo(guardada);
    }

    @Transactional
    public ConfiguracionCorreoAdminResponse usarCorreoPlataforma(Long empresaId, Long usuarioActorId) {
        configuracionCorreoEmpresaRepositorio.findById(empresaId).ifPresent(configuracion -> {
            String snapshotAntes = snapshotConfiguracionCorreo(configuracion);
            configuracionCorreoEmpresaRepositorio.delete(configuracion);
            registrarAuditoriaConfiguracion(
                    empresaId,
                    usuarioActorId,
                    "CORREO",
                    "CORREO_PLATAFORMA_ACTIVADO",
                    "Se activó el correo canónico administrado por Fluora",
                    snapshotAntes,
                    null
            );
        });

        ConfiguracionCorreoEmpresaEntidad fallback = new ConfiguracionCorreoEmpresaEntidad();
        fallback.setEmpresaId(empresaId);
        fallback.setHabilitado(false);
        return mapearConfiguracionCorreo(fallback);
    }

    @Transactional
    public MigracionSecretosCorreoResponse migrarSecretosCorreo(Long empresaId, Long usuarioActorId) {
        ConfiguracionCorreoEmpresaEntidad configuracion = configuracionCorreoEmpresaRepositorio.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No existe configuracion de correo para la empresa"));
        String snapshotAntes = snapshotConfiguracionCorreo(configuracion);

        boolean actualizada = false;
        StringBuilder mensaje = new StringBuilder();

        String smtpPassword = configuracion.getSmtpPassword();
        if (smtpPassword != null && !smtpPassword.isBlank() && !smtpPassword.startsWith("enc:v1:")) {
            configuracion.setSmtpPassword(protectorSecretosCorreo.encriptar(smtpPassword));
            actualizada = true;
            mensaje.append("smtp_password migrado");
        }

        String graphClientSecret = configuracion.getGraphClientSecret();
        if (graphClientSecret != null && !graphClientSecret.isBlank() && !graphClientSecret.startsWith("enc:v1:")) {
            configuracion.setGraphClientSecret(protectorSecretosCorreo.encriptar(graphClientSecret));
            if (!mensaje.isEmpty()) {
                mensaje.append("; ");
            }
            mensaje.append("graph_client_secret migrado");
            actualizada = true;
        }

        String graphPrivateKeyPem = configuracion.getGraphPrivateKeyPem();
        if (graphPrivateKeyPem != null && !graphPrivateKeyPem.isBlank() && !graphPrivateKeyPem.startsWith("enc:v1:")) {
            configuracion.setGraphPrivateKeyPem(protectorSecretosCorreo.encriptar(graphPrivateKeyPem));
            if (!mensaje.isEmpty()) {
                mensaje.append("; ");
            }
            mensaje.append("graph_private_key_pem migrado");
            actualizada = true;
        }

        if (!actualizada) {
            return new MigracionSecretosCorreoResponse(false, "No hay secretos legacy por migrar");
        }

        ConfiguracionCorreoEmpresaEntidad guardada = configuracionCorreoEmpresaRepositorio.save(configuracion);
        registrarAuditoriaConfiguracion(
                empresaId,
                usuarioActorId,
                "CORREO",
                "SECRETOS_CORREO_MIGRADOS",
                "Se migraron secretos legacy de correo a formato cifrado",
                snapshotAntes,
                snapshotConfiguracionCorreo(guardada)
        );
        return new MigracionSecretosCorreoResponse(true, mensaje + " a formato cifrado");
    }

    @Transactional(readOnly = true)
    public ConfiguracionWhatsappAdminResponse obtenerConfiguracionWhatsapp(Long empresaId) {
        ConfiguracionWhatsappEmpresaEntidad configuracion = configuracionWhatsappEmpresaRepositorio.findById(empresaId)
                .orElseGet(() -> {
                    ConfiguracionWhatsappEmpresaEntidad nueva = new ConfiguracionWhatsappEmpresaEntidad();
                    nueva.setEmpresaId(empresaId);
                    nueva.setHabilitado(false);
                    return nueva;
                });
        return mapearConfiguracionWhatsapp(configuracion);
    }

    @Transactional
    public ConfiguracionWhatsappAdminResponse actualizarConfiguracionWhatsapp(Long empresaId, Long usuarioActorId, ConfiguracionWhatsappAdminRequest request) {
        ConfiguracionWhatsappEmpresaEntidad configuracion = configuracionWhatsappEmpresaRepositorio.findById(empresaId)
                .orElseGet(() -> {
                    ConfiguracionWhatsappEmpresaEntidad nueva = new ConfiguracionWhatsappEmpresaEntidad();
                    nueva.setEmpresaId(empresaId);
                    return nueva;
                });
        String snapshotAntes = snapshotConfiguracionWhatsapp(configuracion);

        configuracion.setHabilitado(request.habilitado());
        configuracion.setAccountSid(normalizarOpcional(request.accountSid()));
        configuracion.setNumeroRemitente(normalizarOpcional(request.numeroRemitente()));
        configuracion.setTipoCuentaTwilio(normalizarOpcional(request.tipoCuentaTwilio()));
        configuracion.setSubaccountSid(normalizarOpcional(request.subaccountSid()));
        configuracion.setMessagingServiceSid(normalizarOpcional(request.messagingServiceSid()));
        configuracion.setChannelSenderSid(normalizarOpcional(request.channelSenderSid()));
        configuracion.setStatusCallbackUrl(normalizarOpcional(request.statusCallbackUrl()));
        configuracion.setPlantillaSolicitudConfirmacionSid(normalizarOpcional(request.plantillaSolicitudConfirmacionSid()));
        configuracion.setPlantillaReprogramadaPendienteSid(normalizarOpcional(request.plantillaReprogramadaPendienteSid()));
        configuracion.setPlantillaRecordatorioConfirmacionSid(normalizarOpcional(request.plantillaRecordatorioConfirmacionSid()));
        configuracion.setPlantillaCitaConfirmadaSid(normalizarOpcional(request.plantillaCitaConfirmadaSid()));
        configuracion.setPlantillaRecordatorioSid(normalizarOpcional(request.plantillaRecordatorioSid()));
        configuracion.setPlantillaCancelacionSid(normalizarOpcional(request.plantillaCancelacionSid()));
        configuracion.setPlantillaLiberadaSinConfirmacionSid(normalizarOpcional(request.plantillaLiberadaSinConfirmacionSid()));
        configuracion.setPlantillaGraciasVisitaSid(normalizarOpcional(request.plantillaGraciasVisitaSid()));
        configuracion.setPlantillaRecordatorioRegresoSid(normalizarOpcional(request.plantillaRecordatorioRegresoSid()));
        configuracion.setPlantillaEspacioDisponibleWalkinSid(normalizarOpcional(request.plantillaEspacioDisponibleWalkinSid()));
        configuracion.setPlantillaMenuBienvenidaSid(normalizarOpcional(request.plantillaMenuBienvenidaSid()));
        configuracion.setPlantillasListPickerSids(normalizarOpcional(request.plantillasListPickerSids()));
        configuracion.setSenderDisplayName(normalizarOpcional(request.senderDisplayName()));
        configuracion.setSenderPhoneNumber(normalizarOpcional(request.senderPhoneNumber()));
        configuracion.setSenderStatus(normalizarOpcional(request.senderStatus()));
        configuracion.setQualityRating(normalizarOpcional(request.qualityRating()));
        configuracion.setThroughputMps(request.throughputMps());
        configuracion.setWabaId(normalizarOpcional(request.wabaId()));
        configuracion.setMetaBusinessManagerId(normalizarOpcional(request.metaBusinessManagerId()));

        if (request.authToken() != null) {
            String authTokenNormalizado = normalizarOpcional(request.authToken());
            configuracion.setAuthToken(
                    authTokenNormalizado != null ? cifrarSecretosWhatsappSiEsPosible(authTokenNormalizado, empresaId) : null
            );
        }

        ConfiguracionWhatsappEmpresaEntidad guardada = configuracionWhatsappEmpresaRepositorio.save(configuracion);
        registrarAuditoriaConfiguracion(
                empresaId,
                usuarioActorId,
                "WHATSAPP",
                "CONFIGURACION_WHATSAPP_ACTUALIZADA",
                "Se actualizó la configuración operativa de WhatsApp",
                snapshotAntes,
                snapshotConfiguracionWhatsapp(guardada)
        );
        return mapearConfiguracionWhatsapp(guardada);
    }

    private String cifrarSecretosWhatsappSiEsPosible(String valorPlano, Long empresaId) {
        try {
            return protectorSecretosCorreo.encriptar(valorPlano);
        } catch (IllegalStateException ex) {
            LOGGER.warn(
                    "No se pudo cifrar authToken de WhatsApp para empresa {}. Se guardara temporalmente en texto plano: {}",
                    empresaId,
                    ex.getMessage()
            );
            return valorPlano;
        }
    }

    @Transactional(readOnly = true)
    public List<PlantillaWhatsappAdminResponse> listarPlantillasWhatsapp(Long empresaId) {
        ConfiguracionWhatsappResolvida configuracion = servicioConfiguracionWhatsappEmpresa.resolver(empresaId);
        Map<String, PlantillaWhatsappAdminResponse> plantillas = new LinkedHashMap<>();

        try {
            for (ClienteWhatsappTwilio.PlantillaTwilioWhatsapp plantilla : clienteWhatsappTwilio.listarPlantillasWhatsapp(empresaId)) {
                plantillas.put(
                        plantilla.sid(),
                        new PlantillaWhatsappAdminResponse(
                                plantilla.sid(),
                                plantilla.nombre(),
                                plantilla.idioma(),
                                plantilla.categoria(),
                                plantilla.estado(),
                                plantilla.tipoPlantilla()
                        )
                );
            }
        } catch (Exception ex) {
            // El panel admin debe seguir mostrando al menos las plantillas configuradas localmente.
        }

        for (PlantillaWhatsappAdminResponse plantillaLocal : construirPlantillasLocalmente(configuracion)) {
            plantillas.putIfAbsent(plantillaLocal.sid(), plantillaLocal);
        }

        return new ArrayList<>(plantillas.values());
    }

    @Transactional(readOnly = true)
    public List<PlantillaWhatsappEmpresaAdminResponse> listarPlantillasWhatsappEmpresa(Long empresaId) {
        return plantillaWhatsappEmpresaRepositorio.findByEmpresaIdOrderByUsoAscNombreAsc(empresaId).stream()
                .map(this::mapearPlantillaWhatsappEmpresa)
                .toList();
    }

    @Transactional
    public PlantillaWhatsappEmpresaAdminResponse crearPlantillaWhatsappEmpresa(
            Long empresaId,
            Long usuarioActorId,
            PlantillaWhatsappEmpresaAdminRequest request
    ) {
        String uso = normalizarUsoPlantillaWhatsapp(request.uso());
        String contentSid = normalizarContentSidPlantillaWhatsapp(request.contentSid());
        if (plantillaWhatsappEmpresaRepositorio.existsByEmpresaIdAndUsoIgnoreCaseAndContentSidIgnoreCase(empresaId, uso, contentSid)) {
            throw new ResponseStatusException(CONFLICT, "Ya existe una plantilla con ese uso y Content SID");
        }

        PlantillaWhatsappEmpresaEntidad plantilla = new PlantillaWhatsappEmpresaEntidad();
        plantilla.setEmpresaId(empresaId);
        aplicarPlantillaWhatsappEmpresa(plantilla, request, uso, contentSid);
        PlantillaWhatsappEmpresaEntidad guardada = plantillaWhatsappEmpresaRepositorio.save(plantilla);

        registrarAuditoriaConfiguracion(
                empresaId,
                usuarioActorId,
                "WHATSAPP",
                "PLANTILLA_WHATSAPP_CREADA",
                "Se registró una plantilla WhatsApp para el tenant",
                null,
                snapshotPlantillaWhatsappEmpresa(guardada)
        );
        return mapearPlantillaWhatsappEmpresa(guardada);
    }

    @Transactional
    public PlantillaWhatsappEmpresaAdminResponse actualizarPlantillaWhatsappEmpresa(
            Long empresaId,
            Long usuarioActorId,
            Long plantillaId,
            PlantillaWhatsappEmpresaAdminRequest request
    ) {
        PlantillaWhatsappEmpresaEntidad plantilla = plantillaWhatsappEmpresaRepositorio.findByIdAndEmpresaId(plantillaId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La plantilla WhatsApp no existe para esta empresa"));
        String snapshotAntes = snapshotPlantillaWhatsappEmpresa(plantilla);
        String uso = normalizarUsoPlantillaWhatsapp(request.uso());
        String contentSid = normalizarContentSidPlantillaWhatsapp(request.contentSid());

        plantillaWhatsappEmpresaRepositorio
                .findByEmpresaIdOrderByUsoAscNombreAsc(empresaId)
                .stream()
                .filter(existente -> !existente.getId().equals(plantillaId))
                .filter(existente -> existente.getUso() != null && existente.getUso().equalsIgnoreCase(uso))
                .filter(existente -> existente.getContentSid() != null && existente.getContentSid().equalsIgnoreCase(contentSid))
                .findFirst()
                .ifPresent(existente -> {
                    throw new ResponseStatusException(CONFLICT, "Ya existe otra plantilla con ese uso y Content SID");
                });

        aplicarPlantillaWhatsappEmpresa(plantilla, request, uso, contentSid);
        PlantillaWhatsappEmpresaEntidad guardada = plantillaWhatsappEmpresaRepositorio.save(plantilla);

        registrarAuditoriaConfiguracion(
                empresaId,
                usuarioActorId,
                "WHATSAPP",
                "PLANTILLA_WHATSAPP_ACTUALIZADA",
                "Se actualizó una plantilla WhatsApp del tenant",
                snapshotAntes,
                snapshotPlantillaWhatsappEmpresa(guardada)
        );
        return mapearPlantillaWhatsappEmpresa(guardada);
    }

    @Transactional
    public void eliminarPlantillaWhatsappEmpresa(Long empresaId, Long usuarioActorId, Long plantillaId) {
        PlantillaWhatsappEmpresaEntidad plantilla = plantillaWhatsappEmpresaRepositorio.findByIdAndEmpresaId(plantillaId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La plantilla WhatsApp no existe para esta empresa"));
        String snapshotAntes = snapshotPlantillaWhatsappEmpresa(plantilla);
        plantillaWhatsappEmpresaRepositorio.delete(plantilla);
        registrarAuditoriaConfiguracion(
                empresaId,
                usuarioActorId,
                "WHATSAPP",
                "PLANTILLA_WHATSAPP_ELIMINADA",
                "Se eliminó una plantilla WhatsApp del tenant",
                snapshotAntes,
                null
        );
    }

    @Transactional(readOnly = true)
    public List<LogMensajeWhatsappAdminResponse> listarLogsWhatsapp(Long empresaId) {
        ConfiguracionWhatsappResolvida configuracion = servicioConfiguracionWhatsappEmpresa.resolver(empresaId);
        return bandejaSalidaNotificacionRepositorio.findTop50ByEmpresaIdAndCanalOrderByIdDesc(empresaId, "WHATSAPP")
                .stream()
                .map(log -> mapearLogWhatsapp(log, configuracion))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MensajeWhatsappAdminResponse> listarMensajesWhatsapp(Long empresaId) {
        return mensajeWhatsappRepositorio.findTop200ByEmpresaIdOrderByCreadoEnDesc(empresaId)
                .stream()
                .map(this::mapearMensajeWhatsapp)
                .toList();
    }

    @Transactional
    public MensajeWhatsappAdminResponse enviarMensajeWhatsapp(Long empresaId, EnviarMensajeWhatsappAdminRequest request) {
        String telefono = normalizarOpcional(request.telefono());
        String mensajeTexto = normalizarOpcional(request.mensaje());
        if (telefono == null || mensajeTexto == null) {
            throw new ResponseStatusException(BAD_REQUEST, "El teléfono y el mensaje son obligatorios");
        }

        MensajeWhatsappEntidad mensaje = new MensajeWhatsappEntidad();
        mensaje.setEmpresaId(empresaId);
        String telefonoNormalizado = com.techprotech.agenda.compartido.whatsapp.NormalizadorTelefonoWhatsapp.normalizarComparable(telefono);
        if (telefonoNormalizado.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "El teléfono de WhatsApp no es válido");
        }
        mensaje.setTelefonoNormalizado(telefonoNormalizado);
        mensaje.setDireccion("SALIENTE");
        mensaje.setCuerpo(mensajeTexto);
        mensaje.setCreadoEn(LocalDateTime.now());

        try {
            ResultadoEnvioWhatsapp resultado = clienteWhatsappTwilio.enviarMensaje(empresaId, telefono, mensajeTexto);
            mensaje.setProveedorMensajeId(resultado.proveedorMensajeId());
            mensaje.setEstado(resultado.estadoProveedor());
            mensaje.setCodigoErrorProveedor(resultado.codigoErrorProveedor());
            mensaje.setDetalleErrorProveedor(resultado.detalleErrorProveedor());
        } catch (Exception ex) {
            mensaje.setEstado("ERROR");
            mensaje.setDetalleErrorProveedor(ex.getMessage());
            mensajeWhatsappRepositorio.save(mensaje);
            throw new ResponseStatusException(CONFLICT, "No se pudo enviar el mensaje por WhatsApp: " + ex.getMessage());
        }

        return mapearMensajeWhatsapp(mensajeWhatsappRepositorio.save(mensaje));
    }

    @Transactional
    public ProvisionarSubcuentaWhatsappResponse provisionarSubcuentaWhatsapp(
            Long empresaId,
            Long usuarioActorId,
            ProvisionarSubcuentaWhatsappRequest request
    ) {
        EmpresaEntidad empresa = empresaRepositorio.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No existe la empresa para provisionar la subcuenta"));

        ConfiguracionWhatsappEmpresaEntidad configuracion = configuracionWhatsappEmpresaRepositorio.findById(empresaId)
                .orElseGet(() -> {
                    ConfiguracionWhatsappEmpresaEntidad nueva = new ConfiguracionWhatsappEmpresaEntidad();
                    nueva.setEmpresaId(empresaId);
                    nueva.setHabilitado(false);
                    return nueva;
                });

        if (configuracion.getSubaccountSid() != null && !configuracion.getSubaccountSid().isBlank()) {
            throw new ResponseStatusException(CONFLICT, "Este tenant ya tiene una subcuenta Twilio registrada");
        }

        String friendlyName = normalizarOpcional(request.friendlyName());
        if (friendlyName == null) {
            friendlyName = construirNombreSubcuenta(empresa);
        }

        ClienteWhatsappTwilio.SubcuentaTwilioProvisionada subcuenta = clienteWhatsappTwilio.provisionarSubcuenta(friendlyName);

        configuracion.setTipoCuentaTwilio("SUBCUENTA");
        configuracion.setSubaccountSid(subcuenta.sid());
        configuracion.setAuthToken(protectorSecretosCorreo.encriptar(subcuenta.authToken()));
        if (configuracion.getAccountSid() == null || configuracion.getAccountSid().isBlank()) {
            configuracion.setAccountSid(subcuenta.ownerAccountSid());
        }
        if (configuracion.getSenderStatus() == null || configuracion.getSenderStatus().isBlank()) {
            configuracion.setSenderStatus("PENDIENTE_CONFIGURACION");
        }

        String snapshotAntes = snapshotConfiguracionWhatsapp(configuracion);
        ConfiguracionWhatsappEmpresaEntidad guardada = configuracionWhatsappEmpresaRepositorio.save(configuracion);
        registrarAuditoriaConfiguracion(
                empresaId,
                usuarioActorId,
                "WHATSAPP",
                "WHATSAPP_SUBCUENTA_PROVISIONADA",
                "Se provisionó una subcuenta Twilio para el tenant",
                snapshotAntes,
                snapshotConfiguracionWhatsapp(guardada)
        );
        ConfiguracionWhatsappAdminResponse respuestaConfiguracion = mapearConfiguracionWhatsapp(guardada);

        return new ProvisionarSubcuentaWhatsappResponse(
                true,
                "Subcuenta Twilio creada. Falta registrar el sender de WhatsApp y completar el Messaging Service del tenant.",
                subcuenta.friendlyName(),
                subcuenta.sid(),
                subcuenta.status(),
                respuestaConfiguracion
        );
    }

    @Transactional
    public ProvisionarMessagingServiceWhatsappResponse provisionarMessagingServiceWhatsapp(
            Long empresaId,
            Long usuarioActorId,
            ProvisionarMessagingServiceWhatsappRequest request
    ) {
        EmpresaEntidad empresa = empresaRepositorio.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No existe la empresa para provisionar el Messaging Service"));

        ConfiguracionWhatsappEmpresaEntidad configuracion = configuracionWhatsappEmpresaRepositorio.findById(empresaId)
                .orElseGet(() -> {
                    ConfiguracionWhatsappEmpresaEntidad nueva = new ConfiguracionWhatsappEmpresaEntidad();
                    nueva.setEmpresaId(empresaId);
                    nueva.setHabilitado(false);
                    return nueva;
                });

        if (configuracion.getMessagingServiceSid() != null && !configuracion.getMessagingServiceSid().isBlank()) {
            throw new ResponseStatusException(CONFLICT, "Este tenant ya tiene un Messaging Service registrado");
        }

        String friendlyName = normalizarOpcional(request.friendlyName());
        if (friendlyName == null) {
            friendlyName = construirNombreMessagingService(empresa);
        }

        String inboundRequestUrl = normalizarOpcional(request.inboundRequestUrl());
        String statusCallbackUrl = normalizarOpcional(configuracion.getStatusCallbackUrl());

        ClienteWhatsappTwilio.MessagingServiceProvisionado messagingService = clienteWhatsappTwilio.provisionarMessagingService(
                empresaId,
                friendlyName,
                inboundRequestUrl,
                statusCallbackUrl
        );

        String snapshotAntes = snapshotConfiguracionWhatsapp(configuracion);
        configuracion.setMessagingServiceSid(messagingService.sid());
        ConfiguracionWhatsappEmpresaEntidad guardada = configuracionWhatsappEmpresaRepositorio.save(configuracion);
        registrarAuditoriaConfiguracion(
                empresaId,
                usuarioActorId,
                "WHATSAPP",
                "WHATSAPP_MESSAGING_SERVICE_PROVISIONADO",
                "Se creó un Messaging Service para el tenant",
                snapshotAntes,
                snapshotConfiguracionWhatsapp(guardada)
        );
        ConfiguracionWhatsappAdminResponse respuestaConfiguracion = mapearConfiguracionWhatsapp(guardada);

        return new ProvisionarMessagingServiceWhatsappResponse(
                true,
                "Messaging Service creado. El siguiente paso es asociar el sender de WhatsApp del tenant a este servicio.",
                messagingService.friendlyName(),
                messagingService.sid(),
                messagingService.inboundRequestUrl(),
                respuestaConfiguracion
        );
    }

    @Transactional
    public AsociarChannelSenderWhatsappResponse asociarChannelSenderWhatsapp(
            Long empresaId,
            Long usuarioActorId,
            AsociarChannelSenderWhatsappRequest request
    ) {
        ConfiguracionWhatsappEmpresaEntidad configuracion = configuracionWhatsappEmpresaRepositorio.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No existe configuracion de WhatsApp para este tenant"));

        String messagingServiceSid = normalizarOpcional(configuracion.getMessagingServiceSid());
        if (messagingServiceSid == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Primero debes crear o capturar el Messaging Service SID del tenant");
        }

        String snapshotAntes = snapshotConfiguracionWhatsapp(configuracion);
        String channelSenderSid = request.channelSenderSid().trim();
        clienteWhatsappTwilio.asociarChannelSenderAMessagingService(empresaId, messagingServiceSid, channelSenderSid);
        configuracion.setChannelSenderSid(channelSenderSid);
        ConfiguracionWhatsappEmpresaEntidad guardada = configuracionWhatsappEmpresaRepositorio.save(configuracion);
        registrarAuditoriaConfiguracion(
                empresaId,
                usuarioActorId,
                "WHATSAPP",
                "WHATSAPP_CHANNEL_SENDER_ASOCIADO",
                "Se asoció un Channel Sender al Messaging Service del tenant",
                snapshotAntes,
                snapshotConfiguracionWhatsapp(guardada)
        );
        ConfiguracionWhatsappAdminResponse respuestaConfiguracion = mapearConfiguracionWhatsapp(guardada);

        return new AsociarChannelSenderWhatsappResponse(
                true,
                "Channel Sender asociado correctamente al Messaging Service del tenant.",
                messagingServiceSid,
                channelSenderSid,
                respuestaConfiguracion
        );
    }

    @Transactional
    public DetectarChannelSenderWhatsappResponse detectarChannelSenderWhatsapp(Long empresaId, Long usuarioActorId) {
        ConfiguracionWhatsappEmpresaEntidad configuracion = configuracionWhatsappEmpresaRepositorio.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No existe configuracion de WhatsApp para este tenant"));

        String numeroRemitente = normalizarOpcional(configuracion.getNumeroRemitente());
        if (numeroRemitente == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Primero debes capturar el numero remitente del tenant para detectar su sender en Twilio");
        }

        ClienteWhatsappTwilio.SenderTwilioWhatsapp sender = clienteWhatsappTwilio.detectarSenderWhatsapp(empresaId, numeroRemitente);
        if (sender == null) {
            return new DetectarChannelSenderWhatsappResponse(
                    false,
                    "No se encontro un Channel Sender en Twilio que coincida con el numero remitente configurado.",
                    null,
                    null,
                    null,
                    null,
                    null,
                    mapearConfiguracionWhatsapp(configuracion)
            );
        }

        String snapshotAntes = snapshotConfiguracionWhatsapp(configuracion);
        configuracion.setChannelSenderSid(sender.sid());
        if ((configuracion.getSenderPhoneNumber() == null || configuracion.getSenderPhoneNumber().isBlank()) && sender.senderId() != null) {
            configuracion.setSenderPhoneNumber(sender.senderId());
        }
        if ((configuracion.getSenderStatus() == null || configuracion.getSenderStatus().isBlank()) && sender.status() != null) {
            configuracion.setSenderStatus(sender.status());
        }
        if ((configuracion.getSenderDisplayName() == null || configuracion.getSenderDisplayName().isBlank()) && sender.displayName() != null) {
            configuracion.setSenderDisplayName(sender.displayName());
        }
        if ((configuracion.getWabaId() == null || configuracion.getWabaId().isBlank()) && sender.wabaId() != null) {
            configuracion.setWabaId(sender.wabaId());
        }

        ConfiguracionWhatsappEmpresaEntidad guardada = configuracionWhatsappEmpresaRepositorio.save(configuracion);
        registrarAuditoriaConfiguracion(
                empresaId,
                usuarioActorId,
                "WHATSAPP",
                "WHATSAPP_CHANNEL_SENDER_DETECTADO",
                "Se detectó y guardó el sender de WhatsApp del tenant",
                snapshotAntes,
                snapshotConfiguracionWhatsapp(guardada)
        );
        return new DetectarChannelSenderWhatsappResponse(
                true,
                "Sender detectado correctamente en Twilio.",
                sender.sid(),
                sender.senderId(),
                sender.status(),
                sender.displayName(),
                sender.wabaId(),
                mapearConfiguracionWhatsapp(guardada)
        );
    }

    @Transactional
    public PruebaPlantillaWhatsappResponse probarPlantillaWhatsapp(Long empresaId, PruebaPlantillaWhatsappRequest request) {
        ConfiguracionWhatsappResolvida configuracion = servicioConfiguracionWhatsappEmpresa.resolver(empresaId);
        if (!configuracion.habilitado()) {
            throw new ResponseStatusException(BAD_REQUEST, "WhatsApp no esta habilitado o la configuracion minima esta incompleta para este tenant");
        }

        String plantillaSid = normalizarOpcional(request.plantillaSid()) != null
                ? request.plantillaSid().trim()
                : (configuracion.plantillaSolicitudConfirmacionSid() != null && !configuracion.plantillaSolicitudConfirmacionSid().isBlank()
                ? configuracion.plantillaSolicitudConfirmacionSid()
                : configuracion.plantillaCitaConfirmadaSid());

        if (plantillaSid == null || plantillaSid.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "No hay plantilla de WhatsApp configurada para la prueba");
        }

        ResultadoEnvioWhatsapp resultado = clienteWhatsappTwilio.enviarPlantilla(
                empresaId,
                request.telefonoDestino().trim(),
                plantillaSid,
                construirVariablesPruebaWhatsapp(empresaId, configuracion, plantillaSid, request)
        );

        registrarLogPruebaWhatsapp(empresaId, request, plantillaSid, resultado);
        return new PruebaPlantillaWhatsappResponse(
                true,
                "La prueba de WhatsApp fue enviada a Twilio" + (resultado.proveedorMensajeId() != null ? " con Message SID " + resultado.proveedorMensajeId() : "")
        );
    }

    private Map<String, String> construirVariablesPruebaWhatsapp(
            Long empresaId,
            ConfiguracionWhatsappResolvida configuracion,
            String plantillaSid,
            PruebaPlantillaWhatsappRequest request
    ) {
        String nombreEmpresa = empresaRepositorio.findById(empresaId)
                .map(EmpresaEntidad::getNombre)
                .filter(nombre -> !nombre.isBlank())
                .orElse("Tu negocio");
        String fechaHoraAmigable = "%s a las %s".formatted(request.fecha().trim(), request.hora().trim());

        if (plantillaSid.equals(configuracion.plantillaSolicitudConfirmacionSid())) {
            return Map.of(
                    "1", request.nombreCliente().trim(),
                    "2", nombreEmpresa,
                    "3", fechaHoraAmigable,
                    "4", "1"
            );
        }

        if (plantillaSid.equals(configuracion.plantillaCitaConfirmadaSid())) {
            return Map.of(
                    "1", request.nombreCliente().trim(),
                    "2", nombreEmpresa,
                    "3", request.fecha().trim(),
                    "4", request.hora().trim(),
                    "5", "Sucursal por definir"
            );
        }

        return Map.of(
                "1", request.nombreCliente().trim(),
                "2", request.fecha().trim(),
                "3", request.hora().trim()
        );
    }

    @Transactional(readOnly = true)
    public List<CitaClienteResponse> listar(Long empresaId) {
        return citaRepositorio.findByEmpresaIdOrderByInicioDesc(empresaId)
                .stream()
                .map(cita -> new CitaClienteResponse(
                        cita.getId(),
                        cita.getEstado(),
                        cita.getSucursalId(),
                        cita.getServicioId(),
                        cita.getPrestadorId(),
                        sucursalRepositorio.findById(cita.getSucursalId()).map(SucursalEntidad::getNombre).orElse("Sucursal"),
                        servicioRepositorio.findById(cita.getServicioId()).map(ServicioEntidad::getNombre).orElse("Servicio"),
                        prestadorServicioRepositorio.findById(cita.getPrestadorId()).map(PrestadorServicioEntidad::getNombreMostrar).orElse("Prestador"),
                        cita.getInicio().atZone(ZoneId.of(sucursalRepositorio.findById(cita.getSucursalId()).map(SucursalEntidad::getZonaHoraria).orElse("America/Mexico_City"))).toOffsetDateTime(),
                        cita.getFin().atZone(ZoneId.of(sucursalRepositorio.findById(cita.getSucursalId()).map(SucursalEntidad::getZonaHoraria).orElse("America/Mexico_City"))).toOffsetDateTime(),
                        cita.getPrecio(),
                        cita.getMoneda(),
                        cita.getNotas(),
                        false,
                        clienteRepositorio.findById(cita.getClienteId()).map(ClienteEntidad::getNombreCompleto).orElse("Cliente"),
                        usuarioRepositorio.findById(cita.getClienteId()).map(UsuarioEntidad::getCorreo).orElse(""),
                        clienteRepositorio.findById(cita.getClienteId()).map(ClienteEntidad::getTelefono).orElse("")
                ))
                .toList();
    }

    @Transactional
    public void cambiarEstadoCita(Long empresaId, Long usuarioAdminId, Long citaId, String nuevoEstado) {
        CitaEntidad cita = citaRepositorio.findByIdAndEmpresaId(citaId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La cita no existe para la empresa"));

        if (!List.of("CONFIRMADA", "FINALIZADA", "NO_ASISTIO", "CANCELADA").contains(nuevoEstado)) {
            throw new ResponseStatusException(BAD_REQUEST, "El estado solicitado no es valido para administracion");
        }

        String estadoAnterior = cita.getEstado();
        cita.setEstado(nuevoEstado);

        if ("CANCELADA".equals(nuevoEstado)) {
            cita.setCanceladaEn(java.time.LocalDateTime.now());
            cita.setMotivoCancelacion("Cancelada desde el panel administrativo");
        }

        citaRepositorio.save(cita);

        HistorialEstadoCitaEntidad historial = new HistorialEstadoCitaEntidad();
        historial.setCitaId(cita.getId());
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(nuevoEstado);
        historial.setCambiadoPorUsuarioId(usuarioAdminId);
        historial.setMotivo("Cambio de estado realizado por administracion");
        historialEstadoCitaRepositorio.save(historial);

        if (!nuevoEstado.equals(estadoAnterior)) {
            switch (nuevoEstado) {
                case "CONFIRMADA" -> servicioOutboxCorreoCitas.programarConfirmada(
                        empresaId, cita.getId(), cita.getInicio());
                case "CANCELADA" -> servicioOutboxCorreoCitas.programarCanceladaNegocio(
                        empresaId, cita.getId(), cita.getInicio());
                case "FINALIZADA" -> servicioOutboxCorreoCitas.programarGraciasVisita(
                        empresaId, cita.getId(), cita.getInicio());
                case "NO_ASISTIO" -> servicioOutboxCorreoCitas.programarNoAsistio(
                        empresaId, cita.getId(), cita.getInicio());
                default -> { }
            }
        }

        if ("CANCELADA".equals(nuevoEstado) && !"CANCELADA".equals(estadoAnterior)) {
            clienteRepositorio.findById(cita.getClienteId())
                    .filter(cliente -> cliente.isAceptaWhatsapp() && cliente.getTelefono() != null && !cliente.getTelefono().isBlank())
                    .ifPresent(cliente -> servicioOutboxWhatsappCitas.programarCancelacionNegocio(
                            empresaId,
                            cita.getId(),
                            cliente.getTelefono(),
                            cita.getInicio()
                    ));
        }

        if ("FINALIZADA".equals(nuevoEstado) && !"FINALIZADA".equals(estadoAnterior)) {
            clienteRepositorio.findById(cita.getClienteId())
                    .filter(cliente -> cliente.isAceptaWhatsapp() && cliente.getTelefono() != null && !cliente.getTelefono().isBlank())
                    .ifPresent(cliente -> servicioOutboxWhatsappCitas.programarGraciasVisita(
                            empresaId,
                            cita.getId(),
                            cliente.getTelefono(),
                            cita.getInicio()
                    ));
        }
    }

    @Transactional(readOnly = true)
    public List<SucursalAdminResponse> listarSucursales(Long empresaId) {
        return sucursalRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId)
                .stream()
                .map(this::mapearSucursal)
                .toList();
    }

    @Transactional
    public SucursalAdminResponse crearSucursal(Long empresaId, SucursalAdminRequest request) {
        SucursalEntidad sucursal = new SucursalEntidad();
        sucursal.setEmpresaId(empresaId);
        aplicarSucursal(sucursal, request);
        return mapearSucursal(sucursalRepositorio.save(sucursal));
    }

    @Transactional
    public SucursalAdminResponse actualizarSucursal(Long empresaId, Long sucursalId, SucursalAdminRequest request) {
        SucursalEntidad sucursal = sucursalRepositorio.findByIdAndEmpresaId(sucursalId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sucursal no existe para la empresa"));
        aplicarSucursal(sucursal, request);
        return mapearSucursal(sucursalRepositorio.save(sucursal));
    }

    @Transactional(readOnly = true)
    public List<GrupoServicioAdminResponse> listarGruposServicio(Long empresaId) {
        return grupoServicioRepositorio.findByEmpresaIdOrderByOrdenPublicoAscNombreAsc(empresaId)
                .stream()
                .map(this::mapearGrupoServicio)
                .toList();
    }

    @Transactional
    public GrupoServicioAdminResponse crearGrupoServicio(Long empresaId, GrupoServicioAdminRequest request) {
        GrupoServicioEntidad grupo = new GrupoServicioEntidad();
        grupo.setEmpresaId(empresaId);
        aplicarGrupoServicio(grupo, request);
        validarSlugGrupoDisponible(empresaId, grupo.getSlug(), null);
        return mapearGrupoServicio(grupoServicioRepositorio.save(grupo));
    }

    @Transactional
    public GrupoServicioAdminResponse actualizarGrupoServicio(Long empresaId, Long grupoId, GrupoServicioAdminRequest request) {
        GrupoServicioEntidad grupo = grupoServicioRepositorio.findByIdAndEmpresaId(grupoId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El grupo de servicios no existe para la empresa"));
        aplicarGrupoServicio(grupo, request);
        validarSlugGrupoDisponible(empresaId, grupo.getSlug(), grupoId);
        return mapearGrupoServicio(grupoServicioRepositorio.save(grupo));
    }

    @Transactional(readOnly = true)
    public List<SubgrupoServicioAdminResponse> listarSubgruposServicio(Long empresaId) {
        Map<Long, String> grupos = grupoServicioRepositorio.findByEmpresaIdOrderByOrdenPublicoAscNombreAsc(empresaId).stream()
                .collect(Collectors.toMap(GrupoServicioEntidad::getId, GrupoServicioEntidad::getNombre));
        return subgrupoServicioRepositorio.findByEmpresaIdOrderByOrdenPublicoAscNombreAsc(empresaId)
                .stream()
                .map(subgrupo -> mapearSubgrupoServicio(subgrupo, grupos.getOrDefault(subgrupo.getGrupoId(), "Grupo")))
                .toList();
    }

    @Transactional
    public SubgrupoServicioAdminResponse crearSubgrupoServicio(Long empresaId, SubgrupoServicioAdminRequest request) {
        GrupoServicioEntidad grupo = grupoServicioRepositorio.findByIdAndEmpresaId(request.grupoId(), empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El grupo indicado no existe para la empresa"));
        SubgrupoServicioEntidad subgrupo = new SubgrupoServicioEntidad();
        subgrupo.setEmpresaId(empresaId);
        aplicarSubgrupoServicio(subgrupo, request);
        validarSlugSubgrupoDisponible(empresaId, subgrupo.getGrupoId(), subgrupo.getSlug(), null);
        return mapearSubgrupoServicio(subgrupoServicioRepositorio.save(subgrupo), grupo.getNombre());
    }

    @Transactional
    public SubgrupoServicioAdminResponse actualizarSubgrupoServicio(Long empresaId, Long subgrupoId, SubgrupoServicioAdminRequest request) {
        GrupoServicioEntidad grupo = grupoServicioRepositorio.findByIdAndEmpresaId(request.grupoId(), empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El grupo indicado no existe para la empresa"));
        SubgrupoServicioEntidad subgrupo = subgrupoServicioRepositorio.findByIdAndEmpresaId(subgrupoId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El subgrupo indicado no existe para la empresa"));
        aplicarSubgrupoServicio(subgrupo, request);
        validarSlugSubgrupoDisponible(empresaId, subgrupo.getGrupoId(), subgrupo.getSlug(), subgrupoId);
        return mapearSubgrupoServicio(subgrupoServicioRepositorio.save(subgrupo), grupo.getNombre());
    }

    @Transactional(readOnly = true)
    public List<CatalogoSugeridoAdminResponse> listarCatalogosSugeridos() {
        return CATALOGOS_SUGERIDOS.stream()
                .map(catalogo -> new CatalogoSugeridoAdminResponse(
                        catalogo.id(),
                        catalogo.nombre(),
                        catalogo.descripcion(),
                        catalogo.grupos().size(),
                        catalogo.grupos().stream().mapToInt(grupo -> grupo.subgrupos().size()).sum(),
                        catalogo.grupos().stream()
                                .flatMap(grupo -> grupo.subgrupos().stream())
                                .mapToInt(subgrupo -> subgrupo.servicios().size())
                                .sum()
                ))
                .toList();
    }

    @Transactional
    public ImportarCatalogoSugeridoResponse importarCatalogoSugerido(Long empresaId, ImportarCatalogoSugeridoRequest request) {
        CatalogoSugeridoDef catalogo = CATALOGOS_SUGERIDOS.stream()
                .filter(item -> item.id().equalsIgnoreCase(request.sugerenciaId().trim()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sugerencia indicada no existe"));
        sucursalRepositorio.findByIdAndEmpresaId(request.sucursalId(), empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sucursal indicada no existe para la empresa"));

        int gruposCreados = 0;
        int subgruposCreados = 0;
        int serviciosCreados = 0;
        int serviciosReutilizados = 0;

        for (GrupoSugeridoDef grupoDef : catalogo.grupos()) {
            String grupoSlug = normalizarSlugCatalogo(null, grupoDef.nombre(), "grupo");
            GrupoServicioEntidad grupo = grupoServicioRepositorio.findByEmpresaIdAndSlug(empresaId, grupoSlug).orElse(null);
            if (grupo == null) {
                grupo = new GrupoServicioEntidad();
                grupo.setEmpresaId(empresaId);
                grupo.setNombre(grupoDef.nombre());
                grupo.setSlug(grupoSlug);
                grupo.setDescripcion(grupoDef.descripcion());
                grupo.setImagenUrl(grupoDef.imagenUrl());
                grupo.setIcono(grupoDef.icono());
                grupo.setOrdenPublico(grupoDef.ordenPublico());
                grupo.setActivo(true);
                grupo = grupoServicioRepositorio.save(grupo);
                gruposCreados++;
            }

            for (SubgrupoSugeridoDef subgrupoDef : grupoDef.subgrupos()) {
                String subgrupoSlug = normalizarSlugCatalogo(null, subgrupoDef.nombre(), "subgrupo");
                SubgrupoServicioEntidad subgrupo = subgrupoServicioRepositorio.findByEmpresaIdAndGrupoIdAndSlug(empresaId, grupo.getId(), subgrupoSlug).orElse(null);
                if (subgrupo == null) {
                    subgrupo = new SubgrupoServicioEntidad();
                    subgrupo.setEmpresaId(empresaId);
                    subgrupo.setGrupoId(grupo.getId());
                    subgrupo.setNombre(subgrupoDef.nombre());
                    subgrupo.setSlug(subgrupoSlug);
                    subgrupo.setDescripcion(subgrupoDef.descripcion());
                    subgrupo.setOrdenPublico(subgrupoDef.ordenPublico());
                    subgrupo.setActivo(true);
                    subgrupo = subgrupoServicioRepositorio.save(subgrupo);
                    subgruposCreados++;
                }

                for (ServicioSugeridoDef servicioDef : subgrupoDef.servicios()) {
                    String servicioSlug = normalizarSlugCatalogo(null, servicioDef.nombre(), "servicio");
                    ServicioEntidad existente = servicioRepositorio.findByEmpresaIdAndSlug(empresaId, servicioSlug).orElse(null);
                    if (existente != null) {
                        serviciosReutilizados++;
                        continue;
                    }
                    ServicioEntidad servicio = new ServicioEntidad();
                    servicio.setEmpresaId(empresaId);
                    servicio.setSucursalId(request.sucursalId());
                    servicio.setGrupoId(grupo.getId());
                    servicio.setSubgrupoId(subgrupo.getId());
                    servicio.setNombre(servicioDef.nombre());
                    servicio.setSlug(servicioSlug);
                    servicio.setDescripcion(servicioDef.descripcion());
                    servicio.setImagenUrl(null);
                    servicio.setDuracionMinutos(servicioDef.duracionMinutos());
                    servicio.setBufferAntesMinutos(servicioDef.bufferAntesMinutos());
                    servicio.setBufferDespuesMinutos(servicioDef.bufferDespuesMinutos());
                    servicio.setPrecio(servicioDef.precio());
                    servicio.setMoneda(servicioDef.moneda());
                    servicio.setOrdenPublico(servicioDef.ordenPublico());
                    servicio.setVisiblePublico(true);
                    servicio.setRequiereAnticipo(servicioDef.requiereAnticipo());
                    servicio.setAnticipoTipo(servicioDef.anticipoTipo());
                    servicio.setAnticipoValor(servicioDef.anticipoValor());
                    servicio.setActivo(true);
                    servicio = servicioRepositorio.save(servicio);
                    sincronizarAsignacionesServicio(empresaId, servicio.getId(), List.of(request.sucursalId()));
                    serviciosCreados++;
                }
            }
        }

        return new ImportarCatalogoSugeridoResponse(
                catalogo.id(),
                catalogo.nombre(),
                gruposCreados,
                subgruposCreados,
                serviciosCreados,
                serviciosReutilizados,
                "Se importó la sugerencia " + catalogo.nombre() + " al tenant."
        );
    }

    @Transactional(readOnly = true)
    public List<ServicioAdminResponse> listarServicios(Long empresaId) {
        Map<Long, String> sucursales = sucursalRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .collect(java.util.stream.Collectors.toMap(SucursalEntidad::getId, SucursalEntidad::getNombre));
        Map<Long, String> grupos = grupoServicioRepositorio.findByEmpresaIdOrderByOrdenPublicoAscNombreAsc(empresaId).stream()
                .collect(Collectors.toMap(GrupoServicioEntidad::getId, GrupoServicioEntidad::getNombre));
        Map<Long, String> subgrupos = subgrupoServicioRepositorio.findByEmpresaIdOrderByOrdenPublicoAscNombreAsc(empresaId).stream()
                .collect(Collectors.toMap(SubgrupoServicioEntidad::getId, SubgrupoServicioEntidad::getNombre));
        List<ServicioEntidad> servicios = servicioRepositorio.findByEmpresaIdOrderByOrdenPublicoAscNombreAsc(empresaId);
        Map<Long, List<ServicioSucursalEntidad>> asignacionesPorServicio = servicioSucursalRepositorio
                .findByEmpresaIdAndIdServicioIdInAndActivoTrue(
                        empresaId,
                        servicios.stream().map(ServicioEntidad::getId).toList()
                ).stream()
                .collect(Collectors.groupingBy(asignacion -> asignacion.getId().getServicioId()));
        return servicios
                .stream()
                .map(servicio -> {
                    List<ServicioSucursalEntidad> asignaciones = asignacionesPorServicio.get(servicio.getId());
                    Long sucursalId = resolverSucursalPrincipal(servicio, asignaciones);
                    List<Long> sucursalIds = resolverSucursalesAsignadas(servicio, asignaciones);
                    List<String> sucursalNombres = sucursalIds.stream()
                            .map(id -> sucursales.getOrDefault(id, "Sucursal"))
                            .toList();
                    return mapearServicio(
                            servicio,
                            sucursalId,
                            sucursales.getOrDefault(sucursalId, "Sucursal"),
                            sucursalIds,
                            sucursalNombres,
                            servicio.getGrupoId() != null ? grupos.get(servicio.getGrupoId()) : null,
                            servicio.getSubgrupoId() != null ? subgrupos.get(servicio.getSubgrupoId()) : null
                    );
                })
                .toList();
    }

    @Transactional
    public ServicioAdminResponse crearServicio(Long empresaId, ServicioAdminRequest request) {
        SucursalEntidad sucursal = sucursalRepositorio.findByIdAndEmpresaId(request.sucursalId(), empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sucursal indicada no existe para la empresa"));
        List<Long> sucursalIds = validarSucursalesServicio(empresaId, request.sucursalId(), request.sucursalIds());
        GrupoServicioEntidad grupo = validarGrupoServicio(empresaId, request.grupoId());
        SubgrupoServicioEntidad subgrupo = validarSubgrupoServicio(empresaId, grupo, request.subgrupoId());
        ServicioEntidad servicio = new ServicioEntidad();
        servicio.setEmpresaId(empresaId);
        aplicarServicio(servicio, request);
        validarSlugServicioDisponible(empresaId, servicio.getSlug(), null);
        servicio = servicioRepositorio.save(servicio);
        sincronizarAsignacionesServicio(empresaId, servicio.getId(), sucursalIds);
        return mapearServicio(
                servicio,
                request.sucursalId(),
                sucursal.getNombre(),
                sucursalIds,
                sucursalIds.stream().map(id -> sucursalRepositorio.findById(id).map(SucursalEntidad::getNombre).orElse("Sucursal")).toList(),
                grupo != null ? grupo.getNombre() : null,
                subgrupo != null ? subgrupo.getNombre() : null
        );
    }

    @Transactional
    public ServicioAdminResponse actualizarServicio(Long empresaId, Long servicioId, ServicioAdminRequest request) {
        ServicioEntidad servicio = servicioRepositorio.findByIdAndEmpresaId(servicioId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El servicio no existe para la empresa"));
        SucursalEntidad sucursal = sucursalRepositorio.findByIdAndEmpresaId(request.sucursalId(), empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sucursal indicada no existe para la empresa"));
        List<Long> sucursalIds = validarSucursalesServicio(empresaId, request.sucursalId(), request.sucursalIds());
        GrupoServicioEntidad grupo = validarGrupoServicio(empresaId, request.grupoId());
        SubgrupoServicioEntidad subgrupo = validarSubgrupoServicio(empresaId, grupo, request.subgrupoId());
        aplicarServicio(servicio, request);
        validarSlugServicioDisponible(empresaId, servicio.getSlug(), servicioId);
        servicio = servicioRepositorio.save(servicio);
        sincronizarAsignacionesServicio(empresaId, servicio.getId(), sucursalIds);
        return mapearServicio(
                servicio,
                request.sucursalId(),
                sucursal.getNombre(),
                sucursalIds,
                sucursalIds.stream().map(id -> sucursalRepositorio.findById(id).map(SucursalEntidad::getNombre).orElse("Sucursal")).toList(),
                grupo != null ? grupo.getNombre() : null,
                subgrupo != null ? subgrupo.getNombre() : null
        );
    }

    @Transactional(readOnly = true)
    public List<PrestadorAdminResponse> listarPrestadores(Long empresaId) {
        List<SucursalEntidad> sucursales = sucursalRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId);
        if (sucursales.isEmpty()) {
            return List.of();
        }

        Map<Long, String> sucursalNombres = sucursales.stream()
                .collect(Collectors.toMap(SucursalEntidad::getId, SucursalEntidad::getNombre));
        List<Long> sucursalIds = sucursales.stream().map(SucursalEntidad::getId).toList();
        List<PrestadorServicioEntidad> prestadores = prestadorServicioRepositorio.findBySucursalIdInOrderByNombreMostrarAsc(sucursalIds);
        if (prestadores.isEmpty()) {
            return List.of();
        }

        List<Long> prestadorIds = prestadores.stream().map(PrestadorServicioEntidad::getUsuarioId).toList();
        Map<Long, UsuarioEntidad> usuarios = usuarioRepositorio.findByEmpresaIdAndIdIn(empresaId, prestadorIds).stream()
                .collect(Collectors.toMap(UsuarioEntidad::getId, usuario -> usuario));
        Map<Long, ServicioEntidad> servicios = servicioRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .collect(Collectors.toMap(ServicioEntidad::getId, servicio -> servicio));
        Map<Long, List<AsignacionServicioPrestadorEntidad>> asignacionesPorPrestador =
                asignacionServicioPrestadorRepositorio.findByIdPrestadorIdIn(prestadorIds).stream()
                        .filter(AsignacionServicioPrestadorEntidad::isActiva)
                        .collect(Collectors.groupingBy(asignacion -> asignacion.getId().getPrestadorId()));

        return prestadores.stream()
                .map(prestador -> mapearPrestador(
                        prestador,
                        usuarios.get(prestador.getUsuarioId()),
                        sucursalNombres.getOrDefault(prestador.getSucursalId(), "Sucursal"),
                        asignacionesPorPrestador.getOrDefault(prestador.getUsuarioId(), List.of()),
                        servicios
                ))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UsuarioInternoAdminResponse> listarUsuariosInternos(Long empresaId) {
        List<UsuarioEntidad> usuarios = usuarioRepositorio.findByEmpresaIdOrderByCorreoAsc(empresaId);
        if (usuarios.isEmpty()) {
            return List.of();
        }

        List<Long> usuarioIds = usuarios.stream().map(UsuarioEntidad::getId).toList();
        Map<Long, List<UsuarioRolEmpresaEntidad>> rolesPorUsuario = servicioRolesEmpresa.listarAsignacionesUsuarios(empresaId, usuarioIds).stream()
                .collect(Collectors.groupingBy(usuarioRol -> usuarioRol.getUsuario().getId()));
        Map<Long, List<String>> permisosPorRol = servicioRolesEmpresa.obtenerPermisosPorRolEmpresa(
                rolesPorUsuario.values().stream()
                        .flatMap(List::stream)
                        .map(UsuarioRolEmpresaEntidad::getRolEmpresa)
                        .map(RolEmpresaEntidad::getId)
                        .distinct()
                        .toList()
        );
        Map<Long, List<String>> permisosDirectosPorUsuario = servicioRolesEmpresa.obtenerPermisosDirectosUsuarios(empresaId, usuarioIds);
        Map<Long, UsuarioInternoPerfilEntidad> perfilesPorUsuario = usuarioInternoPerfilRepositorio.findByUsuarioIdIn(usuarioIds).stream()
                .collect(Collectors.toMap(UsuarioInternoPerfilEntidad::getUsuarioId, perfil -> perfil));
        Map<Long, List<Long>> sucursalesScopePorUsuario = usuarioInternoSucursalRepositorio.findByUsuario_IdIn(usuarioIds).stream()
                .filter(asignacion -> asignacion.getSucursal().getEmpresaId().equals(empresaId))
                .collect(Collectors.groupingBy(
                        asignacion -> asignacion.getUsuario().getId(),
                        Collectors.mapping(asignacion -> asignacion.getSucursal().getId(), Collectors.collectingAndThen(Collectors.toList(), ids -> ids.stream().distinct().sorted().toList()))
                ));
        Map<Long, String> sucursalNombres = sucursalRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .collect(Collectors.toMap(SucursalEntidad::getId, SucursalEntidad::getNombre));

        return usuarios.stream()
                .map(usuario -> mapearUsuarioInterno(
                        usuario,
                        perfilesPorUsuario.get(usuario.getId()),
                        rolesPorUsuario.getOrDefault(usuario.getId(), List.of()),
                        sucursalesScopePorUsuario.getOrDefault(usuario.getId(), List.of()),
                        sucursalNombres,
                        permisosPorRol,
                        permisosDirectosPorUsuario.getOrDefault(usuario.getId(), List.of())
                ))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RolInternoAdminResponse> listarRolesInternos(Long empresaId) {
        if (empresaId == null) {
            return List.of();
        }

        List<RolEmpresaEntidad> roles = servicioRolesEmpresa.listarRolesEmpresa(empresaId);
        if (roles.isEmpty()) {
            return List.of();
        }

        Map<Long, List<String>> permisosPorRol = servicioRolesEmpresa.obtenerPermisosPorRolEmpresa(
                roles.stream().map(RolEmpresaEntidad::getId).toList()
        );
        Map<Long, Long> usuariosAsignadosPorRol = roles.stream()
                .collect(Collectors.toMap(RolEmpresaEntidad::getId, rol -> usuarioRolEmpresaRepositorio.countByRolEmpresa_Id(rol.getId())));

        return roles.stream()
                .map(rol -> mapearRolInterno(rol, permisosPorRol, usuariosAsignadosPorRol))
                .filter(rol -> esRolInternoAsignable(rol.permisos()))
                .sorted(Comparator.comparing(RolInternoAdminResponse::nombre))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PermisoAdminResponse> listarPermisos(Long empresaId) {
        validarEmpresaExiste(empresaId);
        return permisoRepositorio.findAllByOrderByNombreAsc().stream()
                .map(permiso -> new PermisoAdminResponse(
                        permiso.getId(),
                        permiso.getCodigo(),
                        permiso.getNombre(),
                        permiso.getDescripcion()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlantillaRolInternoAdminResponse> listarPlantillasRolesInternos(Long empresaId) {
        validarEmpresaExiste(empresaId);
        Set<String> permisosDisponibles = permisoRepositorio.findAllByOrderByNombreAsc().stream()
                .map(PermisoEntidad::getCodigo)
                .collect(Collectors.toCollection(HashSet::new));

        return List.of(
                plantillaRolInterno(
                        "RECEPCION",
                        "Recepción",
                        "Atiende agenda, búsqueda de clientes y check-in sin acceder a configuración administrativa.",
                        "Operación",
                        List.of("RECEPCION_ACCESO", "RECEPCION_CLIENTES_VER", "RECEPCION_CITAS_GESTIONAR", "RECEPCION_CHECKIN"),
                        permisosDisponibles
                ),
                plantillaRolInterno(
                        "CAJA",
                        "Caja",
                        "Opera cobros, sesiones y movimientos de caja sin exponer módulos administrativos.",
                        "Operación",
                        List.of("CAJA_ACCESO", "CAJA_COBRAR", "CAJA_SESION_GESTIONAR", "CAJA_MOVIMIENTOS_GESTIONAR"),
                        permisosDisponibles
                ),
                plantillaRolInterno(
                        "RECEPCION_CAJA",
                        "Recepción y caja",
                        "Pensado para mostrador cuando la misma persona agenda, confirma llegada y cobra.",
                        "Operación",
                        List.of(
                                "RECEPCION_ACCESO",
                                "RECEPCION_CLIENTES_VER",
                                "RECEPCION_CITAS_GESTIONAR",
                                "RECEPCION_CHECKIN",
                                "CAJA_ACCESO",
                                "CAJA_COBRAR"
                        ),
                        permisosDisponibles
                ),
                plantillaRolInterno(
                        "STAFF_OPERATIVO",
                        "Staff operativo",
                        "Consulta agenda propia, gestiona citas del día y administra su disponibilidad.",
                        "Staff",
                        List.of("STAFF_PANEL_ACCESO", "STAFF_AGENDA_VER", "STAFF_CITAS_GESTIONAR", "STAFF_DISPONIBILIDAD_GESTIONAR"),
                        permisosDisponibles
                ),
                plantillaRolInterno(
                        "SUPERVISOR_OPERATIVO",
                        "Supervisor operativo",
                        "Supervisa operación, reportes y gestión de citas sin tocar toda la configuración del negocio.",
                        "Supervisión",
                        List.of("PANEL_ADMIN_ACCESO", "CITAS_ADMIN_GESTIONAR", "PRESTADORES_GESTIONAR", "SERVICIOS_GESTIONAR", "REPORTES_ADMIN_VER"),
                        permisosDisponibles
                ),
                plantillaRolInterno(
                        "ADMIN_OPERATIVO",
                        "Administrador operativo",
                        "Gestiona módulos operativos, usuarios internos y canales sin ser necesariamente dueño total del tenant.",
                        "Administración",
                        List.of(
                                "PANEL_ADMIN_ACCESO",
                                "CITAS_ADMIN_GESTIONAR",
                                "SUCURSALES_GESTIONAR",
                                "SERVICIOS_GESTIONAR",
                                "PRESTADORES_GESTIONAR",
                                "USUARIOS_INTERNOS_GESTIONAR",
                                "REPORTES_ADMIN_VER",
                                "WHATSAPP_CONFIGURAR",
                                "CONFIGURACION_EMPRESA_GESTIONAR"
                        ),
                        permisosDisponibles
                )
        );
    }

    @Transactional(readOnly = true)
    public List<AuditoriaRolInternoAdminResponse> listarAuditoriaRolesInternos(Long empresaId) {
        validarEmpresaExiste(empresaId);
        return auditoriaRolEmpresaRepositorio.findTop30ByEmpresaIdOrderByCreadoEnDesc(empresaId).stream()
                .map(item -> new AuditoriaRolInternoAdminResponse(
                        item.getId(),
                        item.getAccion(),
                        item.getResumen(),
                        item.getActorCorreo(),
                        item.getRolCodigo(),
                        item.getRolNombre(),
                        item.getCreadoEn()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditoriaConfiguracionAdminResponse> listarAuditoriaConfiguracion(Long empresaId) {
        validarEmpresaExiste(empresaId);
        return auditoriaConfiguracionEmpresaRepositorio.findTop30ByEmpresaIdOrderByCreadoEnDesc(empresaId).stream()
                .map(item -> new AuditoriaConfiguracionAdminResponse(
                        item.getId(),
                        item.getModulo(),
                        item.getAccion(),
                        item.getResumen(),
                        item.getActorCorreo(),
                        item.getCreadoEn()
                ))
                .toList();
    }

    @Transactional
    public RolInternoAdminResponse crearRolInterno(Long empresaId, Long usuarioActorId, RolInternoAdminRequest request) {
        validarEmpresaExiste(empresaId);
        String codigo = normalizarCodigoRolEmpresa(request.codigo());
        if (rolEmpresaRepositorio.existsByEmpresaIdAndCodigo(empresaId, codigo)) {
            throw new ResponseStatusException(CONFLICT, "Ya existe un rol con ese codigo en la empresa");
        }

        List<String> permisos = normalizarPermisosRol(request.permisos());
        validarPermisosRolInterno(permisos);

        RolEmpresaEntidad rolEmpresa = new RolEmpresaEntidad();
        rolEmpresa.setEmpresaId(empresaId);
        rolEmpresa.setRolBase(null);
        rolEmpresa.setCodigo(codigo);
        rolEmpresa.setNombre(request.nombre().trim());
        rolEmpresa.setDescripcion(normalizarOpcional(request.descripcion()));
        rolEmpresa.setActivo(request.activo());
        rolEmpresa.setEditable(true);
        rolEmpresa = rolEmpresaRepositorio.save(rolEmpresa);

        guardarPermisosRolEmpresa(rolEmpresa, permisos);
        registrarAuditoriaRol(empresaId, usuarioActorId, "ROL_CREADO", "Se creó el rol " + rolEmpresa.getNombre(), rolEmpresa, null, snapshotRol(rolEmpresa, permisos));
        return construirRolInternoResponse(rolEmpresa);
    }

    @Transactional
    public RolInternoAdminResponse actualizarRolInterno(Long empresaId, Long usuarioActorId, Long rolEmpresaId, RolInternoAdminRequest request) {
        validarEmpresaExiste(empresaId);
        RolEmpresaEntidad rolEmpresa = rolEmpresaRepositorio.findById(rolEmpresaId)
                .filter(rol -> rol.getEmpresaId().equals(empresaId))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El rol interno no existe para la empresa"));
        long usuariosAsignados = usuarioRolEmpresaRepositorio.countByRolEmpresa_Id(rolEmpresaId);
        List<String> permisosAntes = servicioRolesEmpresa.obtenerPermisosPorRolEmpresa(List.of(rolEmpresa.getId()))
                .getOrDefault(rolEmpresa.getId(), List.of())
                .stream()
                .sorted()
                .toList();
        String snapshotAntes = snapshotRol(rolEmpresa, permisosAntes);

        String codigo = normalizarCodigoRolEmpresa(request.codigo());
        if (!rolEmpresa.isEditable() && !rolEmpresa.getCodigo().equals(codigo)) {
            throw new ResponseStatusException(BAD_REQUEST, "El codigo del rol base no se puede modificar");
        }
        if (!rolEmpresa.getCodigo().equals(codigo) && rolEmpresaRepositorio.existsByEmpresaIdAndCodigo(empresaId, codigo)) {
            throw new ResponseStatusException(CONFLICT, "Ya existe otro rol con ese codigo en la empresa");
        }

        List<String> permisos = normalizarPermisosRol(request.permisos());
        validarPermisosRolInterno(permisos);
        if (!request.activo() && usuariosAsignados > 0) {
            throw new ResponseStatusException(CONFLICT, "No puedes desactivar un rol que todavía tiene usuarios asignados");
        }

        rolEmpresa.setCodigo(codigo);
        rolEmpresa.setNombre(request.nombre().trim());
        rolEmpresa.setDescripcion(normalizarOpcional(request.descripcion()));
        rolEmpresa.setActivo(request.activo());
        rolEmpresa = rolEmpresaRepositorio.save(rolEmpresa);

        guardarPermisosRolEmpresa(rolEmpresa, permisos);
        registrarAuditoriaRol(empresaId, usuarioActorId, "ROL_ACTUALIZADO", "Se actualizó el rol " + rolEmpresa.getNombre(), rolEmpresa, snapshotAntes, snapshotRol(rolEmpresa, permisos));
        return construirRolInternoResponse(rolEmpresa);
    }

    @Transactional
    public RolInternoAdminResponse clonarRolInterno(Long empresaId, Long usuarioActorId, Long rolEmpresaOrigenId, RolInternoAdminRequest request) {
        validarEmpresaExiste(empresaId);
        RolEmpresaEntidad rolOrigen = rolEmpresaRepositorio.findById(rolEmpresaOrigenId)
                .filter(rol -> rol.getEmpresaId().equals(empresaId))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El rol origen no existe para la empresa"));

        List<String> permisosOrigen = servicioRolesEmpresa
                .obtenerPermisosPorRolEmpresa(List.of(rolOrigen.getId()))
                .getOrDefault(rolOrigen.getId(), List.of())
                .stream()
                .sorted()
                .toList();

        if (!esRolInternoAsignable(permisosOrigen)) {
            throw new ResponseStatusException(BAD_REQUEST, "Solo puedes clonar roles internos operativos");
        }

        String codigo = normalizarCodigoRolEmpresa(request.codigo());
        if (rolEmpresaRepositorio.existsByEmpresaIdAndCodigo(empresaId, codigo)) {
            throw new ResponseStatusException(CONFLICT, "Ya existe un rol con ese codigo en la empresa");
        }

        List<String> permisos = request.permisos() == null || request.permisos().isEmpty()
                ? permisosOrigen
                : normalizarPermisosRol(request.permisos());
        validarPermisosRolInterno(permisos);

        RolEmpresaEntidad rolEmpresa = new RolEmpresaEntidad();
        rolEmpresa.setEmpresaId(empresaId);
        rolEmpresa.setRolBase(rolOrigen.getRolBase());
        rolEmpresa.setCodigo(codigo);
        rolEmpresa.setNombre(request.nombre().trim());
        rolEmpresa.setDescripcion(normalizarOpcional(request.descripcion()));
        rolEmpresa.setActivo(request.activo());
        rolEmpresa.setEditable(true);
        rolEmpresa = rolEmpresaRepositorio.save(rolEmpresa);

        guardarPermisosRolEmpresa(rolEmpresa, permisos);
        registrarAuditoriaRol(
                empresaId,
                usuarioActorId,
                "ROL_CLONADO",
                "Se clonó el rol " + rolOrigen.getNombre() + " como " + rolEmpresa.getNombre(),
                rolEmpresa,
                snapshotRol(rolOrigen, permisosOrigen),
                snapshotRol(rolEmpresa, permisos)
        );
        return construirRolInternoResponse(rolEmpresa);
    }

    @Transactional
    public void eliminarRolInterno(Long empresaId, Long usuarioActorId, Long rolEmpresaId) {
        validarEmpresaExiste(empresaId);
        RolEmpresaEntidad rolEmpresa = rolEmpresaRepositorio.findById(rolEmpresaId)
                .filter(rol -> rol.getEmpresaId().equals(empresaId))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El rol interno no existe para la empresa"));

        if (!rolEmpresa.isEditable()) {
            throw new ResponseStatusException(BAD_REQUEST, "No puedes eliminar un rol base del sistema");
        }
        long usuariosAsignados = usuarioRolEmpresaRepositorio.countByRolEmpresa_Id(rolEmpresaId);
        List<String> permisosAntes = servicioRolesEmpresa.obtenerPermisosPorRolEmpresa(List.of(rolEmpresa.getId()))
                .getOrDefault(rolEmpresa.getId(), List.of())
                .stream()
                .sorted()
                .toList();
        if (usuariosAsignados > 0) {
            throw new ResponseStatusException(CONFLICT, "No puedes eliminar un rol que todavía tiene usuarios asignados");
        }

        registrarAuditoriaRol(empresaId, usuarioActorId, "ROL_ELIMINADO", "Se eliminó el rol " + rolEmpresa.getNombre(), rolEmpresa, snapshotRol(rolEmpresa, permisosAntes), null);
        rolEmpresaPermisoRepositorio.deleteByRolEmpresa_Id(rolEmpresaId);
        usuarioRolEmpresaRepositorio.deleteByRolEmpresa_Id(rolEmpresaId);
        rolEmpresaRepositorio.delete(rolEmpresa);
    }

    @Transactional
    public UsuarioInternoAdminResponse crearUsuarioInterno(Long empresaId, Long usuarioActorId, UsuarioInternoAdminRequest request) {
        RolEmpresaEntidad rolEmpresa = resolverRolUsuarioInterno(empresaId, request.rolEmpresaId());
        validarSucursalUsuarioInterno(empresaId, request.sucursalId());
        List<Long> sucursalesPermitidas = validarSucursalesPermitidasUsuarioInterno(empresaId, request.sucursalIds(), request.sucursalId());
        List<String> permisosDirectos = normalizarPermisosDirectosUsuario(request.permisosDirectos(), rolEmpresa);

        String correoNormalizado = request.correo().trim().toLowerCase();
        usuarioRepositorio.findByEmpresaIdAndCorreo(empresaId, correoNormalizado)
                .ifPresent(usuario -> {
                    throw new ResponseStatusException(CONFLICT, "Ya existe un usuario con ese correo en la empresa");
                });

        if (request.contrasenaTemporal() == null || request.contrasenaTemporal().isBlank()) {
            throw new ResponseStatusException(CONFLICT, "Debes indicar una contrasena temporal para el usuario interno");
        }

        UsuarioEntidad usuario = new UsuarioEntidad();
        usuario.setEmpresaId(empresaId);
        usuario.setCorreo(correoNormalizado);
        usuario.setContrasenaHash(passwordEncoder.encode(request.contrasenaTemporal()));
        usuario.setHabilitado(request.activo());
        usuario.setBloqueado(false);
        usuario = usuarioRepositorio.save(usuario);

        UsuarioInternoPerfilEntidad perfil = new UsuarioInternoPerfilEntidad();
        perfil.setUsuarioId(usuario.getId());
        aplicarUsuarioInterno(perfil, request);
        usuarioInternoPerfilRepositorio.save(perfil);
        sincronizarSucursalesPermitidas(usuario, sucursalesPermitidas);

        asignarRolUsuarioInterno(usuario, empresaId, rolEmpresa);
        guardarPermisosDirectosUsuario(usuario, permisosDirectos);
        registrarAuditoriaRol(
                empresaId,
                usuarioActorId,
                "USUARIO_INTERNO_CREADO",
                "Se creó el usuario interno " + perfil.getNombreCompleto() + " con el rol " + rolEmpresa.getNombre(),
                rolEmpresa,
                null,
                snapshotUsuarioInterno(usuario, perfil, rolEmpresa, sucursalesPermitidas, permisosDirectos)
        );
        return construirUsuarioInternoResponse(usuario, perfil, rolEmpresa, empresaId);
    }

    @Transactional(readOnly = true)
    public void validarInvitacionUsuarioInterno(Long empresaId, UsuarioInternoAdminRequest request) {
        RolEmpresaEntidad rolEmpresa = resolverRolUsuarioInterno(empresaId, request.rolEmpresaId());
        validarSucursalUsuarioInterno(empresaId, request.sucursalId());
        validarSucursalesPermitidasUsuarioInterno(empresaId, request.sucursalIds(), request.sucursalId());
        normalizarPermisosDirectosUsuario(request.permisosDirectos(), rolEmpresa);
        String correoNormalizado = request.correo().trim().toLowerCase();
        usuarioRepositorio.findByEmpresaIdAndCorreo(empresaId, correoNormalizado).ifPresent(usuario -> {
            throw new ResponseStatusException(CONFLICT, "Ya existe un usuario con ese correo en la empresa");
        });
    }

    @Transactional
    public UsuarioInternoAdminResponse actualizarUsuarioInterno(Long empresaId, Long usuarioActorId, Long usuarioId, UsuarioInternoAdminRequest request) {
        RolEmpresaEntidad rolEmpresa = resolverRolUsuarioInterno(empresaId, request.rolEmpresaId());
        validarSucursalUsuarioInterno(empresaId, request.sucursalId());
        List<Long> sucursalesPermitidas = validarSucursalesPermitidasUsuarioInterno(empresaId, request.sucursalIds(), request.sucursalId());
        List<String> permisosDirectos = normalizarPermisosDirectosUsuario(request.permisosDirectos(), rolEmpresa);

        UsuarioEntidad usuario = usuarioRepositorio.findByIdAndEmpresaId(usuarioId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El usuario interno no existe para la empresa"));
        UsuarioInternoPerfilEntidad perfil = usuarioInternoPerfilRepositorio.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El perfil interno no existe"));
        RolEmpresaEntidad rolActual = usuarioRolEmpresaRepositorio.findByUsuario_IdAndRolEmpresa_EmpresaId(usuarioId, empresaId).stream()
                .map(UsuarioRolEmpresaEntidad::getRolEmpresa)
                .findFirst()
                .orElse(null);
        List<Long> sucursalesActuales = usuarioInternoSucursalRepositorio.findByUsuario_Id(usuarioId).stream()
                .map(asignacion -> asignacion.getSucursal().getId())
                .sorted()
                .toList();
        List<String> permisosDirectosActuales = servicioRolesEmpresa.obtenerPermisosDirectosUsuario(empresaId, usuarioId);
        String snapshotAntes = snapshotUsuarioInterno(usuario, perfil, rolActual, sucursalesActuales, permisosDirectosActuales);

        String correoNormalizado = request.correo().trim().toLowerCase();
        usuarioRepositorio.findByEmpresaIdAndCorreo(empresaId, correoNormalizado)
                .filter(existente -> !existente.getId().equals(usuarioId))
                .ifPresent(existente -> {
                    throw new ResponseStatusException(CONFLICT, "Ya existe otro usuario con ese correo en la empresa");
                });

        usuario.setCorreo(correoNormalizado);
        if (request.contrasenaTemporal() != null && !request.contrasenaTemporal().isBlank()) {
            usuario.setContrasenaHash(passwordEncoder.encode(request.contrasenaTemporal()));
        }
        usuario.setHabilitado(request.activo());
        usuario.setBloqueado(false);
        usuarioRepositorio.save(usuario);

        aplicarUsuarioInterno(perfil, request);
        usuarioInternoPerfilRepositorio.save(perfil);
        sincronizarSucursalesPermitidas(usuario, sucursalesPermitidas);

        reasignarRolUsuarioInterno(usuario, empresaId, rolEmpresa);
        guardarPermisosDirectosUsuario(usuario, permisosDirectos);
        registrarAuditoriaRol(
                empresaId,
                usuarioActorId,
                "USUARIO_INTERNO_ACTUALIZADO",
                "Se actualizó el acceso interno de " + perfil.getNombreCompleto() + " al rol " + rolEmpresa.getNombre(),
                rolEmpresa,
                snapshotAntes,
                snapshotUsuarioInterno(usuario, perfil, rolEmpresa, sucursalesPermitidas, permisosDirectos)
        );
        return construirUsuarioInternoResponse(usuario, perfil, rolEmpresa, empresaId);
    }

    @Transactional
    public PrestadorAdminResponse crearPrestador(Long empresaId, PrestadorAdminRequest request) {
        if (request.contrasenaTemporal() == null || request.contrasenaTemporal().isBlank()) {
            throw new ResponseStatusException(CONFLICT, "Debes indicar una contrasena temporal para el prestador");
        }

        SucursalEntidad sucursal = sucursalRepositorio.findByIdAndEmpresaId(request.sucursalId(), empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sucursal indicada no existe para la empresa"));

        usuarioRepositorio.findByEmpresaIdAndCorreo(empresaId, request.correo().trim().toLowerCase())
                .ifPresent(usuario -> {
                    throw new ResponseStatusException(CONFLICT, "Ya existe un usuario con ese correo en la empresa");
                });

        Map<Long, ServicioEntidad> serviciosSeleccionados = validarServiciosAsignados(empresaId, request.sucursalId(), request.servicioIds());

        UsuarioEntidad usuario = new UsuarioEntidad();
        usuario.setEmpresaId(empresaId);
        usuario.setCorreo(request.correo().trim().toLowerCase());
        usuario.setContrasenaHash(passwordEncoder.encode(request.contrasenaTemporal()));
        usuario.setHabilitado(true);
        usuario.setBloqueado(false);
        usuario = usuarioRepositorio.save(usuario);

        asignarRolStaff(usuario, empresaId);

        PrestadorServicioEntidad prestador = new PrestadorServicioEntidad();
        prestador.setUsuarioId(usuario.getId());
        aplicarPrestador(prestador, request);
        prestador = prestadorServicioRepositorio.save(prestador);

        sincronizarAsignaciones(prestador.getUsuarioId(), request.servicioIds());
        return mapearPrestador(prestador, usuario, sucursal.getNombre(), asignacionServicioPrestadorRepositorio.findByIdPrestadorId(prestador.getUsuarioId()), serviciosSeleccionados);
    }

    @Transactional
    public PrestadorAdminResponse actualizarPrestador(Long empresaId, Long prestadorId, PrestadorAdminRequest request) {
        UsuarioEntidad usuario = usuarioRepositorio.findByIdAndEmpresaId(prestadorId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El usuario del prestador no existe para la empresa"));
        PrestadorServicioEntidad prestador = prestadorServicioRepositorio.findById(prestadorId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El prestador no existe"));
        SucursalEntidad sucursal = sucursalRepositorio.findByIdAndEmpresaId(request.sucursalId(), empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sucursal indicada no existe para la empresa"));

        String correoNormalizado = request.correo().trim().toLowerCase();
        usuarioRepositorio.findByEmpresaIdAndCorreo(empresaId, correoNormalizado)
                .filter(existente -> !existente.getId().equals(prestadorId))
                .ifPresent(existente -> {
                    throw new ResponseStatusException(CONFLICT, "Ya existe otro usuario con ese correo en la empresa");
                });

        Map<Long, ServicioEntidad> serviciosSeleccionados = validarServiciosAsignados(empresaId, request.sucursalId(), request.servicioIds());

        usuario.setCorreo(correoNormalizado);
        if (request.contrasenaTemporal() != null && !request.contrasenaTemporal().isBlank()) {
            usuario.setContrasenaHash(passwordEncoder.encode(request.contrasenaTemporal()));
        }
        usuario.setHabilitado(request.activo());
        usuario.setBloqueado(false);
        usuarioRepositorio.save(usuario);

        aplicarPrestador(prestador, request);
        prestador = prestadorServicioRepositorio.save(prestador);

        sincronizarAsignaciones(prestadorId, request.servicioIds());
        return mapearPrestador(prestador, usuario, sucursal.getNombre(), asignacionServicioPrestadorRepositorio.findByIdPrestadorId(prestadorId), serviciosSeleccionados);
    }

    private void aplicarSucursal(SucursalEntidad sucursal, SucursalAdminRequest request) {
        sucursal.setNombre(request.nombre().trim());
        sucursal.setDireccion(request.direccion() != null ? request.direccion().trim() : null);
        sucursal.setTelefono(request.telefono() != null ? request.telefono().trim() : null);
        sucursal.setZonaHoraria(request.zonaHoraria().trim());
        sucursal.setActiva(request.activa());
    }

    private ConfiguracionCorreoAdminResponse mapearConfiguracionCorreo(ConfiguracionCorreoEmpresaEntidad configuracion) {
        String smtpPassword = configuracion.getSmtpPassword();
        boolean smtpConfigurada = smtpPassword != null && !smtpPassword.isBlank();
        boolean smtpCifrada = smtpConfigurada && smtpPassword.startsWith("enc:v1:");
        String graphClientSecret = configuracion.getGraphClientSecret();
        boolean graphConfigurada = graphClientSecret != null && !graphClientSecret.isBlank();
        boolean graphCifrada = graphConfigurada && graphClientSecret.startsWith("enc:v1:");
        String graphPrivateKeyPem = configuracion.getGraphPrivateKeyPem();
        boolean graphPrivateKeyConfigurada = graphPrivateKeyPem != null && !graphPrivateKeyPem.isBlank();
        boolean graphPrivateKeyCifrada = graphPrivateKeyConfigurada && graphPrivateKeyPem.startsWith("enc:v1:");

        return new ConfiguracionCorreoAdminResponse(
                configuracion.isHabilitado(),
                configuracion.getProveedor(),
                configuracion.getRemitente(),
                configuracion.getNombreRemitente(),
                configuracion.getResponderA(),
                configuracion.getSmtpHost(),
                configuracion.getSmtpPort(),
                configuracion.getSmtpUsername(),
                smtpConfigurada,
                smtpCifrada,
                (smtpConfigurada && !smtpCifrada) || (graphConfigurada && !graphCifrada) || (graphPrivateKeyConfigurada && !graphPrivateKeyCifrada),
                configuracion.getSmtpAuth(),
                configuracion.getSmtpStartTls(),
                configuracion.getGraphTenantId(),
                configuracion.getGraphClientId(),
                configuracion.getGraphUserId(),
                graphConfigurada,
                graphCifrada,
                configuracion.getGraphCertificateThumbprint(),
                graphPrivateKeyConfigurada,
                graphPrivateKeyCifrada,
                configuracion.getGraphOauthRefreshToken() != null && !configuracion.getGraphOauthRefreshToken().isBlank(),
                configuracion.getGraphOauthConectadoEn() != null ? configuracion.getGraphOauthConectadoEn().toString() : null
        );
    }

    private ConfiguracionSitioAdminResponse mapearConfiguracionSitio(EmpresaSitioConfigEntidad configuracion) {
        return new ConfiguracionSitioAdminResponse(
                configuracion.getEmpresaId(),
                configuracion.getSlug(),
                configuracion.getNombreComercial(),
                configuracion.getDominioPrincipal(),
                configuracion.getLogoUrl(),
                configuracion.getDescripcionCorta(),
                configuracion.getColorPrimario(),
                configuracion.getColorSecundario(),
                configuracion.getFuenteTitulos(),
                configuracion.getFuenteCuerpo(),
                configuracion.getHeroTitulo(),
                configuracion.getHeroSubtitulo(),
                configuracion.getHeroImagenUrl(),
                configuracion.getWhatsapp(),
                configuracion.getTelefono(),
                configuracion.getCorreo(),
                configuracion.getDireccion(),
                configuracion.getInstagramUrl(),
                configuracion.getFacebookUrl(),
                configuracion.getTema(),
                configuracion.isPublicado()
        );
    }

    private ConfiguracionWhatsappAdminResponse mapearConfiguracionWhatsapp(ConfiguracionWhatsappEmpresaEntidad configuracion) {
        String authToken = configuracion.getAuthToken();
        boolean authTokenConfigurado = authToken != null && !authToken.isBlank();
        return new ConfiguracionWhatsappAdminResponse(
                configuracion.isHabilitado(),
                configuracion.getAccountSid(),
                authTokenConfigurado,
                configuracion.getTipoCuentaTwilio() != null && !configuracion.getTipoCuentaTwilio().isBlank()
                        ? configuracion.getTipoCuentaTwilio()
                        : "PLATAFORMA",
                configuracion.getSubaccountSid(),
                configuracion.getNumeroRemitente(),
                configuracion.getMessagingServiceSid(),
                configuracion.getChannelSenderSid(),
                configuracion.getStatusCallbackUrl(),
                configuracion.getPlantillaSolicitudConfirmacionSid(),
                configuracion.getPlantillaReprogramadaPendienteSid(),
                configuracion.getPlantillaRecordatorioConfirmacionSid(),
                configuracion.getPlantillaCitaConfirmadaSid(),
                configuracion.getPlantillaRecordatorioSid(),
                configuracion.getPlantillaCancelacionSid(),
                configuracion.getPlantillaLiberadaSinConfirmacionSid(),
                configuracion.getPlantillaGraciasVisitaSid(),
                configuracion.getPlantillaRecordatorioRegresoSid(),
                configuracion.getPlantillaEspacioDisponibleWalkinSid(),
                configuracion.getPlantillaMenuBienvenidaSid(),
                configuracion.getPlantillasListPickerSids(),
                configuracion.getSenderDisplayName(),
                configuracion.getSenderPhoneNumber(),
                configuracion.getSenderStatus(),
                configuracion.getQualityRating(),
                configuracion.getThroughputMps(),
                configuracion.getWabaId(),
                configuracion.getMetaBusinessManagerId()
        );
    }

    private PlantillaWhatsappEmpresaAdminResponse mapearPlantillaWhatsappEmpresa(PlantillaWhatsappEmpresaEntidad plantilla) {
        return new PlantillaWhatsappEmpresaAdminResponse(
                plantilla.getId(),
                plantilla.getNombre(),
                plantilla.getUso(),
                plantilla.getContentSid(),
                plantilla.getTipoContenido(),
                plantilla.getCategoria(),
                plantilla.getEstado(),
                plantilla.isActiva(),
                plantilla.getCreadaEn(),
                plantilla.getActualizadaEn()
        );
    }

    private void aplicarPlantillaWhatsappEmpresa(
            PlantillaWhatsappEmpresaEntidad plantilla,
            PlantillaWhatsappEmpresaAdminRequest request,
            String uso,
            String contentSid
    ) {
        String nombre = normalizarOpcional(request.nombre());
        if (nombre == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Debes indicar un nombre para la plantilla");
        }
        plantilla.setNombre(nombre);
        plantilla.setUso(uso);
        plantilla.setContentSid(contentSid);
        plantilla.setTipoContenido(normalizarOpcional(request.tipoContenido()));
        plantilla.setCategoria(normalizarOpcional(request.categoria()));
        plantilla.setEstado(normalizarOpcional(request.estado()));
        plantilla.setActiva(request.activa());
    }

    private String normalizarUsoPlantillaWhatsapp(String uso) {
        String limpio = normalizarOpcional(uso);
        if (limpio == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Debes indicar el uso de la plantilla");
        }
        return limpio.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private String normalizarContentSidPlantillaWhatsapp(String contentSid) {
        String limpio = normalizarOpcional(contentSid);
        if (limpio == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Debes indicar el Content SID de Twilio");
        }
        return limpio;
    }

    private String snapshotPlantillaWhatsappEmpresa(PlantillaWhatsappEmpresaEntidad plantilla) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", plantilla.getId());
        snapshot.put("nombre", plantilla.getNombre());
        snapshot.put("uso", plantilla.getUso());
        snapshot.put("contentSid", plantilla.getContentSid());
        snapshot.put("tipoContenido", plantilla.getTipoContenido());
        snapshot.put("categoria", plantilla.getCategoria());
        snapshot.put("estado", plantilla.getEstado());
        snapshot.put("activa", plantilla.isActiva());
        return serializarAuditoria(snapshot);
    }

    private String normalizarOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private EmpresaSitioConfigEntidad construirConfiguracionSitioBase(EmpresaEntidad empresa) {
        EmpresaSitioConfigEntidad configuracion = new EmpresaSitioConfigEntidad();
        configuracion.setEmpresaId(empresa.getId());
        configuracion.setSlug(normalizarSlugPublico(empresa.getSlug() != null ? empresa.getSlug() : empresa.getNombre()));
        configuracion.setNombreComercial(
                empresa.getNombre() != null && !empresa.getNombre().isBlank()
                        ? empresa.getNombre().trim()
                        : "Mi negocio"
        );
        configuracion.setColorPrimario("#D14F7D");
        configuracion.setColorSecundario("#F6D9E3");
        configuracion.setFuenteTitulos("JAKARTA");
        configuracion.setFuenteCuerpo("INTER");
        configuracion.setTema("fluora-base");
        configuracion.setPublicado(false);
        return configuracion;
    }

    private void validarSlugSitioDisponible(Long empresaId, String slug) {
        empresaSitioConfigRepositorio.findBySlug(slug)
                .filter(configuracion -> !configuracion.getEmpresaId().equals(empresaId))
                .ifPresent(configuracion -> {
                    throw new ResponseStatusException(CONFLICT, "Ese slug ya está en uso por otro tenant");
                });
    }

    private void validarDominioPrincipalDisponible(Long empresaId, String dominioPrincipal) {
        if (dominioPrincipal == null) {
            return;
        }

        empresaSitioConfigRepositorio.findByDominioPrincipalIgnoreCase(dominioPrincipal)
                .filter(configuracion -> !configuracion.getEmpresaId().equals(empresaId))
                .ifPresent(configuracion -> {
                    throw new ResponseStatusException(CONFLICT, "Ese dominio principal ya está configurado en otro tenant");
                });
    }

    private String normalizarSlugPublico(String valor) {
        String ascii = Normalizer.normalize(valor == null ? "" : valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = ascii
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (slug.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Debes indicar un slug válido para el sitio");
        }
        if (slug.length() > 100) {
            throw new ResponseStatusException(BAD_REQUEST, "El slug del sitio no puede exceder 100 caracteres");
        }
        return slug;
    }

    private String normalizarDominioPrincipal(String valor) {
        String limpio = normalizarOpcional(valor);
        if (limpio == null) {
            return null;
        }

        String candidato = limpio;
        try {
            if (!candidato.contains("://")) {
                candidato = "https://" + candidato;
            }
            URI uri = URI.create(candidato);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("host vacío");
            }
            return IDN.toASCII(host.trim().toLowerCase(Locale.ROOT));
        } catch (Exception exception) {
            throw new ResponseStatusException(BAD_REQUEST, "Debes indicar un dominio principal válido");
        }
    }

    private String normalizarColorHex(String valor) {
        String limpio = normalizarOpcional(valor);
        if (limpio == null) {
            return null;
        }
        String color = limpio.toUpperCase(Locale.ROOT);
        if (!color.matches("^#[0-9A-F]{6}$")) {
            throw new ResponseStatusException(BAD_REQUEST, "Los colores deben estar en formato hexadecimal #RRGGBB");
        }
        return color;
    }

    private String normalizarTema(String valor) {
        String limpio = normalizarOpcional(valor);
        return limpio != null ? limpio : "fluora-base";
    }

    private String normalizarFuente(String valor, String fallback) {
        String limpio = normalizarOpcional(valor);
        if (limpio == null) {
            return fallback;
        }

        String fuente = limpio.trim().toUpperCase(Locale.ROOT);
        return switch (fuente) {
            case "INTER", "JAKARTA", "PLAYFAIR", "MANROPE", "SYSTEM" -> fuente;
            default -> fallback;
        };
    }

    private String construirNombreSubcuenta(EmpresaEntidad empresa) {
        String base = empresa.getNombre() != null && !empresa.getNombre().isBlank()
                ? empresa.getNombre().trim()
                : "tenant-" + empresa.getId();
        String candidato = "Agenda SaaS - " + base;
        return candidato.length() > 64 ? candidato.substring(0, 64) : candidato;
    }

    private String construirNombreMessagingService(EmpresaEntidad empresa) {
        String base = empresa.getNombre() != null && !empresa.getNombre().isBlank()
                ? empresa.getNombre().trim()
                : "tenant-" + empresa.getId();
        String candidato = "WhatsApp - " + base;
        return candidato.length() > 64 ? candidato.substring(0, 64) : candidato;
    }

    private LogMensajeWhatsappAdminResponse mapearLogWhatsapp(
            BandejaSalidaNotificacionEntidad log,
            ConfiguracionWhatsappResolvida configuracion
    ) {
        return new LogMensajeWhatsappAdminResponse(
                log.getId(),
                log.getAgregadoId(),
                log.getTipoEvento(),
                log.getEstado(),
                log.getEstadoEntrega(),
                log.getProveedorMensajeId(),
                extraerCampoPayload(log.getPayloadJson(), "telefonoDestino"),
                resolverPlantillaSid(log, configuracion),
                log.getCodigoErrorProveedor(),
                log.getDetalleErrorProveedor() != null ? log.getDetalleErrorProveedor() : log.getMensajeError(),
                log.getEnviadaEn(),
                log.getEstadoEntregaActualizadoEn()
        );
    }

    private MensajeWhatsappAdminResponse mapearMensajeWhatsapp(MensajeWhatsappEntidad mensaje) {
        return new MensajeWhatsappAdminResponse(
                mensaje.getId(),
                mensaje.getTelefonoNormalizado(),
                mensaje.getDireccion(),
                mensaje.getCuerpo(),
                mensaje.getContentSid(),
                mensaje.getProveedorMensajeId(),
                mensaje.getEstado(),
                mensaje.getCodigoErrorProveedor(),
                mensaje.getDetalleErrorProveedor(),
                mensaje.getCreadoEn()
        );
    }

    private List<PlantillaWhatsappAdminResponse> construirPlantillasLocalmente(ConfiguracionWhatsappResolvida configuracion) {
        List<PlantillaWhatsappAdminResponse> plantillas = new ArrayList<>();
        Set<String> sids = new HashSet<>();

        agregarPlantillaLocal(plantillas, sids, configuracion.plantillaSolicitudConfirmacionSid(), "solicitud_confirmacion_cita");
        agregarPlantillaLocal(plantillas, sids, configuracion.plantillaReprogramadaPendienteSid(), "cita_reprogramada_pendiente_confirmacion");
        agregarPlantillaLocal(plantillas, sids, configuracion.plantillaRecordatorioConfirmacionSid(), "recordatorio_confirmar_cita");
        agregarPlantillaLocal(plantillas, sids, configuracion.plantillaCitaConfirmadaSid(), "cita_confirmada");
        agregarPlantillaLocal(plantillas, sids, configuracion.plantillaRecordatorioSid(), "recordatorio_cita");
        agregarPlantillaLocal(plantillas, sids, configuracion.plantillaCancelacionSid(), "cita_cancelada_negocio");
        agregarPlantillaLocal(plantillas, sids, configuracion.plantillaLiberadaSinConfirmacionSid(), "cita_liberada_sin_confirmacion");
        agregarPlantillaLocal(plantillas, sids, configuracion.plantillaGraciasVisitaSid(), "gracias_por_tu_visita");
        agregarPlantillaLocal(plantillas, sids, configuracion.plantillaRecordatorioRegresoSid(), "recordatorio_regreso");
        agregarPlantillaLocal(plantillas, sids, configuracion.plantillaEspacioDisponibleWalkinSid(), "espacio_disponible_walkin");
        agregarPlantillaLocal(plantillas, sids, configuracion.plantillaMenuBienvenidaSid(), "menu_bienvenida_sesion");

        return plantillas;
    }

    private void agregarPlantillaLocal(
            List<PlantillaWhatsappAdminResponse> plantillas,
            Set<String> sids,
            String sid,
            String nombre
    ) {
        if (sid == null || sid.isBlank() || !sids.add(sid)) {
            return;
        }

        plantillas.add(new PlantillaWhatsappAdminResponse(
                sid,
                nombre,
                "es_MX",
                "UTILITY",
                "CONFIGURADA_LOCAL",
                "CONTENT_TEMPLATE"
        ));
    }

    private String resolverPlantillaSid(BandejaSalidaNotificacionEntidad log, ConfiguracionWhatsappResolvida configuracion) {
        String plantillaPayload = extraerCampoPayload(log.getPayloadJson(), "plantillaSid");
        if (plantillaPayload != null) {
            return plantillaPayload;
        }
        if ("CITA_RECORDATORIO_WHATSAPP".equals(log.getTipoEvento())) {
            return configuracion.plantillaRecordatorioSid();
        }
        if ("CITA_RECORDATORIO_CONFIRMACION_WHATSAPP".equals(log.getTipoEvento())) {
            return configuracion.plantillaRecordatorioConfirmacionSid();
        }
        if ("CITA_REGISTRADA_WHATSAPP".equals(log.getTipoEvento())) {
            return configuracion.plantillaSolicitudConfirmacionSid();
        }
        if ("CITA_REPROGRAMADA_PENDIENTE_WHATSAPP".equals(log.getTipoEvento())) {
            return configuracion.plantillaReprogramadaPendienteSid();
        }
        if ("CITA_CANCELADA_NEGOCIO_WHATSAPP".equals(log.getTipoEvento())) {
            return configuracion.plantillaCancelacionSid();
        }
        if ("CITA_CONFIRMADA_WHATSAPP".equals(log.getTipoEvento())) {
            return configuracion.plantillaCitaConfirmadaSid();
        }
        if ("CITA_LIBERADA_SIN_CONFIRMACION_WHATSAPP".equals(log.getTipoEvento())) {
            return configuracion.plantillaLiberadaSinConfirmacionSid();
        }
        if ("CITA_GRACIAS_VISITA_WHATSAPP".equals(log.getTipoEvento())) {
            return configuracion.plantillaGraciasVisitaSid();
        }
        if ("RECEPCION_ESPACIO_DISPONIBLE_WALKIN".equals(log.getTipoEvento())) {
            return configuracion.plantillaEspacioDisponibleWalkinSid();
        }
        return null;
    }

    private String extraerCampoPayload(String payloadJson, String campo) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return null;
        }
        try {
            JsonNode raiz = objectMapper.readTree(payloadJson);
            JsonNode valor = raiz.get(campo);
            return valor != null && !valor.isNull() ? valor.asText() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private void registrarLogPruebaWhatsapp(
            Long empresaId,
            PruebaPlantillaWhatsappRequest request,
            String plantillaSid,
            ResultadoEnvioWhatsapp resultado
    ) {
        try {
            BandejaSalidaNotificacionEntidad salida = new BandejaSalidaNotificacionEntidad();
            salida.setEmpresaId(empresaId);
            salida.setTipoAgregado("WHATSAPP_TEST");
            salida.setAgregadoId(0L);
            salida.setTipoEvento("WHATSAPP_PRUEBA_PLANTILLA");
            salida.setCanal("WHATSAPP");
            salida.setPayloadJson(objectMapper.writeValueAsString(Map.of(
                    "telefonoDestino", request.telefonoDestino().trim(),
                    "plantillaSid", plantillaSid,
                    "nombreCliente", request.nombreCliente().trim(),
                    "fecha", request.fecha().trim(),
                    "hora", request.hora().trim()
            )));
            salida.setEstado("ENVIADA");
            salida.setProgramadaEn(LocalDateTime.now());
            salida.setEnviadaEn(LocalDateTime.now());
            salida.setIntentos(1);
            salida.setProveedorMensajeId(resultado.proveedorMensajeId());
            salida.setEstadoEntrega(resultado.estadoProveedor());
            salida.setEstadoEntregaActualizadoEn(LocalDateTime.now());
            salida.setCodigoErrorProveedor(resultado.codigoErrorProveedor());
            salida.setDetalleErrorProveedor(resultado.detalleErrorProveedor());
            bandejaSalidaNotificacionRepositorio.save(salida);
        } catch (Exception ex) {
            throw new IllegalStateException("La plantilla se envio, pero no se pudo registrar el log de prueba de WhatsApp", ex);
        }
    }

    private SucursalAdminResponse mapearSucursal(SucursalEntidad sucursal) {
        return new SucursalAdminResponse(
                sucursal.getId(),
                sucursal.getNombre(),
                sucursal.getDireccion(),
                sucursal.getTelefono(),
                sucursal.getZonaHoraria(),
                sucursal.isActiva()
        );
    }

    private void aplicarGrupoServicio(GrupoServicioEntidad grupo, GrupoServicioAdminRequest request) {
        grupo.setNombre(request.nombre().trim());
        grupo.setSlug(normalizarSlugCatalogo(request.slug(), request.nombre(), "grupo"));
        grupo.setDescripcion(normalizarOpcional(request.descripcion()));
        grupo.setImagenUrl(normalizarOpcional(request.imagenUrl()));
        grupo.setIcono(normalizarOpcional(request.icono()));
        grupo.setOrdenPublico(request.ordenPublico());
        grupo.setActivo(request.activo());
    }

    private GrupoServicioAdminResponse mapearGrupoServicio(GrupoServicioEntidad grupo) {
        return new GrupoServicioAdminResponse(
                grupo.getId(),
                grupo.getNombre(),
                grupo.getSlug(),
                grupo.getDescripcion(),
                grupo.getImagenUrl(),
                grupo.getIcono(),
                grupo.getOrdenPublico(),
                grupo.isActivo()
        );
    }

    private void aplicarSubgrupoServicio(SubgrupoServicioEntidad subgrupo, SubgrupoServicioAdminRequest request) {
        subgrupo.setGrupoId(request.grupoId());
        subgrupo.setNombre(request.nombre().trim());
        subgrupo.setSlug(normalizarSlugCatalogo(request.slug(), request.nombre(), "subgrupo"));
        subgrupo.setDescripcion(normalizarOpcional(request.descripcion()));
        subgrupo.setOrdenPublico(request.ordenPublico());
        subgrupo.setActivo(request.activo());
    }

    private SubgrupoServicioAdminResponse mapearSubgrupoServicio(SubgrupoServicioEntidad subgrupo, String grupoNombre) {
        return new SubgrupoServicioAdminResponse(
                subgrupo.getId(),
                subgrupo.getGrupoId(),
                grupoNombre,
                subgrupo.getNombre(),
                subgrupo.getSlug(),
                subgrupo.getDescripcion(),
                subgrupo.getOrdenPublico(),
                subgrupo.isActivo()
        );
    }

    private void aplicarServicio(ServicioEntidad servicio, ServicioAdminRequest request) {
        servicio.setSucursalId(request.sucursalId());
        servicio.setGrupoId(request.grupoId());
        servicio.setSubgrupoId(request.subgrupoId());
        servicio.setNombre(request.nombre().trim());
        servicio.setSlug(normalizarSlugCatalogo(request.slug(), request.nombre(), "servicio"));
        servicio.setDescripcion(normalizarOpcional(request.descripcion()));
        servicio.setImagenUrl(normalizarOpcional(request.imagenUrl()));
        servicio.setDuracionMinutos(request.duracionMinutos());
        servicio.setBufferAntesMinutos(request.bufferAntesMinutos());
        servicio.setBufferDespuesMinutos(request.bufferDespuesMinutos());
        servicio.setPrecio(request.precio());
        servicio.setMoneda(request.moneda().trim().toUpperCase());
        servicio.setOrdenPublico(request.ordenPublico());
        servicio.setVisiblePublico(request.visiblePublico());
        servicio.setRequiereAnticipo(request.requiereAnticipo());
        servicio.setAnticipoTipo(request.requiereAnticipo() ? normalizarAnticipoTipo(request.anticipoTipo()) : null);
        servicio.setAnticipoValor(request.requiereAnticipo() ? validarAnticipoValor(request.anticipoValor()) : null);
        servicio.setActivo(request.activo());
    }

    private ServicioAdminResponse mapearServicio(
            ServicioEntidad servicio,
            Long sucursalId,
            String sucursalNombre,
            List<Long> sucursalIds,
            List<String> sucursalNombres,
            String grupoNombre,
            String subgrupoNombre
    ) {
        return new ServicioAdminResponse(
                servicio.getId(),
                sucursalId,
                sucursalNombre,
                sucursalIds,
                sucursalNombres,
                servicio.getGrupoId(),
                grupoNombre,
                servicio.getSubgrupoId(),
                subgrupoNombre,
                servicio.getNombre(),
                servicio.getSlug(),
                servicio.getDescripcion(),
                servicio.getImagenUrl(),
                servicio.getDuracionMinutos(),
                servicio.getBufferAntesMinutos(),
                servicio.getBufferDespuesMinutos(),
                servicio.getPrecio(),
                servicio.getMoneda(),
                servicio.getOrdenPublico(),
                servicio.isVisiblePublico(),
                servicio.isRequiereAnticipo(),
                servicio.getAnticipoTipo(),
                servicio.getAnticipoValor(),
                servicio.isActivo()
        );
    }

    private List<Long> validarSucursalesServicio(Long empresaId, Long sucursalPrincipalId, List<Long> sucursalIds) {
        LinkedHashSet<Long> seleccionadas = new LinkedHashSet<>();
        if (sucursalIds != null) {
            sucursalIds.stream()
                    .filter(java.util.Objects::nonNull)
                    .forEach(seleccionadas::add);
        }
        if (sucursalPrincipalId != null) {
            seleccionadas.add(sucursalPrincipalId);
        }

        if (seleccionadas.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Debes asignar al menos una sucursal al servicio");
        }
        if (sucursalPrincipalId == null || !seleccionadas.contains(sucursalPrincipalId)) {
            throw new ResponseStatusException(BAD_REQUEST, "La sucursal principal debe formar parte de las sucursales asignadas");
        }

        List<SucursalEntidad> sucursales = sucursalRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .filter(sucursal -> seleccionadas.contains(sucursal.getId()))
                .toList();
        if (sucursales.size() != seleccionadas.size()) {
            throw new ResponseStatusException(NOT_FOUND, "Una o mas sucursales asignadas no existen para la empresa");
        }

        return new ArrayList<>(seleccionadas);
    }

    private void sincronizarAsignacionesServicio(Long empresaId, Long servicioId, List<Long> sucursalIds) {
        Set<Long> seleccionadas = new LinkedHashSet<>(sucursalIds);
        List<ServicioSucursalEntidad> actuales = servicioSucursalRepositorio.findByEmpresaIdAndIdServicioId(empresaId, servicioId);
        Map<Long, ServicioSucursalEntidad> actualesPorSucursal = actuales.stream()
                .collect(Collectors.toMap(asignacion -> asignacion.getId().getSucursalId(), asignacion -> asignacion));

        for (ServicioSucursalEntidad asignacion : actuales) {
            asignacion.setActivo(seleccionadas.contains(asignacion.getId().getSucursalId()));
        }

        for (Long sucursalId : seleccionadas) {
            if (!actualesPorSucursal.containsKey(sucursalId)) {
                ServicioSucursalEntidad nueva = new ServicioSucursalEntidad();
                nueva.setId(new ServicioSucursalId(servicioId, sucursalId));
                nueva.setEmpresaId(empresaId);
                nueva.setActivo(true);
                servicioSucursalRepositorio.save(nueva);
            }
        }

        if (!actuales.isEmpty()) {
            servicioSucursalRepositorio.saveAll(actuales);
        }
    }

    private Long resolverSucursalPrincipal(ServicioEntidad servicio, List<ServicioSucursalEntidad> asignaciones) {
        if (asignaciones == null || asignaciones.isEmpty()) {
            return servicio.getSucursalId();
        }
        if (servicio.getSucursalId() != null) {
            boolean coincide = asignaciones.stream()
                    .anyMatch(asignacion -> servicio.getSucursalId().equals(asignacion.getId().getSucursalId()));
            if (coincide) {
                return servicio.getSucursalId();
            }
        }
        return asignaciones.stream()
                .map(asignacion -> asignacion.getId().getSucursalId())
                .sorted()
                .findFirst()
                .orElse(servicio.getSucursalId());
    }

    private List<Long> resolverSucursalesAsignadas(ServicioEntidad servicio, List<ServicioSucursalEntidad> asignaciones) {
        if (asignaciones == null || asignaciones.isEmpty()) {
            return servicio.getSucursalId() != null ? List.of(servicio.getSucursalId()) : List.of();
        }
        return asignaciones.stream()
                .map(asignacion -> asignacion.getId().getSucursalId())
                .distinct()
                .sorted()
                .toList();
    }

    private GrupoServicioEntidad validarGrupoServicio(Long empresaId, Long grupoId) {
        if (grupoId == null) {
            return null;
        }
        return grupoServicioRepositorio.findByIdAndEmpresaId(grupoId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El grupo indicado no existe para la empresa"));
    }

    private SubgrupoServicioEntidad validarSubgrupoServicio(Long empresaId, GrupoServicioEntidad grupo, Long subgrupoId) {
        if (subgrupoId == null) {
            return null;
        }
        if (grupo == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Debes elegir un grupo antes de seleccionar un subgrupo");
        }
        SubgrupoServicioEntidad subgrupo = subgrupoServicioRepositorio.findByIdAndEmpresaId(subgrupoId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El subgrupo indicado no existe para la empresa"));
        if (!grupo.getId().equals(subgrupo.getGrupoId())) {
            throw new ResponseStatusException(BAD_REQUEST, "El subgrupo no pertenece al grupo seleccionado");
        }
        return subgrupo;
    }

    private void validarSlugGrupoDisponible(Long empresaId, String slug, Long grupoId) {
        boolean existe = grupoId == null
                ? grupoServicioRepositorio.existsByEmpresaIdAndSlug(empresaId, slug)
                : grupoServicioRepositorio.existsByEmpresaIdAndSlugAndIdNot(empresaId, slug, grupoId);
        if (existe) {
            throw new ResponseStatusException(CONFLICT, "Ya existe otro grupo con ese slug");
        }
    }

    private void validarSlugSubgrupoDisponible(Long empresaId, Long grupoId, String slug, Long subgrupoId) {
        boolean existe = subgrupoId == null
                ? subgrupoServicioRepositorio.existsByEmpresaIdAndGrupoIdAndSlug(empresaId, grupoId, slug)
                : subgrupoServicioRepositorio.existsByEmpresaIdAndGrupoIdAndSlugAndIdNot(empresaId, grupoId, slug, subgrupoId);
        if (existe) {
            throw new ResponseStatusException(CONFLICT, "Ya existe otro subgrupo con ese slug dentro del grupo");
        }
    }

    private void validarSlugServicioDisponible(Long empresaId, String slug, Long servicioId) {
        boolean existe = servicioId == null
                ? servicioRepositorio.existsByEmpresaIdAndSlug(empresaId, slug)
                : servicioRepositorio.existsByEmpresaIdAndSlugAndIdNot(empresaId, slug, servicioId);
        if (existe) {
            throw new ResponseStatusException(CONFLICT, "Ya existe otro servicio con ese slug");
        }
    }

    private String normalizarSlugCatalogo(String slugSolicitado, String fallback, String tipo) {
        String base = slugSolicitado != null && !slugSolicitado.isBlank() ? slugSolicitado : fallback;
        String slug = Normalizer.normalize(base == null ? "" : base, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (slug.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Debes indicar un slug válido para el " + tipo);
        }
        if (slug.length() > 140) {
            throw new ResponseStatusException(BAD_REQUEST, "El slug del " + tipo + " no puede exceder 140 caracteres");
        }
        return slug;
    }

    private String normalizarAnticipoTipo(String valor) {
        String tipo = normalizarOpcional(valor);
        if (tipo == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Debes indicar el tipo de anticipo");
        }
        String normalizado = tipo.toUpperCase(Locale.ROOT);
        if (!Set.of("FIJO", "PORCENTAJE").contains(normalizado)) {
            throw new ResponseStatusException(BAD_REQUEST, "El tipo de anticipo debe ser FIJO o PORCENTAJE");
        }
        return normalizado;
    }

    private BigDecimal validarAnticipoValor(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Debes indicar un valor de anticipo mayor a cero");
        }
        return valor;
    }

    private void aplicarPrestador(PrestadorServicioEntidad prestador, PrestadorAdminRequest request) {
        prestador.setSucursalId(request.sucursalId());
        prestador.setNombreMostrar(request.nombreMostrar().trim());
        prestador.setBiografia(request.biografia() != null ? request.biografia().trim() : null);
        prestador.setColorAgenda(request.colorAgenda() != null && !request.colorAgenda().isBlank() ? request.colorAgenda().trim() : "#2563eb");
        prestador.setActivo(request.activo());
    }

    private void asignarRolStaff(UsuarioEntidad usuario, Long empresaId) {
        servicioRolesEmpresa.asignarRolEmpresa(usuario, empresaId, "STAFF");
    }

    private void aplicarUsuarioInterno(UsuarioInternoPerfilEntidad perfil, UsuarioInternoAdminRequest request) {
        perfil.setSucursalId(request.sucursalId());
        perfil.setNombreCompleto(request.nombreCompleto().trim());
        perfil.setTelefono(request.telefono() != null && !request.telefono().isBlank() ? request.telefono().trim() : null);
        perfil.setPuesto(request.puesto() != null && !request.puesto().isBlank() ? request.puesto().trim() : null);
        perfil.setNotas(request.notas() != null && !request.notas().isBlank() ? request.notas().trim() : null);
    }

    private RolInternoAdminResponse construirRolInternoResponse(RolEmpresaEntidad rolEmpresa) {
        return mapearRolInterno(
                rolEmpresa,
                servicioRolesEmpresa.obtenerPermisosPorRolEmpresa(List.of(rolEmpresa.getId())),
                Map.of(rolEmpresa.getId(), usuarioRolEmpresaRepositorio.countByRolEmpresa_Id(rolEmpresa.getId()))
        );
    }

    private RolInternoAdminResponse mapearRolInterno(
            RolEmpresaEntidad rolEmpresa,
            Map<Long, List<String>> permisosPorRol,
            Map<Long, Long> usuariosAsignadosPorRol
    ) {
        long usuariosAsignados = usuariosAsignadosPorRol.getOrDefault(rolEmpresa.getId(), 0L);
        return new RolInternoAdminResponse(
                rolEmpresa.getId(),
                rolEmpresa.getCodigo(),
                rolEmpresa.getNombre(),
                rolEmpresa.getDescripcion(),
                rolEmpresa.isActivo(),
                rolEmpresa.isEditable(),
                usuariosAsignados,
                rolEmpresa.isEditable() && usuariosAsignados == 0,
                permisosPorRol.getOrDefault(rolEmpresa.getId(), List.of()).stream().sorted().toList()
        );
    }

    private RolEmpresaEntidad resolverRolUsuarioInterno(Long empresaId, Long rolEmpresaId) {
        List<RolEmpresaEntidad> rolesInternos = servicioRolesEmpresa.listarRolesEmpresa(empresaId).stream()
                .filter(RolEmpresaEntidad::isActivo)
                .filter(rol -> esRolInternoAsignable(servicioRolesEmpresa.obtenerPermisosPorRolEmpresa(List.of(rol.getId())).getOrDefault(rol.getId(), List.of())))
                .toList();

        if (rolEmpresaId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Debes seleccionar un rol interno");
        }
        return rolesInternos.stream()
                .filter(rol -> rol.getId().equals(rolEmpresaId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "El rol interno indicado no es valido"));
    }

    private PlantillaRolInternoAdminResponse plantillaRolInterno(
            String codigoSugerido,
            String nombreSugerido,
            String descripcion,
            String categoria,
            List<String> permisos,
            Set<String> permisosDisponibles
    ) {
        List<String> permisosFiltrados = permisos.stream()
                .filter(permisosDisponibles::contains)
                .distinct()
                .sorted()
                .toList();
        return new PlantillaRolInternoAdminResponse(
                codigoSugerido,
                nombreSugerido,
                descripcion,
                categoria,
                permisosFiltrados
        );
    }

    private void registrarAuditoriaRol(
            Long empresaId,
            Long usuarioActorId,
            String accion,
            String resumen,
            RolEmpresaEntidad rolEmpresa,
            String detalleAntesJson,
            String detalleDespuesJson
    ) {
        AuditoriaRolEmpresaEntidad auditoria = new AuditoriaRolEmpresaEntidad();
        auditoria.setEmpresaId(empresaId);
        auditoria.setRolEmpresaId(rolEmpresa != null ? rolEmpresa.getId() : null);
        auditoria.setUsuarioActorId(usuarioActorId);
        auditoria.setActorCorreo(
                usuarioActorId != null
                        ? usuarioRepositorio.findById(usuarioActorId).map(UsuarioEntidad::getCorreo).orElse(null)
                        : null
        );
        auditoria.setRolCodigo(rolEmpresa != null ? rolEmpresa.getCodigo() : null);
        auditoria.setRolNombre(rolEmpresa != null ? rolEmpresa.getNombre() : null);
        auditoria.setAccion(accion);
        auditoria.setResumen(resumen);
        auditoria.setDetalleAntesJson(detalleAntesJson);
        auditoria.setDetalleDespuesJson(detalleDespuesJson);
        auditoriaRolEmpresaRepositorio.save(auditoria);
    }

    private String snapshotRol(RolEmpresaEntidad rolEmpresa, List<String> permisos) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("rolEmpresaId", rolEmpresa.getId());
        snapshot.put("codigo", rolEmpresa.getCodigo());
        snapshot.put("nombre", rolEmpresa.getNombre());
        snapshot.put("descripcion", rolEmpresa.getDescripcion());
        snapshot.put("activo", rolEmpresa.isActivo());
        snapshot.put("editable", rolEmpresa.isEditable());
        snapshot.put("permisos", permisos);
        return serializarAuditoria(snapshot);
    }

    private String snapshotUsuarioInterno(
            UsuarioEntidad usuario,
            UsuarioInternoPerfilEntidad perfil,
            RolEmpresaEntidad rolEmpresa,
            List<Long> sucursalesPermitidas,
            List<String> permisosDirectos
    ) {
        List<String> permisosRol = rolEmpresa != null
                ? servicioRolesEmpresa.obtenerPermisosPorRolEmpresa(List.of(rolEmpresa.getId())).getOrDefault(rolEmpresa.getId(), List.of())
                : List.of();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("usuarioId", usuario.getId());
        snapshot.put("correo", usuario.getCorreo());
        snapshot.put("nombreCompleto", perfil.getNombreCompleto());
        snapshot.put("rolEmpresaId", rolEmpresa != null ? rolEmpresa.getId() : null);
        snapshot.put("rolCodigo", rolEmpresa != null ? rolEmpresa.getCodigo() : null);
        snapshot.put("rolNombre", rolEmpresa != null ? rolEmpresa.getNombre() : null);
        snapshot.put("sucursalBaseId", perfil.getSucursalId());
        snapshot.put("sucursalesPermitidas", sucursalesPermitidas);
        snapshot.put("permisosRol", permisosRol);
        snapshot.put("permisosDirectos", permisosDirectos);
        snapshot.put("permisosEfectivos", combinarPermisos(permisosRol, permisosDirectos));
        snapshot.put("activo", usuario.isHabilitado());
        return serializarAuditoria(snapshot);
    }

    private String serializarAuditoria(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            LOGGER.warn("No se pudo serializar detalle de auditoria", exception);
            return null;
        }
    }

    private void registrarAuditoriaConfiguracion(
            Long empresaId,
            Long usuarioActorId,
            String modulo,
            String accion,
            String resumen,
            String detalleAntesJson,
            String detalleDespuesJson
    ) {
        AuditoriaConfiguracionEmpresaEntidad auditoria = new AuditoriaConfiguracionEmpresaEntidad();
        auditoria.setEmpresaId(empresaId);
        auditoria.setUsuarioActorId(usuarioActorId);
        auditoria.setActorCorreo(
                usuarioActorId != null
                        ? usuarioRepositorio.findById(usuarioActorId).map(UsuarioEntidad::getCorreo).orElse(null)
                        : null
        );
        auditoria.setModulo(modulo);
        auditoria.setAccion(accion);
        auditoria.setResumen(resumen);
        auditoria.setDetalleAntesJson(detalleAntesJson);
        auditoria.setDetalleDespuesJson(detalleDespuesJson);
        auditoriaConfiguracionEmpresaRepositorio.save(auditoria);
    }

    private String snapshotConfiguracionCorreo(ConfiguracionCorreoEmpresaEntidad configuracion) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("habilitado", configuracion.isHabilitado());
        snapshot.put("proveedor", configuracion.getProveedor());
        snapshot.put("remitente", configuracion.getRemitente());
        snapshot.put("nombreRemitente", configuracion.getNombreRemitente());
        snapshot.put("responderA", configuracion.getResponderA());
        snapshot.put("smtpHost", configuracion.getSmtpHost());
        snapshot.put("smtpPort", configuracion.getSmtpPort());
        snapshot.put("smtpUsername", configuracion.getSmtpUsername());
        snapshot.put("smtpAuth", configuracion.getSmtpAuth());
        snapshot.put("smtpStartTls", configuracion.getSmtpStartTls());
        snapshot.put("smtpPasswordConfigurada", configuracion.getSmtpPassword() != null && !configuracion.getSmtpPassword().isBlank());
        snapshot.put("graphTenantId", configuracion.getGraphTenantId());
        snapshot.put("graphClientId", configuracion.getGraphClientId());
        snapshot.put("graphUserId", configuracion.getGraphUserId());
        snapshot.put("graphCertificateThumbprint", configuracion.getGraphCertificateThumbprint());
        snapshot.put("graphClientSecretConfigurado", configuracion.getGraphClientSecret() != null && !configuracion.getGraphClientSecret().isBlank());
        snapshot.put("graphPrivateKeyConfigurada", configuracion.getGraphPrivateKeyPem() != null && !configuracion.getGraphPrivateKeyPem().isBlank());
        return serializarAuditoria(snapshot);
    }

    private String snapshotConfiguracionWhatsapp(ConfiguracionWhatsappEmpresaEntidad configuracion) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("habilitado", configuracion.isHabilitado());
        snapshot.put("accountSid", configuracion.getAccountSid());
        snapshot.put("authTokenConfigurado", configuracion.getAuthToken() != null && !configuracion.getAuthToken().isBlank());
        snapshot.put("tipoCuentaTwilio", configuracion.getTipoCuentaTwilio());
        snapshot.put("subaccountSid", configuracion.getSubaccountSid());
        snapshot.put("numeroRemitente", configuracion.getNumeroRemitente());
        snapshot.put("messagingServiceSid", configuracion.getMessagingServiceSid());
        snapshot.put("channelSenderSid", configuracion.getChannelSenderSid());
        snapshot.put("statusCallbackUrl", configuracion.getStatusCallbackUrl());
        snapshot.put("senderDisplayName", configuracion.getSenderDisplayName());
        snapshot.put("senderPhoneNumber", configuracion.getSenderPhoneNumber());
        snapshot.put("senderStatus", configuracion.getSenderStatus());
        snapshot.put("qualityRating", configuracion.getQualityRating());
        snapshot.put("throughputMps", configuracion.getThroughputMps());
        snapshot.put("wabaId", configuracion.getWabaId());
        snapshot.put("metaBusinessManagerId", configuracion.getMetaBusinessManagerId());
        snapshot.put("plantillaSolicitudConfirmacionSid", configuracion.getPlantillaSolicitudConfirmacionSid());
        snapshot.put("plantillaReprogramadaPendienteSid", configuracion.getPlantillaReprogramadaPendienteSid());
        snapshot.put("plantillaRecordatorioConfirmacionSid", configuracion.getPlantillaRecordatorioConfirmacionSid());
        snapshot.put("plantillaCitaConfirmadaSid", configuracion.getPlantillaCitaConfirmadaSid());
        snapshot.put("plantillaRecordatorioSid", configuracion.getPlantillaRecordatorioSid());
        snapshot.put("plantillaCancelacionSid", configuracion.getPlantillaCancelacionSid());
        snapshot.put("plantillaLiberadaSinConfirmacionSid", configuracion.getPlantillaLiberadaSinConfirmacionSid());
        snapshot.put("plantillaGraciasVisitaSid", configuracion.getPlantillaGraciasVisitaSid());
        snapshot.put("plantillaRecordatorioRegresoSid", configuracion.getPlantillaRecordatorioRegresoSid());
        snapshot.put("plantillaEspacioDisponibleWalkinSid", configuracion.getPlantillaEspacioDisponibleWalkinSid());
        snapshot.put("plantillaMenuBienvenidaSid", configuracion.getPlantillaMenuBienvenidaSid());
        snapshot.put("plantillasListPickerSids", configuracion.getPlantillasListPickerSids());
        return serializarAuditoria(snapshot);
    }

    private String snapshotConfiguracionSitio(EmpresaSitioConfigEntidad configuracion) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("empresaId", configuracion.getEmpresaId());
        snapshot.put("slug", configuracion.getSlug());
        snapshot.put("nombreComercial", configuracion.getNombreComercial());
        snapshot.put("dominioPrincipal", configuracion.getDominioPrincipal());
        snapshot.put("logoUrl", configuracion.getLogoUrl());
        snapshot.put("descripcionCorta", configuracion.getDescripcionCorta());
        snapshot.put("colorPrimario", configuracion.getColorPrimario());
        snapshot.put("colorSecundario", configuracion.getColorSecundario());
        snapshot.put("fuenteTitulos", configuracion.getFuenteTitulos());
        snapshot.put("fuenteCuerpo", configuracion.getFuenteCuerpo());
        snapshot.put("heroTitulo", configuracion.getHeroTitulo());
        snapshot.put("heroSubtitulo", configuracion.getHeroSubtitulo());
        snapshot.put("heroImagenUrl", configuracion.getHeroImagenUrl());
        snapshot.put("whatsapp", configuracion.getWhatsapp());
        snapshot.put("telefono", configuracion.getTelefono());
        snapshot.put("correo", configuracion.getCorreo());
        snapshot.put("direccion", configuracion.getDireccion());
        snapshot.put("instagramUrl", configuracion.getInstagramUrl());
        snapshot.put("facebookUrl", configuracion.getFacebookUrl());
        snapshot.put("tema", configuracion.getTema());
        snapshot.put("publicado", configuracion.isPublicado());
        return serializarAuditoria(snapshot);
    }

    private String normalizarCodigoRolEmpresa(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Debes indicar un codigo para el rol");
        }
        return codigo.trim().toUpperCase().replaceAll("[^A-Z0-9_]", "_");
    }

    private List<String> normalizarPermisosRol(List<String> permisos) {
        return permisos != null
                ? permisos.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(valor -> !valor.isBlank())
                .map(String::toUpperCase)
                .distinct()
                .sorted()
                .toList()
                : List.of();
    }

    private void validarPermisosRolInterno(List<String> permisos) {
        if (permisos.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Debes seleccionar al menos un permiso para el rol");
        }
        if (permisos.stream().noneMatch(PERMISOS_ACCESO_INTERNO::contains)) {
            throw new ResponseStatusException(BAD_REQUEST, "El rol debe incluir al menos un permiso de acceso interno");
        }
    }

    private void guardarPermisosRolEmpresa(RolEmpresaEntidad rolEmpresa, List<String> codigosPermiso) {
        List<PermisoEntidad> permisos = permisoRepositorio.findByCodigoIn(codigosPermiso);
        if (permisos.size() != codigosPermiso.size()) {
            throw new ResponseStatusException(BAD_REQUEST, "Uno o más permisos indicados no existen");
        }

        rolEmpresaPermisoRepositorio.deleteByRolEmpresa_Id(rolEmpresa.getId());
        List<RolEmpresaPermisoEntidad> permisosRol = permisos.stream()
                .map(permiso -> new RolEmpresaPermisoEntidad(
                        new RolEmpresaPermisoId(rolEmpresa.getId(), permiso.getId()),
                        rolEmpresa,
                        permiso
                ))
                .toList();
        rolEmpresaPermisoRepositorio.saveAll(permisosRol);
    }

    private List<String> normalizarPermisosDirectosUsuario(List<String> permisosDirectos, RolEmpresaEntidad rolEmpresa) {
        List<String> permisosNormalizados = normalizarPermisosRol(permisosDirectos);
        if (permisosNormalizados.isEmpty() || rolEmpresa == null) {
            return permisosNormalizados;
        }

        List<String> permisosRol = servicioRolesEmpresa.obtenerPermisosPorRolEmpresa(List.of(rolEmpresa.getId()))
                .getOrDefault(rolEmpresa.getId(), List.of());
        return permisosNormalizados.stream()
                .filter(permiso -> !permisosRol.contains(permiso))
                .toList();
    }

    private void guardarPermisosDirectosUsuario(UsuarioEntidad usuario, List<String> codigosPermiso) {
        usuarioPermisoEmpresaRepositorio.deleteByUsuario_Id(usuario.getId());
        if (codigosPermiso == null || codigosPermiso.isEmpty()) {
            return;
        }

        List<PermisoEntidad> permisos = permisoRepositorio.findByCodigoIn(codigosPermiso);
        if (permisos.size() != codigosPermiso.size()) {
            throw new ResponseStatusException(BAD_REQUEST, "Uno o más permisos directos indicados no existen");
        }

        List<UsuarioPermisoEmpresaEntidad> permisosUsuario = permisos.stream()
                .map(permiso -> new UsuarioPermisoEmpresaEntidad(
                        new UsuarioPermisoEmpresaId(usuario.getId(), permiso.getId()),
                        usuario,
                        permiso
                ))
                .toList();
        usuarioPermisoEmpresaRepositorio.saveAll(permisosUsuario);
    }

    private void validarEmpresaExiste(Long empresaId) {
        if (!empresaRepositorio.existsById(empresaId)) {
            throw new ResponseStatusException(NOT_FOUND, "La empresa indicada no existe");
        }
    }

    private void validarSucursalUsuarioInterno(Long empresaId, Long sucursalId) {
        if (sucursalId == null) {
            return;
        }
        sucursalRepositorio.findByIdAndEmpresaId(sucursalId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sucursal indicada no existe para la empresa"));
    }

    private void asignarRolUsuarioInterno(UsuarioEntidad usuario, Long empresaId, RolEmpresaEntidad rolEmpresa) {
        servicioRolesEmpresa.asignarRolEmpresa(usuario, empresaId, rolEmpresa.getCodigo());
    }

    private void reasignarRolUsuarioInterno(UsuarioEntidad usuario, Long empresaId, RolEmpresaEntidad rolEmpresa) {
        List<String> rolesInternos = listarRolesInternos(empresaId).stream()
                .map(RolInternoAdminResponse::codigo)
                .toList();
        servicioRolesEmpresa.reasignarRolesPorCodigo(usuario, empresaId, rolesInternos, rolEmpresa.getCodigo());
        asignarRolUsuarioInterno(usuario, empresaId, rolEmpresa);
    }

    private UsuarioInternoAdminResponse construirUsuarioInternoResponse(
            UsuarioEntidad usuario,
            UsuarioInternoPerfilEntidad perfil,
            RolEmpresaEntidad rolEmpresa,
            Long empresaId
    ) {
        Map<Long, String> sucursalNombres = sucursalRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .collect(Collectors.toMap(SucursalEntidad::getId, SucursalEntidad::getNombre));
        List<Long> sucursalesScope = usuarioInternoSucursalRepositorio.findByUsuario_Id(usuario.getId()).stream()
                .filter(asignacion -> asignacion.getSucursal().getEmpresaId().equals(empresaId))
                .map(asignacion -> asignacion.getSucursal().getId())
                .distinct()
                .sorted()
                .toList();
        List<String> permisosRol = rolEmpresa != null
                ? servicioRolesEmpresa.obtenerPermisosPorRolEmpresa(List.of(rolEmpresa.getId())).getOrDefault(rolEmpresa.getId(), List.of())
                : List.of();
        List<String> permisosDirectos = servicioRolesEmpresa.obtenerPermisosDirectosUsuario(empresaId, usuario.getId());
        return mapearUsuarioInterno(usuario, perfil, rolEmpresa, sucursalesScope, sucursalNombres, permisosRol, permisosDirectos);
    }

    private UsuarioInternoAdminResponse mapearUsuarioInterno(
            UsuarioEntidad usuario,
            UsuarioInternoPerfilEntidad perfil,
            List<UsuarioRolEmpresaEntidad> roles,
            List<Long> sucursalesScope,
            Map<Long, String> sucursalNombres,
            Map<Long, List<String>> permisosPorRol,
            List<String> permisosDirectos
    ) {
        RolEmpresaEntidad rolEmpresa = roles.stream()
                .map(UsuarioRolEmpresaEntidad::getRolEmpresa)
                .filter(rol -> esRolInternoAsignable(permisosPorRol.getOrDefault(rol.getId(), List.of())))
                .findFirst()
                .orElse(null);
        if (rolEmpresa == null || perfil == null) {
            return null;
        }

        return mapearUsuarioInterno(
                usuario,
                perfil,
                rolEmpresa,
                sucursalesScope,
                sucursalNombres,
                permisosPorRol.getOrDefault(rolEmpresa.getId(), List.of()),
                permisosDirectos
        );
    }

    private UsuarioInternoAdminResponse mapearUsuarioInterno(
            UsuarioEntidad usuario,
            UsuarioInternoPerfilEntidad perfil,
            RolEmpresaEntidad rolEmpresa,
            List<Long> sucursalesScope,
            Map<Long, String> sucursalNombres,
            List<String> permisosRol,
            List<String> permisosDirectos
    ) {
        if (perfil == null) {
            return null;
        }

        List<String> permisosRolOrdenados = permisosRol.stream().distinct().sorted().toList();
        List<String> permisosDirectosOrdenados = permisosDirectos.stream().distinct().sorted().toList();
        return new UsuarioInternoAdminResponse(
                usuario.getId(),
                perfil.getSucursalId(),
                perfil.getSucursalId() != null ? sucursalNombres.getOrDefault(perfil.getSucursalId(), "Sucursal") : null,
                sucursalesScope,
                sucursalesScope.stream()
                        .map(id -> sucursalNombres.getOrDefault(id, "Sucursal"))
                        .toList(),
                usuario.getCorreo(),
                perfil.getNombreCompleto(),
                perfil.getTelefono(),
                perfil.getPuesto(),
                rolEmpresa != null ? rolEmpresa.getId() : null,
                rolEmpresa != null ? rolEmpresa.getCodigo() : null,
                rolEmpresa != null ? rolEmpresa.getNombre() : null,
                permisosRolOrdenados,
                permisosDirectosOrdenados,
                combinarPermisos(permisosRolOrdenados, permisosDirectosOrdenados),
                usuario.isHabilitado() && !usuario.isBloqueado(),
                perfil.getNotas()
        );
    }

    private List<String> combinarPermisos(List<String> permisosRol, List<String> permisosDirectos) {
        LinkedHashSet<String> permisos = new LinkedHashSet<>();
        permisos.addAll(permisosRol != null ? permisosRol : List.of());
        permisos.addAll(permisosDirectos != null ? permisosDirectos : List.of());
        return permisos.stream().sorted().toList();
    }

    private List<Long> validarSucursalesPermitidasUsuarioInterno(Long empresaId, List<Long> sucursalIds, Long sucursalBaseId) {
        List<Long> ids = sucursalIds != null
                ? sucursalIds.stream().filter(java.util.Objects::nonNull).distinct().sorted().toList()
                : List.of();
        if (ids.isEmpty()) {
            return List.of();
        }

        List<SucursalEntidad> sucursales = sucursalRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .filter(sucursal -> ids.contains(sucursal.getId()))
                .toList();
        if (sucursales.size() != ids.size()) {
            throw new ResponseStatusException(NOT_FOUND, "Una o mas sucursales permitidas no existen para la empresa");
        }
        if (sucursalBaseId != null && ids.stream().noneMatch(sucursalBaseId::equals)) {
            throw new ResponseStatusException(BAD_REQUEST, "La sucursal base debe formar parte de las sucursales permitidas");
        }
        return ids;
    }

    private void sincronizarSucursalesPermitidas(UsuarioEntidad usuario, List<Long> sucursalIds) {
        usuarioInternoSucursalRepositorio.deleteByUsuario_Id(usuario.getId());
        if (sucursalIds == null || sucursalIds.isEmpty()) {
            return;
        }

        Map<Long, SucursalEntidad> sucursalesPorId = sucursalRepositorio.findByEmpresaIdOrderByNombreAsc(usuario.getEmpresaId()).stream()
                .filter(sucursal -> sucursalIds.contains(sucursal.getId()))
                .collect(Collectors.toMap(SucursalEntidad::getId, sucursal -> sucursal));

        List<UsuarioInternoSucursalEntidad> asignaciones = sucursalIds.stream()
                .map(sucursalId -> new UsuarioInternoSucursalEntidad(
                        new UsuarioInternoSucursalId(usuario.getId(), sucursalId),
                        usuario,
                        sucursalesPorId.get(sucursalId)
                ))
                .toList();
        usuarioInternoSucursalRepositorio.saveAll(asignaciones);
    }

    private boolean esRolInternoAsignable(List<String> permisos) {
        return permisos.stream().anyMatch(PERMISOS_ACCESO_INTERNO::contains);
    }

    private Map<Long, ServicioEntidad> validarServiciosAsignados(Long empresaId, Long sucursalId, List<Long> servicioIds) {
        List<Long> idsSolicitados = servicioIds != null ? servicioIds.stream().distinct().toList() : List.of();
        if (idsSolicitados.isEmpty()) {
            return Map.of();
        }

        List<ServicioEntidad> servicios = servicioRepositorio.findByEmpresaIdAndIdIn(empresaId, idsSolicitados);
        if (servicios.size() != idsSolicitados.size()) {
            throw new ResponseStatusException(NOT_FOUND, "Uno o mas servicios no existen para la empresa");
        }

        boolean todosEnSucursal = servicios.stream()
                .allMatch(servicio -> servicioSucursalRepositorio
                        .existsByEmpresaIdAndIdServicioIdAndIdSucursalIdAndActivoTrue(empresaId, servicio.getId(), sucursalId));
        if (!todosEnSucursal) {
            throw new ResponseStatusException(CONFLICT, "Todos los servicios asignados deben pertenecer a la misma sucursal del prestador");
        }

        return servicios.stream().collect(Collectors.toMap(ServicioEntidad::getId, servicio -> servicio));
    }

    private void sincronizarAsignaciones(Long prestadorId, List<Long> servicioIds) {
        Set<Long> seleccionados = servicioIds != null ? new HashSet<>(servicioIds) : Collections.emptySet();
        List<AsignacionServicioPrestadorEntidad> actuales = asignacionServicioPrestadorRepositorio.findByIdPrestadorId(prestadorId);
        Map<Long, AsignacionServicioPrestadorEntidad> actualesPorServicio = actuales.stream()
                .collect(Collectors.toMap(asignacion -> asignacion.getId().getServicioId(), asignacion -> asignacion));

        for (AsignacionServicioPrestadorEntidad asignacion : actuales) {
            asignacion.setActiva(seleccionados.contains(asignacion.getId().getServicioId()));
        }

        for (Long servicioId : seleccionados) {
            if (!actualesPorServicio.containsKey(servicioId)) {
                AsignacionServicioPrestadorEntidad nueva = new AsignacionServicioPrestadorEntidad();
                nueva.setId(new AsignacionServicioPrestadorId(prestadorId, servicioId));
                nueva.setActiva(true);
                asignacionServicioPrestadorRepositorio.save(nueva);
            }
        }

        if (!actuales.isEmpty()) {
            asignacionServicioPrestadorRepositorio.saveAll(actuales);
        }
    }

    private PrestadorAdminResponse mapearPrestador(
            PrestadorServicioEntidad prestador,
            UsuarioEntidad usuario,
            String sucursalNombre,
            Collection<AsignacionServicioPrestadorEntidad> asignaciones,
            Map<Long, ServicioEntidad> servicios
    ) {
        if (usuario == null) {
            return null;
        }

        List<ServicioEntidad> serviciosAsignados = asignaciones.stream()
                .filter(AsignacionServicioPrestadorEntidad::isActiva)
                .map(asignacion -> servicios.get(asignacion.getId().getServicioId()))
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(ServicioEntidad::getNombre))
                .toList();

        return new PrestadorAdminResponse(
                prestador.getUsuarioId(),
                prestador.getSucursalId(),
                sucursalNombre,
                usuario.getCorreo(),
                prestador.getNombreMostrar(),
                prestador.getBiografia(),
                prestador.getColorAgenda(),
                prestador.isActivo(),
                serviciosAsignados.stream().map(ServicioEntidad::getId).toList(),
                serviciosAsignados.stream().map(ServicioEntidad::getNombre).toList()
        );
    }

    private long contarEstado(List<CitaEntidad> citas, String estado) {
        return citas.stream().filter(cita -> estado.equals(cita.getEstado())).count();
    }

    private BigDecimal sumarIngresos(List<CitaEntidad> citas, Set<String> estados) {
        return citas.stream()
                .filter(cita -> estados.contains(cita.getEstado()))
                .map(CitaEntidad::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularPromedio(BigDecimal total, long cantidad) {
        if (cantidad <= 0) {
            return BigDecimal.ZERO;
        }
        return total.divide(BigDecimal.valueOf(cantidad), 2, java.math.RoundingMode.HALF_UP);
    }

    private record CatalogoSugeridoDef(
            String id,
            String nombre,
            String descripcion,
            List<GrupoSugeridoDef> grupos
    ) {
    }

    private record GrupoSugeridoDef(
            String nombre,
            String descripcion,
            String imagenUrl,
            String icono,
            int ordenPublico,
            List<SubgrupoSugeridoDef> subgrupos
    ) {
    }

    private record SubgrupoSugeridoDef(
            String nombre,
            String descripcion,
            int ordenPublico,
            List<ServicioSugeridoDef> servicios
    ) {
    }

    private record ServicioSugeridoDef(
            String nombre,
            String descripcion,
            int duracionMinutos,
            int bufferAntesMinutos,
            int bufferDespuesMinutos,
            BigDecimal precio,
            String moneda,
            int ordenPublico,
            boolean requiereAnticipo,
            String anticipoTipo,
            BigDecimal anticipoValor
    ) {
    }
}
