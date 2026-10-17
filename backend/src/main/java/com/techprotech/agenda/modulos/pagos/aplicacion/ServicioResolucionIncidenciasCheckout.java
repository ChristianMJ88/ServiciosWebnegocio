package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.admin.infraestructura.entidad.AuditoriaConfiguracionEmpresaEntidad;
import com.techprotech.agenda.modulos.admin.infraestructura.repositorio.AuditoriaConfiguracionEmpresaRepositorio;
import com.techprotech.agenda.modulos.caja.aplicacion.ServicioVentasCheckout;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.VentaRepositorio;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioConsultaDisponibilidad;
import com.techprotech.agenda.modulos.pagos.api.dto.ResolverIncidenciaRequest;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenRepositorio;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class ServicioResolucionIncidenciasCheckout {
    private final CheckoutOrdenRepositorio ordenes;
    private final ServicioConfirmacionCitasCheckout citas;
    private final ServicioInventarioCheckout inventario;
    private final ServicioConsultaDisponibilidad disponibilidad;
    private final ServicioVentasCheckout proyeccion;
    private final VentaRepositorio ventas;
    private final AuditoriaConfiguracionEmpresaRepositorio auditoria;
    private final EntityManager em;
    private final ObjectMapper json;

    public ServicioResolucionIncidenciasCheckout(CheckoutOrdenRepositorio ordenes, ServicioConfirmacionCitasCheckout citas,
            ServicioInventarioCheckout inventario, ServicioConsultaDisponibilidad disponibilidad,
            ServicioVentasCheckout proyeccion, VentaRepositorio ventas,
            AuditoriaConfiguracionEmpresaRepositorio auditoria, EntityManager em, ObjectMapper json) {
        this.ordenes=ordenes; this.citas=citas; this.inventario=inventario; this.disponibilidad=disponibilidad;
        this.proyeccion=proyeccion; this.ventas=ventas; this.auditoria=auditoria; this.em=em; this.json=json;
    }

    @Transactional
    public void resolver(UsuarioAutenticado usuario, String publicId, ResolverIncidenciaRequest request) {
        Long empresa=usuario.empresaId();
        var ref=ordenes.findByEmpresaIdAndPublicId(empresa,publicId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,"Incidencia no encontrada"));
        var orden=ordenes.bloquearPorEmpresaEId(empresa,ref.getId()).orElseThrow();
        em.refresh(orden,LockModeType.PESSIMISTIC_WRITE);
        if (!usuario.puedeAccederSucursal(orden.getSucursalId())) throw new ResponseStatusException(FORBIDDEN,"Sucursal fuera de alcance");
        if (!orden.isRequiereConciliacion() || !"PAGADA_CON_INCIDENCIA".equals(orden.getEstado()))
            throw new ResponseStatusException(CONFLICT,"La incidencia ya está resuelta o no corresponde a un pago aprobado");
        // Acreditar el pago antes de resolver, también si el proyector aún no ha ejecutado.
        proyeccion.proyectar(empresa,orden.getId());
        var grupo=citas.bloquearGrupoOrden(empresa,orden.getId());
        String antes=serializar(Map.of("estado",orden.getEstado(),"motivo",Objects.toString(orden.getMotivoIncidencia(),""),
                "citas",grupo.stream().map(c -> Map.of("citaId",c.getId(),"inicio",c.getInicio(),"fin",c.getFin(),"prestadorId",c.getPrestadorId(),"estado",c.getEstado())).toList()));
        if (request.tipo()==ResolverIncidenciaRequest.Tipo.REPROGRAMADA) {
            var cambios=request.citas()==null ? List.<ResolverIncidenciaRequest.Reprogramacion>of() : request.citas();
            var porId=new HashMap<Long,LocalDateTime>();
            for (var cambio:cambios) if (porId.put(cambio.citaId(),cambio.inicio())!=null)
                throw new ResponseStatusException(BAD_REQUEST,"Una cita no puede repetirse");
            if (grupo.isEmpty() || !porId.keySet().equals(new HashSet<>(grupo.stream().map(c -> c.getId()).toList())))
                throw new ResponseStatusException(BAD_REQUEST,"Indica el nuevo horario de todas las citas de esta reserva");
            for (var cita:grupo) {
                boolean sinCobroPendiente="PENDIENTE".equals(cita.getEstado()) && "SIN_COBRO".equals(cita.getPoliticaCobro());
                if (!"LIBERADA_SIN_PAGO".equals(cita.getEstado()) && !sinCobroPendiente)
                    throw new ResponseStatusException(CONFLICT,"La cita debe estar liberada para reprogramar esta incidencia");
                // Los miembros sin cobro también se reprograman sin que su apartado anterior bloquee el grupo.
                cita.setEstado("LIBERADA_SIN_PAGO");
            }
            em.flush();
            for (var cita:grupo) {
                var inicio=porId.get(cita.getId());
                var fin=inicio.plus(Duration.between(cita.getInicio(),cita.getFin()));
                boolean libre=disponibilidad.obtenerFranjasDisponibles(empresa,cita.getSucursalId(),cita.getServicioId(),cita.getPrestadorId(),inicio.toLocalDate())
                        .stream().anyMatch(f -> OffsetDateTime.parse(f.inicio()).toLocalDateTime().equals(inicio)
                                && !OffsetDateTime.parse(f.fin()).toLocalDateTime().isBefore(fin));
                if (!libre) throw new ResponseStatusException(CONFLICT,"El nuevo horario no está disponible");
                cita.setInicio(inicio); cita.setFin(fin);
            }
            for (var a:grupo) for (var b:grupo) if (a.getId()<b.getId() && Objects.equals(a.getPrestadorId(),b.getPrestadorId())
                    && a.getInicio().isBefore(b.getFin()) && a.getFin().isAfter(b.getInicio()))
                throw new ResponseStatusException(CONFLICT,"Los nuevos horarios se superponen");
            em.flush(); // El refresco de confirmación debe releer los nuevos horarios.
            if (!inventario.confirmarSiDisponible(empresa,orden.getId()) || !citas.confirmar(empresa,orden.getId(),com.techprotech.agenda.modulos.citas.dominio.eventos.OrigenEventoCita.ADMIN,usuario.usuarioId()))
                throw new ResponseStatusException(CONFLICT,"No es posible confirmar la reserva con el pago existente");
        } else if (request.citas()!=null && !request.citas().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST,"Solo la reprogramación admite nuevos horarios");
        }
        if (request.tipo()==ResolverIncidenciaRequest.Tipo.REEMBOLSADA_FUERA_DEL_SISTEMA) {
            orden.setEstado("REEMBOLSADA");
            ventas.findByEmpresaIdAndCheckoutOrdenId(empresa,orden.getId()).ifPresent(v -> {v.setEstado("REEMBOLSADA"); ventas.save(v);});
        } else if (request.tipo()==ResolverIncidenciaRequest.Tipo.REPROGRAMADA) orden.setEstado("PAGADA");
        orden.setRequiereConciliacion(false);
        ordenes.save(orden);
        var registro=new AuditoriaConfiguracionEmpresaEntidad();
        registro.setEmpresaId(empresa); registro.setUsuarioActorId(usuario.usuarioId()); registro.setActorCorreo(usuario.correo());
        registro.setModulo("PAGOS"); registro.setAccion("RESOLVER_INCIDENCIA_CHECKOUT");
        registro.setResumen("Incidencia resuelta en orden " + publicId);
        registro.setDetalleAntesJson(antes);
        registro.setDetalleDespuesJson(serializar(Map.of("estado",orden.getEstado(),"resolucion",request)));
        auditoria.save(registro);
    }

    private String serializar(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("No fue posible registrar la auditoría",ex); }
    }
}
