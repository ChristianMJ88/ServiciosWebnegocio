package com.techprotech.agenda.modulos.caja.aplicacion;

import com.techprotech.agenda.modulos.caja.api.dto.CatalogoVentaResponse;
import com.techprotech.agenda.modulos.caja.api.dto.RegistrarVentaRequest;
import com.techprotech.agenda.modulos.caja.api.dto.VentaResponse;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.productos.aplicacion.ServicioInventarioProductos;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoEntidad;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoExistenciaEntidad;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.ProductoExistenciaRepositorio;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.ProductoRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.techprotech.agenda.modulos.caja.aplicacion.CatalogosCaja.*;

@Service
public class ServicioVentasCaja {
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private final VentaRepositorio ventas;
    private final VentaDetalleRepositorio detalles;
    private final PagoVentaRepositorio pagos;
    private final CajaSesionRepositorio sesiones;
    private final MovimientoCajaRepositorio movimientos;
    private final PagoCitaRepositorio pagosCita;
    private final CitaRepositorio citas;
    private final ClienteRepositorio clientes;
    private final ServicioRepositorio servicios;
    private final ProductoRepositorio productos;
    private final ProductoExistenciaRepositorio existencias;
    private final SucursalRepositorio sucursales;
    private final ServicioInventarioProductos inventario;

    public ServicioVentasCaja(VentaRepositorio ventas, VentaDetalleRepositorio detalles, PagoVentaRepositorio pagos,
                              CajaSesionRepositorio sesiones, MovimientoCajaRepositorio movimientos,
                              PagoCitaRepositorio pagosCita, CitaRepositorio citas, ClienteRepositorio clientes, ServicioRepositorio servicios,
                              ProductoRepositorio productos, ProductoExistenciaRepositorio existencias,
                              SucursalRepositorio sucursales, ServicioInventarioProductos inventario) {
        this.ventas = ventas; this.detalles = detalles; this.pagos = pagos; this.sesiones = sesiones;
        this.movimientos = movimientos; this.pagosCita = pagosCita; this.citas = citas; this.clientes = clientes; this.servicios = servicios;
        this.productos = productos; this.existencias = existencias; this.sucursales = sucursales; this.inventario = inventario;
    }

    @Transactional(readOnly = true)
    public CatalogoVentaResponse catalogo(Long empresaId, List<Long> sucursalesPermitidas, Long sucursalId) {
        validarSucursal(empresaId, sucursalesPermitidas, sucursalId);
        List<ProductoEntidad> activos = productos.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .filter(ProductoEntidad::isActivo).toList();
        List<ProductoExistenciaEntidad> stock = activos.isEmpty() ? List.of()
                : existencias.findByIdEmpresaIdAndIdProductoIdIn(empresaId, activos.stream().map(ProductoEntidad::getId).toList());
        Map<Long, ProductoExistenciaEntidad> porProducto = stock.stream()
                .filter(item -> sucursalId.equals(item.getId().getSucursalId()))
                .collect(Collectors.toMap(item -> item.getId().getProductoId(), Function.identity()));
        return new CatalogoVentaResponse(activos.stream().map(producto -> {
            ProductoExistenciaEntidad existencia = porProducto.get(producto.getId());
            BigDecimal disponible = producto.isControlaInventario()
                    ? existencia == null ? BigDecimal.ZERO : existencia.getExistencia().subtract(existencia.getReservado())
                    : null;
            return new CatalogoVentaResponse.ProductoCajaResponse(producto.getId(), producto.getCategoriaId(), producto.getSku(),
                    producto.getCodigoBarras(), producto.getNombre(), producto.getImagenUrl(), producto.getPrecioBase(),
                    producto.getMoneda(), producto.isControlaInventario(), disponible);
        }).toList());
    }

    @Transactional
    public VentaResponse registrar(Long empresaId, Long usuarioId, List<Long> sucursalesPermitidas, RegistrarVentaRequest request) {
        if (METODO_PAGO_POINT.equalsIgnoreCase(request.metodoPago())) {
            throw badRequest("El cobro Point debe iniciarse desde el flujo de terminal");
        }
        return registrarInterno(empresaId, usuarioId, sucursalesPermitidas, request, false);
    }

    @Transactional
    public VentaResponse prepararPoint(Long empresaId, Long usuarioId, List<Long> sucursalesPermitidas,
                                       RegistrarVentaRequest request) {
        RegistrarVentaRequest point = new RegistrarVentaRequest(request.sucursalId(), request.citaId(), request.clienteId(),
                request.productos(), METODO_PAGO_POINT, null, request.observaciones());
        return registrarInterno(empresaId, usuarioId, sucursalesPermitidas, point, true);
    }

    private VentaResponse registrarInterno(Long empresaId, Long usuarioId, List<Long> sucursalesPermitidas,
                                            RegistrarVentaRequest request, boolean point) {
        validarSucursal(empresaId, sucursalesPermitidas, request.sucursalId());
        String metodoPago = request.metodoPago().trim().toUpperCase(Locale.ROOT);
        if (!point && !contiene(METODOS_PAGO, metodoPago)) throw badRequest("El método de pago no es válido");
        CajaSesionEntidad sesion = sesiones.findByEmpresaIdAndSucursalIdAndEstado(empresaId, request.sucursalId(), ESTADO_SESION_ABIERTA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No hay una caja abierta para la sucursal indicada"));

        List<RegistrarVentaRequest.ProductoVentaRequest> solicitados = request.productos() == null ? List.of() : request.productos();
        Map<Long, BigDecimal> cantidades = solicitados.stream().collect(Collectors.toMap(
                RegistrarVentaRequest.ProductoVentaRequest::productoId,
                RegistrarVentaRequest.ProductoVentaRequest::cantidad,
                BigDecimal::add, LinkedHashMap::new));
        Map<Long, ProductoEntidad> productosVenta = productos.findAllById(cantidades.keySet()).stream()
                .filter(item -> empresaId.equals(item.getEmpresaId()) && item.isActivo())
                .collect(Collectors.toMap(ProductoEntidad::getId, Function.identity()));
        if (productosVenta.size() != cantidades.size()) throw badRequest("Uno o más productos no están disponibles para la empresa");

        CitaEntidad cita = null;
        ServicioEntidad servicio = null;
        BigDecimal pendienteServicio = CERO;
        if (request.citaId() != null) {
            cita = citas.findByIdAndEmpresaId(request.citaId(), empresaId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La cita no existe para la empresa"));
            if (!request.sucursalId().equals(cita.getSucursalId())) throw badRequest("La cita pertenece a otra sucursal");
            if (cita.getCheckInEn() == null && !"FINALIZADA".equalsIgnoreCase(cita.getEstado()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La cita aún no está lista para cobro");
            BigDecimal pagado = seguro(pagosCita.sumarPagadoPorCita(empresaId, cita.getId()))
                    .add(seguro(detalles.sumarServiciosPagadosPorCita(empresaId, cita.getId())));
            pendienteServicio = cita.getPrecio().subtract(pagado).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            if (pendienteServicio.signum() == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "La cita ya está pagada");
            servicio = servicios.findByIdAndEmpresaId(cita.getServicioId(), empresaId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El servicio de la cita ya no existe"));
        }
        if (cita == null && cantidades.isEmpty()) throw badRequest("Agrega una cita o al menos un producto a la venta");
        if (cita == null && request.clienteId() != null && clientes.findByEmpresaId(empresaId).stream().noneMatch(item -> request.clienteId().equals(item.getUsuarioId())))
            throw badRequest("El cliente no pertenece a la empresa");

        String moneda = cita != null ? cita.getMoneda() : productosVenta.values().stream().findFirst().map(ProductoEntidad::getMoneda).orElse("MXN");
        if (productosVenta.values().stream().anyMatch(item -> !moneda.equalsIgnoreCase(item.getMoneda())))
            throw badRequest("Todos los conceptos de una venta deben usar la misma moneda");
        BigDecimal subtotalProductos = cantidades.entrySet().stream()
                .map(item -> productosVenta.get(item.getKey()).getPrecioBase().multiply(item.getValue()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = pendienteServicio.add(subtotalProductos).setScale(2, RoundingMode.HALF_UP);

        VentaEntidad venta = new VentaEntidad();
        venta.setEmpresaId(empresaId); venta.setSucursalId(request.sucursalId()); venta.setCajaSesionId(sesion.getId());
        venta.setClienteId(cita != null ? cita.getClienteId() : request.clienteId()); venta.setCitaId(cita != null ? cita.getId() : null);
        venta.setFolio(generarFolio()); venta.setEstado(point ? "PENDIENTE_PAGO" : "PAGADA"); venta.setCanal("CAJA"); venta.setMoneda(moneda.toUpperCase(Locale.ROOT));
        venta.setSubtotal(total); venta.setDescuentoTotal(CERO); venta.setImpuestosTotal(CERO); venta.setTotal(total);
        venta.setObservaciones(limpiar(request.observaciones())); venta.setCreadaPorUsuarioId(usuarioId); venta.setCreadaEn(LocalDateTime.now());
        venta = ventas.save(venta);

        List<VentaDetalleEntidad> filas = new ArrayList<>();
        if (cita != null) filas.add(detalleServicio(empresaId, venta.getId(), cita, servicio, pendienteServicio));
        for (Map.Entry<Long, BigDecimal> item : cantidades.entrySet())
            filas.add(detalleProducto(empresaId, venta.getId(), productosVenta.get(item.getKey()), item.getValue()));
        filas = detalles.saveAll(filas);

        PagoVentaEntidad pago = null;
        if (!point) {
            pago = new PagoVentaEntidad();
            pago.setEmpresaId(empresaId); pago.setVentaId(venta.getId()); pago.setCajaSesionId(sesion.getId()); pago.setMonto(total);
            pago.setMetodoPago(metodoPago); pago.setReferencia(limpiar(request.referencia())); pago.setRegistradoPorUsuarioId(usuarioId);
            pago.setRegistradoEn(LocalDateTime.now()); pago = pagos.save(pago);
        }

        if (!cantidades.isEmpty()) inventario.descontarVenta(empresaId, usuarioId, sucursalesPermitidas,
                request.sucursalId(), cantidades, venta.getFolio());
        if (!point && METODO_PAGO_EFECTIVO.equals(metodoPago)) registrarIngresoEfectivo(empresaId, usuarioId, sesion.getId(), venta, pago);
        return mapear(venta, pago, filas);
    }

    @Transactional
    public VentaResponse confirmarPoint(Long empresaId, Long ventaId, String referenciaPago) {
        VentaEntidad venta = ventas.findByIdAndEmpresaId(ventaId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta Point no encontrada"));
        List<VentaDetalleEntidad> filas = detalles.findByEmpresaIdAndVentaIdOrderByIdAsc(empresaId, ventaId);
        PagoVentaEntidad existente = pagos.findByEmpresaIdAndVentaIdOrderByRegistradoEnDesc(empresaId, ventaId).stream()
                .filter(item -> METODO_PAGO_POINT.equals(item.getMetodoPago())).findFirst().orElse(null);
        if (existente != null) return mapear(venta, existente, filas);
        if (!"PENDIENTE_PAGO".equals(venta.getEstado())) throw new ResponseStatusException(HttpStatus.CONFLICT, "La venta Point ya no puede confirmarse");
        venta.setEstado("PAGADA");
        ventas.save(venta);
        PagoVentaEntidad pago = new PagoVentaEntidad();
        pago.setEmpresaId(empresaId); pago.setVentaId(ventaId); pago.setCajaSesionId(venta.getCajaSesionId()); pago.setMonto(venta.getTotal());
        pago.setMetodoPago(METODO_PAGO_POINT); pago.setReferencia(limpiar(referenciaPago));
        pago.setRegistradoPorUsuarioId(venta.getCreadaPorUsuarioId()); pago.setRegistradoEn(LocalDateTime.now());
        return mapear(venta, pagos.save(pago), filas);
    }

    @Transactional
    public void cancelarPoint(Long empresaId, Long ventaId, String motivo) {
        VentaEntidad venta = ventas.findByIdAndEmpresaId(ventaId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta Point no encontrada"));
        if ("CANCELADA".equals(venta.getEstado())) return;
        if (!"PENDIENTE_PAGO".equals(venta.getEstado())) return;
        Map<Long, BigDecimal> cantidades = detalles.findByEmpresaIdAndVentaIdOrderByIdAsc(empresaId, ventaId).stream()
                .filter(item -> "PRODUCTO".equals(item.getTipoConcepto()) && item.getProductoId() != null)
                .collect(Collectors.toMap(VentaDetalleEntidad::getProductoId, VentaDetalleEntidad::getCantidad, BigDecimal::add));
        if (!cantidades.isEmpty()) inventario.reintegrarVenta(empresaId, venta.getCreadaPorUsuarioId(), venta.getSucursalId(),
                cantidades, venta.getFolio(), motivo);
        venta.setEstado("CANCELADA");
        ventas.save(venta);
    }

    @Transactional
    public void marcarReembolsadaPoint(Long empresaId, Long ventaId) {
        VentaEntidad venta = ventas.findByIdAndEmpresaId(ventaId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta Point no encontrada"));
        if ("REEMBOLSADA".equals(venta.getEstado())) return;
        Map<Long, BigDecimal> cantidades = detalles.findByEmpresaIdAndVentaIdOrderByIdAsc(empresaId, ventaId).stream()
                .filter(item -> "PRODUCTO".equals(item.getTipoConcepto()) && item.getProductoId() != null)
                .collect(Collectors.toMap(VentaDetalleEntidad::getProductoId, VentaDetalleEntidad::getCantidad, BigDecimal::add));
        if (!cantidades.isEmpty()) inventario.reintegrarVenta(empresaId, venta.getCreadaPorUsuarioId(), venta.getSucursalId(),
                cantidades, venta.getFolio(), "Reembolso de cobro Point");
        venta.setEstado("REEMBOLSADA");
        ventas.save(venta);
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listar(Long empresaId, List<Long> sucursalesPermitidas, Long sucursalId) {
        validarSucursal(empresaId, sucursalesPermitidas, sucursalId);
        return ventas.findTop50ByEmpresaIdAndSucursalIdOrderByCreadaEnDesc(empresaId, sucursalId).stream().map(venta -> {
            PagoVentaEntidad pago = pagos.findByEmpresaIdAndVentaIdOrderByRegistradoEnDesc(empresaId, venta.getId()).stream().findFirst().orElse(null);
            return mapear(venta, pago, detalles.findByEmpresaIdAndVentaIdOrderByIdAsc(empresaId, venta.getId()));
        }).toList();
    }

    private VentaDetalleEntidad detalleServicio(Long empresaId, Long ventaId, CitaEntidad cita, ServicioEntidad servicio, BigDecimal total) {
        VentaDetalleEntidad d = baseDetalle(empresaId, ventaId, "SERVICIO", servicio.getNombre(), BigDecimal.ONE, total, total);
        d.setServicioId(servicio.getId()); d.setCitaId(cita.getId()); return d;
    }
    private VentaDetalleEntidad detalleProducto(Long empresaId, Long ventaId, ProductoEntidad producto, BigDecimal cantidad) {
        BigDecimal total = producto.getPrecioBase().multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
        VentaDetalleEntidad d = baseDetalle(empresaId, ventaId, "PRODUCTO", producto.getNombre(), cantidad, producto.getPrecioBase(), total);
        d.setProductoId(producto.getId()); d.setSku(producto.getSku()); return d;
    }
    private VentaDetalleEntidad baseDetalle(Long empresaId, Long ventaId, String tipo, String nombre, BigDecimal cantidad, BigDecimal precio, BigDecimal total) {
        VentaDetalleEntidad d = new VentaDetalleEntidad(); d.setEmpresaId(empresaId); d.setVentaId(ventaId); d.setTipoConcepto(tipo);
        d.setNombre(nombre); d.setCantidad(cantidad); d.setPrecioUnitario(precio); d.setDescuento(CERO); d.setImpuestos(CERO); d.setTotal(total); return d;
    }
    private void registrarIngresoEfectivo(Long empresaId, Long usuarioId, Long sesionId, VentaEntidad venta, PagoVentaEntidad pago) {
        MovimientoCajaEntidad m = new MovimientoCajaEntidad(); m.setEmpresaId(empresaId); m.setCajaSesionId(sesionId); m.setCitaId(venta.getCitaId());
        m.setTipoMovimiento("INGRESO_VENTA"); m.setMetodoPago(METODO_PAGO_EFECTIVO); m.setMonto(pago.getMonto());
        m.setConcepto("Venta " + venta.getFolio()); m.setReferencia(pago.getReferencia()); m.setObservaciones(venta.getObservaciones());
        m.setRegistradoPorUsuarioId(usuarioId); m.setRegistradoEn(LocalDateTime.now()); movimientos.save(m);
    }
    private VentaResponse mapear(VentaEntidad venta, PagoVentaEntidad pago, List<VentaDetalleEntidad> filas) {
        return new VentaResponse(venta.getId(), venta.getFolio(), venta.getSucursalId(), venta.getCitaId(), venta.getClienteId(), venta.getEstado(),
                venta.getMoneda(), venta.getSubtotal(), venta.getDescuentoTotal(), venta.getImpuestosTotal(), venta.getTotal(),
                pago == null ? null : pago.getMetodoPago(), pago == null ? null : pago.getReferencia(), venta.getCreadaEn(), venta.getCanal(),
                filas.stream().map(d -> new VentaResponse.DetalleVentaResponse(d.getId(), d.getTipoConcepto(), d.getProductoId(), d.getServicioId(),
                        d.getCitaId(), d.getSku(), d.getNombre(), d.getCantidad(), d.getPrecioUnitario(), d.getDescuento(), d.getImpuestos(), d.getTotal())).toList());
    }
    private void validarSucursal(Long empresaId, List<Long> permitidas, Long sucursalId) {
        if (sucursalId == null) throw badRequest("Selecciona una sucursal");
        if (permitidas != null && !permitidas.isEmpty() && !permitidas.contains(sucursalId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a la sucursal indicada");
        sucursales.findByIdAndEmpresaIdAndActivaTrue(sucursalId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sucursal no encontrada"));
    }
    private String generarFolio() { return "V-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss")) + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT); }
    private String limpiar(String valor) { return StringUtils.hasText(valor) ? valor.trim() : null; }
    private BigDecimal seguro(BigDecimal valor) { return valor == null ? BigDecimal.ZERO : valor; }
    private ResponseStatusException badRequest(String mensaje) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje); }
}
