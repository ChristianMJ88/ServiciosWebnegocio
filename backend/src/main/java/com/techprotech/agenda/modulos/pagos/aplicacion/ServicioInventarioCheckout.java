package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenDetalleEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenDetalleRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenRepositorio;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.MovimientoInventarioEntidad;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoEntidad;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoExistenciaEntidad;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.MovimientoInventarioRepositorio;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.ProductoExistenciaRepositorio;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.ProductoRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServicioInventarioCheckout {
    private final CheckoutOrdenRepositorio ordenes;
    private final CheckoutOrdenDetalleRepositorio detalles;
    private final ProductoRepositorio productos;
    private final ProductoExistenciaRepositorio existencias;
    private final MovimientoInventarioRepositorio movimientos;

    public ServicioInventarioCheckout(CheckoutOrdenRepositorio ordenes, CheckoutOrdenDetalleRepositorio detalles,
                                      ProductoRepositorio productos, ProductoExistenciaRepositorio existencias,
                                      MovimientoInventarioRepositorio movimientos) {
        this.ordenes = ordenes;
        this.detalles = detalles;
        this.productos = productos;
        this.existencias = existencias;
        this.movimientos = movimientos;
    }

    @Transactional
    public void reservar(Long empresaId, Long ordenId) {
        CheckoutOrdenEntidad orden = bloquear(empresaId, ordenId);
        if ("NO_APLICA".equals(orden.getInventarioEstado()) || "RESERVADO".equals(orden.getInventarioEstado())) return;
        if ("CONFIRMADO".equals(orden.getInventarioEstado())) {
            throw conflicto("El inventario de la orden ya fue confirmado");
        }
        List<CheckoutOrdenDetalleEntidad> lineas = productos(orden);
        Map<Long, ProductoEntidad> catalogo = catalogo(empresaId, lineas);
        for (CheckoutOrdenDetalleEntidad linea : lineas) {
            ProductoEntidad producto = catalogo.get(linea.getProductoId());
            if (producto == null || !producto.isActivo()) throw conflicto("Un producto de la orden ya no está disponible");
            if (!producto.isControlaInventario()) continue;
            ProductoExistenciaEntidad existencia = bloquearExistencia(empresaId, producto.getId(), orden.getSucursalId());
            BigDecimal disponible = existencia.getExistencia().subtract(existencia.getReservado());
            if (disponible.compareTo(linea.getCantidad()) < 0) {
                throw conflicto("No hay existencia suficiente de " + producto.getNombre());
            }
            existencia.setReservado(existencia.getReservado().add(linea.getCantidad()));
            existencias.save(existencia);
        }
        orden.setInventarioEstado(lineas.isEmpty() ? "NO_APLICA" : "RESERVADO");
        ordenes.save(orden);
    }

    @Transactional
    public void confirmar(Long empresaId, Long ordenId) {
        CheckoutOrdenEntidad orden = bloquear(empresaId, ordenId);
        if ("NO_APLICA".equals(orden.getInventarioEstado()) || "CONFIRMADO".equals(orden.getInventarioEstado())) return;
        if (!"RESERVADO".equals(orden.getInventarioEstado())) throw conflicto("La orden no tiene inventario reservado");
        List<CheckoutOrdenDetalleEntidad> lineas = productos(orden);
        Map<Long, ProductoEntidad> catalogo = catalogo(empresaId, lineas);
        for (CheckoutOrdenDetalleEntidad linea : lineas) {
            ProductoEntidad producto = catalogo.get(linea.getProductoId());
            if (producto == null || !producto.isControlaInventario()) continue;
            ProductoExistenciaEntidad existencia = bloquearExistencia(empresaId, producto.getId(), orden.getSucursalId());
            if (existencia.getReservado().compareTo(linea.getCantidad()) < 0
                    || existencia.getExistencia().compareTo(linea.getCantidad()) < 0) {
                throw conflicto("La reserva de " + producto.getNombre() + " ya no es suficiente");
            }
            BigDecimal anterior = existencia.getExistencia();
            BigDecimal nueva = anterior.subtract(linea.getCantidad());
            existencia.setExistencia(nueva);
            existencia.setReservado(existencia.getReservado().subtract(linea.getCantidad()));
            existencias.save(existencia);
            movimientos.save(movimiento(orden, linea, anterior, nueva));
        }
        orden.setInventarioEstado("CONFIRMADO");
        ordenes.save(orden);
    }

    @Transactional
    public void liberar(Long empresaId, Long ordenId) {
        CheckoutOrdenEntidad orden = bloquear(empresaId, ordenId);
        liberar(orden);
    }

    @Transactional
    public void expirar(Long empresaId, Long ordenId) {
        CheckoutOrdenEntidad orden = bloquear(empresaId, ordenId);
        liberar(orden);
        if (!"PAGADA".equals(orden.getEstado())) orden.setEstado("EXPIRADA");
        ordenes.save(orden);
    }

    private void liberar(CheckoutOrdenEntidad orden) {
        if (!"RESERVADO".equals(orden.getInventarioEstado())) return;
        Map<Long, ProductoEntidad> catalogo = catalogo(orden.getEmpresaId(), productos(orden));
        for (CheckoutOrdenDetalleEntidad linea : productos(orden)) {
            ProductoEntidad producto = catalogo.get(linea.getProductoId());
            if (producto == null || !producto.isControlaInventario()) continue;
            ProductoExistenciaEntidad existencia = bloquearExistencia(orden.getEmpresaId(), producto.getId(), orden.getSucursalId());
            existencia.setReservado(existencia.getReservado().subtract(linea.getCantidad()).max(BigDecimal.ZERO));
            existencias.save(existencia);
        }
        orden.setInventarioEstado("LIBERADO");
        ordenes.save(orden);
    }

    private CheckoutOrdenEntidad bloquear(Long empresaId, Long ordenId) {
        return ordenes.bloquearPorEmpresaEId(empresaId, ordenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada"));
    }

    private List<CheckoutOrdenDetalleEntidad> productos(CheckoutOrdenEntidad orden) {
        return detalles.findByEmpresaIdAndCheckoutOrdenIdOrderByIdAsc(orden.getEmpresaId(), orden.getId()).stream()
                .filter(item -> "PRODUCTO".equals(item.getTipoConcepto()) && item.getProductoId() != null)
                .sorted(Comparator.comparing(CheckoutOrdenDetalleEntidad::getProductoId)).toList();
    }

    private Map<Long, ProductoEntidad> catalogo(Long empresaId, List<CheckoutOrdenDetalleEntidad> lineas) {
        return productos.findByEmpresaIdAndIdIn(empresaId,
                        lineas.stream().map(CheckoutOrdenDetalleEntidad::getProductoId).distinct().toList())
                .stream().collect(Collectors.toMap(ProductoEntidad::getId, Function.identity()));
    }

    private ProductoExistenciaEntidad bloquearExistencia(Long empresaId, Long productoId, Long sucursalId) {
        existencias.asegurarFila(empresaId, productoId, sucursalId);
        return existencias.bloquear(empresaId, productoId, sucursalId)
                .orElseThrow(() -> conflicto("No fue posible reservar el inventario"));
    }

    private MovimientoInventarioEntidad movimiento(CheckoutOrdenEntidad orden, CheckoutOrdenDetalleEntidad linea,
                                                     BigDecimal anterior, BigDecimal nueva) {
        MovimientoInventarioEntidad movimiento = new MovimientoInventarioEntidad();
        movimiento.setEmpresaId(orden.getEmpresaId());
        movimiento.setProductoId(linea.getProductoId());
        movimiento.setSucursalId(orden.getSucursalId());
        movimiento.setTipo("VENTA_ONLINE");
        movimiento.setCantidad(linea.getCantidad().negate());
        movimiento.setExistenciaAnterior(anterior);
        movimiento.setExistenciaNueva(nueva);
        movimiento.setReferencia(orden.getPublicId());
        movimiento.setNotas("Salida por checkout público");
        movimiento.setCreadoPorUsuarioId(null);
        return movimiento;
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }
}
