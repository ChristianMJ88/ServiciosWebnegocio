package com.techprotech.agenda.modulos.caja.aplicacion;

import com.techprotech.agenda.modulos.citas.aplicacion.PublicadorPagosCitas;
import com.techprotech.agenda.modulos.citas.dominio.eventos.OrigenEventoCita;
import static com.techprotech.agenda.compartido.tiempo.MomentoUtc.de;

import java.time.Clock;
import com.techprotech.agenda.compartido.tiempo.ZonaNegocio;
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
import java.util.Set;

import static com.techprotech.agenda.modulos.caja.aplicacion.CatalogosCaja.*;

@Service
public class ServicioVentasCaja {
    private final PublicadorPagosCitas eventosPago;
    private final Clock reloj;
    private final ZonaNegocio zonasNegocio;
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
    private final com.techprotech.agenda.modulos.productos.infraestructura.repositorio.ReglaDescuentoProductoRepositorio descuentos;
    private final com.techprotech.agenda.modulos.productos.infraestructura.repositorio.ZonaPrecioProductoRepositorio zonas;
    private final com.techprotech.agenda.modulos.productos.infraestructura.repositorio.PrecioZonaProductoRepositorio preciosZona;
    private final com.techprotech.agenda.modulos.productos.aplicacion.ServicioComisionesDevengadas comisiones;

    public ServicioVentasCaja(VentaRepositorio ventas, VentaDetalleRepositorio detalles, PagoVentaRepositorio pagos,
                              CajaSesionRepositorio sesiones, MovimientoCajaRepositorio movimientos,
                              PagoCitaRepositorio pagosCita, CitaRepositorio citas, ClienteRepositorio clientes, ServicioRepositorio servicios,
                              ProductoRepositorio productos, ProductoExistenciaRepositorio existencias,
                              SucursalRepositorio sucursales, ServicioInventarioProductos inventario,
                              com.techprotech.agenda.modulos.productos.infraestructura.repositorio.ReglaDescuentoProductoRepositorio descuentos,
                              com.techprotech.agenda.modulos.productos.infraestructura.repositorio.ZonaPrecioProductoRepositorio zonas,
                              com.techprotech.agenda.modulos.productos.infraestructura.repositorio.PrecioZonaProductoRepositorio preciosZona,
                              com.techprotech.agenda.modulos.productos.aplicacion.ServicioComisionesDevengadas comisiones, Clock reloj, ZonaNegocio zonasNegocio, PublicadorPagosCitas eventosPago) {
        this.eventosPago = eventosPago;
        this.reloj = reloj;
        this.zonasNegocio = zonasNegocio;
        this.ventas = ventas; this.detalles = detalles; this.pagos = pagos; this.sesiones = sesiones;
        this.movimientos = movimientos; this.pagosCita = pagosCita; this.citas = citas; this.clientes = clientes; this.servicios = servicios;
        this.productos = productos; this.existencias = existencias; this.sucursales = sucursales; this.inventario = inventario;
        this.descuentos = descuentos;
        this.zonas = zonas; this.preciosZona = preciosZona;
        this.comisiones = comisiones;
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
        var reglas = reglasVigentes(empresaId, sucursalId);
        var contexto = contextoPrecios(empresaId, sucursalId, activos.stream().map(ProductoEntidad::getId).toList());
        return new CatalogoVentaResponse(activos.stream().map(producto -> {
            ProductoExistenciaEntidad existencia = porProducto.get(producto.getId());
            BigDecimal disponible = producto.isControlaInventario()
                    ? existencia == null ? BigDecimal.ZERO : existencia.getExistencia().subtract(existencia.getReservado())
                    : null;
            return new CatalogoVentaResponse.ProductoCajaResponse(producto.getId(), producto.getCategoriaId(), producto.getSku(),
                    producto.getCodigoBarras(), producto.getNombre(), producto.getImagenUrl(), precioProducto(producto, sucursalId, BigDecimal.ONE, reglas, contexto),
                    producto.getMoneda(), producto.isControlaInventario(), disponible);
        }).toList());
    }

    @Transactional(readOnly = true)
    public com.techprotech.agenda.modulos.caja.api.dto.CotizacionCajaResponse cotizarProductos(Long empresaId, List<Long> permitidas,
            com.techprotech.agenda.modulos.caja.api.dto.CotizacionCajaRequest request) {
        validarSucursal(empresaId, permitidas, request.sucursalId());
        var cantidades = request.productos().stream().collect(Collectors.toMap(RegistrarVentaRequest.ProductoVentaRequest::productoId,
                RegistrarVentaRequest.ProductoVentaRequest::cantidad, BigDecimal::add));
        var lista = productos.findByEmpresaIdAndIdIn(empresaId, cantidades.keySet()).stream().filter(ProductoEntidad::isActivo).toList();
        if (lista.size() != cantidades.size()) throw badRequest("Uno o más productos no están disponibles para la empresa");
        var reglas = reglasVigentes(empresaId, request.sucursalId());
        var contexto = contextoPrecios(empresaId, request.sucursalId(), cantidades.keySet());
        BigDecimal subtotal = CERO, total = CERO;
        var lineas = new ArrayList<com.techprotech.agenda.modulos.caja.api.dto.CotizacionCajaResponse.Linea>();
        for (var producto : lista) {
            var cantidad = cantidades.get(producto.getId());
            subtotal = subtotal.add(precioLista(producto, contexto).multiply(cantidad).setScale(2, RoundingMode.HALF_UP));
            var precio = precioProducto(producto, request.sucursalId(), cantidad, reglas, contexto);
            var totalLinea = precio.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
            total = total.add(totalLinea);
            lineas.add(new com.techprotech.agenda.modulos.caja.api.dto.CotizacionCajaResponse.Linea(producto.getId(), precio, totalLinea));
        }
        return new com.techprotech.agenda.modulos.caja.api.dto.CotizacionCajaResponse(subtotal, subtotal.subtract(total), total, lineas);
    }

    @Transactional
    public VentaResponse registrar(Long empresaId, Long usuarioId, List<Long> sucursalesPermitidas, RegistrarVentaRequest request) {
        if(request.ventaPendienteId()!=null) throw badRequest("Las cuentas de Staff se cobran desde su acción de cobro");
        if (METODO_PAGO_POINT.equalsIgnoreCase(request.metodoPago())) {
            throw badRequest("El cobro Point debe iniciarse desde el flujo de terminal");
        }
        return registrarInterno(empresaId, usuarioId, sucursalesPermitidas, request, false);
    }

    @Transactional
    public VentaResponse prepararPoint(Long empresaId, Long usuarioId, List<Long> sucursalesPermitidas,
                                       RegistrarVentaRequest request) {
        if(request.ventaPendienteId()!=null) return prepararPendientePoint(empresaId,usuarioId,sucursalesPermitidas,request.ventaPendienteId(),request.sucursalId());
        RegistrarVentaRequest point = new RegistrarVentaRequest(request.sucursalId(), request.citaId(), request.clienteId(),
                request.productos(), METODO_PAGO_POINT, null, request.observaciones());
        return registrarInterno(empresaId, usuarioId, sucursalesPermitidas, point, true);
    }

    @Transactional
    public VentaResponse registrarStaff(Long empresaId,Long usuarioId,Long sucursalId,Long clienteId,List<RegistrarVentaRequest.ProductoVentaRequest> productos) {
        return registrarInterno(empresaId,usuarioId,List.of(sucursalId),new RegistrarVentaRequest(sucursalId,null,clienteId,productos,"",null,null),false,true);
    }

    private VentaResponse registrarInterno(Long empresaId, Long usuarioId, List<Long> sucursalesPermitidas,
                                            RegistrarVentaRequest request, boolean point) {
        return registrarInterno(empresaId,usuarioId,sucursalesPermitidas,request,point,false);
    }
    private VentaResponse registrarInterno(Long empresaId, Long usuarioId, List<Long> sucursalesPermitidas,
                                            RegistrarVentaRequest request, boolean point, boolean staff) {
        validarSucursal(empresaId, sucursalesPermitidas, request.sucursalId());
        String metodoPago = request.metodoPago().trim().toUpperCase(Locale.ROOT);
        if (!staff && !point && !contiene(METODOS_PAGO, metodoPago)) throw badRequest("El método de pago no es válido");
        CajaSesionEntidad sesion = staff ? null : sesiones.findByEmpresaIdAndSucursalIdAndEstado(empresaId, request.sucursalId(), ESTADO_SESION_ABIERTA)
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
        BigDecimal pagadoServicio = CERO;
        if (request.citaId() != null) {
            cita = citas.findByIdAndEmpresaId(request.citaId(), empresaId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La cita no existe para la empresa"));
            if (!request.sucursalId().equals(cita.getSucursalId())) throw badRequest("La cita pertenece a otra sucursal");
            if (cita.getCheckInEn() == null && !"FINALIZADA".equalsIgnoreCase(cita.getEstado()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La cita aún no está lista para cobro");
            BigDecimal pagado = seguro(pagosCita.sumarPagadoPorCita(empresaId, cita.getId()))
                    .add(seguro(detalles.sumarServiciosPagadosPorCita(empresaId, cita.getId())));
            pagadoServicio = pagado;
            pendienteServicio = cita.getPrecio().subtract(pagado).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            if (pendienteServicio.signum() == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "La cita ya está pagada");
            servicio = servicios.findByIdAndEmpresaId(cita.getServicioId(), empresaId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El servicio de la cita ya no existe"));
        }
        if (cita == null && cantidades.isEmpty()) throw badRequest("Agrega una cita o al menos un producto a la venta");
        if (cita == null && request.clienteId() != null && clientes.findByClienteIdAndEmpresaId(request.clienteId(),empresaId).isEmpty())
            throw badRequest("El cliente no pertenece a la empresa");

        String moneda = cita != null ? cita.getMoneda() : productosVenta.values().stream().findFirst().map(ProductoEntidad::getMoneda).orElse("MXN");
        if (productosVenta.values().stream().anyMatch(item -> !moneda.equalsIgnoreCase(item.getMoneda())))
            throw badRequest("Todos los conceptos de una venta deben usar la misma moneda");
        var reglas = reglasVigentes(empresaId, request.sucursalId());
        var contexto = contextoPrecios(empresaId, request.sucursalId(), cantidades.keySet());
        Map<Long, BigDecimal> preciosAplicados = cantidades.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey, item -> precioProducto(productosVenta.get(item.getKey()), request.sucursalId(), item.getValue(), reglas, contexto)));
        BigDecimal subtotalProductos = cantidades.entrySet().stream()
                .map(item -> precioLista(productosVenta.get(item.getKey()), contexto).multiply(item.getValue()).setScale(2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netoProductos = cantidades.entrySet().stream().map(item -> preciosAplicados.get(item.getKey()).multiply(item.getValue()).setScale(2, RoundingMode.HALF_UP)).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal descuentoTotal = subtotalProductos.setScale(2, RoundingMode.HALF_UP).subtract(netoProductos);
        if (cita != null) descuentoTotal = descuentoTotal.add(com.techprotech.agenda.modulos.citas.aplicacion.DesglosePrecioCita.paraCobro(cita,pendienteServicio,pagadoServicio).descuento());
        BigDecimal total = pendienteServicio.add(netoProductos).setScale(2, RoundingMode.HALF_UP);

        VentaEntidad venta = new VentaEntidad();
        venta.setEmpresaId(empresaId); venta.setSucursalId(request.sucursalId()); venta.setCajaSesionId(sesion == null ? null : sesion.getId());
        venta.setClienteId(cita != null ? cita.getClienteId() : request.clienteId()); venta.setCitaId(cita != null ? cita.getId() : null);
        venta.setFolio(generarFolio(empresaId, request.sucursalId())); venta.setEstado(staff ? "POR_COBRAR" : point ? "PENDIENTE_PAGO" : "PAGADA"); venta.setCanal(staff ? "STAFF" : "CAJA"); venta.setMoneda(moneda.toUpperCase(Locale.ROOT));
        venta.setSubtotal(total.add(descuentoTotal)); venta.setDescuentoTotal(descuentoTotal); venta.setImpuestosTotal(CERO); venta.setTotal(total);
        venta.setObservaciones(limpiar(request.observaciones())); venta.setCreadaPorUsuarioId(usuarioId); venta.setCreadaEn(LocalDateTime.now(reloj));
        venta = ventas.save(venta);

        List<VentaDetalleEntidad> filas = new ArrayList<>();
        if (cita != null) filas.add(detalleServicio(empresaId, venta.getId(), cita, servicio, pendienteServicio, pagadoServicio));
        for (Map.Entry<Long, BigDecimal> item : cantidades.entrySet())
            filas.add(detalleProducto(empresaId, venta.getId(), productosVenta.get(item.getKey()), item.getValue(), preciosAplicados.get(item.getKey()), precioLista(productosVenta.get(item.getKey()), contexto)));
        if (staff) filas.forEach(fila -> fila.setPrestadorResponsableId(usuarioId));
        filas = detalles.saveAll(filas);

        PagoVentaEntidad pago = null;
        if (!staff && !point) {
            pago = new PagoVentaEntidad();
            pago.setEmpresaId(empresaId); pago.setVentaId(venta.getId()); pago.setCajaSesionId(sesion.getId()); pago.setMonto(total);
            pago.setMetodoPago(metodoPago); pago.setReferencia(limpiar(request.referencia())); pago.setRegistradoPorUsuarioId(usuarioId);
            pago.setRegistradoEn(LocalDateTime.now(reloj)); pago = pagos.save(pago);
        }

        if (!staff && !cantidades.isEmpty()) inventario.descontarVenta(empresaId, usuarioId, sucursalesPermitidas,
                request.sucursalId(), cantidades, venta.getFolio());
        if (!staff && !point && METODO_PAGO_EFECTIVO.equals(metodoPago)) registrarIngresoEfectivo(empresaId, usuarioId, sesion.getId(), venta, pago);
        if (!staff && !point) comisiones.registrar(venta, filas);
        if (!staff && !point && cita != null)
            eventosPago.pagoEnVenta(cita, venta, pago, pendienteServicio, OrigenEventoCita.ADMIN, usuarioId);
        return mapear(venta, pago, filas);
    }

    @Transactional
    public VentaResponse confirmarPoint(Long empresaId, Long ventaId, String referenciaPago) {
        VentaEntidad venta = ventas.findConBloqueoByIdAndEmpresaId(ventaId, empresaId)
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
        pago.setRegistradoPorUsuarioId(venta.getCreadaPorUsuarioId()); pago.setRegistradoEn(LocalDateTime.now(reloj));
        comisiones.registrar(venta, filas);
        pago = pagos.save(pago);
        eventosPago.venta(venta, pago, filas, OrigenEventoCita.WEBHOOK, null);
        return mapear(venta, pago, filas);
    }

    @Transactional
    public void cancelarPoint(Long empresaId, Long ventaId, String motivo) {
        VentaEntidad venta = ventas.findConBloqueoByIdAndEmpresaId(ventaId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta Point no encontrada"));
        if ("CANCELADA".equals(venta.getEstado())) return;
        if (!"PENDIENTE_PAGO".equals(venta.getEstado())) return;
        Map<Long, BigDecimal> cantidades = detalles.findByEmpresaIdAndVentaIdOrderByIdAsc(empresaId, ventaId).stream()
                .filter(item -> "PRODUCTO".equals(item.getTipoConcepto()) && item.getProductoId() != null)
                .collect(Collectors.toMap(VentaDetalleEntidad::getProductoId, VentaDetalleEntidad::getCantidad, BigDecimal::add));
        if (!cantidades.isEmpty()) inventario.reintegrarVenta(empresaId, venta.getCreadaPorUsuarioId(), venta.getSucursalId(),
                cantidades, venta.getFolio(), motivo);
        venta.setEstado("STAFF".equals(venta.getCanal()) ? "POR_COBRAR" : "CANCELADA");
        ventas.save(venta);
    }

    @Transactional
    public void marcarReembolsadaPoint(Long empresaId, Long ventaId) {
        VentaEntidad venta = ventas.findConBloqueoByIdAndEmpresaId(ventaId, empresaId)
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

    /**
     * Lista las últimas 50 ventas con batch-loading.
     * Antes: 1 + N*2 queries (101 para 50 ventas). Ahora: 3 queries fijas.
     */
    @Transactional(readOnly = true)
    public List<VentaResponse> listar(Long empresaId, List<Long> sucursalesPermitidas, Long sucursalId) {
        validarSucursal(empresaId, sucursalesPermitidas, sucursalId);
        List<VentaEntidad> lista = ventas.findTop50ByEmpresaIdAndSucursalIdOrderByCreadaEnDesc(empresaId, sucursalId);
        if (lista.isEmpty()) return List.of();

        Set<Long> ventaIds = lista.stream().map(VentaEntidad::getId).collect(Collectors.toSet());

        // Query batch: seleccionar el pago más reciente con desempate por ID.
        Map<Long, PagoVentaEntidad> pagoPorVenta = pagos.findByEmpresaIdAndVentaIdIn(empresaId, ventaIds)
                .stream().collect(Collectors.toMap(PagoVentaEntidad::getVentaId, Function.identity(), (a, b) -> Comparator.comparing(PagoVentaEntidad::getRegistradoEn)
                        .thenComparing(PagoVentaEntidad::getId).compare(a,b) >= 0 ? a : b));

        // Query batch: todos los detalles de las ventas
        Map<Long, List<VentaDetalleEntidad>> detallesPorVenta = detalles.findByEmpresaIdAndVentaIdIn(empresaId, ventaIds)
                .stream().collect(Collectors.groupingBy(VentaDetalleEntidad::getVentaId));

        return lista.stream()
                .map(venta -> mapear(
                        venta,
                        pagoPorVenta.get(venta.getId()),
                        detallesPorVenta.getOrDefault(venta.getId(), List.of())
                ))
                .toList();
    }

    public record PendienteStaff(VentaResponse venta, String clienteNombre, String clienteTelefono) {}

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<PendienteStaff> pendientesStaff(Long empresaId, List<Long> permitidas, Long sucursalId, int pagina) {
        validarSucursal(empresaId, permitidas, sucursalId);
        if (pagina < 0) throw badRequest("La página no es válida");
        var pagina20 = org.springframework.data.domain.PageRequest.of(pagina, 20);
        var lista = ventas.findByEmpresaIdAndSucursalIdAndEstadoAndCanalOrderByCreadaEnAsc(empresaId, sucursalId, "POR_COBRAR", "STAFF", pagina20);

        if (lista.isEmpty()) return lista.map(v -> new PendienteStaff(null, "", ""));

        // Batch-load detalles y clientes para la página actual
        Set<Long> ventaIds = lista.stream().map(VentaEntidad::getId).collect(Collectors.toSet());
        Map<Long, List<VentaDetalleEntidad>> detallesPorVenta = detalles.findByEmpresaIdAndVentaIdIn(empresaId, ventaIds)
                .stream().collect(Collectors.groupingBy(VentaDetalleEntidad::getVentaId));

        Set<Long> clienteIds = lista.stream().filter(v -> v.getClienteId() != null)
                .map(VentaEntidad::getClienteId).collect(Collectors.toSet());
        Map<Long, com.techprotech.agenda.modulos.clientes.infraestructura.entidad.ClienteEntidad> clientePorId =
                clienteIds.isEmpty() ? Map.of() : clientes.findByEmpresaIdAndUsuarioIdIn(empresaId, clienteIds).stream()
                        .collect(Collectors.toMap(
                                com.techprotech.agenda.modulos.clientes.infraestructura.entidad.ClienteEntidad::getUsuarioId,
                                Function.identity()
                        ));

        return lista.map(v -> {
            var cliente = v.getClienteId() != null ? clientePorId.get(v.getClienteId()) : null;
            return new PendienteStaff(
                    mapear(v, null, detallesPorVenta.getOrDefault(v.getId(), List.of())),
                    cliente == null ? "" : cliente.getNombreCompleto(),
                    cliente == null ? "" : cliente.getTelefono()
            );
        });
    }

    private VentaResponse prepararPendientePoint(Long empresaId,Long usuarioId,List<Long> permitidas,Long ventaId,Long sucursalId) {
        var venta=ventas.findConBloqueoByIdAndEmpresaId(ventaId,empresaId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"La venta no existe"));
        validarSucursal(empresaId,permitidas,venta.getSucursalId());
        if(!venta.getSucursalId().equals(sucursalId) || !"POR_COBRAR".equals(venta.getEstado()) || !"STAFF".equals(venta.getCanal()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"La cuenta no está disponible para esta terminal");
        var sesion=sesiones.findByEmpresaIdAndSucursalIdAndEstado(empresaId,sucursalId,ESTADO_SESION_ABIERTA)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Abre la caja de la sucursal para cobrar"));
        var filas=detalles.findByEmpresaIdAndVentaIdOrderByIdAsc(empresaId,ventaId);
        var cantidades=filas.stream().filter(f->"PRODUCTO".equals(f.getTipoConcepto())).collect(Collectors.toMap(VentaDetalleEntidad::getProductoId,VentaDetalleEntidad::getCantidad,BigDecimal::add));
        inventario.descontarVenta(empresaId,usuarioId,permitidas,sucursalId,cantidades,venta.getFolio());
        venta.setCajaSesionId(sesion.getId());venta.setEstado("PENDIENTE_PAGO");ventas.save(venta);
        return mapear(venta,null,filas);
    }

    @Transactional
    public VentaResponse cobrarPendiente(Long empresaId,Long usuarioId,List<Long> permitidas,Long ventaId,String metodo,String referencia) {
        VentaEntidad venta=ventas.findConBloqueoByIdAndEmpresaId(ventaId,empresaId)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"La venta no existe"));
        validarSucursal(empresaId,permitidas,venta.getSucursalId());
        if(!"POR_COBRAR".equals(venta.getEstado()) || !"STAFF".equals(venta.getCanal()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"La venta ya no está pendiente de cobro");
        String codigo=metodo==null?"":metodo.trim().toUpperCase(Locale.ROOT);
        if(!contiene(METODOS_PAGO,codigo) || METODO_PAGO_POINT.equals(codigo)) throw badRequest("Selecciona un método de cobro directo válido");
        var sesion=sesiones.findByEmpresaIdAndSucursalIdAndEstado(empresaId,venta.getSucursalId(),ESTADO_SESION_ABIERTA)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Abre la caja de la sucursal para cobrar"));
        var filas=detalles.findByEmpresaIdAndVentaIdOrderByIdAsc(empresaId,ventaId);
        Map<Long,BigDecimal> cantidades=filas.stream().filter(f->"PRODUCTO".equals(f.getTipoConcepto()))
            .collect(Collectors.toMap(VentaDetalleEntidad::getProductoId,VentaDetalleEntidad::getCantidad,BigDecimal::add));
        inventario.descontarVenta(empresaId,usuarioId,permitidas,venta.getSucursalId(),cantidades,venta.getFolio());
        venta.setCajaSesionId(sesion.getId()); venta.setEstado("PAGADA"); ventas.save(venta);
        var pago=new PagoVentaEntidad(); pago.setEmpresaId(empresaId);pago.setVentaId(ventaId);pago.setCajaSesionId(sesion.getId());
        pago.setMonto(venta.getTotal());pago.setMetodoPago(codigo);pago.setReferencia(limpiar(referencia));
        pago.setRegistradoPorUsuarioId(usuarioId);pago.setRegistradoEn(LocalDateTime.now(reloj));pago=pagos.save(pago);
        if(METODO_PAGO_EFECTIVO.equals(codigo)) registrarIngresoEfectivo(empresaId,usuarioId,sesion.getId(),venta,pago);
        comisiones.registrar(venta,filas);
        eventosPago.venta(venta, pago, filas, OrigenEventoCita.ADMIN, usuarioId);
        return mapear(venta,pago,filas);
    }

    private VentaDetalleEntidad detalleServicio(Long empresaId, Long ventaId, CitaEntidad cita, ServicioEntidad servicio, BigDecimal total, BigDecimal pagado) {
        var desglose = com.techprotech.agenda.modulos.citas.aplicacion.DesglosePrecioCita.paraCobro(cita,total,pagado);
        VentaDetalleEntidad d = baseDetalle(empresaId, ventaId, "SERVICIO", servicio.getNombre(), BigDecimal.ONE, desglose.precioLista(), total);
        d.setDescuento(desglose.descuento());
        d.setServicioId(servicio.getId()); d.setCitaId(cita.getId()); return d;
    }
    private VentaDetalleEntidad detalleProducto(Long empresaId, Long ventaId, ProductoEntidad producto, BigDecimal cantidad, BigDecimal precioAplicado, BigDecimal precioLista) {
        BigDecimal total = precioAplicado.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
        VentaDetalleEntidad d = baseDetalle(empresaId, ventaId, "PRODUCTO", producto.getNombre(), cantidad, precioLista, total);
        d.setProductoId(producto.getId()); d.setSku(producto.getSku());
        d.setDescuento(precioLista.multiply(cantidad).setScale(2, RoundingMode.HALF_UP).subtract(total));
        d.setCostoUnitarioHistorico(producto.getCosto()); return d;
    }
    private List<com.techprotech.agenda.modulos.productos.infraestructura.entidad.ReglaDescuentoProductoEntidad> reglasVigentes(Long empresaId, Long sucursalId) {
        var hoy = java.time.LocalDate.now(reloj.withZone(zonasNegocio.de(empresaId, sucursalId)));
        return descuentos.findByEmpresaIdOrderByPrioridadDescNombreAsc(empresaId).stream().filter(regla -> com.techprotech.agenda.modulos.productos.aplicacion.CalculadorDescuentosProductos.vigente(regla, hoy)).toList();
    }
    private BigDecimal precioProducto(ProductoEntidad producto, Long sucursalId, BigDecimal cantidad,
            List<com.techprotech.agenda.modulos.productos.infraestructura.entidad.ReglaDescuentoProductoEntidad> reglas, ContextoPrecio contexto) {
        return com.techprotech.agenda.modulos.productos.aplicacion.CalculadorDescuentosProductos.calcular(producto, contexto.zona(), sucursalId, cantidad, precioLista(producto, contexto), producto.getMoneda(), reglas).precio();
    }
    private record ContextoPrecio(com.techprotech.agenda.modulos.productos.infraestructura.entidad.ZonaPrecioProductoEntidad zona,
            Map<Long, com.techprotech.agenda.modulos.productos.infraestructura.entidad.PrecioZonaProductoEntidad> precios) {}
    private ContextoPrecio contextoPrecios(Long empresaId, Long sucursalId, Collection<Long> ids) {
        var sede = sucursales.findByIdAndEmpresaIdAndActivaTrue(sucursalId, empresaId).orElseThrow(() -> badRequest("Sucursal no disponible"));
        if (sede.getZonaPrecioId() == null) return new ContextoPrecio(null, Map.of());
        var zona = zonas.findByIdAndEmpresaId(sede.getZonaPrecioId(), empresaId).filter(z -> z.isActiva()).orElseThrow(() -> badRequest("La zona de precio de la sucursal está inactiva o no pertenece al negocio"));
        var precios = ids.isEmpty() ? Map.<Long, com.techprotech.agenda.modulos.productos.infraestructura.entidad.PrecioZonaProductoEntidad>of() : preciosZona.findByEmpresaIdAndProductoIdIn(empresaId, ids).stream().filter(p -> p.isActivo() && zona.getId().equals(p.getZonaPrecioId())).collect(Collectors.toMap(p -> p.getProductoId(), Function.identity()));
        return new ContextoPrecio(zona, precios);
    }
    private BigDecimal precioLista(ProductoEntidad producto, ContextoPrecio contexto) {
        var precio = contexto.precios().get(producto.getId());
        if (precio == null) return producto.getPrecioBase();
        if (!producto.getMoneda().equalsIgnoreCase(precio.getMoneda())) throw badRequest("La moneda del precio regional debe coincidir con la del producto para cobrar en Caja");
        return precio.getPrecio();
    }
    private VentaDetalleEntidad baseDetalle(Long empresaId, Long ventaId, String tipo, String nombre, BigDecimal cantidad, BigDecimal precio, BigDecimal total) {
        VentaDetalleEntidad d = new VentaDetalleEntidad(); d.setEmpresaId(empresaId); d.setVentaId(ventaId); d.setTipoConcepto(tipo);
        d.setNombre(nombre); d.setCantidad(cantidad); d.setPrecioUnitario(precio); d.setDescuento(CERO); d.setImpuestos(CERO); d.setTotal(total); return d;
    }
    private void registrarIngresoEfectivo(Long empresaId, Long usuarioId, Long sesionId, VentaEntidad venta, PagoVentaEntidad pago) {
        MovimientoCajaEntidad m = new MovimientoCajaEntidad(); m.setEmpresaId(empresaId); m.setCajaSesionId(sesionId); m.setCitaId(venta.getCitaId());
        m.setTipoMovimiento("INGRESO_VENTA"); m.setMetodoPago(METODO_PAGO_EFECTIVO); m.setMonto(pago.getMonto());
        m.setConcepto("Venta " + venta.getFolio()); m.setReferencia(pago.getReferencia()); m.setObservaciones(venta.getObservaciones());
        m.setRegistradoPorUsuarioId(usuarioId); m.setRegistradoEn(LocalDateTime.now(reloj)); movimientos.save(m);
    }
    private VentaResponse mapear(VentaEntidad venta, PagoVentaEntidad pago, List<VentaDetalleEntidad> filas) {
        return new VentaResponse(venta.getId(), venta.getFolio(), venta.getSucursalId(), venta.getCitaId(), venta.getClienteId(), venta.getEstado(),
                venta.getMoneda(), venta.getSubtotal(), venta.getDescuentoTotal(), venta.getImpuestosTotal(), venta.getTotal(),
                pago == null ? null : pago.getMetodoPago(), pago == null ? null : pago.getReferencia(), de(venta.getCreadaEn()), venta.getCanal(),
                filas.stream().map(d -> new VentaResponse.DetalleVentaResponse(d.getId(), d.getTipoConcepto(), d.getProductoId(), d.getServicioId(),
                        d.getCitaId(), d.getSku(), d.getNombre(), d.getCantidad(), d.getPrecioUnitario(), d.getDescuento(), d.getImpuestos(), d.getTotal(), d.getCostoUnitarioHistorico())).toList());
    }
    private void validarSucursal(Long empresaId, List<Long> permitidas, Long sucursalId) {
        if (sucursalId == null) throw badRequest("Selecciona una sucursal");
        if (permitidas != null && !permitidas.isEmpty() && !permitidas.contains(sucursalId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a la sucursal indicada");
        sucursales.findByIdAndEmpresaIdAndActivaTrue(sucursalId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sucursal no encontrada"));
    }
    private String generarFolio(Long empresaId, Long sucursalId) { return "V-" + LocalDateTime.now(reloj.withZone(zonasNegocio.de(empresaId, sucursalId))).format(DateTimeFormatter.ofPattern("yyMMddHHmmss")) + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT); }
    private String limpiar(String valor) { return StringUtils.hasText(valor) ? valor.trim() : null; }
    private BigDecimal seguro(BigDecimal valor) { return valor == null ? BigDecimal.ZERO : valor; }
    private ResponseStatusException badRequest(String mensaje) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje); }
}
