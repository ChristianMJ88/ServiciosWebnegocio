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

    public ServicioCatalogoProductosPublico(EmpresaSitioConfigRepositorio sitios,
            ServicioConfiguracionTienda configuracionTienda, ProductoRepositorio productos,
            CategoriaProductoRepositorio categorias, ZonaPrecioProductoRepositorio zonasPrecios,
            PrecioZonaProductoRepositorio preciosZona, ReglaDescuentoProductoRepositorio descuentos,
            ProductoExistenciaRepositorio existencias) {
        this.sitios = sitios;
        this.configuracionTienda = configuracionTienda;
        this.productos = productos;
        this.categorias = categorias;
        this.zonasPrecios = zonasPrecios;
        this.preciosZona = preciosZona;
        this.descuentos = descuentos;
        this.existencias = existencias;
    }

    @Transactional(readOnly = true)
    public CatalogoProductosPublicoResponse obtener(String slug, String claveZona, String slugProducto) {
        EmpresaSitioConfigEntidad sitio = sitios.findBySlug(slug.trim().toLowerCase(Locale.ROOT))
                .filter(EmpresaSitioConfigEntidad::isPublicado)
                .orElseThrow(() -> noEncontrado("No existe un sitio publicado para ese tenant"));
        ConfiguracionTiendaResponse tienda = configuracionTienda.obtener(sitio.getEmpresaId());
        if (!tienda.habilitada()) throw noEncontrado("La tienda no está publicada");

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
        Map<Long, BigDecimal> existenciaPorProducto = existenciasPorProducto(sitio.getEmpresaId(), productoIds);
        List<ReglaDescuentoProductoEntidad> reglas = descuentos.findByEmpresaIdOrderByPrioridadDescNombreAsc(sitio.getEmpresaId())
                .stream().filter(this::vigente).toList();

        List<CatalogoProductosPublicoResponse.ProductoPublico> publicados = listaProductos.stream()
                .filter(item -> item.getCategoriaId() == null || categoriaPorId.containsKey(item.getCategoriaId()))
                .map(item -> mapearProducto(item, categoriaPorId.get(item.getCategoriaId()), precioPorProducto.get(item.getId()),
                        reglas, zona, existenciaPorProducto.getOrDefault(item.getId(), BigDecimal.ZERO), tienda.mostrarStock()))
                .toList();

        return new CatalogoProductosPublicoResponse(
                sitio.getEmpresaId(), sitio.getSlug(), sitio.getNombreComercial(), tienda,
                zona == null ? null : mapearZona(zona), zonas.stream().map(this::mapearZona).toList(),
                listaCategorias.stream().map(item -> new CatalogoProductosPublicoResponse.CategoriaPublica(
                        item.getId(), item.getCategoriaPadreId(), item.getNombre(), item.getSlug(), item.getDescripcion())).toList(),
                publicados
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
            boolean mostrarStock) {
        BigDecimal precioBase = precioZona == null ? producto.getPrecioBase() : precioZona.getPrecio();
        String moneda = precioZona == null ? producto.getMoneda() : precioZona.getMoneda();
        ResultadoDescuento resultado = aplicarDescuentos(producto, precioBase, moneda, reglas, zona);
        boolean disponible = !producto.isControlaInventario() || existencia.compareTo(BigDecimal.ZERO) > 0;
        return new CatalogoProductosPublicoResponse.ProductoPublico(
                producto.getId(), producto.getCategoriaId(), categoria == null ? null : categoria.getNombre(),
                producto.getNombre(), producto.getSlug(), producto.getDescripcion(), producto.getImagenUrl(),
                resultado.precio(), resultado.precio().compareTo(precioBase) < 0 ? precioBase : null, moneda,
                resultado.etiqueta(), producto.isControlaInventario(), disponible,
                mostrarStock && producto.isControlaInventario() ? existencia.max(BigDecimal.ZERO) : null
        );
    }

    private ResultadoDescuento aplicarDescuentos(ProductoEntidad producto, BigDecimal precioInicial, String moneda,
                                                  List<ReglaDescuentoProductoEntidad> reglas,
                                                  ZonaPrecioProductoEntidad zona) {
        BigDecimal precio = precioInicial;
        List<String> aplicados = new ArrayList<>();
        for (ReglaDescuentoProductoEntidad regla : reglas) {
            if (!aplica(regla, producto, zona) || regla.getCantidadMinima().compareTo(BigDecimal.ONE) > 0) continue;
            if (!"PORCENTAJE".equals(regla.getTipoDescuento())
                    && !Objects.equals(moneda, regla.getMoneda())) continue;
            precio = switch (regla.getTipoDescuento()) {
                case "PORCENTAJE" -> precio.subtract(precio.multiply(regla.getValor()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                case "MONTO" -> precio.subtract(regla.getValor());
                case "PRECIO_FIJO" -> regla.getValor();
                default -> precio;
            };
            precio = precio.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            aplicados.add(regla.getNombre());
            if (!regla.isCombinable()) break;
        }
        return new ResultadoDescuento(precio, aplicados.isEmpty() ? null : String.join(" + ", aplicados));
    }

    private boolean aplica(ReglaDescuentoProductoEntidad regla, ProductoEntidad producto, ZonaPrecioProductoEntidad zona) {
        if (regla.getZonaPrecioId() != null && (zona == null || !regla.getZonaPrecioId().equals(zona.getId()))) return false;
        if (regla.getSucursalId() != null) return false;
        return switch (regla.getAlcance()) {
            case "GLOBAL" -> true;
            case "PRODUCTO" -> Objects.equals(regla.getProductoId(), producto.getId());
            case "CATEGORIA" -> Objects.equals(regla.getCategoriaId(), producto.getCategoriaId());
            default -> false;
        };
    }

    private boolean vigente(ReglaDescuentoProductoEntidad regla) {
        LocalDate hoy = LocalDate.now();
        return regla.isActiva()
                && (regla.getFechaInicio() == null || !hoy.isBefore(regla.getFechaInicio()))
                && (regla.getFechaFin() == null || !hoy.isAfter(regla.getFechaFin()));
    }

    private Map<Long, PrecioZonaProductoEntidad> precioPorProducto(Long empresaId, List<Long> ids,
                                                                   ZonaPrecioProductoEntidad zona) {
        if (ids.isEmpty() || zona == null) return Map.of();
        return preciosZona.findByEmpresaIdAndProductoIdIn(empresaId, ids).stream()
                .filter(PrecioZonaProductoEntidad::isActivo)
                .filter(item -> item.getZonaPrecioId().equals(zona.getId()))
                .collect(Collectors.toMap(PrecioZonaProductoEntidad::getProductoId, Function.identity()));
    }

    private Map<Long, BigDecimal> existenciasPorProducto(Long empresaId, List<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return existencias.findByIdEmpresaIdAndIdProductoIdIn(empresaId, ids).stream()
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
