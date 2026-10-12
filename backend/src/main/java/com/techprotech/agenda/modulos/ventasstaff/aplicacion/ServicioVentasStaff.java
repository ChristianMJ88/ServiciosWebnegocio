package com.techprotech.agenda.modulos.ventasstaff.aplicacion;

import com.techprotech.agenda.modulos.ventasstaff.infraestructura.VentasStaffRepositorio;
import java.time.LocalDate;
import com.techprotech.agenda.modulos.ventasstaff.api.VentaStaffResponse;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServicioVentasStaff {
    private final VentasStaffRepositorio repositorio;
    public ServicioVentasStaff(VentasStaffRepositorio repositorio) { this.repositorio=repositorio; }
    @Transactional(readOnly=true)
    public Page<VentaStaffResponse> consultar(Long empresa,Long usuario,LocalDate desde,LocalDate hasta,String concepto,String busqueda,int pagina,int tamano) {
        LocalDate fin=hasta==null?LocalDate.now():hasta;
        LocalDate inicio=desde==null?fin.withDayOfMonth(1):desde;
        String tipo=concepto==null?"":concepto.trim().toUpperCase(java.util.Locale.ROOT);
        if(inicio.isAfter(fin)||!Set.of("","SERVICIO","PRODUCTO").contains(tipo)||pagina<0||tamano<1||tamano>100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Revisa las fechas, el concepto y la paginación");
        return repositorio.consultar(empresa,usuario,inicio.atStartOfDay(),fin.plusDays(1).atStartOfDay(),tipo,
            busqueda==null?"":busqueda.trim().toLowerCase(java.util.Locale.ROOT),PageRequest.of(pagina,tamano)).map(f->new VentaStaffResponse(f.getId(),f.getFolio(),f.getNombre(),f.getConcepto(),f.getEstado(),f.getMoneda(),f.getCantidad(),f.getTotal(),f.getFecha()));
    }
}
