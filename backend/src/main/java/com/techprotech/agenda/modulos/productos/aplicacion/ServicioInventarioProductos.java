package com.techprotech.agenda.modulos.productos.aplicacion;

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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServicioInventarioProductos {
    private static final BigDecimal CERO = BigDecimal.ZERO;
    private static final Set<String> TIPOS_AJUSTE = Set.of("ENTRADA", "SALIDA", "AJUSTE");

    private final ProductoRepositorio productos;
    private final ProductoExistenciaRepositorio existencias;
    private final MovimientoInventarioRepositorio movimientos;
    private final SucursalRepositorio sucursales;

    public ServicioInventarioProductos(ProductoRepositorio productos,
                                       ProductoExistenciaRepositorio existencias,
                                       MovimientoInventarioRepositorio movimientos,
                                       SucursalRepositorio sucursales) {
        this.productos = productos;
        this.existencias = existencias;
        this.movimientos = movimientos;
        this.sucursales = sucursales;
    }

    @Transactional(readOnly = true)
    public CatalogoInventarioResponse obtenerCatalogo(Long empresaId, List<Long> sucursalesPermitidas) {
        List<SucursalEntidad> sedes = sucursales.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .filter(SucursalEntidad::isActiva)
                .filter(sucursal -> sinScope(sucursalesPermitidas) || sucursalesPermitidas.contains(sucursal.getId()))
                .toList();
        List<ProductoEntidad> articulos = productos.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .filter(ProductoEntidad::isActivo)
                .filter(ProductoEntidad::isControlaInventario)
                .toList();
        List<Long> sedeIds = sedes.stream().map(SucursalEntidad::getId).toList();
        Map<ProductoExistenciaId, ProductoExistenciaEntidad> porClave = sedeIds.isEmpty()
                ? Map.of()
                : existencias.findByIdEmpresaIdAndIdSucursalIdIn(empresaId, sedeIds).stream()
                    .collect(Collectors.toMap(ProductoExistenciaEntidad::getId, Function.identity()));
        Map<Long, String> nombresProducto = articulos.stream().collect(Collectors.toMap(ProductoEntidad::getId, ProductoEntidad::getNombre));
        Map<Long, String> nombresSucursal = sedes.stream().collect(Collectors.toMap(SucursalEntidad::getId, SucursalEntidad::getNombre));

        List<ExistenciaProductoResponse> filas = articulos.stream()
                .flatMap(producto -> sedes.stream().map(sucursal -> mapearExistencia(
                        producto, sucursal, porClave.get(new ProductoExistenciaId(empresaId, producto.getId(), sucursal.getId())))))
                .toList();
        List<MovimientoInventarioResponse> recientes = sedeIds.isEmpty() ? List.of()
                : movimientos.findTop100ByEmpresaIdAndSucursalIdInOrderByCreadoEnDesc(empresaId, sedeIds).stream()
                    .map(item -> mapearMovimiento(item, nombresProducto, nombresSucursal)).toList();

        return new CatalogoInventarioResponse(
                sedes.stream().map(item -> new CatalogoInventarioResponse.SucursalInventario(item.getId(), item.getNombre())).toList(),
                articulos.stream().map(item -> new CatalogoInventarioResponse.ProductoInventario(item.getId(), item.getSku(), item.getNombre(), item.getStockMinimo())).toList(),
                filas,
                recientes,
                List.of(
                        new CatalogoInventarioResponse.TipoMovimientoInventario("ENTRADA", "Entrada", "Agrega unidades a la existencia."),
                        new CatalogoInventarioResponse.TipoMovimientoInventario("SALIDA", "Salida", "Descuenta unidades disponibles."),
                        new CatalogoInventarioResponse.TipoMovimientoInventario("AJUSTE", "Conteo físico", "Reemplaza la existencia con la cantidad capturada.")
                )
        );
    }

    @Transactional
    public ExistenciaProductoResponse ajustar(Long empresaId, Long usuarioId, List<Long> sucursalesPermitidas, AjusteInventarioRequest request) {
        ProductoEntidad producto = validarProducto(empresaId, request.productoId());
        SucursalEntidad sucursal = validarSucursal(empresaId, sucursalesPermitidas, request.sucursalId());
        String tipo = request.tipo().trim().toUpperCase(Locale.ROOT);
        if (!TIPOS_AJUSTE.contains(tipo)) throw error("El tipo de movimiento no es válido");
        if (!"AJUSTE".equals(tipo) && request.cantidad().signum() <= 0) throw error("La cantidad debe ser mayor que cero");

        ProductoExistenciaEntidad existencia = bloquearExistencia(empresaId, producto.getId(), sucursal.getId());
        BigDecimal anterior = existencia.getExistencia();
        BigDecimal nueva = switch (tipo) {
            case "ENTRADA" -> anterior.add(request.cantidad());
            case "SALIDA" -> anterior.subtract(request.cantidad());
            default -> request.cantidad();
        };
        if (nueva.compareTo(existencia.getReservado()) < 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El movimiento afecta unidades reservadas; libera las reservas antes de continuar");
        existencia.setExistencia(nueva);
        existencias.save(existencia);
        guardarMovimiento(empresaId, usuarioId, producto.getId(), sucursal.getId(), tipo,
                nueva.subtract(anterior), anterior, nueva, request.referencia(), request.notas());
        return mapearExistencia(producto, sucursal, existencia);
    }

    @Transactional
    public List<ExistenciaProductoResponse> transferir(Long empresaId, Long usuarioId, List<Long> sucursalesPermitidas, TransferenciaInventarioRequest request) {
        if (Objects.equals(request.sucursalOrigenId(), request.sucursalDestinoId())) throw error("Selecciona sucursales diferentes");
        ProductoEntidad producto = validarProducto(empresaId, request.productoId());
        SucursalEntidad origen = validarSucursal(empresaId, sucursalesPermitidas, request.sucursalOrigenId());
        SucursalEntidad destino = validarSucursal(empresaId, sucursalesPermitidas, request.sucursalDestinoId());

        Long primeraId = Math.min(origen.getId(), destino.getId());
        Long segundaId = Math.max(origen.getId(), destino.getId());
        ProductoExistenciaEntidad primera = bloquearExistencia(empresaId, producto.getId(), primeraId);
        ProductoExistenciaEntidad segunda = bloquearExistencia(empresaId, producto.getId(), segundaId);
        ProductoExistenciaEntidad existenciaOrigen = primeraId.equals(origen.getId()) ? primera : segunda;
        ProductoExistenciaEntidad existenciaDestino = primeraId.equals(destino.getId()) ? primera : segunda;
        BigDecimal anteriorOrigen = existenciaOrigen.getExistencia();
        BigDecimal anteriorDestino = existenciaDestino.getExistencia();
        BigDecimal nuevaOrigen = anteriorOrigen.subtract(request.cantidad());
        if (nuevaOrigen.compareTo(existenciaOrigen.getReservado()) < 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La transferencia supera la existencia disponible en la sucursal de origen");
        BigDecimal nuevaDestino = anteriorDestino.add(request.cantidad());
        existenciaOrigen.setExistencia(nuevaOrigen);
        existenciaDestino.setExistencia(nuevaDestino);
        existencias.saveAll(List.of(existenciaOrigen, existenciaDestino));

        String referencia = StringUtils.hasText(request.referencia()) ? request.referencia().trim() : "TR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        guardarMovimiento(empresaId, usuarioId, producto.getId(), origen.getId(), "TRANSFERENCIA_SALIDA", request.cantidad().negate(), anteriorOrigen, nuevaOrigen, referencia, request.notas());
        guardarMovimiento(empresaId, usuarioId, producto.getId(), destino.getId(), "TRANSFERENCIA_ENTRADA", request.cantidad(), anteriorDestino, nuevaDestino, referencia, request.notas());
        return List.of(mapearExistencia(producto, origen, existenciaOrigen), mapearExistencia(producto, destino, existenciaDestino));
    }

    @Transactional
    public void descontarVenta(Long empresaId, Long usuarioId, List<Long> sucursalesPermitidas,
                               Long sucursalId, Map<Long, BigDecimal> cantidades, String referencia) {
        validarSucursal(empresaId, sucursalesPermitidas, sucursalId);
        cantidades.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(item -> {
            ProductoEntidad producto = productos.findByIdAndEmpresaId(item.getKey(), empresaId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
            if (!producto.isActivo()) throw error("El producto ya no está activo");
            if (!producto.isControlaInventario()) return;
            BigDecimal cantidad = item.getValue();
            if (cantidad == null || cantidad.signum() <= 0) throw error("La cantidad vendida debe ser mayor que cero");
            ProductoExistenciaEntidad existencia = bloquearExistencia(empresaId, producto.getId(), sucursalId);
            BigDecimal anterior = existencia.getExistencia();
            BigDecimal nueva = anterior.subtract(cantidad);
            if (nueva.compareTo(existencia.getReservado()) < 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No hay existencia disponible de " + producto.getNombre() + " en la sucursal");
            }
            existencia.setExistencia(nueva);
            existencias.save(existencia);
            guardarMovimiento(empresaId, usuarioId, producto.getId(), sucursalId, "VENTA",
                    cantidad.negate(), anterior, nueva, referencia, "Salida automática por venta en caja");
        });
    }

    private ProductoExistenciaEntidad bloquearExistencia(Long empresaId, Long productoId, Long sucursalId) {
        existencias.asegurarFila(empresaId, productoId, sucursalId);
        return existencias.bloquear(empresaId, productoId, sucursalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No fue posible preparar la existencia"));
    }

    private ProductoEntidad validarProducto(Long empresaId, Long productoId) {
        ProductoEntidad producto = productos.findByIdAndEmpresaId(productoId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        if (!producto.isActivo() || !producto.isControlaInventario()) throw error("El producto no tiene control de inventario activo");
        return producto;
    }

    private SucursalEntidad validarSucursal(Long empresaId, List<Long> permitidas, Long sucursalId) {
        if (!sinScope(permitidas) && !permitidas.contains(sucursalId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a la sucursal indicada");
        return sucursales.findByIdAndEmpresaIdAndActivaTrue(sucursalId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sucursal no encontrada"));
    }

    private void guardarMovimiento(Long empresaId, Long usuarioId, Long productoId, Long sucursalId, String tipo,
                                   BigDecimal cantidad, BigDecimal anterior, BigDecimal nueva, String referencia, String notas) {
        MovimientoInventarioEntidad movimiento = new MovimientoInventarioEntidad();
        movimiento.setEmpresaId(empresaId); movimiento.setProductoId(productoId); movimiento.setSucursalId(sucursalId);
        movimiento.setTipo(tipo); movimiento.setCantidad(cantidad); movimiento.setExistenciaAnterior(anterior); movimiento.setExistenciaNueva(nueva);
        movimiento.setReferencia(limpiar(referencia)); movimiento.setNotas(limpiar(notas)); movimiento.setCreadoPorUsuarioId(usuarioId);
        movimientos.save(movimiento);
    }

    private ExistenciaProductoResponse mapearExistencia(ProductoEntidad producto, SucursalEntidad sucursal, ProductoExistenciaEntidad item) {
        BigDecimal existencia = item == null ? CERO : item.getExistencia();
        BigDecimal reservado = item == null ? CERO : item.getReservado();
        BigDecimal disponible = existencia.subtract(reservado);
        BigDecimal minimo = item != null && item.getStockMinimo() != null ? item.getStockMinimo() : producto.getStockMinimo();
        String estado = disponible.signum() <= 0 ? "AGOTADO" : disponible.compareTo(minimo) <= 0 ? "BAJO" : "DISPONIBLE";
        return new ExistenciaProductoResponse(producto.getId(), producto.getSku(), producto.getNombre(), sucursal.getId(), sucursal.getNombre(),
                existencia, reservado, disponible, minimo, item == null ? null : item.getStockMaximo(), estado, item == null ? null : item.getActualizadoEn());
    }

    private MovimientoInventarioResponse mapearMovimiento(MovimientoInventarioEntidad item, Map<Long, String> productos, Map<Long, String> sucursales) {
        return new MovimientoInventarioResponse(item.getId(), item.getProductoId(), productos.getOrDefault(item.getProductoId(), "Producto"),
                item.getSucursalId(), sucursales.getOrDefault(item.getSucursalId(), "Sucursal"), item.getTipo(), item.getCantidad(),
                item.getExistenciaAnterior(), item.getExistenciaNueva(), item.getReferencia(), item.getNotas(), item.getCreadoEn());
    }

    private boolean sinScope(List<Long> permitidas) { return permitidas == null || permitidas.isEmpty(); }
    private String limpiar(String valor) { return StringUtils.hasText(valor) ? valor.trim() : null; }
    private ResponseStatusException error(String mensaje) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje); }
}
