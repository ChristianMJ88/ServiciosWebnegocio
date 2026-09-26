package com.techprotech.agenda.modulos.productos.aplicacion;

import com.techprotech.agenda.modulos.parametros.aplicacion.ServicioParametrosSistema;
import com.techprotech.agenda.modulos.productos.api.dto.*;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServicioDescuentosProductos {
    private static final Set<String> ALCANCES = Set.of("GLOBAL", "PRODUCTO", "CATEGORIA");
    private static final Set<String> TIPOS = Set.of("PORCENTAJE", "MONTO", "PRECIO_FIJO");
    private final ReglaDescuentoProductoRepositorio reglas;
    private final ProductoRepositorio productos;
    private final CategoriaProductoRepositorio categorias;
    private final ZonaPrecioProductoRepositorio zonas;
    private final SucursalRepositorio sucursales;
    private final ServicioParametrosSistema parametros;

    public ServicioDescuentosProductos(ReglaDescuentoProductoRepositorio reglas, ProductoRepositorio productos,
            CategoriaProductoRepositorio categorias, ZonaPrecioProductoRepositorio zonas,
            SucursalRepositorio sucursales, ServicioParametrosSistema parametros) {
        this.reglas=reglas; this.productos=productos; this.categorias=categorias; this.zonas=zonas;
        this.sucursales=sucursales; this.parametros=parametros;
    }

    @Transactional(readOnly=true)
    public CatalogoDescuentosProductoResponse catalogo(Long empresaId) {
        List<ProductoEntidad> listaProductos=productos.findByEmpresaIdOrderByNombreAsc(empresaId);
        List<CategoriaProductoEntidad> listaCategorias=categorias.findByEmpresaIdOrderByOrdenAscNombreAsc(empresaId);
        List<ZonaPrecioProductoEntidad> listaZonas=zonas.findByEmpresaIdOrderByPrioridadAscNombreAsc(empresaId);
        List<SucursalEntidad> listaSucursales=sucursales.findByEmpresaIdOrderByNombreAsc(empresaId);
        Map<Long,String> p=mapa(listaProductos, ProductoEntidad::getId, ProductoEntidad::getNombre);
        Map<Long,String> c=mapa(listaCategorias, CategoriaProductoEntidad::getId, CategoriaProductoEntidad::getNombre);
        Map<Long,String> z=mapa(listaZonas, ZonaPrecioProductoEntidad::getId, ZonaPrecioProductoEntidad::getNombre);
        Map<Long,String> s=mapa(listaSucursales, SucursalEntidad::getId, SucursalEntidad::getNombre);
        return new CatalogoDescuentosProductoResponse(moneda(empresaId), opciones(p), opciones(c), opciones(z), opciones(s),
                reglas.findByEmpresaIdOrderByPrioridadDescNombreAsc(empresaId).stream().map(item->mapear(item,p,c,z,s)).toList());
    }

    @Transactional
    public ReglaDescuentoProductoResponse crear(Long empresaId, ReglaDescuentoProductoRequest request) {
        ReglaDescuentoProductoEntidad entidad=new ReglaDescuentoProductoEntidad(); entidad.setEmpresaId(empresaId);
        aplicar(empresaId,entidad,request); reglas.save(entidad); return buscarEnCatalogo(empresaId,entidad.getId());
    }

    @Transactional
    public ReglaDescuentoProductoResponse actualizar(Long empresaId, Long id, ReglaDescuentoProductoRequest request) {
        ReglaDescuentoProductoEntidad entidad=reglas.findByIdAndEmpresaId(id,empresaId)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Descuento no encontrado"));
        aplicar(empresaId,entidad,request); reglas.save(entidad); return buscarEnCatalogo(empresaId,id);
    }

    private void aplicar(Long empresaId, ReglaDescuentoProductoEntidad e, ReglaDescuentoProductoRequest r) {
        String alcance=normalizarCodigo(r.alcance()); String tipo=normalizarCodigo(r.tipoDescuento());
        if(!ALCANCES.contains(alcance)) throw error("El alcance del descuento no es válido");
        if(!TIPOS.contains(tipo)) throw error("El tipo de descuento no es válido");
        if(r.fechaInicio()!=null && r.fechaFin()!=null && r.fechaFin().isBefore(r.fechaInicio())) throw error("La fecha final debe ser posterior a la fecha inicial");
        if("PORCENTAJE".equals(tipo) && r.valor().compareTo(BigDecimal.valueOf(100))>0) throw error("El porcentaje no puede superar 100");
        if("PRODUCTO".equals(alcance) && r.productoId()==null) throw error("Selecciona el producto al que aplica el descuento");
        if("CATEGORIA".equals(alcance) && r.categoriaId()==null) throw error("Selecciona la categoría a la que aplica el descuento");
        if(r.productoId()!=null) productos.findByIdAndEmpresaId(r.productoId(),empresaId).orElseThrow(()->error("Producto no válido para este tenant"));
        if(r.categoriaId()!=null) categorias.findByIdAndEmpresaId(r.categoriaId(),empresaId).orElseThrow(()->error("Categoría no válida para este tenant"));
        if(r.zonaPrecioId()!=null) zonas.findByIdAndEmpresaId(r.zonaPrecioId(),empresaId).orElseThrow(()->error("Zona no válida para este tenant"));
        if(r.sucursalId()!=null) sucursales.findByIdAndEmpresaId(r.sucursalId(),empresaId).orElseThrow(()->error("Sucursal no válida para este tenant"));
        String moneda="PORCENTAJE".equals(tipo)?null:(StringUtils.hasText(r.moneda())?r.moneda().trim().toUpperCase(Locale.ROOT):moneda(empresaId));
        e.setNombre(r.nombre().trim()); e.setDescripcion(texto(r.descripcion())); e.setAlcance(alcance);
        e.setProductoId("PRODUCTO".equals(alcance)?r.productoId():null); e.setCategoriaId("CATEGORIA".equals(alcance)?r.categoriaId():null);
        e.setZonaPrecioId(r.zonaPrecioId()); e.setSucursalId(r.sucursalId()); e.setTipoDescuento(tipo); e.setValor(r.valor()); e.setMoneda(moneda);
        e.setCantidadMinima(r.cantidadMinima()); e.setFechaInicio(r.fechaInicio()); e.setFechaFin(r.fechaFin());
        e.setPrioridad(r.prioridad()); e.setCombinable(r.combinable()); e.setActiva(r.activa());
    }

    private ReglaDescuentoProductoResponse buscarEnCatalogo(Long empresaId,Long id){return catalogo(empresaId).reglas().stream().filter(x->x.id().equals(id)).findFirst().orElseThrow();}
    private ReglaDescuentoProductoResponse mapear(ReglaDescuentoProductoEntidad e,Map<Long,String> p,Map<Long,String> c,Map<Long,String> z,Map<Long,String> s){
        return new ReglaDescuentoProductoResponse(e.getId(),e.getNombre(),e.getDescripcion(),e.getAlcance(),e.getProductoId(),p.get(e.getProductoId()),
                e.getCategoriaId(),c.get(e.getCategoriaId()),e.getZonaPrecioId(),z.get(e.getZonaPrecioId()),e.getSucursalId(),s.get(e.getSucursalId()),
                e.getTipoDescuento(),e.getValor(),e.getMoneda(),e.getCantidadMinima(),e.getFechaInicio(),e.getFechaFin(),e.getPrioridad(),e.isCombinable(),e.isActiva(),estado(e));
    }
    private String estado(ReglaDescuentoProductoEntidad e){LocalDate hoy=LocalDate.now();if(!e.isActiva())return "INACTIVA";if(e.getFechaInicio()!=null&&hoy.isBefore(e.getFechaInicio()))return "PROGRAMADA";if(e.getFechaFin()!=null&&hoy.isAfter(e.getFechaFin()))return "EXPIRADA";return "VIGENTE";}
    private String moneda(Long empresaId){return parametros.listar(empresaId).stream().filter(x->"MONEDA_PREDETERMINADA".equals(x.clave())).map(x->x.valor()).findFirst().orElseThrow(()->new IllegalStateException("Falta el parámetro MONEDA_PREDETERMINADA"));}
    private <T> Map<Long,String> mapa(List<T> datos,Function<T,Long> id,Function<T,String> nombre){return datos.stream().collect(Collectors.toMap(id,nombre));}
    private List<CatalogoDescuentosProductoResponse.OpcionDescuento> opciones(Map<Long,String> mapa){return mapa.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(x->new CatalogoDescuentosProductoResponse.OpcionDescuento(x.getKey(),x.getValue())).toList();}
    private String normalizarCodigo(String valor){return valor.trim().toUpperCase(Locale.ROOT);}
    private String texto(String valor){return StringUtils.hasText(valor)?valor.trim():null;}
    private ResponseStatusException error(String mensaje){return new ResponseStatusException(HttpStatus.BAD_REQUEST,mensaje);}
}
