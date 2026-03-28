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
import com.techprotech.agenda.compartido.whatsapp.ResultadoEnvioWhatsapp;
import com.techprotech.agenda.compartido.whatsapp.ServicioConfiguracionWhatsappEmpresa;
import com.techprotech.agenda.compartido.whatsapp.ServicioOutboxWhatsappCitas;
import com.techprotech.agenda.modulos.admin.api.dto.ConfiguracionCorreoAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.ConfiguracionCorreoAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ConfiguracionWhatsappAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.ConfiguracionWhatsappAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.DetectarChannelSenderWhatsappResponse;
import com.techprotech.agenda.modulos.admin.api.dto.LogMensajeWhatsappAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.MigracionSecretosCorreoResponse;
import com.techprotech.agenda.modulos.admin.api.dto.AsociarChannelSenderWhatsappRequest;
import com.techprotech.agenda.modulos.admin.api.dto.AsociarChannelSenderWhatsappResponse;
import com.techprotech.agenda.modulos.admin.api.dto.PlantillaWhatsappAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.PrestadorAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.PrestadorAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ProvisionarSubcuentaWhatsappRequest;
import com.techprotech.agenda.modulos.admin.api.dto.ProvisionarMessagingServiceWhatsappRequest;
import com.techprotech.agenda.modulos.admin.api.dto.ProvisionarMessagingServiceWhatsappResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ProvisionarSubcuentaWhatsappResponse;
import com.techprotech.agenda.modulos.admin.api.dto.PruebaPlantillaWhatsappRequest;
import com.techprotech.agenda.modulos.admin.api.dto.PruebaPlantillaWhatsappResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ReporteServicioAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ServicioAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.ServicioAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.SucursalAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.SucursalAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.UsuarioInternoAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.UsuarioInternoAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ResumenAdminResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.RolEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioInternoPerfilEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioRolEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioRolId;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.RolRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioInternoPerfilRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRolRepositorio;
import com.techprotech.agenda.modulos.citas.api.dto.CitaClienteResponse;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.HistorialEstadoCitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.HistorialEstadoCitaRepositorio;
import com.techprotech.agenda.modulos.prestadores.infraestructura.entidad.PrestadorServicioEntidad;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.AsignacionServicioPrestadorEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.AsignacionServicioPrestadorId;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.AsignacionServicioPrestadorRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Comparator;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class ServicioAdminCitas {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioAdminCitas.class);
    private static final Set<String> ROLES_USUARIO_INTERNO = Set.of("ADMIN", "RECEPCIONISTA", "CAJERO");

    private final CitaRepositorio citaRepositorio;
    private final HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio;
    private final SucursalRepositorio sucursalRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final PrestadorServicioRepositorio prestadorServicioRepositorio;
    private final AsignacionServicioPrestadorRepositorio asignacionServicioPrestadorRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final UsuarioInternoPerfilRepositorio usuarioInternoPerfilRepositorio;
    private final UsuarioRolRepositorio usuarioRolRepositorio;
    private final RolRepositorio rolRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final EmpresaRepositorio empresaRepositorio;
    private final ConfiguracionCorreoEmpresaRepositorio configuracionCorreoEmpresaRepositorio;
    private final ConfiguracionWhatsappEmpresaRepositorio configuracionWhatsappEmpresaRepositorio;
    private final ServicioConfiguracionWhatsappEmpresa servicioConfiguracionWhatsappEmpresa;
    private final ClienteWhatsappTwilio clienteWhatsappTwilio;
    private final ServicioOutboxWhatsappCitas servicioOutboxWhatsappCitas;
    private final BandejaSalidaNotificacionRepositorio bandejaSalidaNotificacionRepositorio;
    private final ObjectMapper objectMapper;
    private final ProtectorSecretosCorreo protectorSecretosCorreo;

    public ServicioAdminCitas(
            CitaRepositorio citaRepositorio,
            HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio,
            SucursalRepositorio sucursalRepositorio,
            ServicioRepositorio servicioRepositorio,
            PrestadorServicioRepositorio prestadorServicioRepositorio,
            AsignacionServicioPrestadorRepositorio asignacionServicioPrestadorRepositorio,
            ClienteRepositorio clienteRepositorio,
            UsuarioRepositorio usuarioRepositorio,
            UsuarioInternoPerfilRepositorio usuarioInternoPerfilRepositorio,
            UsuarioRolRepositorio usuarioRolRepositorio,
            RolRepositorio rolRepositorio,
            PasswordEncoder passwordEncoder,
            EmpresaRepositorio empresaRepositorio,
            ConfiguracionCorreoEmpresaRepositorio configuracionCorreoEmpresaRepositorio,
            ConfiguracionWhatsappEmpresaRepositorio configuracionWhatsappEmpresaRepositorio,
            ServicioConfiguracionWhatsappEmpresa servicioConfiguracionWhatsappEmpresa,
            ClienteWhatsappTwilio clienteWhatsappTwilio,
            ServicioOutboxWhatsappCitas servicioOutboxWhatsappCitas,
            BandejaSalidaNotificacionRepositorio bandejaSalidaNotificacionRepositorio,
            ObjectMapper objectMapper,
            ProtectorSecretosCorreo protectorSecretosCorreo
    ) {
        this.citaRepositorio = citaRepositorio;
        this.historialEstadoCitaRepositorio = historialEstadoCitaRepositorio;
        this.sucursalRepositorio = sucursalRepositorio;
        this.servicioRepositorio = servicioRepositorio;
        this.prestadorServicioRepositorio = prestadorServicioRepositorio;
        this.asignacionServicioPrestadorRepositorio = asignacionServicioPrestadorRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.usuarioInternoPerfilRepositorio = usuarioInternoPerfilRepositorio;
        this.usuarioRolRepositorio = usuarioRolRepositorio;
        this.rolRepositorio = rolRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.empresaRepositorio = empresaRepositorio;
        this.configuracionCorreoEmpresaRepositorio = configuracionCorreoEmpresaRepositorio;
        this.configuracionWhatsappEmpresaRepositorio = configuracionWhatsappEmpresaRepositorio;
        this.servicioConfiguracionWhatsappEmpresa = servicioConfiguracionWhatsappEmpresa;
        this.clienteWhatsappTwilio = clienteWhatsappTwilio;
        this.servicioOutboxWhatsappCitas = servicioOutboxWhatsappCitas;
        this.bandejaSalidaNotificacionRepositorio = bandejaSalidaNotificacionRepositorio;
        this.objectMapper = objectMapper;
        this.protectorSecretosCorreo = protectorSecretosCorreo;
    }

    @Transactional(readOnly = true)
    public ResumenAdminResponse resumen(Long empresaId) {
        List<CitaEntidad> citas = citaRepositorio.findByEmpresaIdOrderByInicioDesc(empresaId);
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
    public List<ReporteServicioAdminResponse> reporteServicios(Long empresaId) {
        Map<Long, ServicioEntidad> servicios = servicioRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .collect(Collectors.toMap(ServicioEntidad::getId, servicio -> servicio));
        return citaRepositorio.findByEmpresaIdOrderByInicioDesc(empresaId).stream()
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

    @Transactional
    public ConfiguracionCorreoAdminResponse actualizarConfiguracionCorreo(Long empresaId, ConfiguracionCorreoAdminRequest request) {
        ConfiguracionCorreoEmpresaEntidad configuracion = configuracionCorreoEmpresaRepositorio.findById(empresaId)
                .orElseGet(() -> {
                    ConfiguracionCorreoEmpresaEntidad nueva = new ConfiguracionCorreoEmpresaEntidad();
                    nueva.setEmpresaId(empresaId);
                    return nueva;
        });

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

        return mapearConfiguracionCorreo(configuracionCorreoEmpresaRepositorio.save(configuracion));
    }

    @Transactional
    public MigracionSecretosCorreoResponse migrarSecretosCorreo(Long empresaId) {
        ConfiguracionCorreoEmpresaEntidad configuracion = configuracionCorreoEmpresaRepositorio.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No existe configuracion de correo para la empresa"));

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
            if (mensaje.length() > 0) {
                mensaje.append("; ");
            }
            mensaje.append("graph_client_secret migrado");
            actualizada = true;
        }

        String graphPrivateKeyPem = configuracion.getGraphPrivateKeyPem();
        if (graphPrivateKeyPem != null && !graphPrivateKeyPem.isBlank() && !graphPrivateKeyPem.startsWith("enc:v1:")) {
            configuracion.setGraphPrivateKeyPem(protectorSecretosCorreo.encriptar(graphPrivateKeyPem));
            if (mensaje.length() > 0) {
                mensaje.append("; ");
            }
            mensaje.append("graph_private_key_pem migrado");
            actualizada = true;
        }

        if (!actualizada) {
            return new MigracionSecretosCorreoResponse(false, "No hay secretos legacy por migrar");
        }

        configuracionCorreoEmpresaRepositorio.save(configuracion);
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
    public ConfiguracionWhatsappAdminResponse actualizarConfiguracionWhatsapp(Long empresaId, ConfiguracionWhatsappAdminRequest request) {
        ConfiguracionWhatsappEmpresaEntidad configuracion = configuracionWhatsappEmpresaRepositorio.findById(empresaId)
                .orElseGet(() -> {
                    ConfiguracionWhatsappEmpresaEntidad nueva = new ConfiguracionWhatsappEmpresaEntidad();
                    nueva.setEmpresaId(empresaId);
                    return nueva;
                });

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

        return mapearConfiguracionWhatsapp(configuracionWhatsappEmpresaRepositorio.save(configuracion));
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
    public List<LogMensajeWhatsappAdminResponse> listarLogsWhatsapp(Long empresaId) {
        ConfiguracionWhatsappResolvida configuracion = servicioConfiguracionWhatsappEmpresa.resolver(empresaId);
        return bandejaSalidaNotificacionRepositorio.findTop50ByEmpresaIdAndCanalOrderByIdDesc(empresaId, "WHATSAPP")
                .stream()
                .map(log -> mapearLogWhatsapp(log, configuracion))
                .toList();
    }

    @Transactional
    public ProvisionarSubcuentaWhatsappResponse provisionarSubcuentaWhatsapp(
            Long empresaId,
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

        ConfiguracionWhatsappEmpresaEntidad guardada = configuracionWhatsappEmpresaRepositorio.save(configuracion);
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

        configuracion.setMessagingServiceSid(messagingService.sid());
        ConfiguracionWhatsappEmpresaEntidad guardada = configuracionWhatsappEmpresaRepositorio.save(configuracion);
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
            AsociarChannelSenderWhatsappRequest request
    ) {
        ConfiguracionWhatsappEmpresaEntidad configuracion = configuracionWhatsappEmpresaRepositorio.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No existe configuracion de WhatsApp para este tenant"));

        String messagingServiceSid = normalizarOpcional(configuracion.getMessagingServiceSid());
        if (messagingServiceSid == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Primero debes crear o capturar el Messaging Service SID del tenant");
        }

        String channelSenderSid = request.channelSenderSid().trim();
        clienteWhatsappTwilio.asociarChannelSenderAMessagingService(empresaId, messagingServiceSid, channelSenderSid);
        configuracion.setChannelSenderSid(channelSenderSid);
        ConfiguracionWhatsappEmpresaEntidad guardada = configuracionWhatsappEmpresaRepositorio.save(configuracion);
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
    public DetectarChannelSenderWhatsappResponse detectarChannelSenderWhatsapp(Long empresaId) {
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
                Map.of(
                        "1", request.nombreCliente().trim(),
                        "2", request.fecha().trim(),
                        "3", request.hora().trim()
                )
        );

        registrarLogPruebaWhatsapp(empresaId, request, plantillaSid, resultado);
        return new PruebaPlantillaWhatsappResponse(
                true,
                "La prueba de WhatsApp fue enviada a Twilio" + (resultado.proveedorMensajeId() != null ? " con Message SID " + resultado.proveedorMensajeId() : "")
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
                        sucursalRepositorio.findById(cita.getSucursalId()).map(s -> s.getNombre()).orElse("Sucursal"),
                        servicioRepositorio.findById(cita.getServicioId()).map(s -> s.getNombre()).orElse("Servicio"),
                        prestadorServicioRepositorio.findById(cita.getPrestadorId()).map(p -> p.getNombreMostrar()).orElse("Prestador"),
                        cita.getInicio().atZone(ZoneId.of(sucursalRepositorio.findById(cita.getSucursalId()).map(s -> s.getZonaHoraria()).orElse("America/Mexico_City"))).toOffsetDateTime(),
                        cita.getFin().atZone(ZoneId.of(sucursalRepositorio.findById(cita.getSucursalId()).map(s -> s.getZonaHoraria()).orElse("America/Mexico_City"))).toOffsetDateTime(),
                        cita.getPrecio(),
                        cita.getMoneda(),
                        cita.getNotas(),
                        false,
                        clienteRepositorio.findById(cita.getClienteId()).map(cliente -> cliente.getNombreCompleto()).orElse("Cliente"),
                        usuarioRepositorio.findById(cita.getClienteId()).map(usuario -> usuario.getCorreo()).orElse(""),
                        clienteRepositorio.findById(cita.getClienteId()).map(cliente -> cliente.getTelefono()).orElse("")
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
    public List<ServicioAdminResponse> listarServicios(Long empresaId) {
        Map<Long, String> sucursales = sucursalRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .collect(java.util.stream.Collectors.toMap(SucursalEntidad::getId, SucursalEntidad::getNombre));
        return servicioRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId)
                .stream()
                .map(servicio -> mapearServicio(servicio, sucursales.getOrDefault(servicio.getSucursalId(), "Sucursal")))
                .toList();
    }

    @Transactional
    public ServicioAdminResponse crearServicio(Long empresaId, ServicioAdminRequest request) {
        SucursalEntidad sucursal = sucursalRepositorio.findByIdAndEmpresaId(request.sucursalId(), empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sucursal indicada no existe para la empresa"));
        ServicioEntidad servicio = new ServicioEntidad();
        servicio.setEmpresaId(empresaId);
        aplicarServicio(servicio, request);
        return mapearServicio(servicioRepositorio.save(servicio), sucursal.getNombre());
    }

    @Transactional
    public ServicioAdminResponse actualizarServicio(Long empresaId, Long servicioId, ServicioAdminRequest request) {
        ServicioEntidad servicio = servicioRepositorio.findByIdAndEmpresaId(servicioId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El servicio no existe para la empresa"));
        SucursalEntidad sucursal = sucursalRepositorio.findByIdAndEmpresaId(request.sucursalId(), empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sucursal indicada no existe para la empresa"));
        aplicarServicio(servicio, request);
        return mapearServicio(servicioRepositorio.save(servicio), sucursal.getNombre());
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
        Map<Long, List<UsuarioRolEntidad>> rolesPorUsuario = usuarioRolRepositorio.findByUsuario_IdIn(usuarioIds).stream()
                .collect(Collectors.groupingBy(usuarioRol -> usuarioRol.getUsuario().getId()));
        Map<Long, UsuarioInternoPerfilEntidad> perfilesPorUsuario = usuarioInternoPerfilRepositorio.findByUsuarioIdIn(usuarioIds).stream()
                .collect(Collectors.toMap(UsuarioInternoPerfilEntidad::getUsuarioId, perfil -> perfil));
        Map<Long, String> sucursalNombres = sucursalRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .collect(Collectors.toMap(SucursalEntidad::getId, SucursalEntidad::getNombre));

        return usuarios.stream()
                .map(usuario -> mapearUsuarioInterno(usuario, perfilesPorUsuario.get(usuario.getId()), rolesPorUsuario.getOrDefault(usuario.getId(), List.of()), sucursalNombres))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional
    public UsuarioInternoAdminResponse crearUsuarioInterno(Long empresaId, UsuarioInternoAdminRequest request) {
        validarRolUsuarioInterno(request.rolCodigo());
        validarSucursalUsuarioInterno(empresaId, request.sucursalId());

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

        asignarRolUsuarioInterno(usuario, empresaId, request.rolCodigo().trim().toUpperCase());
        return construirUsuarioInternoResponse(usuario, perfil, request.rolCodigo().trim().toUpperCase(), empresaId);
    }

    @Transactional
    public UsuarioInternoAdminResponse actualizarUsuarioInterno(Long empresaId, Long usuarioId, UsuarioInternoAdminRequest request) {
        validarRolUsuarioInterno(request.rolCodigo());
        validarSucursalUsuarioInterno(empresaId, request.sucursalId());

        UsuarioEntidad usuario = usuarioRepositorio.findByIdAndEmpresaId(usuarioId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El usuario interno no existe para la empresa"));
        UsuarioInternoPerfilEntidad perfil = usuarioInternoPerfilRepositorio.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El perfil interno no existe"));

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

        reasignarRolUsuarioInterno(usuario, empresaId, request.rolCodigo().trim().toUpperCase());
        return construirUsuarioInternoResponse(usuario, perfil, request.rolCodigo().trim().toUpperCase(), empresaId);
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
                graphPrivateKeyCifrada
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
                configuracion.getSenderDisplayName(),
                configuracion.getSenderPhoneNumber(),
                configuracion.getSenderStatus(),
                configuracion.getQualityRating(),
                configuracion.getThroughputMps(),
                configuracion.getWabaId(),
                configuracion.getMetaBusinessManagerId()
        );
    }

    private String normalizarOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
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

    private void aplicarServicio(ServicioEntidad servicio, ServicioAdminRequest request) {
        servicio.setSucursalId(request.sucursalId());
        servicio.setNombre(request.nombre().trim());
        servicio.setDescripcion(request.descripcion() != null ? request.descripcion().trim() : null);
        servicio.setDuracionMinutos(request.duracionMinutos());
        servicio.setBufferAntesMinutos(request.bufferAntesMinutos());
        servicio.setBufferDespuesMinutos(request.bufferDespuesMinutos());
        servicio.setPrecio(request.precio());
        servicio.setMoneda(request.moneda().trim().toUpperCase());
        servicio.setActivo(request.activo());
    }

    private ServicioAdminResponse mapearServicio(ServicioEntidad servicio, String sucursalNombre) {
        return new ServicioAdminResponse(
                servicio.getId(),
                servicio.getSucursalId(),
                sucursalNombre,
                servicio.getNombre(),
                servicio.getDescripcion(),
                servicio.getDuracionMinutos(),
                servicio.getBufferAntesMinutos(),
                servicio.getBufferDespuesMinutos(),
                servicio.getPrecio(),
                servicio.getMoneda(),
                servicio.isActivo()
        );
    }

    private void aplicarPrestador(PrestadorServicioEntidad prestador, PrestadorAdminRequest request) {
        prestador.setSucursalId(request.sucursalId());
        prestador.setNombreMostrar(request.nombreMostrar().trim());
        prestador.setBiografia(request.biografia() != null ? request.biografia().trim() : null);
        prestador.setColorAgenda(request.colorAgenda() != null && !request.colorAgenda().isBlank() ? request.colorAgenda().trim() : "#2563eb");
        prestador.setActivo(request.activo());
    }

    private void asignarRolStaff(UsuarioEntidad usuario, Long empresaId) {
        RolEntidad rolStaff = rolRepositorio.findByCodigo("STAFF")
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No existe el rol STAFF"));
        UsuarioRolId usuarioRolId = new UsuarioRolId(usuario.getId(), rolStaff.getId(), empresaId);
        if (!usuarioRolRepositorio.existsById(usuarioRolId)) {
            usuarioRolRepositorio.save(new UsuarioRolEntidad(usuarioRolId, usuario, rolStaff));
        }
    }

    private void aplicarUsuarioInterno(UsuarioInternoPerfilEntidad perfil, UsuarioInternoAdminRequest request) {
        perfil.setSucursalId(request.sucursalId());
        perfil.setNombreCompleto(request.nombreCompleto().trim());
        perfil.setTelefono(request.telefono() != null && !request.telefono().isBlank() ? request.telefono().trim() : null);
        perfil.setPuesto(request.puesto() != null && !request.puesto().isBlank() ? request.puesto().trim() : null);
        perfil.setNotas(request.notas() != null && !request.notas().isBlank() ? request.notas().trim() : null);
    }

    private void validarRolUsuarioInterno(String rolCodigo) {
        String rolNormalizado = rolCodigo != null ? rolCodigo.trim().toUpperCase() : "";
        if (!ROLES_USUARIO_INTERNO.contains(rolNormalizado)) {
            throw new ResponseStatusException(BAD_REQUEST, "El rol interno indicado no es valido");
        }
    }

    private void validarSucursalUsuarioInterno(Long empresaId, Long sucursalId) {
        if (sucursalId == null) {
            return;
        }
        sucursalRepositorio.findByIdAndEmpresaId(sucursalId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sucursal indicada no existe para la empresa"));
    }

    private void asignarRolUsuarioInterno(UsuarioEntidad usuario, Long empresaId, String rolCodigo) {
        RolEntidad rol = rolRepositorio.findByCodigo(rolCodigo)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No existe el rol " + rolCodigo));
        UsuarioRolId usuarioRolId = new UsuarioRolId(usuario.getId(), rol.getId(), empresaId);
        if (!usuarioRolRepositorio.existsById(usuarioRolId)) {
            usuarioRolRepositorio.save(new UsuarioRolEntidad(usuarioRolId, usuario, rol));
        }
    }

    private void reasignarRolUsuarioInterno(UsuarioEntidad usuario, Long empresaId, String rolCodigo) {
        usuarioRolRepositorio.deleteByUsuario_IdAndIdEmpresaIdAndRol_CodigoIn(usuario.getId(), empresaId, ROLES_USUARIO_INTERNO);
        asignarRolUsuarioInterno(usuario, empresaId, rolCodigo);
    }

    private UsuarioInternoAdminResponse construirUsuarioInternoResponse(
            UsuarioEntidad usuario,
            UsuarioInternoPerfilEntidad perfil,
            String rolCodigo,
            Long empresaId
    ) {
        Map<Long, String> sucursalNombres = sucursalRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .collect(Collectors.toMap(SucursalEntidad::getId, SucursalEntidad::getNombre));
        return mapearUsuarioInterno(usuario, perfil, rolCodigo, sucursalNombres);
    }

    private UsuarioInternoAdminResponse mapearUsuarioInterno(
            UsuarioEntidad usuario,
            UsuarioInternoPerfilEntidad perfil,
            List<UsuarioRolEntidad> roles,
            Map<Long, String> sucursalNombres
    ) {
        String rolCodigo = roles.stream()
                .map(usuarioRol -> usuarioRol.getRol().getCodigo())
                .filter(ROLES_USUARIO_INTERNO::contains)
                .findFirst()
                .orElse(null);
        if (rolCodigo == null || perfil == null) {
            return null;
        }

        return mapearUsuarioInterno(usuario, perfil, rolCodigo, sucursalNombres);
    }

    private UsuarioInternoAdminResponse mapearUsuarioInterno(
            UsuarioEntidad usuario,
            UsuarioInternoPerfilEntidad perfil,
            String rolCodigo,
            Map<Long, String> sucursalNombres
    ) {
        if (perfil == null) {
            return null;
        }

        return new UsuarioInternoAdminResponse(
                usuario.getId(),
                perfil.getSucursalId(),
                perfil.getSucursalId() != null ? sucursalNombres.getOrDefault(perfil.getSucursalId(), "Sucursal") : null,
                usuario.getCorreo(),
                perfil.getNombreCompleto(),
                perfil.getTelefono(),
                perfil.getPuesto(),
                rolCodigo,
                usuario.isHabilitado() && !usuario.isBloqueado(),
                perfil.getNotas()
        );
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

        boolean todosEnSucursal = servicios.stream().allMatch(servicio -> servicio.getSucursalId().equals(sucursalId));
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
}
