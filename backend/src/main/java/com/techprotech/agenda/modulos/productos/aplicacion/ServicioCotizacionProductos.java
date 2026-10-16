package com.techprotech.agenda.modulos.productos.aplicacion;

import java.time.Clock;
import com.techprotech.agenda.compartido.tiempo.ZonaNegocio;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServicioCotizacionProductos {
 private final Clock reloj; private final ZonaNegocio zonasNegocio;
    private final ProductoRepositorio productos;
    private final ZonaPrecioProductoRepositorio zonas;
    private final PrecioZonaProductoRepositorio preciosZona;
    private final ReglaDescuentoProductoRepositorio descuentos;
    private final ProductoExistenciaRepositorio existencias;

    public ServicioCotizacionProductos(ProductoRepositorio productos, ZonaPrecioProductoRepositorio zonas,
                                       PrecioZonaProductoRepositorio preciosZona,
                                       ReglaDescuentoProductoRepositorio descuentos,
                                       ProductoExistenciaRepositorio existencias, Clock reloj, ZonaNegocio zonasNegocio) {this.reloj=reloj;this.zonasNegocio=zonasNegocio;
        this.productos = productos;
        this.zonas = zonas;
        this.preciosZona = preciosZona;
        this.descuentos = descuentos;
        this.existencias = existencias;
    }

    public ResultadoCotizacion cotizar(Long empresaId, Long sucursalId, String claveZona,
                                       Map<Long, BigDecimal> cantidades) {
        if (cantidades.isEmpty()) return new ResultadoCotizacion(null, List.of());
        ZonaPrecioProductoEntidad zona = resolverZona(empresaId, claveZona);
        List<ProductoEntidad> encontrados = productos.findByEmpresaIdAndIdIn(empresaId, cantidades.keySet()).stream()
                .filter(ProductoEntidad::isActivo).filter(ProductoEntidad::isVisiblePublico).toList();
        if (encontrados.size() != cantidades.size()) throw error(HttpStatus.BAD_REQUEST, "Uno o más productos no están disponibles");
        Map<Long, PrecioZonaProductoEntidad> precios = zona == null ? Map.of() : preciosZona
                .findByEmpresaIdAndProductoIdIn(empresaId, cantidades.keySet()).stream()
                .filter(PrecioZonaProductoEntidad::isActivo)
                .filter(item -> zona.getId().equals(item.getZonaPrecioId()))
                .collect(Collectors.toMap(PrecioZonaProductoEntidad::getProductoId, Function.identity()));
        var hoy = LocalDate.now(reloj.withZone(zonasNegocio.de(empresaId, sucursalId)));
        List<ReglaDescuentoProductoEntidad> reglas = descuentos.findByEmpresaIdOrderByPrioridadDescNombreAsc(empresaId)
                .stream().filter(regla -> CalculadorDescuentosProductos.vigente(regla, hoy)).toList();
        Map<Long, ProductoExistenciaEntidad> stock = existencias
                .findByIdEmpresaIdAndIdSucursalIdIn(empresaId, List.of(sucursalId)).stream()
                .collect(Collectors.toMap(item -> item.getId().getProductoId(), Function.identity()));

        List<CotizacionProducto> lineas = encontrados.stream().map(producto -> {
            BigDecimal cantidad = cantidades.get(producto.getId());
            if (cantidad == null || cantidad.signum() <= 0) throw error(HttpStatus.BAD_REQUEST, "La cantidad debe ser mayor que cero");
            PrecioZonaProductoEntidad precioZona = precios.get(producto.getId());
            BigDecimal precioLista = precioZona == null ? producto.getPrecioBase() : precioZona.getPrecio();
            String moneda = precioZona == null ? producto.getMoneda() : precioZona.getMoneda();
            BigDecimal precio = aplicarDescuentos(producto, zona, sucursalId, cantidad, precioLista, moneda, reglas);
            if (producto.isControlaInventario()) {
                ProductoExistenciaEntidad existencia = stock.get(producto.getId());
                BigDecimal disponible = existencia == null ? BigDecimal.ZERO
                        : existencia.getExistencia().subtract(existencia.getReservado());
                if (disponible.compareTo(cantidad) < 0) throw error(HttpStatus.CONFLICT,
                        "No hay existencia suficiente de " + producto.getNombre());
            }
            BigDecimal totalLista = precioLista.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
            BigDecimal total = precio.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
            return new CotizacionProducto(producto.getId(), producto.getNombre(), cantidad,
                    precioLista.setScale(2, RoundingMode.HALF_UP), precio, totalLista.subtract(total), total, moneda);
        }).toList();
        return new ResultadoCotizacion(zona == null ? null : zona.getId(), lineas);
    }

    private BigDecimal aplicarDescuentos(ProductoEntidad producto, ZonaPrecioProductoEntidad zona, Long sucursalId,
                                         BigDecimal cantidad, BigDecimal inicial, String moneda,
                                         List<ReglaDescuentoProductoEntidad> reglas) {
        return CalculadorDescuentosProductos.calcular(producto,zona,sucursalId,cantidad,inicial,moneda,reglas).precio();
    }

    private ZonaPrecioProductoEntidad resolverZona(Long empresaId, String clave) {
        if (!StringUtils.hasText(clave)) return null;
        return zonas.findByEmpresaIdOrderByPrioridadAscNombreAsc(empresaId).stream()
                .filter(ZonaPrecioProductoEntidad::isActiva)
                .filter(item -> item.getClave().equalsIgnoreCase(clave.trim()))
                .findFirst().orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "Zona de precio no válida"));
    }

    private ResponseStatusException error(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }

    public record ResultadoCotizacion(Long zonaPrecioId, List<CotizacionProducto> productos) {}
    public record CotizacionProducto(Long productoId, String nombre, BigDecimal cantidad,
                                     BigDecimal precioListaUnitario, BigDecimal precioUnitario,
                                     BigDecimal descuento, BigDecimal total, String moneda) {}
}
