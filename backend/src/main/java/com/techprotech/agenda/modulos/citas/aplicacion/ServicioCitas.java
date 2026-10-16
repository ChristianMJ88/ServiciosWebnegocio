package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioCapacidadesEmpresa;

import com.techprotech.agenda.compartido.correo.ServicioCorreoCitas;
import com.techprotech.agenda.compartido.correo.ServicioOutboxCorreoCitas;
import com.techprotech.agenda.compartido.whatsapp.ServicioOutboxWhatsappCitas;
import com.techprotech.agenda.modulos.autenticacion.aplicacion.ServicioRolesEmpresa;
import com.techprotech.agenda.modulos.citas.api.dto.CitaCreadaResponse;
import com.techprotech.agenda.modulos.citas.api.dto.CitasMultiplesCreadasResponse;
import com.techprotech.agenda.modulos.citas.api.dto.CrearCitaMultipleItemRequest;
import com.techprotech.agenda.modulos.citas.api.dto.CrearCitaRequest;
import com.techprotech.agenda.modulos.citas.api.dto.CrearCitasMultiplesRequest;
import com.techprotech.agenda.modulos.clientes.infraestructura.entidad.ClienteEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.HistorialEstadoCitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.HistorialEstadoCitaRepositorio;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.FranjaDisponibleResponse;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioConsultaDisponibilidad;
import com.techprotech.agenda.modulos.prestadores.infraestructura.entidad.PrestadorServicioEntidad;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.AsignacionServicioPrestadorEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.AsignacionServicioPrestadorRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioSucursalRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServicioCitas {
    private final java.time.Clock reloj;

    private static final String PREFIJO_CONTRASENA_PROVISIONAL = "registro-cita";
    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioCitas.class);

    private final ServicioConsultaDisponibilidad servicioConsultaDisponibilidad;
    private final SucursalRepositorio sucursalRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final ServicioSucursalRepositorio servicioSucursalRepositorio;
    private final PrestadorServicioRepositorio prestadorServicioRepositorio;
    private final AsignacionServicioPrestadorRepositorio asignacionServicioPrestadorRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final EmpresaRepositorio empresaRepositorio;
    private final ServicioRolesEmpresa servicioRolesEmpresa;
    private final CitaRepositorio citaRepositorio;
    private final HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final ServicioCorreoCitas servicioCorreoCitas;
    private final ServicioOutboxCorreoCitas servicioOutboxCorreoCitas;
    private final ServicioOutboxWhatsappCitas servicioOutboxWhatsappCitas;
    private final ServicioCapacidadesEmpresa capacidades;
    private final ServicioCotizacionCitas cotizador;
    private final ServicioPoliticaReservaPago politicaPago;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public ServicioCitas(
            ServicioConsultaDisponibilidad servicioConsultaDisponibilidad,
            SucursalRepositorio sucursalRepositorio,
            ServicioRepositorio servicioRepositorio,
            ServicioSucursalRepositorio servicioSucursalRepositorio,
            PrestadorServicioRepositorio prestadorServicioRepositorio,
            AsignacionServicioPrestadorRepositorio asignacionServicioPrestadorRepositorio,
            UsuarioRepositorio usuarioRepositorio,
            ClienteRepositorio clienteRepositorio,
            EmpresaRepositorio empresaRepositorio,
            ServicioRolesEmpresa servicioRolesEmpresa,
            CitaRepositorio citaRepositorio,
            HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio,
            PasswordEncoder passwordEncoder,
            ServicioCorreoCitas servicioCorreoCitas,
            ServicioOutboxCorreoCitas servicioOutboxCorreoCitas,
            ServicioOutboxWhatsappCitas servicioOutboxWhatsappCitas,
            ServicioCapacidadesEmpresa capacidades,
            ServicioCotizacionCitas cotizador,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            ServicioPoliticaReservaPago politicaPago,
            java.time.Clock reloj
    ) {
        this.reloj = reloj;
        this.servicioConsultaDisponibilidad = servicioConsultaDisponibilidad;
        this.sucursalRepositorio = sucursalRepositorio;
        this.servicioRepositorio = servicioRepositorio;
        this.servicioSucursalRepositorio = servicioSucursalRepositorio;
        this.prestadorServicioRepositorio = prestadorServicioRepositorio;
        this.asignacionServicioPrestadorRepositorio = asignacionServicioPrestadorRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.empresaRepositorio = empresaRepositorio;
        this.servicioRolesEmpresa = servicioRolesEmpresa;
        this.citaRepositorio = citaRepositorio;
        this.historialEstadoCitaRepositorio = historialEstadoCitaRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.servicioCorreoCitas = servicioCorreoCitas;
        this.servicioOutboxCorreoCitas = servicioOutboxCorreoCitas;
        this.servicioOutboxWhatsappCitas = servicioOutboxWhatsappCitas;
        this.capacidades=capacidades;
        this.politicaPago=politicaPago;
        this.cotizador=cotizador;
        this.objectMapper=objectMapper;
    }

    @Transactional
    public CitaCreadaResponse crearCita(CrearCitaRequest request) {
        return crearCita(request, true, false);
    }

@Transactional
public CitaCreadaResponse crearCitaPublica(CrearCitaRequest request) { return crearCita(request,true,true); }
private CitaCreadaResponse crearCita(CrearCitaRequest request, boolean programarWhatsappConfirmacion, boolean publico) {

        if (request.empresaId() == null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "La empresa es obligatoria");
        Long empresaId = request.empresaId();
        capacidades.exigirSuscripcion(empresaId);
        EmpresaEntidad empresa = empresaRepositorio.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La empresa no existe"));

        SucursalEntidad sucursal = sucursalRepositorio.findByIdAndEmpresaIdAndActivaTrue(request.sucursalId(), empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sucursal no existe o no esta activa"));
        ServicioEntidad servicio = servicioRepositorio.findByIdAndEmpresaIdAndActivoTrue(request.servicioId(), empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El servicio no existe o no esta activo"));
        if (!servicioSucursalRepositorio.existsByEmpresaIdAndIdServicioIdAndIdSucursalIdAndActivoTrue(empresaId, request.servicioId(), request.sucursalId())) {
            throw new ResponseStatusException(NOT_FOUND, "El servicio no existe o no esta activo en la sucursal");
        }

        PrestadorServicioEntidad prestador = resolverPrestador(request.prestadorId(), request.sucursalId(), request.servicioId());
        ZoneId zona = ZoneId.of(sucursal.getZonaHoraria());
        OffsetDateTime inicioSolicitado = request.inicio().atZoneSameInstant(zona).toOffsetDateTime();
        LocalDate fechaLocal = inicioSolicitado.toLocalDate();

        List<FranjaDisponibleResponse> franjas = servicioConsultaDisponibilidad.obtenerFranjasDisponibles(
                empresaId,
                request.sucursalId(),
                request.servicioId(),
                prestador.getUsuarioId(),
                fechaLocal
        );

        FranjaDisponibleResponse franjaSeleccionada = franjas.stream()
                .filter(franja -> coincideInicioFranja(franja.inicio(), inicioSolicitado))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(CONFLICT, "La franja solicitada ya no esta disponible"));

        LocalDateTime inicio = OffsetDateTime.parse(franjaSeleccionada.inicio()).toLocalDateTime();
        LocalDateTime fin = OffsetDateTime.parse(franjaSeleccionada.fin()).toLocalDateTime();

        prestadorServicioRepositorio.bloquearPorEmpresa(empresaId, prestador.getUsuarioId())
                .orElseThrow(() -> new ResponseStatusException(CONFLICT, "El prestador ya no está disponible"));
        if (!citaRepositorio.buscarConflictosBloqueando(prestador.getUsuarioId(), inicio, fin, List.of("PENDIENTE", "PENDIENTE_PAGO", "CONFIRMADA")).isEmpty()) {
            throw new ResponseStatusException(CONFLICT, "Existe un conflicto de horario para el prestador");
        }

        var cotizacion = cotizador.cotizarPrecio(empresaId, request.sucursalId(), request.promocionId(),
                List.of(new com.techprotech.agenda.modulos.citas.api.dto.CotizarCitasRequest.ItemCotizacion(request.servicioId(),request.inicio())));
        if (request.promocionId()!=null && cotizacion.promocion()==null) throw new ResponseStatusException(CONFLICT,cotizacion.motivoNoAplicada());

        var politica = politicaServicio(cotizacion.lineas().getFirst().precioNeto(),servicio);
        var plan = politicaPago.plan(empresaId,publico,List.of(politica.importeRequerido()));
        Long clienteId = resolverOCrearCliente(
                empresaId,
                request.nombreCliente(),
                request.correoCliente(),
                request.telefonoCliente()
        );

        CitaEntidad cita = new CitaEntidad();
        cita.setEmpresaId(empresaId);
        cita.setSucursalId(request.sucursalId());
        cita.setServicioId(request.servicioId());
        cita.setPrestadorId(prestador.getUsuarioId());
        cita.setClienteId(clienteId);
        cita.setEstado("PENDIENTE");
        cita.setInicio(inicio);
        cita.setFin(fin);
        aplicarCotizacion(cita,cotizacion,0,request.promocionId());
        politicaPago.aplicar(cita,plan,politica);
        cita.setMoneda(servicio.getMoneda());
        cita.setNotas(request.notas());
        cita.setCreadaPorUsuarioId(clienteId);
        cita.setCreadaEn(LocalDateTime.now(reloj));
        cita = citaRepositorio.save(cita);

        HistorialEstadoCitaEntidad historial = new HistorialEstadoCitaEntidad();
        historial.setCitaId(cita.getId());
        historial.setEstadoAnterior(null);
        historial.setEstadoNuevo(cita.getEstado());
        historial.setCambiadoPorUsuarioId(clienteId);
        historial.setMotivo("Creacion inicial de cita");
        historialEstadoCitaRepositorio.save(historial);

        OffsetDateTime inicioRespuesta = cita.getInicio().atZone(zona).toOffsetDateTime();
        OffsetDateTime finRespuesta = cita.getFin().atZone(zona).toOffsetDateTime();
        boolean correoConfirmacionProgramado = !plan.requierePago() && servicioOutboxCorreoCitas.programarRegistrada(
                empresaId, cita.getId(), cita.getInicio());
        if (programarWhatsappConfirmacion && !plan.requierePago()) {
            servicioOutboxWhatsappCitas.programarConfirmacion(
                    empresaId,
                    cita.getId(),
                    request.telefonoCliente().trim(),
                    cita.getInicio()
            );
        }

        return new CitaCreadaResponse(
                cita.getId(),
                cita.getEstado(),
                cita.getEmpresaId(),
                cita.getSucursalId(),
                cita.getServicioId(),
                cita.getPrestadorId(),
                inicioRespuesta,
                finRespuesta,
                mensajeReserva(cita,plan,correoConfirmacionProgramado),
                correoConfirmacionProgramado,
                false,
                cita.getPrecioLista(), cita.getDescuento(), cita.getPrecio(),
                plan.requierePago(), cita.getImporteRequerido(), cita.getPagoExpiraEn()==null?null:cita.getPagoExpiraEn().atOffset(java.time.ZoneOffset.UTC), cita.getPoliticaCobro(), politicaPago.limiteConfirmacion(cita,zona)
        );
    }

    @Transactional
public CitasMultiplesCreadasResponse crearCitasMultiples(CrearCitasMultiplesRequest request) { return crearCitasMultiples(request,false); }
@Transactional
public CitasMultiplesCreadasResponse crearCitasMultiplesPublicas(CrearCitasMultiplesRequest request) { return crearCitasMultiples(request,true); }
private CitasMultiplesCreadasResponse crearCitasMultiples(CrearCitasMultiplesRequest request,boolean publico) {

        if (request.empresaId() == null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "La empresa es obligatoria");
        Long empresaId = request.empresaId();
        capacidades.exigirSuscripcion(empresaId);
        EmpresaEntidad empresa = empresaRepositorio.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La empresa no existe"));
        SucursalEntidad sucursal = sucursalRepositorio.findByIdAndEmpresaIdAndActivaTrue(request.sucursalId(), empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sucursal no existe o no esta activa"));
        ZoneId zona = ZoneId.of(sucursal.getZonaHoraria());

        List<CrearCitaMultipleItemRequest> items = request.items().stream()
                .sorted(Comparator.comparing(CrearCitaMultipleItemRequest::inicio))
                .toList();

        if (items.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Debes indicar al menos un servicio para reservar");
        }

        List<RangoReservaValidado> rangos = new ArrayList<>();
        for (CrearCitaMultipleItemRequest item : items) {
            List<FranjaDisponibleResponse> franjas = servicioConsultaDisponibilidad.obtenerFranjasDisponibles(
                    request.empresaId(),
                    request.sucursalId(),
                    item.servicioId(),
                    item.prestadorId(),
                    item.inicio().atZoneSameInstant(zona).toLocalDate()
            );

            FranjaDisponibleResponse franja = franjas.stream()
                    .filter(disponible -> coincideInicioFranja(disponible.inicio(), item.inicio()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(CONFLICT, "Uno de los horarios seleccionados ya no esta disponible"));

            rangos.add(new RangoReservaValidado(
                    item.servicioId(),
                    franja.prestadorId(),
                    OffsetDateTime.parse(franja.inicio()),
                    OffsetDateTime.parse(franja.fin())
            ));
        }

        for (int index = 1; index < rangos.size(); index++) {
            RangoReservaValidado anterior = rangos.get(index - 1);
            RangoReservaValidado actual = rangos.get(index);
            OffsetDateTime finAnterior = normalizarMinuto(anterior.fin());
            OffsetDateTime inicioActual = normalizarMinuto(actual.inicio());
            if (inicioActual.isBefore(finAnterior)) {
                LOGGER.warn(
                        "Conflicto al crear citas multiples empresa={} sucursal={} servicioAnterior={} finAnterior={} servicioActual={} inicioActual={}",
                        request.empresaId(),
                        request.sucursalId(),
                        anterior.servicioId(),
                        anterior.fin(),
                        actual.servicioId(),
                        actual.inicio()
                );
                throw new ResponseStatusException(CONFLICT, "Los servicios seleccionados se traslapan. Ajusta el itinerario antes de confirmar.");
            }
        }

        for (Long id : rangos.stream().map(RangoReservaValidado::prestadorId).distinct().sorted().toList()) {
            prestadorServicioRepositorio.bloquearPorEmpresa(empresaId, id)
                    .orElseThrow(() -> new ResponseStatusException(CONFLICT, "Un prestador ya no está disponible"));
        }
        for (var rango : rangos) {
            if (!citaRepositorio.buscarConflictosBloqueando(rango.prestadorId(), rango.inicio().toLocalDateTime(), rango.fin().toLocalDateTime(), List.of("PENDIENTE", "PENDIENTE_PAGO", "CONFIRMADA")).isEmpty())
                throw new ResponseStatusException(CONFLICT, "Uno de los horarios ya no está disponible");
        }
        var cotizacion = cotizador.cotizarPrecio(empresaId,request.sucursalId(),request.promocionId(),
                rangos.stream().map(r -> new com.techprotech.agenda.modulos.citas.api.dto.CotizarCitasRequest.ItemCotizacion(r.servicioId(),r.inicio())).toList());
        if (request.promocionId()!=null && cotizacion.promocion()==null) throw new ResponseStatusException(CONFLICT,cotizacion.motivoNoAplicada());
var politicas=new java.util.ArrayList<PoliticaCobroServicio>();
for(int i=0;i<rangos.size();i++) {
    var servicioPolitica=servicioRepositorio.findByIdAndEmpresaIdAndActivoTrue(rangos.get(i).servicioId(),empresaId).orElseThrow();
    politicas.add(politicaServicio(cotizacion.lineas().get(i).precioNeto(),servicioPolitica));
}
var plan=politicaPago.plan(empresaId,publico,politicas.stream().map(PoliticaCobroServicio::importeRequerido).toList());
        Long clienteId = resolverOCrearCliente(
                empresaId,
                request.nombreCliente(),
                request.correoCliente(),
                request.telefonoCliente()
        );
        List<CitaCreadaResponse> citas = new ArrayList<>();
        int indiceCotizacion = 0;
        for (RangoReservaValidado rango : rangos) {
            ServicioEntidad servicio = servicioRepositorio.findByIdAndEmpresaIdAndActivoTrue(rango.servicioId(), empresaId)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El servicio no existe o no esta activo"));

            CitaEntidad cita = new CitaEntidad();
            cita.setEmpresaId(empresaId);
            cita.setSucursalId(request.sucursalId());
            cita.setServicioId(rango.servicioId());
            cita.setPrestadorId(rango.prestadorId());
            cita.setClienteId(clienteId);
            cita.setEstado("PENDIENTE");
            cita.setInicio(rango.inicio().toLocalDateTime());
            cita.setFin(rango.fin().toLocalDateTime());
            aplicarCotizacion(cita,cotizacion,indiceCotizacion++,request.promocionId());
            politicaPago.aplicar(cita,plan,politicas.get(indiceCotizacion-1));
            cita.setMoneda(servicio.getMoneda());
            cita.setNotas(request.notas());
            cita.setCreadaPorUsuarioId(clienteId);
        cita.setCreadaEn(LocalDateTime.now(reloj));
            cita = citaRepositorio.save(cita);

            HistorialEstadoCitaEntidad historial = new HistorialEstadoCitaEntidad();
            historial.setCitaId(cita.getId());
            historial.setEstadoAnterior(null);
            historial.setEstadoNuevo(cita.getEstado());
            historial.setCambiadoPorUsuarioId(clienteId);
            historial.setMotivo("Creacion inicial de cita");
            historialEstadoCitaRepositorio.save(historial);

            OffsetDateTime inicioRespuesta = cita.getInicio().atZone(zona).toOffsetDateTime();
            OffsetDateTime finRespuesta = cita.getFin().atZone(zona).toOffsetDateTime();
            boolean correoConfirmacionProgramado = !plan.requierePago() && servicioOutboxCorreoCitas.programarRegistrada(
                    empresaId, cita.getId(), cita.getInicio());

            citas.add(new CitaCreadaResponse(
                    cita.getId(),
                    cita.getEstado(),
                    cita.getEmpresaId(),
                    cita.getSucursalId(),
                    cita.getServicioId(),
                    cita.getPrestadorId(),
                    inicioRespuesta,
                    finRespuesta,
                    mensajeReserva(cita,plan,correoConfirmacionProgramado),
                    correoConfirmacionProgramado,
                    false,
                    cita.getPrecioLista(), cita.getDescuento(), cita.getPrecio(),
                plan.requierePago(), cita.getImporteRequerido(), cita.getPagoExpiraEn()==null?null:cita.getPagoExpiraEn().atOffset(java.time.ZoneOffset.UTC), cita.getPoliticaCobro(), politicaPago.limiteConfirmacion(cita,zona)
            ));
        }

        if (!citas.isEmpty() && !plan.requierePago()) {
            servicioOutboxWhatsappCitas.programarConfirmacion(
                    empresaId,
                    citas.getFirst().id(),
                    request.telefonoCliente().trim(),
                    citas.getFirst().inicio().toLocalDateTime(),
                    citas.size()
            );
        }

        return new CitasMultiplesCreadasResponse(
                citas,
                citas.size(),
                "Las reservas fueron creadas correctamente sin choques de horario."
        );
    }

private String mensajeReserva(CitaEntidad cita,ServicioPoliticaReservaPago.PlanReserva plan,boolean correoProgramado) {
    if(plan.requierePago()) return "Tu horario está apartado y pendiente de pago. Se confirmará cuando el pago sea aprobado.";
    if(cita.getImporteRequerido()!=null && cita.getImporteRequerido().signum()>0) return "Tu cita está pendiente de confirmación. El anticipo se paga en el negocio.";
    return "Tu cita está pendiente de confirmación. Confírmala desde el mensaje que te enviamos antes del plazo indicado o se liberará el horario.";
}
private PoliticaCobroServicio politicaServicio(java.math.BigDecimal neto,ServicioEntidad servicio) {
    try { return PoliticaCobroServicio.resolver(neto,servicio); }
    catch(ResponseStatusException ex) {
        if(ex.getStatusCode()!=CONFLICT) throw ex;
        LOGGER.warn("Servicio {} con anticipo inválido; reserva sin cobro automático",servicio.getId());
        return new PoliticaCobroServicio("SIN_COBRO",java.math.BigDecimal.ZERO.setScale(2),neto);
    }
}
    private void aplicarCotizacion(CitaEntidad cita,
            com.techprotech.agenda.modulos.citas.api.dto.ResultadoCotizacionCitas resultado, int indice, Long promocionId) {
        if (promocionId != null && resultado.promocion()==null) throw new ResponseStatusException(CONFLICT,resultado.motivoNoAplicada());
        var linea = resultado.lineas().get(indice);
        cita.setPrecio(linea.precioNeto()); cita.setPrecioLista(linea.precioLista()); cita.setDescuento(linea.descuento());
        if (linea.promocionAplicada()) {
            cita.setPromocionId(resultado.promocion().id());
            try { cita.setPromocionSnapshotJson(objectMapper.writeValueAsString(resultado.promocion())); }
            catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new IllegalStateException("No fue posible guardar el precio pactado",ex); }
        }
    }

    private String resolverMensajeRespuesta(Long empresaId, boolean correoConfirmacionProgramado) {
        if (!servicioCorreoCitas.estaHabilitado(empresaId)) {
            return "Cita creada correctamente";
        }

        if (correoConfirmacionProgramado) {
            return "Cita creada correctamente. Te enviaremos una confirmacion por correo en breve.";
        }

        return "Cita creada correctamente. La empresa no tiene correo de confirmacion disponible en este momento.";
    }

    private OffsetDateTime normalizarMinuto(OffsetDateTime fechaHora) {
        return fechaHora == null ? null : fechaHora.truncatedTo(ChronoUnit.MINUTES);
    }

    private boolean coincideInicioFranja(String inicioFranja, OffsetDateTime inicioSolicitado) {
        if (inicioFranja == null || inicioSolicitado == null) {
            return false;
        }
        return OffsetDateTime.parse(inicioFranja).toInstant().equals(inicioSolicitado.toInstant());
    }

    private PrestadorServicioEntidad resolverPrestador(Long prestadorId, Long sucursalId, Long servicioId) {
        if (prestadorId != null) {
            PrestadorServicioEntidad prestador = prestadorServicioRepositorio.findByUsuarioIdAndSucursalIdAndActivoTrue(prestadorId, sucursalId)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El prestador no existe o no pertenece a la sucursal"));

            asignacionServicioPrestadorRepositorio.findByIdPrestadorIdAndIdServicioIdAndActivaTrue(prestadorId, servicioId)
                    .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "El prestador no ofrece el servicio indicado"));
            return prestador;
        }

        AsignacionServicioPrestadorEntidad asignacion = asignacionServicioPrestadorRepositorio.findByIdServicioIdAndActivaTrue(servicioId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No hay prestadores activos para el servicio"));

        return prestadorServicioRepositorio.findByUsuarioIdAndSucursalIdAndActivoTrue(asignacion.getId().getPrestadorId(), sucursalId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No hay prestadores activos en la sucursal"));
    }

    private Long resolverOCrearCliente(Long empresaId, String nombreCliente, String correoCliente, String telefonoCliente) {
        String correo = correoCliente.trim().toLowerCase();
        return usuarioRepositorio.findByEmpresaIdAndCorreo(empresaId, correo)
                .map(usuario -> actualizarClienteExistente(usuario.getId(), nombreCliente, telefonoCliente))
                .orElseGet(() -> crearCliente(empresaId, nombreCliente, correo, telefonoCliente));
    }

    Long actualizarClienteExistente(Long usuarioId, String nombreCliente, String telefonoCliente) {
        String nombreNormalizado = nombreCliente.trim();
        String telefonoNormalizado = telefonoCliente.trim();
        ClienteEntidad cliente = clienteRepositorio.findById(usuarioId).orElseGet(() -> {
            ClienteEntidad nuevoCliente = new ClienteEntidad();
            nuevoCliente.setUsuarioId(usuarioId);
            nuevoCliente.setAceptaWhatsapp(true);
            return nuevoCliente;
        });

        boolean nuevoPerfil = cliente.getNombreCompleto() == null;
        boolean nombreCambio = !nombreNormalizado.equals(cliente.getNombreCompleto());
        boolean telefonoCambio = !telefonoNormalizado.equals(cliente.getTelefono());
        if (nuevoPerfil || nombreCambio || telefonoCambio) {
            cliente.setNombreCompleto(nombreNormalizado);
            cliente.setTelefono(telefonoNormalizado);
            clienteRepositorio.save(cliente);
        }
        return usuarioId;
    }

    private Long crearCliente(Long empresaId, String nombreCliente, String correo, String telefono) {
        UsuarioEntidad usuario = new UsuarioEntidad();
        usuario.setEmpresaId(empresaId);
        usuario.setCorreo(correo);
        usuario.setContrasenaHash(passwordEncoder.encode(generarContrasenaProvisional()));
        usuario.setHabilitado(false);
        usuario.setBloqueado(false);
        usuario = usuarioRepositorio.save(usuario);

        ClienteEntidad cliente = new ClienteEntidad();
        cliente.setUsuarioId(usuario.getId());
        cliente.setNombreCompleto(nombreCliente.trim());
        cliente.setTelefono(telefono.trim());
        cliente.setAceptaWhatsapp(true);
        clienteRepositorio.save(cliente);

        servicioRolesEmpresa.asignarRolEmpresa(usuario, empresaId, "CLIENTE");

        return usuario.getId();
    }

    private String generarContrasenaProvisional() {
        return PREFIJO_CONTRASENA_PROVISIONAL + "-" + UUID.randomUUID();
    }

    private record RangoReservaValidado(
            Long servicioId,
            Long prestadorId,
            OffsetDateTime inicio,
            OffsetDateTime fin
    ) {
    }
}
