package com.techprotech.agenda.compartido.whatsapp;

import com.techprotech.agenda.compartido.correo.BandejaSalidaNotificacionRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.ClienteEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.HistorialEstadoCitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.HistorialEstadoCitaRepositorio;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProgramadorRecordatoriosWhatsapp {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgramadorRecordatoriosWhatsapp.class);

    private final CitaRepositorio citaRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final BandejaSalidaNotificacionRepositorio bandejaSalidaNotificacionRepositorio;
    private final ServicioOutboxWhatsappCitas servicioOutboxWhatsappCitas;
    private final PropiedadesWhatsapp propiedadesWhatsapp;
    private final HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio;
    private final ConfiguracionWhatsappEmpresaRepositorio configuracionWhatsappEmpresaRepositorio;

    public ProgramadorRecordatoriosWhatsapp(
            CitaRepositorio citaRepositorio,
            ClienteRepositorio clienteRepositorio,
            BandejaSalidaNotificacionRepositorio bandejaSalidaNotificacionRepositorio,
            ServicioOutboxWhatsappCitas servicioOutboxWhatsappCitas,
            PropiedadesWhatsapp propiedadesWhatsapp,
            HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio,
            ConfiguracionWhatsappEmpresaRepositorio configuracionWhatsappEmpresaRepositorio
    ) {
        this.citaRepositorio = citaRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.bandejaSalidaNotificacionRepositorio = bandejaSalidaNotificacionRepositorio;
        this.servicioOutboxWhatsappCitas = servicioOutboxWhatsappCitas;
        this.propiedadesWhatsapp = propiedadesWhatsapp;
        this.historialEstadoCitaRepositorio = historialEstadoCitaRepositorio;
        this.configuracionWhatsappEmpresaRepositorio = configuracionWhatsappEmpresaRepositorio;
    }

    @Scheduled(
            fixedDelayString = "${aplicacion.whatsapp.recordatorios.delay-ms:60000}",
            initialDelayString = "${aplicacion.whatsapp.recordatorios.initial-delay-ms:30000}"
    )
    public void programarPendientes() {
        if (!propiedadesWhatsapp.habilitado()) {
            return;
        }

        for (ConfiguracionWhatsappEmpresaEntidad configuracion : configuracionWhatsappEmpresaRepositorio.findByHabilitadoTrueOrderByEmpresaIdAsc()) {
            try {
                programarPendientes(configuracion.getEmpresaId());
            } catch (Exception ex) {
                LOGGER.error("No se pudieron programar recordatorios de WhatsApp para la empresa {}", configuracion.getEmpresaId(), ex);
            }
        }
    }

    private void programarPendientes(Long empresaId) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicioVentana = ahora.plusHours(propiedadesWhatsapp.recordatorioHorasAntes());
        LocalDateTime finVentana = inicioVentana.plusMinutes(propiedadesWhatsapp.ventanaRecordatorioMinutos());

        List<CitaEntidad> citas = citaRepositorio.findByEmpresaIdAndInicioBetweenAndEstadoInOrderByInicioAsc(
                empresaId,
                inicioVentana,
                finVentana,
                List.of("PENDIENTE", "CONFIRMADA")
        );

        for (CitaEntidad cita : citas) {
            String tipoEvento = "PENDIENTE".equals(cita.getEstado())
                    ? "CITA_RECORDATORIO_CONFIRMACION_WHATSAPP"
                    : "CITA_RECORDATORIO_WHATSAPP";

            if (bandejaSalidaNotificacionRepositorio.existsByAgregadoIdAndCanalAndTipoEventoAndEstadoIn(
                    cita.getId(),
                    "WHATSAPP",
                    tipoEvento,
                    List.of("PENDIENTE", "PROCESANDO", "ENVIADA")
            )) {
                continue;
            }

            ClienteEntidad cliente = clienteRepositorio.findById(cita.getClienteId()).orElse(null);
            if (cliente == null || !cliente.isAceptaWhatsapp() || cliente.getTelefono() == null || cliente.getTelefono().isBlank()) {
                continue;
            }

            if ("PENDIENTE".equals(cita.getEstado())) {
                servicioOutboxWhatsappCitas.programarRecordatorioConfirmacion(
                        empresaId,
                        cita.getId(),
                        cliente.getTelefono(),
                        cita.getInicio(),
                        ahora
                );
            } else {
                servicioOutboxWhatsappCitas.programarRecordatorio(
                        empresaId,
                        cita.getId(),
                        cliente.getTelefono(),
                        cita.getInicio(),
                        ahora
                );
            }
        }
    }

    @Scheduled(
            fixedDelayString = "${aplicacion.whatsapp.recordatorios.delay-ms:60000}",
            initialDelayString = "${aplicacion.whatsapp.recordatorios.initial-delay-ms:45000}"
    )
    public void liberarPendientesSinConfirmacion() {
        if (!propiedadesWhatsapp.habilitado()) {
            return;
        }

        for (ConfiguracionWhatsappEmpresaEntidad configuracion : configuracionWhatsappEmpresaRepositorio.findByHabilitadoTrueOrderByEmpresaIdAsc()) {
            try {
                liberarPendientesSinConfirmacion(configuracion.getEmpresaId());
            } catch (Exception ex) {
                LOGGER.error("No se pudieron liberar citas sin confirmacion para la empresa {}", configuracion.getEmpresaId(), ex);
            }
        }
    }

    private void liberarPendientesSinConfirmacion(Long empresaId) {
        LocalDateTime limite = LocalDateTime.now().plusHours(propiedadesWhatsapp.liberacionSinConfirmacionHorasAntes());

        List<CitaEntidad> citas = citaRepositorio.findByEmpresaIdAndInicioBeforeAndEstadoOrderByInicioAsc(
                empresaId,
                limite,
                "PENDIENTE"
        );

        for (CitaEntidad cita : citas) {
            String estadoAnterior = cita.getEstado();
            cita.setEstado("LIBERADA_SIN_CONFIRMACION");
            citaRepositorio.save(cita);

            HistorialEstadoCitaEntidad historial = new HistorialEstadoCitaEntidad();
            historial.setCitaId(cita.getId());
            historial.setEstadoAnterior(estadoAnterior);
            historial.setEstadoNuevo("LIBERADA_SIN_CONFIRMACION");
            historial.setCambiadoPorUsuarioId(cita.getCreadaPorUsuarioId());
            historial.setMotivo("Liberada automaticamente por falta de confirmacion");
            historialEstadoCitaRepositorio.save(historial);

            ClienteEntidad cliente = clienteRepositorio.findById(cita.getClienteId()).orElse(null);
            if (cliente == null || !cliente.isAceptaWhatsapp() || cliente.getTelefono() == null || cliente.getTelefono().isBlank()) {
                continue;
            }

            servicioOutboxWhatsappCitas.programarLiberadaSinConfirmacion(
                    empresaId,
                    cita.getId(),
                    cliente.getTelefono(),
                    cita.getInicio()
            );
        }
    }
}
