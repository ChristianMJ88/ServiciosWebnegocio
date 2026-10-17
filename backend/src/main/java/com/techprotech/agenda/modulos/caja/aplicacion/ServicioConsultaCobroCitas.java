package com.techprotech.agenda.modulos.caja.aplicacion;

import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.CajaSesionRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.api.dto.ResultadoCierreCitaResponse;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioCapacidadesEmpresa;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.techprotech.agenda.modulos.plataforma.dominio.CodigosModuloPlataforma.CAJA;
import static com.techprotech.agenda.modulos.caja.aplicacion.CatalogosCaja.ESTADO_SESION_ABIERTA;

@Service
@Transactional(readOnly = true)
public class ServicioConsultaCobroCitas {
    private final ServicioSaldoCitas saldos;
    private final CajaSesionRepositorio sesiones;
    private final ServicioCapacidadesEmpresa capacidades;

    public ServicioConsultaCobroCitas(ServicioSaldoCitas saldos,CajaSesionRepositorio sesiones,ServicioCapacidadesEmpresa capacidades) {
        this.saldos=saldos;this.sesiones=sesiones;this.capacidades=capacidades;
    }

    public Map<Long,CobroCita> consultar(UsuarioAutenticado usuario,Collection<CitaEntidad> citas) {
        var montos=saldos.calcular(usuario.empresaId(),citas);
        if(montos.isEmpty())return Map.of();
        var sucursales=citas.stream().filter(c -> montos.containsKey(c.getId()) && usuario.puedeAccederSucursal(c.getSucursalId()))
                .map(CitaEntidad::getSucursalId).filter(Objects::nonNull).distinct().toList();
        Set<Long> abiertas=Set.of();
        // Las mismas condiciones de acceso que usa Caja: permisos del usuario y módulo contratado.
        if(!usuario.esPlataforma() && usuario.tienePermiso("CAJA_COBRAR") && usuario.tienePermiso("CAJA_ACCESO") && !sucursales.isEmpty()) {
            var capacidad=capacidades.obtener(usuario.empresaId());
            if(capacidad.suscripcionVigente() && capacidad.modulos().stream().anyMatch(m -> CAJA.equalsIgnoreCase(m.codigo())))
                abiertas=new HashSet<>(sesiones.buscarSucursalesConSesion(usuario.empresaId(),sucursales,ESTADO_SESION_ABIERTA));
        }
        var resultado=new HashMap<Long,CobroCita>();
        for(var cita:citas) {
            var saldo=montos.get(cita.getId());if(saldo==null)continue;
            boolean puede=abiertas.contains(cita.getSucursalId()) && usuario.puedeAccederSucursal(cita.getSucursalId())
                    && ServicioSaldoCitas.esCobrable(cita) && saldo.pendiente().signum()>0;
            resultado.put(cita.getId(),new CobroCita(saldo,puede));
        }
        return Map.copyOf(resultado);
    }

    public SaldoCita saldo(CitaEntidad cita) { return saldos.de(cita); }

    public CobroCita de(UsuarioAutenticado usuario,CitaEntidad cita) { return consultar(usuario,List.of(cita)).get(cita.getId()); }

    public ResultadoCierreCitaResponse respuesta(UsuarioAutenticado usuario,CitaEntidad cita) {
        var cobro=de(usuario,cita);var s=cobro.saldo();
        return new ResultadoCierreCitaResponse(cita.getId(),cita.getSucursalId(),cita.getEstado(),s.precio(),s.pagadoCaja(),
                s.pagadoVenta(),s.pagado(),s.condonado(),s.pendiente(),s.estadoCobro(),cobro.puedeCobrar(),cita.getMoneda());
    }
}
