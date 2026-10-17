package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.pagos.api.dto.ConfiguracionCheckoutPublicaResponse;
import com.techprotech.agenda.modulos.pagos.api.dto.ConfiguracionCheckoutRequest;
import com.techprotech.agenda.modulos.pagos.api.dto.ConfiguracionCheckoutResponse;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.EmpresaCheckoutConfigEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.EmpresaProveedorPagoEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.ProveedorPagoEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago.PropiedadesMercadoPago;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.EmpresaCheckoutConfigRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.EmpresaProveedorPagoRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.ProveedorPagoRepositorio;
import com.techprotech.agenda.modulos.envios.infraestructura.repositorio.EmpresaEnvioConfigRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioConfigEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Locale;

@Service
public class ServicioConfiguracionCheckout {
    private static final String MERCADO_PAGO = "MERCADO_PAGO";
    private static final String PRUEBA = "PRUEBA";
    private static final String PRODUCCION = "PRODUCCION";

    private final EmpresaCheckoutConfigRepositorio configuraciones;
    private final ProveedorPagoRepositorio proveedores;
    private final EmpresaProveedorPagoRepositorio conexiones;
    private final EmpresaSitioConfigRepositorio sitios;
    private final PropiedadesMercadoPago propiedadesMercadoPago;
    private final EmpresaEnvioConfigRepositorio configuracionesEnvio;

    private final com.techprotech.agenda.modulos.politicas.aplicacion.ServicioPoliticaEmpresa politicas;

    public ServicioConfiguracionCheckout(
            EmpresaCheckoutConfigRepositorio configuraciones,
            ProveedorPagoRepositorio proveedores,
            EmpresaProveedorPagoRepositorio conexiones,
            EmpresaSitioConfigRepositorio sitios,
            PropiedadesMercadoPago propiedadesMercadoPago,
            EmpresaEnvioConfigRepositorio configuracionesEnvio
    , com.techprotech.agenda.modulos.politicas.aplicacion.ServicioPoliticaEmpresa politicas) {
        this.politicas = politicas;
        this.configuraciones = configuraciones;
        this.proveedores = proveedores;
        this.conexiones = conexiones;
        this.sitios = sitios;
        this.propiedadesMercadoPago = propiedadesMercadoPago;
        this.configuracionesEnvio = configuracionesEnvio;
    }

    @Transactional(readOnly = true)
    public ConfiguracionCheckoutResponse obtener(Long empresaId) {
        ProveedorPagoEntidad proveedor = proveedorMercadoPago();
        EmpresaCheckoutConfigEntidad configuracion = configuraciones.findById(empresaId).orElseGet(() -> predeterminada(empresaId));
        EmpresaProveedorPagoEntidad conexion = conexion(empresaId, proveedor.getId());
        return respuesta(configuracion, proveedor.getCodigo(), conexion);
    }

    @Transactional
    public ConfiguracionCheckoutResponse actualizar(Long empresaId, ConfiguracionCheckoutRequest request) {
        ProveedorPagoEntidad proveedor = proveedorMercadoPago();
        EmpresaProveedorPagoEntidad conexion = conexion(empresaId, proveedor.getId());
        validar(request, conexion);
        if (request.habilitado()) politicas.exigirPublicada(empresaId);
        EmpresaCheckoutConfigEntidad configuracion = configuraciones.findById(empresaId).orElseGet(() -> predeterminada(empresaId));
        configuracion.setProveedorPredeterminadoId(proveedor.getId());
        configuracion.setHabilitado(request.habilitado());
        configuracion.setAceptarTarjeta(request.aceptarTarjeta());
        configuracion.setAceptarTransferencia(request.aceptarTransferencia());
        configuracion.setAceptarEfectivoReferenciado(request.aceptarEfectivoReferenciado());
        configuracion.setMostrarEstadoPago(request.mostrarEstadoPago());
        configuracion.setPermitirPagoProductos(request.permitirPagoProductos());
        configuracion.setPermitirPagoServicios(request.permitirPagoServicios());
        configuracion.setPermitirRecoleccion(request.permitirRecoleccion());
        configuracion.setPermitirEntregaLocal(request.permitirEntregaLocal());
        configuracion.setCostoEntregaLocal(seguro(request.costoEntregaLocal()));
        configuracion.setPermitirEnvioNacional(request.permitirEnvioNacional());
        configuracion.setCostoEnvioNacional(seguro(request.costoEnvioNacional()));
        configuracion.setModalidadServicios(request.modalidadServicios());
        configuracion.setAnticipoPorcentaje("ANTICIPO_PORCENTAJE".equals(request.modalidadServicios()) ? request.anticipoPorcentaje() : null);
        configuracion.setAnticipoImporte("ANTICIPO_IMPORTE".equals(request.modalidadServicios()) ? request.anticipoImporte() : null);
        configuracion.setMoneda(request.moneda().toUpperCase(Locale.ROOT));
        return respuesta(configuraciones.save(configuracion), proveedor.getCodigo(), conexion);
    }

    @Transactional(readOnly = true)
    public ConfiguracionCheckoutPublicaResponse obtenerPublica(String slug) {
        EmpresaSitioConfigEntidad sitio = sitios.findBySlug(slug.trim().toLowerCase(Locale.ROOT))
                .filter(EmpresaSitioConfigEntidad::isPublicado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un sitio publicado para ese tenant"));
        ProveedorPagoEntidad proveedor = proveedorMercadoPago();
        EmpresaCheckoutConfigEntidad configuracion = configuraciones.findById(sitio.getEmpresaId())
                .orElseGet(() -> predeterminada(sitio.getEmpresaId()));
        EmpresaProveedorPagoEntidad conexion = conexion(sitio.getEmpresaId(), proveedor.getId());
        var politica = politicas.vigente(sitio.getEmpresaId()).orElse(null);
        boolean habilitado = configuracion.isHabilitado() && conectada(conexion) && politica != null;
        return new ConfiguracionCheckoutPublicaResponse(
                habilitado,
                proveedor.getCodigo(),
                ambienteActivo(),
                habilitado ? conexion.getLlavePublica() : null,
                habilitado && configuracion.isAceptarTarjeta(),
                habilitado && configuracion.isAceptarTransferencia(),
                habilitado && configuracion.isAceptarEfectivoReferenciado(),
                configuracion.isMostrarEstadoPago(),
                habilitado && configuracion.isPermitirPagoProductos(),
                habilitado && configuracion.isPermitirPagoServicios(),
                habilitado && configuracion.isPermitirRecoleccion(),
                habilitado && configuracion.isPermitirEntregaLocal(),
                configuracion.getCostoEntregaLocal(),
                habilitado && configuracion.isPermitirEnvioNacional(),
                configuracion.getCostoEnvioNacional(),
                configuracionesEnvio.findById(sitio.getEmpresaId()).map(item -> item.isHabilitado())
                        .orElse(false),
                configuracion.getModalidadServicios(),
                configuracion.getAnticipoPorcentaje(),
                configuracion.getAnticipoImporte(),
                configuracion.getMoneda(),
                politica == null ? null : new ConfiguracionCheckoutPublicaResponse.Politica(politica.getVersion(), politica.getPublicadaEn().atOffset(java.time.ZoneOffset.UTC)),
                habilitado && configuracion.isPermitirPagoProductos() && (configuracion.isAceptarTarjeta() || configuracion.isAceptarTransferencia() || configuracion.isAceptarEfectivoReferenciado()),
                !habilitado || !configuracion.isPermitirPagoProductos() || !(configuracion.isAceptarTarjeta() || configuracion.isAceptarTransferencia() || configuracion.isAceptarEfectivoReferenciado())
                    ? "El negocio no tiene disponible el pago de productos en línea en este momento. Tu carrito se conserva; contacta al negocio para comprar." : null
        );
    }

    private void validar(ConfiguracionCheckoutRequest request, EmpresaProveedorPagoEntidad conexion) {
        if (request.habilitado() && !conectada(conexion)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Conecta Mercado Pago antes de habilitar el checkout");
        }
        if (request.habilitado() && !request.aceptarTarjeta() && !request.aceptarTransferencia() && !request.aceptarEfectivoReferenciado()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona al menos un método de pago");
        }
        if (request.habilitado() && !request.permitirPagoProductos() && !request.permitirPagoServicios()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona productos, servicios o ambos para el checkout");
        }
        if (request.habilitado() && request.permitirPagoProductos()
                && !request.permitirRecoleccion() && !request.permitirEntregaLocal() && !request.permitirEnvioNacional()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona al menos una modalidad de entrega para productos");
        }
        if ("ANTICIPO_PORCENTAJE".equals(request.modalidadServicios()) && request.anticipoPorcentaje() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica el porcentaje de anticipo");
        }
        if ("ANTICIPO_IMPORTE".equals(request.modalidadServicios()) && request.anticipoImporte() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica el importe del anticipo");
        }
    }

    private ConfiguracionCheckoutResponse respuesta(EmpresaCheckoutConfigEntidad config, String proveedor,
                                                      EmpresaProveedorPagoEntidad conexion) {
        return new ConfiguracionCheckoutResponse(
                config.isHabilitado(), proveedor, conectada(conexion), ambienteActivo(), config.isAceptarTarjeta(),
                config.isAceptarTransferencia(), config.isAceptarEfectivoReferenciado(), config.isMostrarEstadoPago(),
                config.isPermitirPagoProductos(),
                config.isPermitirPagoServicios(), config.isPermitirRecoleccion(), config.isPermitirEntregaLocal(),
                config.getCostoEntregaLocal(), config.isPermitirEnvioNacional(), config.getCostoEnvioNacional(),
                config.getModalidadServicios(), config.getAnticipoPorcentaje(),
                config.getAnticipoImporte(), config.getMoneda()
        );
    }

    private EmpresaCheckoutConfigEntidad predeterminada(Long empresaId) {
        EmpresaCheckoutConfigEntidad config = new EmpresaCheckoutConfigEntidad();
        config.setEmpresaId(empresaId);
        config.setHabilitado(false);
        config.setAceptarTarjeta(true);
        config.setAceptarTransferencia(false);
        config.setAceptarEfectivoReferenciado(false);
        config.setMostrarEstadoPago(true);
        config.setPermitirPagoProductos(true);
        config.setPermitirPagoServicios(true);
        config.setPermitirRecoleccion(true);
        config.setPermitirEntregaLocal(false);
        config.setCostoEntregaLocal(BigDecimal.ZERO);
        config.setPermitirEnvioNacional(false);
        config.setCostoEnvioNacional(BigDecimal.ZERO);
        config.setModalidadServicios("PAGO_COMPLETO");
        config.setMoneda("MXN");
        return config;
    }

    private ProveedorPagoEntidad proveedorMercadoPago() {
        return proveedores.findByCodigoAndActivoTrue(MERCADO_PAGO)
                .orElseThrow(() -> new IllegalStateException("Mercado Pago no está disponible"));
    }

    private EmpresaProveedorPagoEntidad conexion(Long empresaId, Long proveedorId) {
        return conexiones.findByEmpresaIdAndProveedorPagoIdAndAmbiente(empresaId, proveedorId, ambienteActivo()).orElse(null);
    }

    private boolean conectada(EmpresaProveedorPagoEntidad conexion) {
        return conexion != null && "CONECTADO".equals(conexion.getEstado())
                && conexion.getLlavePublica() != null && !conexion.getLlavePublica().isBlank();
    }

    private String ambienteActivo() {
        return PRUEBA.equalsIgnoreCase(propiedadesMercadoPago.ambiente()) ? PRUEBA : PRODUCCION;
    }

    private BigDecimal seguro(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
