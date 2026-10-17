package com.techprotech.agenda.modulos.caja.aplicacion;

import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.PagoCitaRepositorio;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.VentaDetalleRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional(readOnly = true)
public class ServicioSaldoCitas {
    public static final List<String> ESTADOS_COBRABLES = List.of("PENDIENTE", "CONFIRMADA", "FINALIZADA");
    private final CitaRepositorio citas;
    private final PagoCitaRepositorio pagos;
    private final VentaDetalleRepositorio ventas;

    public ServicioSaldoCitas(CitaRepositorio citas, PagoCitaRepositorio pagos, VentaDetalleRepositorio ventas) {
        this.citas=citas; this.pagos=pagos; this.ventas=ventas;
    }

    public Map<Long, SaldoCita> saldos(Long empresaId, Collection<Long> citaIds) {
        if (empresaId==null || citaIds==null || citaIds.isEmpty()) return Map.of();
        var ids=citaIds.stream().filter(Objects::nonNull).distinct().toList();
        return ids.isEmpty()?Map.of():calcular(empresaId,citas.findByEmpresaIdAndIdIn(empresaId,ids));
    }

    /** Reutiliza las citas ya consultadas; las sumas se hacen una sola vez por lote y tenant. */
    public Map<Long, SaldoCita> calcular(Long empresaId, Collection<CitaEntidad> consultadas) {
        if (empresaId==null || consultadas==null || consultadas.isEmpty()) return Map.of();
        var propias=consultadas.stream().filter(c -> empresaId.equals(c.getEmpresaId()) && c.getId()!=null).toList();
        var ids=propias.stream().map(CitaEntidad::getId).distinct().toList();
        if(ids.isEmpty()) return Map.of();
        var caja=totales(pagos.sumarPagadoPorCitas(empresaId,ids));
        var venta=totales(ventas.sumarServiciosPagadosPorCitas(empresaId,ids));
        var resultado=new HashMap<Long,SaldoCita>();
        for(var cita:propias) {
            var precio=seguro(cita.getPrecio()).max(BigDecimal.ZERO);
            var pagadoCaja=caja.getOrDefault(cita.getId(),BigDecimal.ZERO);
            var pagadoVenta=venta.getOrDefault(cita.getId(),BigDecimal.ZERO);
            // D2 aún no tiene persistencia: se reserva el contrato sin inventar condonaciones.
            var condonado=BigDecimal.ZERO;
            var pagado=pagadoCaja.add(pagadoVenta);
            var pendiente=precio.subtract(pagado).subtract(condonado).max(BigDecimal.ZERO);
            EstadoCobroCita estado;
            if(!estadoCobrable(cita)) { estado=EstadoCobroCita.NO_COBRABLE; pendiente=BigDecimal.ZERO; }
            else if(precio.signum()==0) estado=EstadoCobroCita.SIN_COSTO;
            else if(pendiente.signum()==0) estado=EstadoCobroCita.PAGADA;
            else if(pagado.signum()>0) estado=EstadoCobroCita.PARCIAL;
            else estado=EstadoCobroCita.PENDIENTE;
            resultado.put(cita.getId(),new SaldoCita(precio,pagadoCaja,pagadoVenta,condonado,pendiente,estado));
        }
        return Map.copyOf(resultado);
    }

    public SaldoCita de(CitaEntidad cita) {
        var saldo=calcular(cita.getEmpresaId(),List.of(cita)).get(cita.getId());
        if(saldo==null)throw new ResponseStatusException(NOT_FOUND,"La cita no existe para la empresa");
        return saldo;
    }

    public static boolean estadoCobrable(CitaEntidad cita) {
        return cita!=null && ESTADOS_COBRABLES.contains(cita.getEstado()==null?"":cita.getEstado());
    }
    public static boolean esCobrable(CitaEntidad cita) {
        return estadoCobrable(cita) && (cita.getCheckInEn()!=null || "FINALIZADA".equals(cita.getEstado()));
    }
    private static Map<Long,BigDecimal> totales(List<Object[]> filas) {
        var mapa=new HashMap<Long,BigDecimal>();
        for(var fila:filas)mapa.put(((Number)fila[0]).longValue(),seguro((BigDecimal)fila[1]));
        return mapa;
    }
    private static BigDecimal seguro(BigDecimal valor) { return valor==null?BigDecimal.ZERO:valor; }
}
