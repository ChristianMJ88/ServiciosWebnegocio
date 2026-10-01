package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.pagos.api.dto.ConfiguracionCheckoutPublicaResponse;
import com.techprotech.agenda.modulos.pagos.api.dto.ConfigurarMetodoPagoCheckoutRequest;
import com.techprotech.agenda.modulos.pagos.api.dto.MetodoPagoCheckoutResponse;
import com.techprotech.agenda.modulos.pagos.dominio.MetodoPagoProveedor;
import com.techprotech.agenda.modulos.pagos.dominio.ProveedorPago;
import com.techprotech.agenda.modulos.pagos.dominio.ProveedorPagosPort;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.EmpresaMetodoPagoCheckoutEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.checkout.PropiedadesCheckout;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.ProveedorPagoEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.EmpresaMetodoPagoCheckoutRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.ProveedorPagoRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioConfigEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServicioMetodosPagoCheckout {
    private static final String MERCADO_PAGO = "MERCADO_PAGO";
    private static final Set<String> TIPOS_DIFERIDOS = Set.of("ticket", "atm", "bank_transfer");

    private final ProveedorPagoRepositorio proveedores;
    private final EmpresaMetodoPagoCheckoutRepositorio configuraciones;
    private final EmpresaSitioConfigRepositorio sitios;
    private final ServicioConfiguracionCheckout checkout;
    private final List<ProveedorPagosPort> adaptadores;
    private final PropiedadesCheckout propiedades;

    public ServicioMetodosPagoCheckout(ProveedorPagoRepositorio proveedores,
                                       EmpresaMetodoPagoCheckoutRepositorio configuraciones,
                                       EmpresaSitioConfigRepositorio sitios,
                                       ServicioConfiguracionCheckout checkout,
                                       List<ProveedorPagosPort> adaptadores,
                                       PropiedadesCheckout propiedades) {
        this.proveedores = proveedores;
        this.configuraciones = configuraciones;
        this.sitios = sitios;
        this.checkout = checkout;
        this.adaptadores = adaptadores;
        this.propiedades = propiedades;
    }

    @Transactional(readOnly = true)
    public List<MetodoPagoCheckoutResponse> listarAdmin(Long empresaId) {
        Contexto contexto = contexto(empresaId);
        Map<String, EmpresaMetodoPagoCheckoutEntidad> guardadas = configuraciones
                .findByEmpresaIdAndProveedorPagoIdOrderByNombreAsc(empresaId, contexto.proveedor().getId()).stream()
                .collect(Collectors.toMap(EmpresaMetodoPagoCheckoutEntidad::getMetodoExternoId, Function.identity()));
        return contexto.adaptador().listarMetodosPago(empresaId).stream()
                .filter(this::esDiferido)
                .map(metodo -> respuesta(metodo, guardadas.get(metodo.id())))
                .toList();
    }

    @Transactional
    public MetodoPagoCheckoutResponse configurar(Long empresaId, ConfigurarMetodoPagoCheckoutRequest request) {
        Contexto contexto = contexto(empresaId);
        MetodoPagoProveedor remoto = contexto.adaptador().listarMetodosPago(empresaId).stream()
                .filter(this::esDiferido)
                .filter(item -> item.id().equalsIgnoreCase(request.metodoId().trim()))
                .findFirst().orElseThrow(() -> error(HttpStatus.BAD_REQUEST,
                        "El medio de pago no está disponible para la cuenta conectada"));
        EmpresaMetodoPagoCheckoutEntidad entidad = configuraciones
                .findByEmpresaIdAndProveedorPagoIdAndMetodoExternoId(empresaId, contexto.proveedor().getId(), remoto.id())
                .orElseGet(EmpresaMetodoPagoCheckoutEntidad::new);
        entidad.setEmpresaId(empresaId);
        entidad.setProveedorPagoId(contexto.proveedor().getId());
        entidad.setMetodoExternoId(remoto.id());
        entidad.setTipoExterno(remoto.tipo());
        entidad.setNombre(remoto.nombre());
        entidad.setActivo(request.activo());
        // En esta etapa los medios diferidos se limitan por contrato a órdenes que contienen solo productos.
        entidad.setSoloProductos(true);
        entidad.setVigenciaDias(request.vigenciaDias());
        configuraciones.save(entidad);
        return respuesta(remoto, entidad);
    }

    @Transactional(readOnly = true)
    public List<MetodoPagoCheckoutResponse> listarPublicos(String slug) {
        EmpresaSitioConfigEntidad sitio = sitio(slug);
        ConfiguracionCheckoutPublicaResponse config = checkout.obtenerPublica(slug);
        if (!config.habilitado() || !config.permitirPagoProductos()) return List.of();
        Contexto contexto = contexto(sitio.getEmpresaId());
        Map<String, MetodoPagoProveedor> remotos = contexto.adaptador().listarMetodosPago(sitio.getEmpresaId()).stream()
                .filter(this::esDiferido)
                .filter(item -> "active".equalsIgnoreCase(item.estado()))
                .collect(Collectors.toMap(MetodoPagoProveedor::id, Function.identity()));
        return configuraciones.findByEmpresaIdAndProveedorPagoIdOrderByNombreAsc(sitio.getEmpresaId(), contexto.proveedor().getId()).stream()
                .filter(EmpresaMetodoPagoCheckoutEntidad::isActivo)
                .filter(item -> habilitadoPorCategoria(item.getTipoExterno(), config))
                .filter(item -> remotos.containsKey(item.getMetodoExternoId()))
                .map(item -> respuesta(remotos.get(item.getMetodoExternoId()), item))
                .toList();
    }

    @Transactional(readOnly = true)
    public EmpresaMetodoPagoCheckoutEntidad obtenerActivo(Long empresaId, String metodoId) {
        Contexto contexto = contexto(empresaId);
        EmpresaMetodoPagoCheckoutEntidad configuracion = configuraciones.findByEmpresaIdAndProveedorPagoIdAndMetodoExternoId(
                        empresaId, contexto.proveedor().getId(), metodoId.trim())
                .filter(EmpresaMetodoPagoCheckoutEntidad::isActivo)
                .orElseThrow(() -> error(HttpStatus.CONFLICT, "El medio de pago no está habilitado para este negocio"));
        boolean disponible = contexto.adaptador().listarMetodosPago(empresaId).stream()
                .anyMatch(item -> item.id().equals(configuracion.getMetodoExternoId())
                        && item.tipo().equals(configuracion.getTipoExterno())
                        && "active".equalsIgnoreCase(item.estado()));
        if (!disponible) throw error(HttpStatus.CONFLICT, "El medio de pago ya no está disponible en Mercado Pago");
        return configuracion;
    }

    private boolean habilitadoPorCategoria(String tipo, ConfiguracionCheckoutPublicaResponse config) {
        return "bank_transfer".equalsIgnoreCase(tipo) ? config.aceptarTransferencia()
                : config.aceptarEfectivoReferenciado();
    }

    private boolean esDiferido(MetodoPagoProveedor metodo) {
        return metodo.tipo() != null && TIPOS_DIFERIDOS.contains(metodo.tipo().toLowerCase(Locale.ROOT));
    }

    private MetodoPagoCheckoutResponse respuesta(MetodoPagoProveedor metodo, EmpresaMetodoPagoCheckoutEntidad config) {
        return new MetodoPagoCheckoutResponse(metodo.id(), metodo.nombre(), metodo.tipo(), metodo.estado(),
                metodo.imagenUrl(), config != null && config.isActivo(), true,
                config == null ? propiedades.diasVigenciaPagoDiferido() : config.getVigenciaDias());
    }

    private Contexto contexto(Long empresaId) {
        ProveedorPagoEntidad proveedor = proveedores.findByCodigoAndActivoTrue(MERCADO_PAGO)
                .orElseThrow(() -> error(HttpStatus.SERVICE_UNAVAILABLE, "Mercado Pago no está disponible"));
        ProveedorPagosPort adaptador = adaptadores.stream()
                .filter(item -> item.proveedor() == ProveedorPago.MERCADO_PAGO).findFirst()
                .orElseThrow(() -> error(HttpStatus.SERVICE_UNAVAILABLE, "Mercado Pago no está disponible"));
        return new Contexto(proveedor, adaptador);
    }

    private EmpresaSitioConfigEntidad sitio(String slug) {
        return sitios.findBySlug(slug.trim().toLowerCase(Locale.ROOT)).filter(EmpresaSitioConfigEntidad::isPublicado)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "No existe un sitio publicado para ese tenant"));
    }

    private ResponseStatusException error(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }

    private record Contexto(ProveedorPagoEntidad proveedor, ProveedorPagosPort adaptador) {}
}
