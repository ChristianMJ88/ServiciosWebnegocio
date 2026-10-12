package com.techprotech.agenda.modulos.sucursales.aplicacion;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.ZonaPrecioProductoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;
@Service
public class ServicioZonaPrecioSucursal {
    private final SucursalRepositorio sucursales;
    private final ZonaPrecioProductoRepositorio zonas;
    public ServicioZonaPrecioSucursal(SucursalRepositorio sucursales, ZonaPrecioProductoRepositorio zonas) { this.sucursales=sucursales; this.zonas=zonas; }
    public record Opcion(Long id,String nombre,boolean activa) {}
    public record Configuracion(Long zonaPrecioId,List<Opcion> zonas) {}
    @Transactional(readOnly=true)
    public Configuracion consultar(Long empresa,Long sucursal) {
        var sede=sucursales.findByIdAndEmpresaId(sucursal,empresa).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Sucursal no encontrada"));
        return new Configuracion(sede.getZonaPrecioId(),zonas.findByEmpresaIdOrderByPrioridadAscNombreAsc(empresa).stream().map(z->new Opcion(z.getId(),z.getNombre(),z.isActiva())).toList());
    }
    @Transactional
    public Configuracion guardar(Long empresa,Long sucursal,Long zona) {
        var sede=sucursales.findByIdAndEmpresaId(sucursal,empresa).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Sucursal no encontrada"));
        if(zona!=null) zonas.findByIdAndEmpresaId(zona,empresa).filter(z->z.isActiva()).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selecciona una zona activa del negocio"));
        sede.setZonaPrecioId(zona); sucursales.save(sede); return consultar(empresa,sucursal);
    }
}
