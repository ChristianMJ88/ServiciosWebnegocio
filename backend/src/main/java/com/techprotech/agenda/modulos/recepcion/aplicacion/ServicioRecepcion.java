package com.techprotech.agenda.modulos.recepcion.aplicacion;

import com.techprotech.agenda.compartido.whatsapp.ClienteWhatsappTwilio;
import com.techprotech.agenda.compartido.whatsapp.ServicioConfiguracionWhatsappEmpresa;
import com.techprotech.agenda.compartido.whatsapp.ServicioOutboxWhatsappCitas;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.ClienteEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.citas.api.dto.CitaClienteResponse;
import com.techprotech.agenda.modulos.citas.api.dto.CitaCreadaResponse;
import com.techprotech.agenda.modulos.citas.api.dto.CrearCitaRequest;
import com.techprotech.agenda.modulos.citas.aplicacion.ServicioCitas;
import com.techprotech.agenda.modulos.citas.aplicacion.ServicioCitasCliente;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.HistorialEstadoCitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.HistorialEstadoCitaRepositorio;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.modulos.recepcion.api.dto.CatalogoRecepcionResponse;
import static com.techprotech.agenda.modulos.recepcion.aplicacion.CatalogosRecepcion.ESTADO_CITA_PENDIENTE;
import static com.techprotech.agenda.modulos.recepcion.aplicacion.CatalogosRecepcion.ESTADO_CITA_CONFIRMADA;
import static com.techprotech.agenda.modulos.recepcion.aplicacion.CatalogosRecepcion.ESTADOS_CITA;
import static com.techprotech.agenda.modulos.recepcion.aplicacion.CatalogosRecepcion.ESTADOS_CITA_CANCELABLES;
import static com.techprotech.agenda.modulos.recepcion.aplicacion.CatalogosRecepcion.ESTADOS_CITA_FINALIZABLES;
import static com.techprotech.agenda.modulos.recepcion.aplicacion.CatalogosRecepcion.ESTADOS_ESPERA;
import static com.techprotech.agenda.modulos.recepcion.aplicacion.CatalogosRecepcion.ESTADO_ESPERA_NOTIFICADA;
import static com.techprotech.agenda.modulos.recepcion.aplicacion.CatalogosRecepcion.ESTADO_ESPERA_PENDIENTE;
import com.techprotech.agenda.modulos.recepcion.api.dto.CitaRecepcionResponse;
import com.techprotech.agenda.modulos.recepcion.api.dto.ClienteRecepcionResponse;
import com.techprotech.agenda.modulos.recepcion.api.dto.CrearCitaRecepcionRequest;
import com.techprotech.agenda.modulos.recepcion.api.dto.CrearSolicitudEsperaRecepcionRequest;
import com.techprotech.agenda.modulos.recepcion.api.dto.FranjaRecepcionDisponibleResponse;
import com.techprotech.agenda.modulos.recepcion.api.dto.ReagendarRecepcionRequest;
import com.techprotech.agenda.modulos.recepcion.api.dto.ServicioRecepcionCatalogoResponse;
import com.techprotech.agenda.modulos.recepcion.api.dto.SolicitudEsperaRecepcionResponse;
import com.techprotech.agenda.modulos.recepcion.api.dto.SucursalRecepcionCatalogoResponse;
import com.techprotech.agenda.modulos.recepcion.infraestructura.entidad.SolicitudEsperaRecepcionEntidad;
import com.techprotech.agenda.modulos.recepcion.infraestructura.repositorio.SolicitudEsperaRecepcionRepositorio;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.FranjaDisponibleResponse;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioConsultaDisponibilidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioSucursalEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioSucursalRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServicioRecepcion {

    private final CitaRepositorio citaRepositorio;
    private final HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final SucursalRepositorio sucursalRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final ServicioSucursalRepositorio servicioSucursalRepositorio;
    private final PrestadorServicioRepositorio prestadorServicioRepositorio;
    private final ServicioCitas servicioCitas;
    private final ServicioCitasCliente servicioCitasCliente;
    private final ServicioOutboxWhatsappCitas servicioOutboxWhatsappCitas;
    private final ServicioConsultaDisponibilidad servicioConsultaDisponibilidad;
    private final SolicitudEsperaRecepcionRepositorio solicitudEsperaRecepcionRepositorio;
    private final ClienteWhatsappTwilio clienteWhatsappTwilio;
    private final ServicioConfiguracionWhatsappEmpresa servicioConfiguracionWhatsappEmpresa;

    public ServicioRecepcion(
            CitaRepositorio citaRepositorio,
            HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio,
            ClienteRepositorio clienteRepositorio,
            UsuarioRepositorio usuarioRepositorio,
            SucursalRepositorio sucursalRepositorio,
            ServicioRepositorio servicioRepositorio,
            ServicioSucursalRepositorio servicioSucursalRepositorio,
            PrestadorServicioRepositorio prestadorServicioRepositorio,
            ServicioCitas servicioCitas,
            ServicioCitasCliente servicioCitasCliente,
            ServicioOutboxWhatsappCitas servicioOutboxWhatsappCitas,
            ServicioConsultaDisponibilidad servicioConsultaDisponibilidad,
            SolicitudEsperaRecepcionRepositorio solicitudEsperaRecepcionRepositorio,
            ClienteWhatsappTwilio clienteWhatsappTwilio,
            ServicioConfiguracionWhatsappEmpresa servicioConfiguracionWhatsappEmpresa
    ) {
        this.citaRepositorio = citaRepositorio;
        this.historialEstadoCitaRepositorio = historialEstadoCitaRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.sucursalRepositorio = sucursalRepositorio;
        this.servicioRepositorio = servicioRepositorio;
        this.servicioSucursalRepositorio = servicioSucursalRepositorio;
        this.prestadorServicioRepositorio = prestadorServicioRepositorio;
        this.servicioCitas = servicioCitas;
        this.servicioCitasCliente = servicioCitasCliente;
        this.servicioOutboxWhatsappCitas = servicioOutboxWhatsappCitas;
        this.servicioConsultaDisponibilidad = servicioConsultaDisponibilidad;
        this.solicitudEsperaRecepcionRepositorio = solicitudEsperaRecepcionRepositorio;
        this.clienteWhatsappTwilio = clienteWhatsappTwilio;
        this.servicioConfiguracionWhatsappEmpresa = servicioConfiguracionWhatsappEmpresa;
    }

    @Transactional(readOnly = true)
    public CatalogoRecepcionResponse catalogo(Long empresaId, List<Long> sucursalesPermitidas, Long sucursalId) {
        if (sucursalId != null) {
            validarAccesoSucursal(sucursalesPermitidas, sucursalId);
        }

        List<SucursalEntidad> sucursalesActivas = sucursalRepositorio.findByEmpresaIdAndActivaTrue(empresaId).stream()
                .filter(sucursal -> !tieneScopeSucursales(sucursalesPermitidas) || sucursalesPermitidas.contains(sucursal.getId()))
                .toList();

        List<SucursalEntidad> sucursalesDisponibles = !sucursalesActivas.isEmpty()
                ? sucursalesActivas
                : sucursalRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .filter(sucursal -> !tieneScopeSucursales(sucursalesPermitidas) || sucursalesPermitidas.contains(sucursal.getId()))
                .toList();

        Long sucursalOperativaId = sucursalId != null
                ? sucursalId
                : sucursalesPermitidas != null && !sucursalesPermitidas.isEmpty()
                ? sucursalesPermitidas.get(0)
                : sucursalesDisponibles.stream().findFirst().map(SucursalEntidad::getId).orElse(null);

        List<ServicioRecepcionCatalogoResponse> servicios = List.of();
        if (sucursalOperativaId != null) {
            servicios = servicioRepositorio.findBySucursalIdAndActivoTrue(sucursalOperativaId).stream()
                    .map(servicio -> new ServicioRecepcionCatalogoResponse(
                            servicio.getId(),
                            sucursalOperativaId,
                            servicio.getNombre(),
                            servicio.getDescripcion(),
                            servicio.getDuracionMinutos(),
                            servicio.getBufferAntesMinutos(),
                            servicio.getBufferDespuesMinutos(),
                            servicio.getPrecio(),
                            servicio.getMoneda()
                    ))
                    .toList();

            if (servicios.isEmpty()) {
                Map<Long, ServicioSucursalEntidad> asignacionesPorServicio = servicioSucursalRepositorio
                        .findByEmpresaIdAndIdSucursalIdInAndActivoTrue(
                                empresaId,
                                sucursalesDisponibles.stream().map(SucursalEntidad::getId).toList()
                        ).stream()
                        .collect(Collectors.toMap(asignacion -> asignacion.getId().getServicioId(), Function.identity(), (primera, segunda) -> primera));
                servicios = servicioRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                        .filter(servicio -> servicio.isActivo())
                        .filter(servicio -> asignacionesPorServicio.containsKey(servicio.getId()))
                        .map(servicio -> new ServicioRecepcionCatalogoResponse(
                                servicio.getId(),
                                asignacionesPorServicio.get(servicio.getId()).getId().getSucursalId(),
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

        return new CatalogoRecepcionResponse(
                sucursalOperativaId,
                sucursalesDisponibles.stream()
                        .map(sucursal -> new SucursalRecepcionCatalogoResponse(
                                sucursal.getId(),
                                sucursal.getEmpresaId(),
                                sucursal.getNombre(),
                                sucursal.getDireccion(),
                                sucursal.getTelefono(),
                                sucursal.getZonaHoraria()
                        ))
                        .toList(),
                servicios,
                ESTADOS_CITA.stream()
                        .map(opcion -> new CatalogoRecepcionResponse.OpcionRecepcionResponse(opcion.codigo(), opcion.etiqueta()))
                        .toList(),
                ESTADO_CITA_PENDIENTE,
                ESTADO_CITA_CONFIRMADA,
                ESTADOS_CITA_FINALIZABLES,
                ESTADOS_CITA_CANCELABLES,
                ESTADOS_ESPERA.stream()
                        .map(opcion -> new CatalogoRecepcionResponse.OpcionRecepcionResponse(opcion.codigo(), opcion.etiqueta()))
                        .toList(),
                ESTADO_ESPERA_PENDIENTE,
                ESTADO_ESPERA_NOTIFICADA
        );
    }

    @Transactional(readOnly = true)
    public List<CitaRecepcionResponse> agenda(Long empresaId, List<Long> sucursalesPermitidas, LocalDate fecha, Long sucursalId) {
        LocalDate fechaOperativa = fecha != null ? fecha : LocalDate.now();
        if (sucursalId != null) {
            validarAccesoSucursal(sucursalesPermitidas, sucursalId);
        }
        List<CitaEntidad> citas = citaRepositorio.findByEmpresaIdAndInicioBetweenOrderByInicioAsc(
                empresaId,
                fechaOperativa.atStartOfDay(),
                fechaOperativa.plusDays(1).atStartOfDay()
        );

        return citas.stream()
                .filter(cita -> sucursalId == null || sucursalId.equals(cita.getSucursalId()))
                .filter(cita -> !tieneScopeSucursales(sucursalesPermitidas) || sucursalesPermitidas.contains(cita.getSucursalId()))
                .map(this::mapearCita)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClienteRecepcionResponse> buscarClientes(Long empresaId, String texto) {
        String consulta = texto == null ? "" : texto.trim();
        if (consulta.length() < 2) {
            return List.of();
        }

        List<ClienteEntidad> clientes = clienteRepositorio.buscarPorNombreOTelefono(consulta);
        Map<Long, UsuarioEntidad> usuarios = usuarioRepositorio.findByEmpresaIdAndIdIn(
                        empresaId,
                        clientes.stream().map(ClienteEntidad::getUsuarioId).toList()
                ).stream()
                .collect(Collectors.toMap(UsuarioEntidad::getId, Function.identity()));

        return clientes.stream()
                .filter(cliente -> usuarios.containsKey(cliente.getUsuarioId()))
                .limit(12)
                .map(cliente -> new ClienteRecepcionResponse(
                        cliente.getUsuarioId(),
                        cliente.getNombreCompleto(),
                        cliente.getTelefono(),
                        usuarios.get(cliente.getUsuarioId()).getCorreo(),
                        cliente.isAceptaWhatsapp()
                ))
                .toList();
    }

    @Transactional
    public CitaCreadaResponse crearCita(Long empresaId, List<Long> sucursalesPermitidas, CrearCitaRecepcionRequest request) {
        validarAccesoSucursal(sucursalesPermitidas, request.sucursalId());
        return servicioCitas.crearCita(new CrearCitaRequest(
                empresaId,
                request.sucursalId(),
                request.servicioId(),
                request.prestadorId(),
                request.nombreCliente(),
                request.correoCliente(),
                request.telefonoCliente(),
                request.inicio(),
                request.notas()
        ));
    }

    @Transactional(readOnly = true)
    public List<SolicitudEsperaRecepcionResponse> solicitudesEspera(
            Long empresaId,
            List<Long> sucursalesPermitidas,
            LocalDate fecha,
            Long sucursalId
    ) {
        LocalDate fechaOperativa = fecha != null ? fecha : LocalDate.now();
        if (sucursalId != null) {
            validarAccesoSucursal(sucursalesPermitidas, sucursalId);
        }

        return solicitudEsperaRecepcionRepositorio.findByEmpresaIdAndFechaDeseadaOrderByCreadaEnAsc(empresaId, fechaOperativa).stream()
                .filter(solicitud -> sucursalId == null || sucursalId.equals(solicitud.getSucursalId()))
                .filter(solicitud -> !tieneScopeSucursales(sucursalesPermitidas) || sucursalesPermitidas.contains(solicitud.getSucursalId()))
                .map(this::mapearSolicitudEspera)
                .toList();
    }

    @Transactional
    public SolicitudEsperaRecepcionResponse registrarEspera(
            Long empresaId,
            Long usuarioId,
            List<Long> sucursalesPermitidas,
            CrearSolicitudEsperaRecepcionRequest request
    ) {
        validarAccesoSucursal(sucursalesPermitidas, request.sucursalId());
        servicioRepositorio.findById(request.servicioId())
                .filter(servicio -> servicio.getEmpresaId().equals(empresaId))
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "El servicio no pertenece a la empresa"));

        Long clienteId = null;
        if (request.clienteId() != null) {
            usuarioRepositorio.findByIdAndEmpresaId(request.clienteId(), empresaId)
                    .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "El cliente indicado no pertenece a la empresa"));
            clienteId = request.clienteId();
        }

        SolicitudEsperaRecepcionEntidad solicitud = new SolicitudEsperaRecepcionEntidad();
        solicitud.setEmpresaId(empresaId);
        solicitud.setSucursalId(request.sucursalId());
        solicitud.setServicioId(request.servicioId());
        solicitud.setClienteId(clienteId);
        solicitud.setNombreCliente(request.nombreCliente().trim());
        solicitud.setTelefonoCliente(request.telefonoCliente().trim());
        solicitud.setFechaDeseada(request.fechaDeseada());
        solicitud.setHoraDesde(request.horaDesde());
        solicitud.setHoraHasta(request.horaHasta());
        solicitud.setAceptaWhatsapp(request.aceptaWhatsapp());
        solicitud.setCanalOrigen(normalizarCanalOrigen(request.canalOrigen()));
        solicitud.setEstado("PENDIENTE");
        solicitud.setNotas(normalizarOpcional(request.notas()));
        solicitud.setCreadoPorUsuarioId(usuarioId);

        return mapearSolicitudEspera(solicitudEsperaRecepcionRepositorio.save(solicitud));
    }

    @Transactional
    public SolicitudEsperaRecepcionResponse notificarEspera(
            Long empresaId,
            Long usuarioId,
            List<Long> sucursalesPermitidas,
            Long solicitudId
    ) {
        SolicitudEsperaRecepcionEntidad solicitud = solicitudEsperaRecepcionRepositorio.findById(solicitudId)
                .filter(item -> item.getEmpresaId().equals(empresaId))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La solicitud de espera no existe para la empresa"));

        validarAccesoSucursal(sucursalesPermitidas, solicitud.getSucursalId());

        if (!"PENDIENTE".equalsIgnoreCase(solicitud.getEstado())) {
            throw new ResponseStatusException(BAD_REQUEST, "Solo se pueden notificar solicitudes en espera pendientes");
        }
        if (!solicitud.isAceptaWhatsapp()) {
            throw new ResponseStatusException(BAD_REQUEST, "Esta solicitud está marcada para seguimiento sin WhatsApp");
        }

        String plantillaSid = servicioConfiguracionWhatsappEmpresa.resolver(empresaId).plantillaEspacioDisponibleWalkinSid();
        if (plantillaSid == null || plantillaSid.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "La empresa aún no tiene configurada una plantilla aprobada para espacio disponible walk-in");
        }

        List<FranjaDisponibleResponse> franjas = servicioConsultaDisponibilidad.obtenerFranjasDisponibles(
                empresaId,
                solicitud.getSucursalId(),
                solicitud.getServicioId(),
                null,
                solicitud.getFechaDeseada()
        );

        ZoneId zona = sucursalRepositorio.findById(solicitud.getSucursalId())
                .map(SucursalEntidad::getZonaHoraria)
                .map(ZoneId::of)
                .orElse(ZoneId.of("America/Mexico_City"));

        ZonedDateTime ahora = ZonedDateTime.now(zona);
        FranjaDisponibleResponse franja = franjas.stream()
                .filter(item -> !OffsetDateTime.parse(item.inicio()).atZoneSameInstant(zona).isBefore(ahora))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "No hay un horario real disponible para notificar en este momento"));

        String sucursalNombre = sucursalRepositorio.findById(solicitud.getSucursalId())
                .map(SucursalEntidad::getNombre)
                .orElse("Sucursal");
        String servicioNombre = servicioRepositorio.findById(solicitud.getServicioId())
                .map(servicio -> servicio.getNombre())
                .orElse("Servicio");
        OffsetDateTime inicio = OffsetDateTime.parse(franja.inicio());
        String horaVisible = inicio.atZoneSameInstant(zona).toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));

        clienteWhatsappTwilio.enviarPlantilla(
                empresaId,
                solicitud.getTelefonoCliente(),
                plantillaSid,
                Map.of(
                        "1", solicitud.getNombreCliente(),
                        "2", servicioNombre,
                        "3", sucursalNombre,
                        "4", horaVisible
                )
        );

        solicitud.setEstado("NOTIFICADO");
        solicitud.setNotificadaEn(LocalDateTime.now());
        solicitud.setCreadoPorUsuarioId(usuarioId);

        return mapearSolicitudEspera(solicitudEsperaRecepcionRepositorio.save(solicitud));
    }

    @Transactional(readOnly = true)
    public List<FranjaRecepcionDisponibleResponse> franjasDisponibles(
            Long empresaId,
            List<Long> sucursalesPermitidas,
            Long sucursalId,
            Long servicioId,
            Long prestadorId,
            LocalDate fecha
    ) {
        validarAccesoSucursal(sucursalesPermitidas, sucursalId);

        List<FranjaDisponibleResponse> franjas = servicioConsultaDisponibilidad.obtenerFranjasDisponibles(
                empresaId,
                sucursalId,
                servicioId,
                prestadorId,
                fecha != null ? fecha : LocalDate.now()
        );

        ZoneId zona = sucursalRepositorio.findById(sucursalId)
                .map(SucursalEntidad::getZonaHoraria)
                .map(ZoneId::of)
                .orElse(ZoneId.of("America/Mexico_City"));

        ZonedDateTime ahora = ZonedDateTime.now(zona);

        return franjas.stream()
                .map(franja -> {
                    OffsetDateTime inicio = OffsetDateTime.parse(franja.inicio());
                    OffsetDateTime fin = OffsetDateTime.parse(franja.fin());
                    return new FranjaRecepcionDisponibleResponse(
                            franja.inicio(),
                            franja.fin(),
                            String.format("%02d:%02d", inicio.getHour(), inicio.getMinute()),
                            franja.prestadorId(),
                            franja.servicioId(),
                            franja.sucursalId(),
                            inicio,
                            fin
                    );
                })
                .filter(franja -> !franja.inicioAt().atZoneSameInstant(zona).isBefore(ahora))
                .limit(8)
                .toList();
    }

    @Transactional
    public CitaRecepcionResponse checkIn(Long empresaId, Long usuarioId, List<Long> sucursalesPermitidas, Long citaId) {
        CitaEntidad cita = obtenerCitaEmpresa(empresaId, sucursalesPermitidas, citaId);
        if (!List.of("PENDIENTE", "CONFIRMADA").contains(cita.getEstado())) {
            throw new ResponseStatusException(BAD_REQUEST, "Solo se puede hacer check-in a citas pendientes o confirmadas");
        }

        cita.setCheckInEn(LocalDateTime.now());
        cita.setCheckInPorUsuarioId(usuarioId);
        if ("PENDIENTE".equals(cita.getEstado())) {
            String estadoAnterior = cita.getEstado();
            cita.setEstado("CONFIRMADA");
            guardarHistorial(cita.getId(), estadoAnterior, "CONFIRMADA", usuarioId, "Check-in en recepción");
        }
        return mapearCita(citaRepositorio.save(cita));
    }

    @Transactional
    public CitaRecepcionResponse confirmar(Long empresaId, Long usuarioId, List<Long> sucursalesPermitidas, Long citaId) {
        CitaEntidad cita = obtenerCitaEmpresa(empresaId, sucursalesPermitidas, citaId);
        if (!"PENDIENTE".equals(cita.getEstado())) {
            throw new ResponseStatusException(BAD_REQUEST, "Solo se pueden confirmar citas pendientes");
        }

        String estadoAnterior = cita.getEstado();
        cita.setEstado("CONFIRMADA");
        citaRepositorio.save(cita);
        guardarHistorial(cita.getId(), estadoAnterior, "CONFIRMADA", usuarioId, "Confirmación desde recepción");
        return mapearCita(cita);
    }

    @Transactional
    public CitaRecepcionResponse finalizar(Long empresaId, Long usuarioId, List<Long> sucursalesPermitidas, Long citaId) {
        CitaEntidad cita = obtenerCitaEmpresa(empresaId, sucursalesPermitidas, citaId);
        if (!List.of("PENDIENTE", "CONFIRMADA").contains(cita.getEstado())) {
            throw new ResponseStatusException(BAD_REQUEST, "Solo se pueden finalizar citas pendientes o confirmadas");
        }

        String estadoAnterior = cita.getEstado();
        cita.setEstado("FINALIZADA");
        citaRepositorio.save(cita);
        guardarHistorial(cita.getId(), estadoAnterior, "FINALIZADA", usuarioId, "Cierre desde recepción");
        clienteRepositorio.findById(cita.getClienteId())
                .filter(cliente -> cliente.isAceptaWhatsapp() && cliente.getTelefono() != null && !cliente.getTelefono().isBlank())
                .ifPresent(cliente -> servicioOutboxWhatsappCitas.programarGraciasVisita(
                        empresaId,
                        cita.getId(),
                        cliente.getTelefono(),
                        cita.getInicio()
                ));
        return mapearCita(cita);
    }

    @Transactional
    public CitaRecepcionResponse cancelar(Long empresaId, Long usuarioId, List<Long> sucursalesPermitidas, Long citaId) {
        CitaEntidad cita = obtenerCitaEmpresa(empresaId, sucursalesPermitidas, citaId);
        if (!List.of("PENDIENTE", "CONFIRMADA").contains(cita.getEstado())) {
            throw new ResponseStatusException(BAD_REQUEST, "Solo se pueden cancelar citas pendientes o confirmadas");
        }

        String estadoAnterior = cita.getEstado();
        cita.setEstado("CANCELADA");
        cita.setCanceladaEn(LocalDateTime.now());
        cita.setMotivoCancelacion("Cancelada desde recepción");
        citaRepositorio.save(cita);
        guardarHistorial(cita.getId(), estadoAnterior, "CANCELADA", usuarioId, "Cancelación desde recepción");
        clienteRepositorio.findById(cita.getClienteId())
                .filter(cliente -> cliente.isAceptaWhatsapp() && cliente.getTelefono() != null && !cliente.getTelefono().isBlank())
                .ifPresent(cliente -> servicioOutboxWhatsappCitas.programarCancelacionNegocio(
                        empresaId,
                        cita.getId(),
                        cliente.getTelefono(),
                        cita.getInicio()
                ));
        return mapearCita(cita);
    }

    @Transactional
    public CitaClienteResponse reprogramar(Long empresaId, Long usuarioId, List<Long> sucursalesPermitidas, Long citaId, ReagendarRecepcionRequest request) {
        CitaEntidad cita = obtenerCitaEmpresa(empresaId, sucursalesPermitidas, citaId);
        return servicioCitasCliente.reprogramar(empresaId, cita.getClienteId(), citaId, request.nuevoInicio());
    }

    private CitaEntidad obtenerCitaEmpresa(Long empresaId, List<Long> sucursalesPermitidas, Long citaId) {
        CitaEntidad cita = citaRepositorio.findByIdAndEmpresaId(citaId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La cita no existe para la empresa"));
        validarAccesoSucursal(sucursalesPermitidas, cita.getSucursalId());
        return cita;
    }

    private boolean tieneScopeSucursales(List<Long> sucursalesPermitidas) {
        return sucursalesPermitidas != null && !sucursalesPermitidas.isEmpty();
    }

    private void validarAccesoSucursal(List<Long> sucursalesPermitidas, Long sucursalId) {
        if (tieneScopeSucursales(sucursalesPermitidas) && (sucursalId == null || !sucursalesPermitidas.contains(sucursalId))) {
            throw new ResponseStatusException(FORBIDDEN, "No tienes acceso a la sucursal indicada");
        }
    }

    private void guardarHistorial(Long citaId, String estadoAnterior, String estadoNuevo, Long usuarioId, String motivo) {
        HistorialEstadoCitaEntidad historial = new HistorialEstadoCitaEntidad();
        historial.setCitaId(citaId);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoNuevo);
        historial.setCambiadoPorUsuarioId(usuarioId);
        historial.setMotivo(motivo);
        historialEstadoCitaRepositorio.save(historial);
    }

    private SolicitudEsperaRecepcionResponse mapearSolicitudEspera(SolicitudEsperaRecepcionEntidad solicitud) {
        String sucursalNombre = sucursalRepositorio.findById(solicitud.getSucursalId())
                .map(SucursalEntidad::getNombre)
                .orElse("Sucursal");
        String servicioNombre = servicioRepositorio.findById(solicitud.getServicioId())
                .map(servicio -> servicio.getNombre())
                .orElse("Servicio");

        return new SolicitudEsperaRecepcionResponse(
                solicitud.getId(),
                solicitud.getSucursalId(),
                sucursalNombre,
                solicitud.getServicioId(),
                servicioNombre,
                solicitud.getClienteId(),
                solicitud.getNombreCliente(),
                solicitud.getTelefonoCliente(),
                solicitud.getFechaDeseada(),
                solicitud.getHoraDesde(),
                solicitud.getHoraHasta(),
                solicitud.isAceptaWhatsapp(),
                solicitud.getCanalOrigen(),
                solicitud.getEstado(),
                solicitud.getNotas(),
                solicitud.getCreadaEn(),
                solicitud.getNotificadaEn(),
                solicitud.getCerradoEn()
        );
    }

    private String normalizarCanalOrigen(String canalOrigen) {
        String valor = normalizarOpcional(canalOrigen);
        return valor != null ? valor.toUpperCase() : "MOSTRADOR";
    }

    private String normalizarOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isBlank() ? null : limpio;
    }

    private CitaRecepcionResponse mapearCita(CitaEntidad cita) {
        SucursalEntidad sucursal = sucursalRepositorio.findById(cita.getSucursalId()).orElse(null);
        String zonaHoraria = sucursal != null ? sucursal.getZonaHoraria() : "America/Mexico_City";
        String sucursalNombre = sucursal != null ? sucursal.getNombre() : "Sucursal";
        String servicioNombre = servicioRepositorio.findById(cita.getServicioId()).map(servicio -> servicio.getNombre()).orElse("Servicio");
        String prestadorNombre = prestadorServicioRepositorio.findById(cita.getPrestadorId()).map(prestador -> prestador.getNombreMostrar()).orElse("Prestador");
        String clienteNombre = clienteRepositorio.findById(cita.getClienteId()).map(ClienteEntidad::getNombreCompleto).orElse("Cliente");
        String clienteTelefono = clienteRepositorio.findById(cita.getClienteId()).map(ClienteEntidad::getTelefono).orElse("");
        String clienteCorreo = usuarioRepositorio.findById(cita.getClienteId()).map(UsuarioEntidad::getCorreo).orElse("");
        ZoneId zoneId = ZoneId.of(zonaHoraria);

        return new CitaRecepcionResponse(
                cita.getId(),
                cita.getEstado(),
                cita.getSucursalId(),
                cita.getServicioId(),
                cita.getPrestadorId(),
                sucursalNombre,
                servicioNombre,
                prestadorNombre,
                clienteNombre,
                clienteCorreo,
                clienteTelefono,
                cita.getInicio().atZone(zoneId).toOffsetDateTime(),
                cita.getFin().atZone(zoneId).toOffsetDateTime(),
                cita.getPrecio(),
                cita.getMoneda(),
                cita.getNotas(),
                cita.getCheckInEn(),
                cita.getCheckInPorUsuarioId()
        );
    }
}
