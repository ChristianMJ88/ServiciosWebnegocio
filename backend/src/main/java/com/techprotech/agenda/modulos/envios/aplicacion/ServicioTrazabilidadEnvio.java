package com.techprotech.agenda.modulos.envios.aplicacion;

import com.techprotech.agenda.modulos.envios.infraestructura.entidad.EnvioEventoEntidad;
import com.techprotech.agenda.modulos.envios.infraestructura.entidad.EnvioPedidoEntidad;
import com.techprotech.agenda.modulos.envios.infraestructura.repositorio.EnvioEventoRepositorio;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ServicioTrazabilidadEnvio {
    private final EnvioEventoRepositorio eventos;

    public ServicioTrazabilidadEnvio(EnvioEventoRepositorio eventos) { this.eventos = eventos; }

    public void registrar(EnvioPedidoEntidad envio, String tipo, String descripcion, String referenciaExterna) {
        if (referenciaExterna != null
                && eventos.existsByEmpresaIdAndReferenciaExterna(envio.getEmpresaId(), referenciaExterna)) return;
        EnvioEventoEntidad evento = new EnvioEventoEntidad();
        evento.setEmpresaId(envio.getEmpresaId());
        evento.setEnvioPedidoId(envio.getId());
        evento.setTipo(tipo);
        evento.setEstado(envio.getEstado());
        evento.setDescripcion(descripcion);
        evento.setReferenciaExterna(referenciaExterna);
        try { eventos.save(evento); }
        catch (DataIntegrityViolationException ex) {
            if (referenciaExterna == null) throw ex;
        }
    }
}
