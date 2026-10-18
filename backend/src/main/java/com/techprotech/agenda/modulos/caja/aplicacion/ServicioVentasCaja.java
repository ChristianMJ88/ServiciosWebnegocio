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
@lombok.extern.slf4j.Slf4j
public class ServicioVentasCaja {
    private final PublicadorPagosCitas eventosPago;
    private final ServicioSaldoCitas saldos;
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
                              com.techprotech.agenda.modulos.productos.aplicacion.ServicioComisionesDevengadas comisiones, Clock reloj, ZonaNegocio zonasNegocio, PublicadorPagosCitas eventosPago, ServicioSaldoCitas saldos) {
        this.eventosPago = eventosPago; this.saldos = saldos;
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

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public VentaResponse registrar(Long empresaId, Long usuarioId, List<Long> sucursalesPermitidas, RegistrarVentaRequest request) {
        if(request.ventaPendienteId()!=null) return cobrarPendienteInterno(empresaId,usuarioId,sucursalesPermitidas,request.ventaPendienteId(),request);
        if (METODO_PAGO_POINT.equalsIgnoreCase(request.metodoPago())) {
            throw badRequest("El cobro Point debe iniciarse desde el flujo de terminal");
        }
        return registrarInterno(empresaId, usuarioId, sucursalesPermitidas, request, false);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public VentaResponse prepararPoint(Long empresaId, Long usuarioId, List<Long> sucursalesPermitidas,
                                       RegistrarVentaRequest request) {
        if(request.ventaPendienteId()!=null) return prepararPendientePoint(empresaId,usuarioId,sucursalesPermitidas,request.ventaPendienteId(),request.sucursalId());
        RegistrarVentaRequest point = new RegistrarVentaRequest(request.sucursalId(), request.citaId(), request.clienteId(),
                request.productos(), request.pagos()==null?METODO_PAGO_POINT:null, null, request.observaciones(), null, request.citaIds(),request.pagos());
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
        String metodoPago = request.metodoPago()==null?null:request.metodoPago().trim().toUpperCase(Locale.ROOT);
        CajaSesionEntidad sesion = staff ? null : sesiones.findByEmpresaIdAndSucursalIdAndEstado(empresaId, request.sucursalId(), ESTADO_SESION_ABIERTA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No hay una caja abierta para la sucursal indicada"));

        if(point) {
            sesion=sesiones.findParaAperturaConBloqueo(empresaId,sesion.getSucursalId(),ESTADO_SESION_ABIERTA)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,"La caja se cerró antes de preparar el cobro"));
        }
        List<RegistrarVentaRequest.ProductoVentaRequest> solicitados = request.productos() == null ? List.of() : request.productos();
        Map<Long, BigDecimal> cantidades = solicitados.stream().collect(Collectors.toMap(
                RegistrarVentaRequest.ProductoVentaRequest::productoId,
                RegistrarVentaRequest.ProductoVentaRequest::cantidad,
                BigDecimal::add, LinkedHashMap::new));
        Map<Long, ProductoEntidad> productosVenta = productos.findAllById(cantidades.keySet()).stream()
                .filter(item -> empresaId.equals(item.getEmpresaId()) && item.isActivo())
                .collect(Collectors.toMap(ProductoEntidad::getId, Function.identity()));
        if (productosVenta.size() != cantidades.size()) throw badRequest("Uno o más productos no están disponibles para la empresa");

        // Mismo orden que checkout: existencias → prestadores (si aplica) → citas.
        if (!staff && !cantidades.isEmpty()) inventario.bloquearParaVenta(empresaId,request.sucursalId(),cantidades.keySet());
        var bloqueadas = new ArrayList<CitaEntidad>();
        for (Long id : request.citasNormalizadas()) {
            var c = citas.bloquearPorIdYEmpresa(id,empresaId).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,"La cita " + id + " no está disponible para este negocio"));
            if (!request.sucursalId().equals(c.getSucursalId())) throw conflictoCita(id,"pertenece a otra sucursal");
            if (!ServicioSaldoCitas.esCobrable(c)) throw conflictoCita(id,"aún no está lista para cobro");
            bloqueadas.add(c);
        }
        CitaEntidad cita = bloqueadas.isEmpty() ? null : bloqueadas.getFirst();
        if (cita == null && cantidades.isEmpty()) throw badRequest("Agrega una cita o al menos un producto a la venta");
        if (cita == null && request.clienteId() != null && clientes.findByClienteIdAndEmpresaId(request.clienteId(),empresaId).isEmpty()) throw badRequest("El cliente no pertenece a la empresa");
        if (cita != null && request.clienteId() != null && !request.clienteId().equals(cita.getClienteId())) throw conflictoCita(cita.getId(),"no corresponde al cliente seleccionado");
        String moneda = cita != null ? cita.getMoneda() : productosVenta.values().iterator().next().getMoneda();
        Long clienteCitas = cita == null ? null : cita.getClienteId();
        for (var c : bloqueadas) {
            if (!Objects.equals(clienteCitas,c.getClienteId())) throw conflictoCita(c.getId(),"pertenece a otro cliente");
            if (!moneda.equalsIgnoreCase(c.getMoneda())) throw conflictoCita(c.getId(),"usa otra moneda");
        }
        if (productosVenta.values().stream().anyMatch(item -> !moneda.equalsIgnoreCase(item.getMoneda()))) throw new ResponseStatusException(HttpStatus.CONFLICT,"Todos los conceptos de una venta deben usar la misma moneda");
        var saldosCitas = saldos.calcularBajoBloqueo(empresaId,bloqueadas);
        Map<Long, ServicioEntidad> serviciosVenta = new HashMap<>();
        for (var c : bloqueadas) {
            if (saldosCitas.get(c.getId()).pendiente().signum() <= 0) throw conflictoCita(c.getId(),"ya está pagada");
            serviciosVenta.put(c.getId(),servicios.findByIdAndEmpresaId(c.getServicioId(),empresaId).orElseThrow(() -> conflictoCita(c.getId(),"no tiene un servicio disponible")));
        }
        BigDecimal pendienteServicio = bloqueadas.stream().map(c -> saldosCitas.get(c.getId()).pendiente()).reduce(CERO,BigDecimal::add);
        var reglas = reglasVigentes(empresaId, request.sucursalId());
        var contexto = contextoPrecios(empresaId, request.sucursalId(), cantidades.keySet());
        Map<Long, BigDecimal> preciosAplicados = cantidades.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey, item -> precioProducto(productosVenta.get(item.getKey()), request.sucursalId(), item.getValue(), reglas, contexto)));
        BigDecimal subtotalProductos = cantidades.entrySet().stream()
                .map(item -> precioLista(productosVenta.get(item.getKey()), contexto).multiply(item.getValue()).setScale(2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netoProductos = cantidades.entrySet().stream().map(item -> preciosAplicados.get(item.getKey()).multiply(item.getValue()).setScale(2, RoundingMode.HALF_UP)).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal descuentoTotal = subtotalProductos.setScale(2, RoundingMode.HALF_UP).subtract(netoProductos);
        for (var c : bloqueadas) descuentoTotal = descuentoTotal.add(com.techprotech.agenda.modulos.citas.aplicacion.DesglosePrecioCita.paraCobro(c,saldosCitas.get(c.getId()).pendiente(),saldosCitas.get(c.getId()).pagado()).descuento());
        BigDecimal total = pendienteServicio.add(netoProductos).setScale(2, RoundingMode.HALF_UP);

        var reparto = staff ? null : PlanPagosVenta.resolver(total,request,point);
        VentaEntidad venta = new VentaEntidad();
        venta.setEmpresaId(empresaId); venta.setSucursalId(request.sucursalId()); venta.setCajaSesionId(sesion == null ? null : sesion.getId());
        venta.setClienteId(cita != null ? cita.getClienteId() : request.clienteId()); venta.setCitaId(bloqueadas.size() == 1 ? cita.getId() : null);
        venta.setFolio(generarFolio(empresaId, request.sucursalId())); venta.setEstado(staff ? "POR_COBRAR" : point ? "PENDIENTE_PAGO" : "PAGADA"); venta.setCanal(staff ? "STAFF" : "CAJA"); venta.setMoneda(moneda.toUpperCase(Locale.ROOT));
        venta.setSubtotal(total.add(descuentoTotal)); venta.setDescuentoTotal(descuentoTotal); venta.setImpuestosTotal(CERO); venta.setTotal(total);
        venta.setObservaciones(limpiar(request.observaciones())); venta.setCreadaPorUsuarioId(usuarioId); venta.setCreadaEn(LocalDateTime.now(reloj));
        venta = ventas.save(venta);

        List<VentaDetalleEntidad> filas = new ArrayList<>();
        for (var c : bloqueadas) filas.add(detalleServicio(empresaId,venta.getId(),c,serviciosVenta.get(c.getId()),saldosCitas.get(c.getId()).pendiente(),saldosCitas.get(c.getId()).pagado()));
        for (Map.Entry<Long, BigDecimal> item : cantidades.entrySet())
            filas.add(detalleProducto(empresaId, venta.getId(), productosVenta.get(item.getKey()), item.getValue(), preciosAplicados.get(item.getKey()), precioLista(productosVenta.get(item.getKey()), contexto)));
        if (staff) filas.forEach(fila -> fila.setPrestadorResponsableId(usuarioId));
        filas = detalles.saveAll(filas);

        List<PagoVentaEntidad> registrados = new ArrayList<>();
        if (!staff && !point) {
            for(var parte:reparto.partes()) {
                var pago=registrarParte(venta,parte,usuarioId);
                registrados.add(pago);
                if(METODO_PAGO_EFECTIVO.equals(parte.metodoPago())) registrarIngresoEfectivo(empresaId,usuarioId,sesion.getId(),venta,pago);
            }
        }
        PagoVentaEntidad pago=registrados.isEmpty()?null:registrados.getFirst();

        if (!staff && !cantidades.isEmpty()) inventario.descontarVenta(empresaId, usuarioId, sucursalesPermitidas,
                request.sucursalId(), cantidades, venta.getFolio());
        if (!staff && !point) comisiones.registrar(venta, filas);
        if (!staff && !point) for (var c : bloqueadas)
            eventosPago.pagoEnVenta(c, venta, pago, saldosCitas.get(c.getId()).pendiente(), OrigenEventoCita.ADMIN, usuarioId);
        return mapearPagos(venta,registrados,filas);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public VentaResponse confirmarPoint(Long empresaId,Long ventaId,String referenciaPago) {
        return confirmarPointInterno(empresaId,ventaId,referenciaPago,null,List.of(),null).venta();
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public VentaResponse confirmarPoint(Long empresaId,Long ventaId,String referenciaPago,BigDecimal importePoint,List<PlanPagosVenta.Parte> previstas) {
        return confirmarPointInterno(empresaId,ventaId,referenciaPago,importePoint,previstas,null).venta();
    }
    public record ConfirmacionPoint(VentaResponse venta,boolean requiereRevision) {}
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ConfirmacionPoint confirmarPoint(com.techprotech.agenda.modulos.pagos.infraestructura.entidad.OrdenPointCajaEntidad orden,String referenciaPago,List<PlanPagosVenta.Parte> previstas) {
        return confirmarPointInterno(orden.getEmpresaId(),orden.getVentaId(),referenciaPago,orden.getImporte(),previstas,orden.getId());
    }
    private ConfirmacionPoint confirmarPointInterno(Long empresaId,Long ventaId,String referenciaPago,BigDecimal importePoint,List<PlanPagosVenta.Parte> previstas,Long ordenId) {
        VentaEntidad venta=ventas.findConBloqueoByIdAndEmpresaId(ventaId,empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Venta Point no encontrada"));
        var filas=detalles.findByEmpresaIdAndVentaIdOrderByIdAsc(empresaId,ventaId);
        var registrados=pagos.findByEmpresaIdAndVentaIdOrderByRegistradoEnDesc(empresaId,ventaId);
        if(registrados.stream().anyMatch(p -> METODO_PAGO_POINT.equals(p.getMetodoPago()) && Objects.equals(p.getReferencia(),limpiar(referenciaPago)))) return new ConfirmacionPoint(mapearPagos(venta,registrados,filas),false);
        if(!"PENDIENTE_PAGO".equals(venta.getEstado()) || ordenId!=null && !ventas.esOrdenPointVigente(empresaId,ventaId,ordenId)) {
            marcarRevisionPoint(venta,"aprobación tardía o de una orden sustituida; revisar el cobro del proveedor");ventas.save(venta);
            return new ConfirmacionPoint(mapearPagos(venta,registrados,filas),true);
        }
        var citasVenta=filas.stream().filter(f -> f.getCitaId()!=null).map(VentaDetalleEntidad::getCitaId).distinct().sorted()
                .map(id -> citas.bloquearPorIdYEmpresa(id,empresaId).orElseThrow(() -> conflictoCita(id,"no está disponible"))).toList();
        var actuales=saldos.calcular(empresaId,citasVenta);
        var excedidas=filas.stream().filter(f -> f.getCitaId()!=null).filter(f -> actuales.get(f.getCitaId()).pendiente().compareTo(f.getTotal())<0).map(VentaDetalleEntidad::getCitaId).toList();
        if(!excedidas.isEmpty()) marcarRevisionPoint(venta,"saldo previamente cubierto, citas "+excedidas);
        BigDecimal point=importePoint==null?venta.getTotal():importePoint;
        var suma=previstas.stream().map(PlanPagosVenta.Parte::monto).reduce(point,BigDecimal::add);
        boolean cuadra=suma.compareTo(venta.getTotal())==0;
        if(!cuadra) marcarRevisionPoint(venta,"reparto no coincide con total; se registra solo Point");
        var pago=registrarParte(venta,new PlanPagosVenta.Parte(METODO_PAGO_POINT,point,null,null,limpiar(referenciaPago)),venta.getCreadaPorUsuarioId());
        var nuevos=new ArrayList<PagoVentaEntidad>();nuevos.add(pago);
        if(cuadra) for(var parte:previstas) {
            var registrado=registrarParte(venta,parte,venta.getCreadaPorUsuarioId());nuevos.add(registrado);
            if(METODO_PAGO_EFECTIVO.equals(parte.metodoPago())) registrarIngresoEfectivo(empresaId,venta.getCreadaPorUsuarioId(),venta.getCajaSesionId(),venta,registrado);
        }
        venta.setEstado("PAGADA");ventas.save(venta);comisiones.registrar(venta,filas);
        eventosPago.venta(venta,pago,filas,OrigenEventoCita.WEBHOOK,null);
        return new ConfirmacionPoint(mapearPagos(venta,nuevos,filas),!excedidas.isEmpty() || !cuadra);
    }

    private void marcarRevisionPoint(VentaEntidad venta,String motivo) {
        String nota=(venta.getObservaciones()==null?"":venta.getObservaciones()+"; ")+"Revisión Point: "+motivo;
        venta.setObservaciones(nota.substring(0,Math.min(500,nota.length())));
        log.warn("Point requiere revisión: empresa={}, venta={}, motivo={}",venta.getEmpresaId(),venta.getId(),motivo);
    }

    private PagoVentaEntidad registrarParte(VentaEntidad venta,PlanPagosVenta.Parte parte,Long usuario) {
        var pago=new PagoVentaEntidad();pago.setEmpresaId(venta.getEmpresaId());pago.setVentaId(venta.getId());pago.setCajaSesionId(venta.getCajaSesionId());
        pago.setMonto(parte.monto());pago.setMetodoPago(parte.metodoPago());pago.setMontoRecibido(parte.montoRecibido());pago.setCambio(parte.cambio());
        pago.setReferencia(limpiar(parte.referencia()));pago.setRegistradoPorUsuarioId(usuario);pago.setRegistradoEn(LocalDateTime.now(reloj));return pagos.save(pago);
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
    public void revisarReembolsoPoint(Long empresaId,Long ventaId) {
        var venta=ventas.findConBloqueoByIdAndEmpresaId(ventaId,empresaId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Venta no encontrada"));
        marcarRevisionPoint(venta,"reembolso externo de la parte Point de un pago dividido; reversión completa pendiente");ventas.save(venta);
    }

    @Transactional
    public boolean marcarReembolsadaPoint(com.techprotech.agenda.modulos.pagos.infraestructura.entidad.OrdenPointCajaEntidad orden) {
        Long empresaId=orden.getEmpresaId(),ventaId=orden.getVentaId();
        VentaEntidad venta = ventas.findConBloqueoByIdAndEmpresaId(ventaId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta Point no encontrada"));
        boolean acreditada=pagos.findByEmpresaIdAndVentaIdOrderByRegistradoEnDesc(empresaId,ventaId).stream()
                .anyMatch(p -> METODO_PAGO_POINT.equals(p.getMetodoPago()) && p.getReferencia()!=null
                    && (p.getReferencia().equals(orden.getProveedorPagoId()) || p.getReferencia().equals(orden.getProveedorOrdenId())));
        if(acreditada && "REEMBOLSADA".equals(venta.getEstado())) return true;
        if (!"PAGADA".equals(venta.getEstado()) || !acreditada) {
            marcarRevisionPoint(venta,"reembolso de orden no acreditada en esta venta; no se revierte inventario ni otros cobros");ventas.save(venta);return false;
        }
        Map<Long, BigDecimal> cantidades = detalles.findByEmpresaIdAndVentaIdOrderByIdAsc(empresaId, ventaId).stream()
                .filter(item -> "PRODUCTO".equals(item.getTipoConcepto()) && item.getProductoId() != null)
                .collect(Collectors.toMap(VentaDetalleEntidad::getProductoId, VentaDetalleEntidad::getCantidad, BigDecimal::add));
        if (!cantidades.isEmpty()) inventario.reintegrarVenta(empresaId, venta.getCreadaPorUsuarioId(), venta.getSucursalId(),
                cantidades, venta.getFolio(), "Reembolso de cobro Point");
        venta.setEstado("REEMBOLSADA");
        ventas.save(venta);
        return true;
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
        Map<Long,List<PagoVentaEntidad>> pagoPorVenta=pagos.findByEmpresaIdAndVentaIdIn(empresaId,ventaIds).stream().collect(Collectors.groupingBy(PagoVentaEntidad::getVentaId));

        // Query batch: todos los detalles de las ventas
        Map<Long, List<VentaDetalleEntidad>> detallesPorVenta = detalles.findByEmpresaIdAndVentaIdIn(empresaId, ventaIds)
                .stream().collect(Collectors.groupingBy(VentaDetalleEntidad::getVentaId));

        return lista.stream()
                .map(venta -> mapearPagos(
                        venta,
                        pagoPorVenta.getOrDefault(venta.getId(),List.of()),
                        detallesPorVenta.getOrDefault(venta.getId(), List.of())
                ))
                .toList();
    }

    @Transactional(readOnly=true)
    public VentaResponse comprobante(Long empresaId,List<Long> permitidas,Long ventaId) {
        var venta=ventas.findByIdAndEmpresaId(ventaId,empresaId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Venta no encontrada"));
        validarSucursal(empresaId,permitidas,venta.getSucursalId());
        return mapearPagos(venta,pagos.findByEmpresaIdAndVentaIdOrderByRegistradoEnDesc(empresaId,ventaId),detalles.findByEmpresaIdAndVentaIdOrderByIdAsc(empresaId,ventaId));
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
        sesion=sesiones.findParaAperturaConBloqueo(empresaId,sesion.getSucursalId(),ESTADO_SESION_ABIERTA)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,"La caja se cerró antes de preparar el cobro"));
        inventario.descontarVenta(empresaId,usuarioId,permitidas,sucursalId,cantidades,venta.getFolio());
        venta.setCajaSesionId(sesion.getId());venta.setEstado("PENDIENTE_PAGO");ventas.save(venta);
        return mapear(venta,null,filas);
    }

    @Transactional
    public VentaResponse cobrarPendiente(Long empresaId,Long usuarioId,List<Long> permitidas,Long ventaId,String metodo,String referencia) {
        return cobrarPendienteInterno(empresaId,usuarioId,permitidas,ventaId,new RegistrarVentaRequest(null,null,null,List.of(),metodo,referencia,null));
    }

    private VentaResponse cobrarPendienteInterno(Long empresaId,Long usuarioId,List<Long> permitidas,Long ventaId,RegistrarVentaRequest request) {
        VentaEntidad venta=ventas.findConBloqueoByIdAndEmpresaId(ventaId,empresaId)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"La venta no existe"));
        validarSucursal(empresaId,permitidas,venta.getSucursalId());
        if(!"POR_COBRAR".equals(venta.getEstado()) || !"STAFF".equals(venta.getCanal()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"La venta ya no está pendiente de cobro");
        if(request.sucursalId()!=null && !request.sucursalId().equals(venta.getSucursalId())) throw badRequest("La cuenta pertenece a otra sucursal");
        var plan=PlanPagosVenta.resolver(venta.getTotal(),request,false);
        var sesion=sesiones.findByEmpresaIdAndSucursalIdAndEstado(empresaId,venta.getSucursalId(),ESTADO_SESION_ABIERTA)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Abre la caja de la sucursal para cobrar"));
        var filas=detalles.findByEmpresaIdAndVentaIdOrderByIdAsc(empresaId,ventaId);
        Map<Long,BigDecimal> cantidades=filas.stream().filter(f->"PRODUCTO".equals(f.getTipoConcepto()))
            .collect(Collectors.toMap(VentaDetalleEntidad::getProductoId,VentaDetalleEntidad::getCantidad,BigDecimal::add));
        inventario.descontarVenta(empresaId,usuarioId,permitidas,venta.getSucursalId(),cantidades,venta.getFolio());
        venta.setCajaSesionId(sesion.getId()); venta.setEstado("PAGADA"); ventas.save(venta);
        var registrados=new ArrayList<PagoVentaEntidad>();
        for(var parte:plan.partes()) {
            var pago=registrarParte(venta,parte,usuarioId);registrados.add(pago);
            if(METODO_PAGO_EFECTIVO.equals(parte.metodoPago())) registrarIngresoEfectivo(empresaId,usuarioId,sesion.getId(),venta,pago);
        }
        comisiones.registrar(venta,filas);
        eventosPago.venta(venta,registrados.getFirst(),filas,OrigenEventoCita.ADMIN,usuarioId);
        return mapearPagos(venta,registrados,filas);
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
    private VentaResponse mapear(VentaEntidad venta,PagoVentaEntidad pago,List<VentaDetalleEntidad> filas) {
        return mapearPagos(venta,pago==null?List.of():List.of(pago),filas);
    }
    private VentaResponse mapearPagos(VentaEntidad venta,List<PagoVentaEntidad> registrados,List<VentaDetalleEntidad> filas) {
        var ordenados=registrados.stream().sorted(Comparator.comparing(PagoVentaEntidad::getRegistradoEn,Comparator.nullsLast(Comparator.naturalOrder())).reversed().thenComparing(PagoVentaEntidad::getId,Comparator.nullsLast(Comparator.reverseOrder()))).toList();
        var pago=ordenados.isEmpty()?null:ordenados.getFirst();
        return new VentaResponse(venta.getId(), venta.getFolio(), venta.getSucursalId(), venta.getCitaId(), venta.getClienteId(), venta.getEstado(),
                venta.getMoneda(), venta.getSubtotal(), venta.getDescuentoTotal(), venta.getImpuestosTotal(), venta.getTotal(),
                ordenados.stream().map(PagoVentaEntidad::getMetodoPago).distinct().count()>1?"MIXTO":pago == null ? null : pago.getMetodoPago(), pago == null ? null : pago.getReferencia(), de(venta.getCreadaEn()), venta.getCanal(),
                filas.stream().map(d -> new VentaResponse.DetalleVentaResponse(d.getId(), d.getTipoConcepto(), d.getProductoId(), d.getServicioId(),
                        d.getCitaId(), d.getSku(), d.getNombre(), d.getCantidad(), d.getPrecioUnitario(), d.getDescuento(), d.getImpuestos(), d.getTotal(), d.getCostoUnitarioHistorico())).toList(),
                ordenados.stream().map(p -> new VentaResponse.PagoVentaResponse(p.getMetodoPago(),METODO_PAGO_POINT.equals(p.getMetodoPago())?"Tarjeta (Point)":METODOS_PAGO.stream().filter(m -> m.codigo().equals(p.getMetodoPago())).map(Opcion::etiqueta).findFirst().orElse(p.getMetodoPago()),p.getMonto(),p.getMontoRecibido(),p.getCambio(),p.getReferencia())).toList());
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
    private ResponseStatusException conflictoCita(Long id, String motivo) { return new ResponseStatusException(HttpStatus.CONFLICT,"La cita " + id + " " + motivo); }
    private ResponseStatusException badRequest(String mensaje) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje); }
}
