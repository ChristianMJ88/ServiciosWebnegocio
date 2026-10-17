package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.compartido.correo.ServicioOutboxCorreoCitas;
import com.techprotech.agenda.compartido.whatsapp.ServicioOutboxWhatsappCitas;
import com.techprotech.agenda.modulos.citas.dominio.eventos.*;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import java.util.function.Consumer;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Síncrono: cambio, historial y outbox se confirman o revierten juntos. Nunca enviar al proveedor aquí. */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class NotificacionesEventosCita {
    private final ServicioOutboxCorreoCitas correo;
    private final ServicioOutboxWhatsappCitas whatsapp;
    private final ClienteRepositorio clientes;

    public NotificacionesEventosCita(ServicioOutboxCorreoCitas correo, ServicioOutboxWhatsappCitas whatsapp,
            ClienteRepositorio clientes) {
        this.correo = correo; this.whatsapp = whatsapp; this.clientes = clientes;
    }

    @EventListener
    public void finalizada(CitaFinalizada evento) {
        var d = evento.datos();
        correo.programarGraciasVisita(d.empresaId(), d.citaId(), d.inicio());
        conWhatsapp(d, telefono -> whatsapp.programarGraciasVisita(d.empresaId(), d.citaId(), telefono, d.inicio()));
    }

    @EventListener
    public void noAsistio(CitaNoAsistio evento) {
        var d = evento.datos();
        correo.programarNoAsistio(d.empresaId(), d.citaId(), d.inicio());
    }

    @EventListener
    public void cancelada(CitaCancelada evento) {
        var d = evento.datos();
        if (d.origen() == OrigenEventoCita.CLIENTE) correo.programarCanceladaCliente(d.empresaId(), d.citaId(), d.inicio());
        else {
            correo.programarCanceladaNegocio(d.empresaId(), d.citaId(), d.inicio());
            conWhatsapp(d, telefono -> whatsapp.programarCancelacionNegocio(d.empresaId(), d.citaId(), telefono, d.inicio()));
        }
    }

    @EventListener
    public void confirmada(CitaConfirmada evento) {
        var d = evento.datos();
        correo.programarConfirmada(d.empresaId(), d.citaId(), d.inicio());
        if (evento.avisarNegocio()) correo.programarNuevaCitaNegocio(d.empresaId(), d.citaId(), d.inicio());
        if (evento.avisarWhatsapp()) conWhatsapp(d,
                telefono -> whatsapp.programarCitaConfirmada(d.empresaId(), d.citaId(), telefono, d.inicio()));
    }

    @EventListener
    public void reprogramada(CitaReprogramada evento) {
        var d = evento.datos();
        correo.programarReprogramada(d.empresaId(), d.citaId(), d.inicio());
        conWhatsapp(d, telefono -> whatsapp.programarCitaReprogramadaPendiente(d.empresaId(), d.citaId(), telefono, d.inicio()));
    }

    @EventListener
    public void apartadoVencido(CitaApartadoVencido evento) {
        var d = evento.datos(); correo.programarApartadoVencido(d.empresaId(), d.citaId(), d.inicio());
    }

    @EventListener
    public void pagoConIncidencia(PagoCitaConIncidencia evento) {
        var d = evento.datos(); correo.programarIncidenciaPagoNegocio(d.empresaId(), d.citaId(), d.inicio());
    }

    private void conWhatsapp(DatosEventoCita datos, Consumer<String> accion) {
        clientes.findByClienteIdAndEmpresaId(datos.clienteId(), datos.empresaId())
                .filter(c -> c.isAceptaWhatsapp() && c.getTelefono() != null && !c.getTelefono().isBlank())
                .ifPresent(c -> accion.accept(c.getTelefono()));
    }
}
