package com.techprotech.agenda.modulos.envios.aplicacion;

import com.techprotech.agenda.modulos.envios.api.dto.CotizacionEnvioPublicaResponse;
import com.techprotech.agenda.modulos.envios.api.dto.CotizarEnvioPublicoRequest;
import com.techprotech.agenda.modulos.envios.dominio.ProveedorCotizacionesEnvio;
import com.techprotech.agenda.modulos.envios.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.envios.infraestructura.envia.PropiedadesEnvia;
import com.techprotech.agenda.modulos.envios.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.productos.aplicacion.ServicioCotizacionProductos;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoEntidad;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.ProductoRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioConfigEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServicioCotizacionesEnvio {
    private final EmpresaSitioConfigRepositorio sitios;
    private final EmpresaEnvioConfigRepositorio configuraciones;
    private final SucursalEnvioOrigenRepositorio origenes;
    private final ProductoRepositorio productos;
    private final ServicioCotizacionProductos cotizadorProductos;
    private final EnvioCotizacionRepositorio cotizaciones;
    private final ProveedorCotizacionesEnvio proveedor;
    private final ResolutorCredencialEnvio credenciales;
    private final PropiedadesEnvia propiedades;

    public ServicioCotizacionesEnvio(EmpresaSitioConfigRepositorio sitios,
                                     EmpresaEnvioConfigRepositorio configuraciones,
                                     SucursalEnvioOrigenRepositorio origenes,
                                     ProductoRepositorio productos,
                                     ServicioCotizacionProductos cotizadorProductos,
                                     EnvioCotizacionRepositorio cotizaciones,
                                     ProveedorCotizacionesEnvio proveedor,
                                     ResolutorCredencialEnvio credenciales,
                                     PropiedadesEnvia propiedades) {
        this.sitios = sitios; this.configuraciones = configuraciones; this.origenes = origenes;
        this.productos = productos; this.cotizadorProductos = cotizadorProductos;
        this.cotizaciones = cotizaciones; this.proveedor = proveedor; this.credenciales = credenciales;
        this.propiedades = propiedades;
    }

    @Transactional
    public CotizacionEnvioPublicaResponse cotizar(String slug, CotizarEnvioPublicoRequest request) {
        EmpresaSitioConfigEntidad sitio = sitio(slug);
        EmpresaEnvioConfigEntidad config = configuraciones.findById(sitio.getEmpresaId())
                .filter(EmpresaEnvioConfigEntidad::isHabilitado)
                .orElseThrow(() -> error(HttpStatus.CONFLICT, "El envío por paquetería no está configurado para este negocio"));
        SucursalEnvioOrigenEntidad origen = origenes.findByEmpresaIdOrderBySucursalIdAsc(sitio.getEmpresaId()).stream()
                .filter(SucursalEnvioOrigenEntidad::isActivo)
                .filter(item -> request.sucursalId().equals(item.getSucursalId())).findFirst()
                .orElseThrow(() -> error(HttpStatus.CONFLICT, "La sucursal no tiene un origen de envío disponible"));

        Map<Long, BigDecimal> cantidades = request.productos().stream().collect(Collectors.toMap(
                CotizarEnvioPublicoRequest.Producto::productoId, CotizarEnvioPublicoRequest.Producto::cantidad,
                BigDecimal::add, LinkedHashMap::new));
        ServicioCotizacionProductos.ResultadoCotizacion precios = cotizadorProductos.cotizar(
                sitio.getEmpresaId(), request.sucursalId(), request.zonaPrecioClave(), cantidades);
        Map<Long, ProductoEntidad> catalogo = productos.findByEmpresaIdAndIdIn(sitio.getEmpresaId(), cantidades.keySet())
                .stream().collect(Collectors.toMap(ProductoEntidad::getId, Function.identity()));
        List<String> incompletos = catalogo.values().stream().filter(this::sinDimensiones)
                .map(ProductoEntidad::getNombre).sorted().toList();
        if (!incompletos.isEmpty()) throw error(HttpStatus.CONFLICT,
                "No es posible cotizar hasta completar peso y medidas de: " + String.join(", ", incompletos));

        Map<Long, ServicioCotizacionProductos.CotizacionProducto> preciosPorProducto = precios.productos().stream()
                .collect(Collectors.toMap(ServicioCotizacionProductos.CotizacionProducto::productoId, Function.identity()));
        String moneda = precios.productos().stream().map(ServicioCotizacionProductos.CotizacionProducto::moneda)
                .distinct().reduce((a, b) -> { throw error(HttpStatus.BAD_REQUEST, "Los productos usan monedas diferentes"); })
                .orElse("MXN");
        List<ProveedorCotizacionesEnvio.Paquete> paquetes = cantidades.entrySet().stream().map(item -> {
            ProductoEntidad producto = catalogo.get(item.getKey());
            var precio = preciosPorProducto.get(item.getKey());
            return new ProveedorCotizacionesEnvio.Paquete(producto.getNombre(), item.getValue(), precio.total(),
                    producto.getPesoKg(), producto.getLargoCm(), producto.getAnchoCm(), producto.getAltoCm());
        }).toList();
        var solicitud = new ProveedorCotizacionesEnvio.Solicitud(
                direccionOrigen(origen, sitio), direccionDestino(request), paquetes, moneda);
        List<ProveedorCotizacionesEnvio.Opcion> opciones = proveedor.cotizar(
                credenciales.resolver(config), config.getAmbiente(), solicitud);
        if (opciones.isEmpty()) throw error(HttpStatus.CONFLICT,
                "No encontramos servicios de paquetería para esta dirección y estos paquetes");

        String carritoHash = hashCarrito(cantidades);
        String destinoHash = hashDestino(request.direccion());
        LocalDateTime expiraEn = LocalDateTime.now(ZoneOffset.UTC)
                .plusMinutes(Math.max(1, propiedades.vigenciaCotizacionMinutos()));
        List<EnvioCotizacionEntidad> guardadas = opciones.stream().filter(item -> moneda.equalsIgnoreCase(item.moneda()))
                .map(item -> entidad(sitio.getEmpresaId(), request.sucursalId(), config, carritoHash, destinoHash,
                        item, aplicarRecargo(item.costo(), config), expiraEn)).toList();
        if (guardadas.isEmpty()) throw error(HttpStatus.CONFLICT,
                "Envia.com devolvió tarifas en una moneda distinta a la configurada por el negocio");
        cotizaciones.saveAll(guardadas);
        return new CotizacionEnvioPublicaResponse(guardadas.stream().map(this::respuesta).toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal validar(String id, Long empresaId, Long sucursalId,
                              CotizarEnvioPublicoRequest.Direccion direccion, Map<Long, BigDecimal> productos) {
        EnvioCotizacionEntidad cotizacion = cotizaciones.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "La tarifa de envío seleccionada no existe"));
        if (!sucursalId.equals(cotizacion.getSucursalId())
                || !hashCarrito(productos).equals(cotizacion.getCarritoHash())
                || !hashDestino(direccion).equals(cotizacion.getDestinoHash())) {
            throw error(HttpStatus.CONFLICT, "La tarifa ya no corresponde al carrito o a la dirección actual");
        }
        if (cotizacion.getExpiraEn().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            throw error(HttpStatus.CONFLICT, "La tarifa de envío expiró; solicita una nueva cotización");
        }
        return cotizacion.getCostoCliente();
    }

    private ProveedorCotizacionesEnvio.Direccion direccionOrigen(SucursalEnvioOrigenEntidad item,
                                                                  EmpresaSitioConfigEntidad sitio) {
        return new ProveedorCotizacionesEnvio.Direccion(item.getNombreContacto(), sitio.getNombreComercial(),
                item.getCorreo(), item.getTelefono(), item.getCalle(), item.getNumeroExterior(), item.getColonia(),
                item.getMunicipio(), item.getEstado(), item.getCodigoPais(), item.getCodigoPostal(), item.getReferencias());
    }

    private ProveedorCotizacionesEnvio.Direccion direccionDestino(CotizarEnvioPublicoRequest request) {
        var item = request.direccion();
        return new ProveedorCotizacionesEnvio.Direccion(request.nombreDestinatario(), null,
                request.correoDestinatario(), request.telefonoDestinatario(), item.calle(), item.numeroExterior(),
                item.colonia(), item.municipio(), item.estado(), item.codigoPais().toUpperCase(Locale.ROOT),
                item.codigoPostal(), item.referencias());
    }

    private EnvioCotizacionEntidad entidad(Long empresaId, Long sucursalId, EmpresaEnvioConfigEntidad config,
                                            String carritoHash, String destinoHash,
                                            ProveedorCotizacionesEnvio.Opcion opcion, BigDecimal costoCliente,
                                            LocalDateTime expiraEn) {
        EnvioCotizacionEntidad entidad = new EnvioCotizacionEntidad();
        entidad.setId(UUID.randomUUID().toString()); entidad.setEmpresaId(empresaId); entidad.setSucursalId(sucursalId);
        entidad.setProveedor(config.getProveedor()); entidad.setAmbiente(config.getAmbiente());
        entidad.setCarritoHash(carritoHash); entidad.setDestinoHash(destinoHash);
        entidad.setPaqueteria(opcion.paqueteria()); entidad.setPaqueteriaNombre(opcion.paqueteriaNombre());
        entidad.setServicio(opcion.servicio()); entidad.setServicioNombre(opcion.servicioNombre());
        entidad.setEntregaEstimada(opcion.entregaEstimada()); entidad.setCostoProveedor(opcion.costo());
        entidad.setCostoCliente(costoCliente); entidad.setMoneda(opcion.moneda().toUpperCase(Locale.ROOT));
        entidad.setExpiraEn(expiraEn); return entidad;
    }

    private CotizacionEnvioPublicaResponse.Opcion respuesta(EnvioCotizacionEntidad item) {
        return new CotizacionEnvioPublicaResponse.Opcion(item.getId(), item.getPaqueteria(), item.getPaqueteriaNombre(),
                item.getServicio(), item.getServicioNombre(), item.getEntregaEstimada(), item.getCostoCliente(),
                item.getMoneda(), item.getExpiraEn());
    }

    private BigDecimal aplicarRecargo(BigDecimal costo, EmpresaEnvioConfigEntidad config) {
        BigDecimal resultado = switch (config.getTipoRecargo()) {
            case "IMPORTE" -> costo.add(config.getValorRecargo());
            case "PORCENTAJE" -> costo.add(costo.multiply(config.getValorRecargo())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            default -> costo;
        };
        return resultado.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean sinDimensiones(ProductoEntidad item) {
        return noPositivo(item.getPesoKg()) || noPositivo(item.getLargoCm())
                || noPositivo(item.getAnchoCm()) || noPositivo(item.getAltoCm());
    }
    private boolean noPositivo(BigDecimal valor) { return valor == null || valor.signum() <= 0; }

    public String hashCarrito(Map<Long, BigDecimal> productos) {
        String valor = productos.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(item -> item.getKey() + ":" + item.getValue().stripTrailingZeros().toPlainString())
                .collect(Collectors.joining("|"));
        return sha256(valor);
    }

    public String hashDestino(CotizarEnvioPublicoRequest.Direccion direccion) {
        return sha256(String.join("|", normalizados(direccion.calle(), direccion.numeroExterior(),
                direccion.numeroInterior(), direccion.colonia(), direccion.municipio(), direccion.estado(),
                direccion.codigoPostal(), direccion.codigoPais())));
    }

    private List<String> normalizados(String... valores) {
        return Arrays.stream(valores).map(valor -> valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT)).toList();
    }
    private String sha256(String valor) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(valor.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException("No fue posible proteger la cotización", ex); }
    }
    private EmpresaSitioConfigEntidad sitio(String slug) {
        return sitios.findBySlug(slug.trim().toLowerCase(Locale.ROOT)).filter(EmpresaSitioConfigEntidad::isPublicado)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "No existe un sitio publicado para ese tenant"));
    }
    private ResponseStatusException error(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }
}
