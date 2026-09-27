package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.PagoCitaRepositorio;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.VentaDetalleRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.pagos.api.dto.*;
import com.techprotech.agenda.modulos.pagos.infraestructura.checkout.PropiedadesCheckout;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenDetalleEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenDetalleRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenRepositorio;
import com.techprotech.agenda.modulos.productos.aplicacion.ServicioCotizacionProductos;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioConfigEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServicioOrdenCheckout {
    private static final BigDecimal CERO = new BigDecimal("0.00");
    private static final Set<String> ESTADOS_CITA_COBRABLES = Set.of("PENDIENTE", "CONFIRMADA");

    private final ServicioConfiguracionCheckout configuracionCheckout;
    private final EmpresaSitioConfigRepositorio sitios;
    private final SucursalRepositorio sucursales;
    private final ServicioCotizacionProductos cotizadorProductos;
    private final CitaRepositorio citas;
    private final ServicioRepositorio servicios;
    private final UsuarioRepositorio usuarios;
    private final PagoCitaRepositorio pagosCita;
    private final VentaDetalleRepositorio detallesVenta;
    private final CheckoutOrdenRepositorio ordenes;
    private final CheckoutOrdenDetalleRepositorio detalles;
    private final PropiedadesCheckout propiedades;

    public ServicioOrdenCheckout(ServicioConfiguracionCheckout configuracionCheckout,
                                 EmpresaSitioConfigRepositorio sitios, SucursalRepositorio sucursales,
                                 ServicioCotizacionProductos cotizadorProductos, CitaRepositorio citas,
                                 ServicioRepositorio servicios, UsuarioRepositorio usuarios,
                                 PagoCitaRepositorio pagosCita, VentaDetalleRepositorio detallesVenta,
                                 CheckoutOrdenRepositorio ordenes, CheckoutOrdenDetalleRepositorio detalles,
                                 PropiedadesCheckout propiedades) {
        this.configuracionCheckout = configuracionCheckout;
        this.sitios = sitios;
        this.sucursales = sucursales;
        this.cotizadorProductos = cotizadorProductos;
        this.citas = citas;
        this.servicios = servicios;
        this.usuarios = usuarios;
        this.pagosCita = pagosCita;
        this.detallesVenta = detallesVenta;
        this.ordenes = ordenes;
        this.detalles = detalles;
        this.propiedades = propiedades;
    }

    @Transactional
    public OrdenCheckoutResponse crear(String slug, String idempotencia, CrearOrdenCheckoutRequest request) {
        if (idempotencia == null || idempotencia.isBlank() || idempotencia.length() > 128)
            throw error(HttpStatus.BAD_REQUEST, "Idempotency-Key es obligatorio y debe tener máximo 128 caracteres");
        EmpresaSitioConfigEntidad sitio = sitio(slug);
        Optional<CheckoutOrdenEntidad> existente = ordenes.findByEmpresaIdAndIdempotencia(sitio.getEmpresaId(), idempotencia.trim());
        if (existente.isPresent()) return mapear(existente.get());
        ConfiguracionCheckoutPublicaResponse config = configuracionCheckout.obtenerPublica(slug);
        if (!config.habilitado()) throw error(HttpStatus.CONFLICT, "El checkout no está habilitado para este negocio");
        sucursales.findByIdAndEmpresaIdAndActivaTrue(request.sucursalId(), sitio.getEmpresaId())
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Sucursal no encontrada"));

        Map<Long, BigDecimal> cantidades = productos(request).stream().collect(Collectors.toMap(
                CrearOrdenCheckoutRequest.ProductoOrdenRequest::productoId,
                CrearOrdenCheckoutRequest.ProductoOrdenRequest::cantidad,
                BigDecimal::add, LinkedHashMap::new));
        List<Long> citaIds = request.citaIds() == null ? List.of() : request.citaIds().stream().distinct().toList();
        if (cantidades.isEmpty() && citaIds.isEmpty()) throw error(HttpStatus.BAD_REQUEST, "Agrega productos o citas a la orden");
        if (!cantidades.isEmpty() && !config.permitirPagoProductos()) throw error(HttpStatus.CONFLICT, "El pago de productos no está habilitado");
        if (!citaIds.isEmpty() && !config.permitirPagoServicios()) throw error(HttpStatus.CONFLICT, "El pago de servicios no está habilitado");
        BigDecimal costoEnvio = validarEntrega(request, config, !cantidades.isEmpty());

        ServicioCotizacionProductos.ResultadoCotizacion cotizacion = cotizadorProductos.cotizar(
                sitio.getEmpresaId(), request.sucursalId(), request.zonaPrecioClave(), cantidades);
        List<Linea> lineas = new ArrayList<>();
        cotizacion.productos().forEach(producto -> lineas.add(new Linea("PRODUCTO", producto.productoId(), null, null,
                producto.nombre(), producto.cantidad(), producto.precioListaUnitario(), producto.precioUnitario(),
                producto.descuento(), CERO, producto.total(), producto.moneda())));
        lineas.addAll(cotizarCitas(sitio.getEmpresaId(), request.sucursalId(), request.correoCliente(), citaIds, config));
        if (costoEnvio.signum() > 0) {
            String nombreEnvio = "ENTREGA_LOCAL".equals(request.modalidadEntrega()) ? "Entrega local" : "Envío nacional";
            lineas.add(new Linea("ENVIO", null, null, null, nombreEnvio, BigDecimal.ONE,
                    costoEnvio, costoEnvio, CERO, CERO, costoEnvio, config.moneda()));
        }
        validarMoneda(lineas, config.moneda());

        BigDecimal descuento = lineas.stream().map(Linea::descuento).reduce(CERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal impuestos = lineas.stream().map(Linea::impuestos).reduce(CERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = lineas.stream().map(Linea::total).reduce(CERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = total.add(descuento).subtract(impuestos).setScale(2, RoundingMode.HALF_UP);
        if (total.signum() <= 0) throw error(HttpStatus.CONFLICT, "La orden no tiene saldo por cobrar");

        CheckoutOrdenEntidad orden = new CheckoutOrdenEntidad();
        orden.setPublicId(UUID.randomUUID().toString());
        orden.setEmpresaId(sitio.getEmpresaId());
        orden.setSucursalId(request.sucursalId());
        orden.setZonaPrecioId(cotizacion.zonaPrecioId());
        orden.setIdempotencia(idempotencia.trim());
        orden.setEstado("PENDIENTE_PAGO");
        orden.setInventarioEstado(cantidades.isEmpty() ? "NO_APLICA" : "PENDIENTE");
        orden.setCorreoCliente(request.correoCliente().trim().toLowerCase(Locale.ROOT));
        orden.setModalidadEntrega(request.modalidadEntrega());
        orden.setNombreDestinatario(request.nombreDestinatario().trim());
        orden.setTelefonoDestinatario(request.telefonoDestinatario().trim());
        aplicarDireccion(orden, request.direccion());
        orden.setMoneda(config.moneda());
        orden.setSubtotal(subtotal);
        orden.setDescuentoTotal(descuento);
        orden.setImpuestosTotal(impuestos);
        orden.setCostoEnvio(costoEnvio);
        orden.setTotal(total);
        orden.setExpiraEn(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(propiedades.minutosVigenciaOrden()));
        orden = ordenes.save(orden);
        Long ordenId = orden.getId();
        detalles.saveAll(lineas.stream().map(linea -> entidadDetalle(ordenId, sitio.getEmpresaId(), linea)).toList());
        return mapear(orden);
    }

    @Transactional(readOnly = true)
    public OrdenCheckoutResponse obtener(String slug, String publicId) {
        EmpresaSitioConfigEntidad sitio = sitio(slug);
        CheckoutOrdenEntidad orden = ordenes.findByEmpresaIdAndPublicId(sitio.getEmpresaId(), publicId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Orden no encontrada"));
        return mapear(orden);
    }

    private List<Linea> cotizarCitas(Long empresaId, Long sucursalId, String correo, List<Long> ids,
                                     ConfiguracionCheckoutPublicaResponse config) {
        if (ids.isEmpty()) return List.of();
        UsuarioEntidad cliente = usuarios.findByEmpresaIdAndCorreo(empresaId, correo.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "El correo no corresponde a las citas seleccionadas"));
        Map<Long, CitaEntidad> encontradas = citas.findAllById(ids).stream()
                .filter(item -> empresaId.equals(item.getEmpresaId()) && cliente.getId().equals(item.getClienteId()))
                .collect(Collectors.toMap(CitaEntidad::getId, Function.identity()));
        if (encontradas.size() != ids.size()) throw error(HttpStatus.BAD_REQUEST, "Una o más citas no pertenecen al cliente o al negocio");
        Map<Long, ServicioEntidad> serviciosPorId = servicios.findByEmpresaIdAndIdIn(empresaId,
                encontradas.values().stream().map(CitaEntidad::getServicioId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(ServicioEntidad::getId, Function.identity()));
        List<Linea> resultado = new ArrayList<>();
        for (Long id : ids) {
            CitaEntidad cita = encontradas.get(id);
            if (!sucursalId.equals(cita.getSucursalId())) throw error(HttpStatus.BAD_REQUEST, "Todas las citas deben pertenecer a la sucursal seleccionada");
            if (!ESTADOS_CITA_COBRABLES.contains(cita.getEstado().toUpperCase(Locale.ROOT))) throw error(HttpStatus.CONFLICT, "Una cita ya no está disponible para cobro");
            BigDecimal pagado = seguro(pagosCita.sumarPagadoPorCita(empresaId, id))
                    .add(seguro(detallesVenta.sumarServiciosPagadosPorCita(empresaId, id)));
            BigDecimal pendiente = cita.getPrecio().subtract(pagado).max(CERO).setScale(2, RoundingMode.HALF_UP);
            if (pendiente.signum() <= 0) throw error(HttpStatus.CONFLICT, "Una cita ya está pagada");
            BigDecimal cobrar = importeServicio(pendiente, config);
            ServicioEntidad servicio = serviciosPorId.get(cita.getServicioId());
            String nombre = servicio == null ? "Servicio reservado" : servicio.getNombre();
            resultado.add(new Linea("SERVICIO", null, cita.getServicioId(), cita.getId(), nombre,
                    BigDecimal.ONE, cobrar, cobrar, CERO, CERO, cobrar, cita.getMoneda()));
        }
        return resultado;
    }

    private BigDecimal importeServicio(BigDecimal pendiente, ConfiguracionCheckoutPublicaResponse config) {
        BigDecimal resultado = switch (config.modalidadServicios()) {
            case "ANTICIPO_PORCENTAJE" -> pendiente.multiply(config.anticipoPorcentaje())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case "ANTICIPO_IMPORTE" -> config.anticipoImporte();
            default -> pendiente;
        };
        return resultado.min(pendiente).max(CERO).setScale(2, RoundingMode.HALF_UP);
    }

    private void validarMoneda(List<Linea> lineas, String moneda) {
        if (lineas.stream().anyMatch(linea -> !moneda.equalsIgnoreCase(linea.moneda())))
            throw error(HttpStatus.BAD_REQUEST, "Todos los conceptos deben usar la moneda configurada para el checkout");
    }

    private CheckoutOrdenDetalleEntidad entidadDetalle(Long ordenId, Long empresaId, Linea linea) {
        CheckoutOrdenDetalleEntidad detalle = new CheckoutOrdenDetalleEntidad();
        detalle.setCheckoutOrdenId(ordenId); detalle.setEmpresaId(empresaId); detalle.setTipoConcepto(linea.tipo());
        detalle.setProductoId(linea.productoId()); detalle.setServicioId(linea.servicioId()); detalle.setCitaId(linea.citaId());
        detalle.setNombre(linea.nombre()); detalle.setCantidad(linea.cantidad()); detalle.setPrecioListaUnitario(linea.precioLista());
        detalle.setPrecioUnitario(linea.precio()); detalle.setDescuento(linea.descuento()); detalle.setImpuestos(linea.impuestos());
        detalle.setTotal(linea.total()); return detalle;
    }

    private OrdenCheckoutResponse mapear(CheckoutOrdenEntidad orden) {
        List<OrdenCheckoutResponse.Detalle> filas = detalles.findByEmpresaIdAndCheckoutOrdenIdOrderByIdAsc(orden.getEmpresaId(), orden.getId())
                .stream().map(item -> new OrdenCheckoutResponse.Detalle(item.getTipoConcepto(), item.getProductoId(),
                        item.getServicioId(), item.getCitaId(), item.getNombre(), item.getCantidad(), item.getPrecioListaUnitario(),
                        item.getPrecioUnitario(), item.getDescuento(), item.getImpuestos(), item.getTotal())).toList();
        return new OrdenCheckoutResponse(orden.getPublicId(), orden.getEstado(), orden.getSucursalId(), orden.getMoneda(),
                orden.getSubtotal(), orden.getDescuentoTotal(), orden.getImpuestosTotal(), orden.getCostoEnvio(), orden.getTotal(),
                orden.getModalidadEntrega(), orden.getExpiraEn(), filas);
    }

    private EmpresaSitioConfigEntidad sitio(String slug) {
        return sitios.findBySlug(slug.trim().toLowerCase(Locale.ROOT)).filter(EmpresaSitioConfigEntidad::isPublicado)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "No existe un sitio publicado para ese tenant"));
    }
    private List<CrearOrdenCheckoutRequest.ProductoOrdenRequest> productos(CrearOrdenCheckoutRequest request) {
        return request.productos() == null ? List.of() : request.productos();
    }
    private BigDecimal seguro(BigDecimal valor) { return valor == null ? CERO : valor; }

    private BigDecimal validarEntrega(CrearOrdenCheckoutRequest request, ConfiguracionCheckoutPublicaResponse config,
                                      boolean contieneProductos) {
        if (!contieneProductos) return CERO;
        return switch (request.modalidadEntrega()) {
            case "RECOGE_EN_SUCURSAL" -> {
                if (!config.permitirRecoleccion()) throw error(HttpStatus.CONFLICT, "La recolección en sucursal no está disponible");
                yield CERO;
            }
            case "ENTREGA_LOCAL" -> {
                if (!config.permitirEntregaLocal()) throw error(HttpStatus.CONFLICT, "La entrega local no está disponible");
                exigirDireccion(request.direccion());
                yield seguro(config.costoEntregaLocal()).setScale(2, RoundingMode.HALF_UP);
            }
            case "ENVIO_NACIONAL" -> {
                if (!config.permitirEnvioNacional()) throw error(HttpStatus.CONFLICT, "El envío nacional no está disponible");
                exigirDireccion(request.direccion());
                yield seguro(config.costoEnvioNacional()).setScale(2, RoundingMode.HALF_UP);
            }
            default -> throw error(HttpStatus.BAD_REQUEST, "Modalidad de entrega no válida");
        };
    }

    private void exigirDireccion(CrearOrdenCheckoutRequest.DireccionEntregaRequest direccion) {
        if (direccion == null) throw error(HttpStatus.BAD_REQUEST, "Captura la dirección completa para la entrega");
    }

    private void aplicarDireccion(CheckoutOrdenEntidad orden, CrearOrdenCheckoutRequest.DireccionEntregaRequest direccion) {
        if (direccion == null || "RECOGE_EN_SUCURSAL".equals(orden.getModalidadEntrega())) return;
        orden.setCalle(direccion.calle().trim());
        orden.setNumeroExterior(direccion.numeroExterior().trim());
        orden.setNumeroInterior(texto(direccion.numeroInterior()));
        orden.setColonia(direccion.colonia().trim());
        orden.setEstadoDireccion(direccion.estado().trim());
        orden.setMunicipio(direccion.municipio().trim());
        orden.setCodigoPostal(direccion.codigoPostal().trim());
        orden.setReferenciasDireccion(texto(direccion.referencias()));
    }

    private String texto(String valor) { return valor == null || valor.isBlank() ? null : valor.trim(); }
    private ResponseStatusException error(HttpStatus estado, String mensaje) { return new ResponseStatusException(estado, mensaje); }
    private record Linea(String tipo, Long productoId, Long servicioId, Long citaId, String nombre,
                         BigDecimal cantidad, BigDecimal precioLista, BigDecimal precio, BigDecimal descuento,
                         BigDecimal impuestos, BigDecimal total, String moneda) {}
}
