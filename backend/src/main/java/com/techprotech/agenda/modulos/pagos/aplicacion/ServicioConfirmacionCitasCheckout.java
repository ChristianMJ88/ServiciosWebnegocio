package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.compartido.correo.ServicioOutboxCorreoCitas;
import com.techprotech.agenda.compartido.whatsapp.ServicioOutboxWhatsappCitas;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.HistorialEstadoCitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.HistorialEstadoCitaRepositorio;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioConfiguracionReservas;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenDetalleRepositorio;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioConfirmacionCitasCheckout {
    private static final List<String> OCUPADOS = List.of("PENDIENTE", "PENDIENTE_PAGO", "CONFIRMADA");
    private final CheckoutOrdenDetalleRepositorio detalles;
    private final CitaRepositorio citas;
    private final HistorialEstadoCitaRepositorio historial;
    private final ServicioOutboxCorreoCitas correo;
    private final ServicioOutboxWhatsappCitas whatsapp;
    private final ClienteRepositorio clientes;
    private final EntityManager entityManager;
    private final PrestadorServicioRepositorio prestadores;
    private final SucursalRepositorio sucursales;
    private final ServicioConfiguracionReservas reservas;
    private final Clock reloj;

    public ServicioConfirmacionCitasCheckout(CheckoutOrdenDetalleRepositorio detalles, CitaRepositorio citas,
            HistorialEstadoCitaRepositorio historial, ServicioOutboxCorreoCitas correo,
            ServicioOutboxWhatsappCitas whatsapp, ClienteRepositorio clientes, EntityManager entityManager,
            PrestadorServicioRepositorio prestadores, SucursalRepositorio sucursales,
            ServicioConfiguracionReservas reservas, Clock reloj) {
        this.detalles = detalles; this.citas = citas; this.historial = historial;
        this.correo = correo; this.whatsapp = whatsapp; this.clientes = clientes;
        this.entityManager = entityManager; this.prestadores = prestadores; this.sucursales = sucursales;
        this.reservas = reservas; this.reloj = reloj;
    }

    /** False es incidencia operativa; no revierte un pago por el estado de la cita. */
    @Transactional
    public boolean confirmar(Long empresaId, Long ordenId) {
        List<CitaEntidad> grupo = grupoDeOrden(empresaId, ordenId);
        if (grupo.stream().anyMatch(c -> !Set.of("PENDIENTE", "PENDIENTE_PAGO", "LIBERADA_SIN_PAGO", "CONFIRMADA").contains(c.getEstado()))) return false;
        Set<Long> ids = new HashSet<>();
        grupo.forEach(c -> ids.add(c.getId()));
        for (CitaEntidad cita : grupo) {
            if (!"LIBERADA_SIN_PAGO".equals(cita.getEstado())) continue;
            var sucursal = sucursales.findByIdAndEmpresaIdAndActivaTrue(cita.getSucursalId(), empresaId);
            if (sucursal.isEmpty() || !cita.getInicio().isAfter(LocalDateTime.now(reloj.withZone(ZoneId.of(sucursal.get().getZonaHoraria()))))) return false;
            if (citas.buscarConflictosBloqueando(cita.getPrestadorId(), cita.getInicio(), cita.getFin(), OCUPADOS)
                    .stream().anyMatch(otra -> !ids.contains(otra.getId()))) return false;
        }
        for (CitaEntidad cita : grupo) {
            if ("CONFIRMADA".equals(cita.getEstado())) continue;
            cita.setPagoExpiraEn(null);
            cambiar(cita, "CONFIRMADA", "Pago en línea aprobado");
            correo.programarConfirmada(empresaId, cita.getId(), cita.getInicio());
            correo.programarNuevaCitaNegocio(empresaId, cita.getId(), cita.getInicio());
            clientes.findByClienteIdAndEmpresaId(cita.getClienteId(), empresaId)
                    .filter(c -> c.isAceptaWhatsapp()).ifPresent(cliente ->
                        whatsapp.programarCitaConfirmada(empresaId, cita.getId(), cliente.getTelefono(), cita.getInicio()));
        }
        return true;
    }

    @Transactional
    public void avisarIncidencia(Long empresaId, Long ordenId) {
        grupoDeOrden(empresaId, ordenId).stream().findFirst().ifPresent(cita ->
                correo.programarIncidenciaPagoNegocio(empresaId, cita.getId(), cita.getInicio()));
    }

    @Transactional
    public void liberar(Long empresaId, Long ordenId) { liberarGrupo(grupoDeOrden(empresaId, ordenId)); }

    @Transactional
    public OffsetDateTime prorrogar(Long empresaId, Long ordenId, OffsetDateTime vencimiento) {
        List<CitaEntidad> grupo = grupoDeOrden(empresaId, ordenId);
        Instant limite = vencimiento == null
                ? reloj.instant().plus(Duration.ofHours(reservas.obtener(empresaId).horasTopeProcesamientoPago()))
                : vencimiento.toInstant();
        if (vencimiento == null) {
            for (CitaEntidad cita : grupo) {
                var sucursal = sucursales.findByIdAndEmpresaIdAndActivaTrue(cita.getSucursalId(), empresaId);
                if (sucursal.isPresent()) {
                    Instant inicio = cita.getInicio().atZone(ZoneId.of(sucursal.get().getZonaHoraria())).toInstant();
                    if (inicio.isBefore(limite)) limite = inicio;
                }
            }
        }
        LocalDateTime fecha = LocalDateTime.ofInstant(limite, ZoneOffset.UTC);
        for (CitaEntidad cita : grupo) if ("PENDIENTE_PAGO".equals(cita.getEstado())) {
            cita.setPagoExpiraEn(fecha); citas.save(cita);
        }
        return limite.atOffset(ZoneOffset.UTC);
    }

    @Transactional
    public void liberarCitasVencidas() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        // IDs solamente: no precargar entidades obsoletas antes del bloqueo.
        for (Long id : citas.buscarIdsApartadosVencidos(ahora, PageRequest.of(0, 100))) {
List<CitaEntidad> grupo = grupoPorId(null, id);
if (grupo.stream().anyMatch(c -> "PENDIENTE_PAGO".equals(c.getEstado()))
        && grupo.stream().filter(c -> "PENDIENTE_PAGO".equals(c.getEstado()))
        .allMatch(c -> c.getPagoExpiraEn() != null && !c.getPagoExpiraEn().isAfter(ahora))) liberarGrupo(grupo);
        }
    }

    private void liberarGrupo(List<CitaEntidad> grupo) {
        for (CitaEntidad cita : grupo) if ("PENDIENTE_PAGO".equals(cita.getEstado())) {
            cita.setPagoExpiraEn(null);
            cambiar(cita, "LIBERADA_SIN_PAGO", "Apartado vencido sin pago");
            correo.programarApartadoVencido(cita.getEmpresaId(), cita.getId(), cita.getInicio());
        }
    }

    private List<CitaEntidad> grupoDeOrden(Long empresaId, Long ordenId) {
        var ids = detalles.findByEmpresaIdAndCheckoutOrdenIdOrderByIdAsc(empresaId, ordenId).stream()
                .filter(d -> "SERVICIO".equals(d.getTipoConcepto()) && d.getCitaId() != null)
                .map(d -> d.getCitaId()).distinct().sorted().toList();
        bloquearPrestadores(empresaId, ids);
        Map<Long, CitaEntidad> resultado = new LinkedHashMap<>();
        for (Long id : ids) grupoPorIdBloqueado(empresaId, id).forEach(c -> resultado.put(c.getId(), c));
        return List.copyOf(resultado.values());
    }

    private void bloquearPrestadores(Long empresaId, List<Long> ids) {
        var profesionales = new TreeSet<Long>();
        for (Long id : ids) citas.buscarReferenciaReserva(id)
                .filter(r -> empresaId == null || empresaId.equals(r.getEmpresaId()))
                .ifPresent(r -> profesionales.addAll(citas.buscarPrestadoresReserva(r.getEmpresaId(), id, r.getReservaGrupoId())));
        for (Long profesional : profesionales) {
            Long empresa = empresaId != null ? empresaId : citas.buscarReferenciaReserva(ids.get(0)).orElseThrow().getEmpresaId();
            if (prestadores.bloquearPorEmpresa(empresa, profesional).isEmpty())
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "El prestador ya no está disponible");
        }
    }

    private List<CitaEntidad> grupoPorId(Long empresaId, Long id) {
        bloquearPrestadores(empresaId, List.of(id));
        return grupoPorIdBloqueado(empresaId, id);
    }

    private List<CitaEntidad> grupoPorIdBloqueado(Long empresaId, Long id) {
        var referencia = citas.buscarReferenciaReserva(id);
        if (referencia.isEmpty() || empresaId != null && !empresaId.equals(referencia.get().getEmpresaId())) return List.of();
        var ref = referencia.get();
        List<CitaEntidad> grupo = ref.getReservaGrupoId() == null
                ? citas.bloquearPorIdYEmpresa(id, ref.getEmpresaId()).map(List::of).orElseGet(List::of)
                : citas.findByEmpresaIdAndReservaGrupoIdOrderByIdAsc(ref.getEmpresaId(), ref.getReservaGrupoId());
        grupo.forEach(c -> entityManager.refresh(c, LockModeType.PESSIMISTIC_WRITE));
        return grupo;
    }

    @Transactional
    public List<CitaEntidad> bloquearGrupoOrden(Long empresaId, Long ordenId) {
        return grupoDeOrden(empresaId, ordenId);
    }

    private void cambiar(CitaEntidad cita, String estado, String motivo) {
        var cambio = new HistorialEstadoCitaEntidad(); cambio.setCitaId(cita.getId());
        cambio.setEstadoAnterior(cita.getEstado()); cambio.setEstadoNuevo(estado);
        cambio.setCambiadoPorUsuarioId(cita.getClienteId()); cambio.setMotivo(motivo);
        cita.setEstado(estado); citas.save(cita); historial.save(cambio);
    }
}
