package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.compartido.correo.ServicioOutboxCorreoCitas;
import com.techprotech.agenda.compartido.whatsapp.ServicioOutboxWhatsappCitas;
import com.techprotech.agenda.compartido.tiempo.ZonaNegocio;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class ServicioCierreCita {
    private final CitaRepositorio citas;
    private final HistorialEstadoCitaRepositorio historial;
    private final ServicioOutboxCorreoCitas correo;
    private final ServicioOutboxWhatsappCitas whatsapp;
    private final ClienteRepositorio clientes;
    private final Clock reloj;
    private final ZonaNegocio zonas;

    public ServicioCierreCita(CitaRepositorio citas,HistorialEstadoCitaRepositorio historial,ServicioOutboxCorreoCitas correo,
            ServicioOutboxWhatsappCitas whatsapp,ClienteRepositorio clientes,Clock reloj,ZonaNegocio zonas) {
        this.citas=citas;this.historial=historial;this.correo=correo;this.whatsapp=whatsapp;this.clientes=clientes;this.reloj=reloj;this.zonas=zonas;
    }

    public boolean puedeCerrar(CitaEntidad cita) {
        return cita != null && Set.of("PENDIENTE", "CONFIRMADA").contains(cita.getEstado() == null ? "" : cita.getEstado())
                && yaInicio(cita);
    }

    public Map<Long, Boolean> elegibilidad(Collection<CitaEntidad> consultadas) {
        record Sucursal(Long empresa, Long sucursal) {}
        var ahoraPorSucursal = new HashMap<Sucursal, LocalDateTime>();
        var resultado = new HashMap<Long, Boolean>();
        for (var cita : consultadas) {
            boolean estado = Set.of("PENDIENTE", "CONFIRMADA").contains(cita.getEstado() == null ? "" : cita.getEstado());
            boolean habilitada = false;
            if (estado && cita.getInicio() != null) {
                var clave = new Sucursal(cita.getEmpresaId(), cita.getSucursalId());
                var ahora = ahoraPorSucursal.computeIfAbsent(clave,
                        id -> LocalDateTime.now(reloj.withZone(zonas.de(id.empresa(), id.sucursal()))));
                habilitada = !cita.getInicio().isAfter(ahora);
            }
            resultado.put(cita.getId(), habilitada);
        }
        return Map.copyOf(resultado);
    }

    private boolean yaInicio(CitaEntidad cita) {
        var ahora = LocalDateTime.now(reloj.withZone(zonas.de(cita.getEmpresaId(), cita.getSucursalId())));
        return cita.getInicio() != null && !cita.getInicio().isAfter(ahora);
    }

    /** Los controladores conservan el RBAC de cada ruta; aquí se valida además tenant, alcance y propiedad. */
    @Transactional
    public CitaEntidad cerrar(UsuarioAutenticado usuario,Long citaId,String estado,OrigenCierreCita origen) {
        if(!Set.of("FINALIZADA","NO_ASISTIO").contains(estado==null?"":estado) || origen==null)
            throw new ResponseStatusException(BAD_REQUEST,"El estado no es un cierre de atención");
        if(usuario==null || usuario.empresaId()==null || usuario.esPlataforma())
            throw new ResponseStatusException(NOT_FOUND,"La cita no existe para la empresa");
        var cita=citas.bloquearPorIdYEmpresa(citaId,usuario.empresaId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,"La cita no existe para la empresa"));
        if(!usuario.puedeAccederSucursal(cita.getSucursalId()) || (origen==OrigenCierreCita.STAFF && !Objects.equals(usuario.usuarioId(),cita.getPrestadorId())))
            throw new ResponseStatusException(NOT_FOUND,"La cita no existe en tu alcance");
        if(estado.equals(cita.getEstado()))return cita; // Reintento del mismo cierre: no duplica historial ni outbox.
        if("PENDIENTE_PAGO".equals(cita.getEstado()))
            throw new ResponseStatusException(CONFLICT,"La cita todavía requiere completar su pago de reserva");
        if(!Set.of("PENDIENTE","CONFIRMADA").contains(cita.getEstado()==null?"":cita.getEstado()))
            throw new ResponseStatusException(BAD_REQUEST,"Solo se pueden cerrar citas pendientes o confirmadas");
        if(!yaInicio(cita))throw new ResponseStatusException(BAD_REQUEST,"No puedes cerrar una cita futura antes de que inicie");
        var anterior=cita.getEstado();cita.setEstado(estado);citas.save(cita);
        var cambio=new HistorialEstadoCitaEntidad();cambio.setCitaId(citaId);cambio.setEstadoAnterior(anterior);cambio.setEstadoNuevo(estado);
        cambio.setCambiadoPorUsuarioId(usuario.usuarioId());
        cambio.setMotivo("Cierre desde "+origen.name()+ ("FINALIZADA".equals(estado) && cita.getCheckInEn()==null?"; finalizada sin check-in":""));
        historial.save(cambio);
        if("FINALIZADA".equals(estado)) {
            correo.programarGraciasVisita(usuario.empresaId(),citaId,cita.getInicio());
            clientes.findByClienteIdAndEmpresaId(cita.getClienteId(),usuario.empresaId())
                    .filter(c -> c.isAceptaWhatsapp() && c.getTelefono()!=null && !c.getTelefono().isBlank())
                    .ifPresent(c -> whatsapp.programarGraciasVisita(usuario.empresaId(),citaId,c.getTelefono(),cita.getInicio()));
        } else correo.programarNoAsistio(usuario.empresaId(),citaId,cita.getInicio());
        return cita;
    }
}
