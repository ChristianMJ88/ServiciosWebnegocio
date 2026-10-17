package com.techprotech.agenda.modulos.admin.aplicacion;

import com.techprotech.agenda.compartido.tiempo.ZonaNegocio;
import com.techprotech.agenda.modulos.admin.api.dto.FinanzasCitasResponse;
import com.techprotech.agenda.modulos.admin.infraestructura.repositorio.ReporteCobroCitasRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
@Transactional(readOnly=true)
public class ServicioReporteCobroCitas {
    private final ReporteCobroCitasRepositorio reporte;
    private final SucursalRepositorio sucursales;
    private final ZonaNegocio zonas;
    public ServicioReporteCobroCitas(ReporteCobroCitasRepositorio reporte,SucursalRepositorio sucursales,ZonaNegocio zonas) {
        this.reporte=reporte;this.sucursales=sucursales;this.zonas=zonas;
    }
    public List<FinanzasCitasResponse> consultar(UsuarioAutenticado usuario,LocalDate desde,LocalDate hasta) {
        if(usuario==null || usuario.empresaId()==null || usuario.esPlataforma()) throw new ResponseStatusException(NOT_FOUND);
        if(!usuario.tienePermiso("REPORTES_ADMIN_VER")) throw new ResponseStatusException(FORBIDDEN,"No tienes permiso para consultar reportes");
        if((desde==null)!=(hasta==null) || desde!=null && desde.isAfter(hasta))
            throw new ResponseStatusException(BAD_REQUEST,"El periodo del reporte no es válido");
        var totales=new TreeMap<String,BigDecimal[]>();
        for(var sucursal:sucursales.findByEmpresaIdOrderByNombreAsc(usuario.empresaId())) {
            if(!usuario.puedeAccederSucursal(sucursal.getId())) continue;
            var zona=zonas.de(usuario.empresaId(),sucursal.getId());
            var inicio=desde==null?null:desde.atStartOfDay();var fin=hasta==null?null:hasta.plusDays(1).atStartOfDay();
            var inicioUtc=inicio==null?null:inicio.atZone(zona).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
            var finUtc=fin==null?null:fin.atZone(zona).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
            for(var fila:reporte.consultar(usuario.empresaId(),sucursal.getId(),inicio,fin,inicioUtc,finUtc)) {
                var t=totales.computeIfAbsent(fila.moneda(),m->new BigDecimal[]{BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO});
                t[0]=t[0].add(seguro(fila.programado()));t[1]=t[1].add(seguro(fila.devengado()));
                t[2]=t[2].add(seguro(fila.cobrado()));t[3]=t[3].add(seguro(fila.pendiente()));
            }
        }
        return totales.entrySet().stream().map(e->{var t=e.getValue();return new FinanzasCitasResponse(e.getKey(),redondear(t[0]),
            redondear(t[1]),redondear(t[2]),redondear(t[3]),null,null,false);}).toList();
    }
    private BigDecimal seguro(BigDecimal n) {return n==null?BigDecimal.ZERO:n;}
    private BigDecimal redondear(BigDecimal n) {return n.setScale(2,RoundingMode.HALF_UP);}
}
