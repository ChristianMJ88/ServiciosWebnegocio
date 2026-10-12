package com.techprotech.agenda.modulos.productos.aplicacion;

import com.techprotech.agenda.modulos.productos.api.dto.CatalogoProductosPublicoResponse;
import com.techprotech.agenda.modulos.productos.api.dto.ConfiguracionTiendaResponse;
import com.techprotech.agenda.modulos.productos.api.dto.IndiceProductosPublicosResponse;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioConfigEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServicioCatalogoProductosPublico {
    private final EmpresaSitioConfigRepositorio sitios;
    private final ServicioConfiguracionTienda configuracionTienda;
    private final ProductoRepositorio productos;
    private final CategoriaProductoRepositorio categorias;
    private final ZonaPrecioProductoRepositorio zonasPrecios;
    private final PrecioZonaProductoRepositorio preciosZona;
    private final ReglaDescuentoProductoRepositorio descuentos;
    private final ProductoExistenciaRepositorio existencias;
    private final com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio sucursales;

    public ServicioCatalogoProductosPublico(EmpresaSitioConfigRepositorio sitios,
            ServicioConfiguracionTienda configuracionTienda, ProductoRepositorio productos,
            CategoriaProductoRepositorio categorias, ZonaPrecioProductoRepositorio zonasPrecios,
            PrecioZonaProductoRepositorio preciosZona, ReglaDescuentoProductoRepositorio descuentos,
            ProductoExistenciaRepositorio existencias, com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio sucursales) {
        this.sitios = sitios;
        this.configuracionTienda = configuracionTienda;
        this.productos = productos;
        this.categorias = categorias;
        this.zonasPrecios = zonasPrecios;
        this.preciosZona = preciosZona;
        this.descuentos = descuentos;
        this.existencias = existencias; this.sucursales=sucursales;
    }

    @Transactional(readOnly = true)
    public CatalogoProductosPublicoResponse obtener(String slug, String claveZona, String slugProducto) {
        return obtener(slug,claveZona,slugProducto,null);
    }
    @Transactional(readOnly = true)
    public CatalogoProductosPublicoResponse obtener(String slug, String claveZona, String slugProducto, Long sucursalId) {
        EmpresaSitioConfigEntidad sitio = sitios.findBySlug(slug.trim().toLowerCase(Locale.ROOT))
                .filter(EmpresaSitioConfigEntidad::isPublicado)
                .orElseThrow(() -> noEncontrado("No existe un sitio publicado para ese tenant"));
        ConfiguracionTiendaResponse tienda = configuracionTienda.obtener(sitio.getEmpresaId());
        if (!tienda.habilitada()) throw noEncontrado("La tienda no está publicada");

        var sedes=sucursales.findByEmpresaIdAndActivaTrue(sitio.getEmpresaId());
        Long seleccionada=sucursalId;
        if(seleccionada==null && sedes.size()==1) seleccionada=sedes.get(0).getId();
        final Long sede=seleccionada;
        if(sede!=null && sedes.stream().noneMatch(s->s.getId().equals(sede))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Sucursal no válida");
        List<ZonaPrecioProductoEntidad> zonas = zonasPrecios.findByEmpresaIdOrderByPrioridadAscNombreAsc(sitio.getEmpresaId())
                .stream().filter(ZonaPrecioProductoEntidad::isActiva).toList();
        ZonaPrecioProductoEntidad zona = resolverZona(zonas, claveZona);
        List<ProductoEntidad> listaProductos = productos.findByEmpresaIdAndActivoTrueAndVisiblePublicoTrueOrderByNombreAsc(sitio.getEmpresaId());
        if (StringUtils.hasText(slugProducto)) {
            listaProductos = listaProductos.stream().filter(item -> item.getSlug().equals(slugProducto)).toList();
            if (listaProductos.isEmpty()) throw noEncontrado("Producto no encontrado");
        }

        List<CategoriaProductoEntidad> listaCategorias = categorias.findByEmpresaIdOrderByOrdenAscNombreAsc(sitio.getEmpresaId())
                .stream().filter(CategoriaProductoEntidad::isActiva).toList();
        Map<Long, CategoriaProductoEntidad> categoriaPorId = listaCategorias.stream()
                .collect(Collectors.toMap(CategoriaProductoEntidad::getId, Function.identity()));
        List<Long> productoIds = listaProductos.stream().map(ProductoEntidad::getId).toList();
        Map<Long, PrecioZonaProductoEntidad> precioPorProducto = precioPorProducto(sitio.getEmpresaId(), productoIds, zona);
        Map<Long, BigDecimal> existenciaPorProducto = existenciasPorProducto(sitio.getEmpresaId(), productoIds, sede);
        List<ReglaDescuentoProductoEntidad> reglas = descuentos.findByEmpresaIdOrderByPrioridadDescNombreAsc(sitio.getEmpresaId())
                .stream().filter(this::vigente).toList();

        List<CatalogoProductosPublicoResponse.ProductoPublico> publicados = listaProductos.stream()
                .filter(item -> item.getCategoriaId() == null || categoriaPorId.containsKey(item.getCategoriaId()))
                .map(item -> mapearProducto(item, categoriaPorId.get(item.getCategoriaId()), precioPorProducto.get(item.getId()),
                        reglas, zona, existenciaPorProducto.getOrDefault(item.getId(), BigDecimal.ZERO), tienda.mostrarStock(), sede, tienda.diasNuevo()))
                .toList();

        return new CatalogoProductosPublicoResponse(
                sitio.getEmpresaId(), sitio.getSlug(), sitio.getNombreComercial(), tienda,
                zona == null ? null : mapearZona(zona), zonas.stream().map(this::mapearZona).toList(),
                listaCategorias.stream().map(item -> new CatalogoProductosPublicoResponse.CategoriaPublica(
                        item.getId(), item.getCategoriaPadreId(), item.getNombre(), item.getSlug(), item.getDescripcion())).toList(),
                publicados, sede, sedes.stream().map(s->new CatalogoProductosPublicoResponse.SucursalPublica(s.getId(),s.getNombre())).toList()
        );
    }

    @Transactional(readOnly = true)
    public IndiceProductosPublicosResponse indice() {
        List<IndiceProductosPublicosResponse.ProductoIndexado> indexados = productos.findIndiceProductosPublicados().stream()
                .map(item -> new IndiceProductosPublicosResponse.ProductoIndexado(item.getSitioSlug(), item.getProductoSlug()))
                .toList();
        return new IndiceProductosPublicosResponse(indexados);
    }

    private CatalogoProductosPublicoResponse.ProductoPublico mapearProducto(
            ProductoEntidad producto, CategoriaProductoEntidad categoria, PrecioZonaProductoEntidad precioZona,
            List<ReglaDescuentoProductoEntidad> reglas, ZonaPrecioProductoEntidad zona, BigDecimal existencia,
            boolean mostrarStock, Long sucursalId, int diasNuevo) {
        BigDecimal precioBase = precioZona == null ? producto.getPrecioBase() : precioZona.getPrecio();
        String moneda = precioZona == null ? producto.getMoneda() : precioZona.getMoneda();
        var resultado = CalculadorDescuentosProductos.calcular(producto,zona,sucursalId,BigDecimal.ONE,precioBase,moneda,reglas);
        boolean disponible = !producto.isControlaInventario() || existencia.compareTo(BigDecimal.ZERO) > 0;
        return new CatalogoProductosPublicoResponse.ProductoPublico(
                producto.getId(), producto.getCategoriaId(), categoria == null ? null : categoria.getNombre(),
                producto.getNombre(), producto.getSlug(), producto.getDescripcion(), producto.getImagenUrl(),
                resultado.precio(), resultado.precio().compareTo(precioBase) < 0 ? precioBase : null, moneda,
                resultado.etiqueta(), producto.isControlaInventario(), disponible,
                mostrarStock && producto.isControlaInventario() ? existencia.max(BigDecimal.ZERO) : null,
                PoliticaNovedadProductos.esNuevo(producto,diasNuevo,LocalDate.now())
        );
    }

    private boolean vigente(ReglaDescuentoProductoEntidad regla) {
        return CalculadorDescuentosProductos.vigente(regla,LocalDate.now());
    }

    private Map<Long, PrecioZonaProductoEntidad> precioPorProducto(Long empresaId, List<Long> ids,
                                                                   ZonaPrecioProductoEntidad zona) {
        if (ids.isEmpty() || zona == null) return Map.of();
        return preciosZona.findByEmpresaIdAndProductoIdIn(empresaId, ids).stream()
                .filter(PrecioZonaProductoEntidad::isActivo)
                .filter(item -> item.getZonaPrecioId().equals(zona.getId()))
                .collect(Collectors.toMap(PrecioZonaProductoEntidad::getProductoId, Function.identity()));
    }

    private Map<Long, BigDecimal> existenciasPorProducto(Long empresaId, List<Long> ids, Long sucursalId) {
        if (ids.isEmpty()) return Map.of();
        return existencias.findByIdEmpresaIdAndIdProductoIdIn(empresaId, ids).stream()
                .filter(item -> sucursalId==null || sucursalId.equals(item.getId().getSucursalId()))
                .collect(Collectors.groupingBy(item -> item.getId().getProductoId(),
                        Collectors.reducing(BigDecimal.ZERO,
                                item -> item.getExistencia().subtract(item.getReservado()), BigDecimal::add)));
    }

    private ZonaPrecioProductoEntidad resolverZona(List<ZonaPrecioProductoEntidad> zonas, String claveZona) {
        if (!StringUtils.hasText(claveZona)) return null;
        return zonas.stream().filter(item -> item.getClave().equalsIgnoreCase(claveZona.trim())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zona de precio no válida"));
    }

    private CatalogoProductosPublicoResponse.ZonaPublica mapearZona(ZonaPrecioProductoEntidad zona) {
        return new CatalogoProductosPublicoResponse.ZonaPublica(zona.getId(), zona.getNombre(), zona.getClave());
    }

    private ResponseStatusException noEncontrado(String mensaje) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensaje);
    }

    private record ResultadoDescuento(BigDecimal precio, String etiqueta) {}
}
