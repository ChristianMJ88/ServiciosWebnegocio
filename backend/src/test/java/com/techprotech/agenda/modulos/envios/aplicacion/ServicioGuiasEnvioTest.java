package com.techprotech.agenda.modulos.envios.aplicacion;

import com.techprotech.agenda.modulos.envios.dominio.ProveedorGuiasEnvio;
import com.techprotech.agenda.modulos.envios.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.envios.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenDetalleEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenDetalleRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenRepositorio;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoEntidad;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.ProductoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServicioGuiasEnvioTest {
    private EnvioPedidoRepositorio envios;
    private CheckoutOrdenRepositorio ordenes;
    private CheckoutOrdenDetalleRepositorio detalles;
    private EmpresaEnvioConfigRepositorio configuraciones;
    private SucursalEnvioOrigenRepositorio origenes;
    private ProductoRepositorio productos;
    private ResolutorCredencialEnvio credenciales;
    private ProveedorGuiasEnvio proveedor;
    private ServicioGuiasEnvio servicio;
    private EnvioPedidoEntidad envio;

    @BeforeEach
    void preparar() {
        envios = mock(EnvioPedidoRepositorio.class); ordenes = mock(CheckoutOrdenRepositorio.class);
        detalles = mock(CheckoutOrdenDetalleRepositorio.class); configuraciones = mock(EmpresaEnvioConfigRepositorio.class);
        origenes = mock(SucursalEnvioOrigenRepositorio.class); productos = mock(ProductoRepositorio.class);
        credenciales = mock(ResolutorCredencialEnvio.class); proveedor = mock(ProveedorGuiasEnvio.class);
        PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
        when(tx.getTransaction(any())).thenAnswer(invocacion -> new SimpleTransactionStatus());
        when(proveedor.codigo()).thenReturn("ENVIA");
        servicio = new ServicioGuiasEnvio(envios, ordenes, detalles, configuraciones, origenes, productos,
                credenciales, mock(ServicioTrazabilidadEnvio.class), List.of(proveedor), tx);

        envio = new EnvioPedidoEntidad(); envio.setId(7L); envio.setEmpresaId(25L); envio.setCheckoutOrdenId(41L);
        envio.setSucursalId(3L); envio.setProveedor("ENVIA"); envio.setAmbiente("PRUEBA");
        envio.setEstado("LISTO_PARA_GUIA"); envio.setPaqueteria("fedex"); envio.setServicio("ground");
        envio.setMoneda("MXN"); envio.setCostoCliente(new BigDecimal("129.00"));
        when(envios.findById(7L)).thenReturn(Optional.of(envio));
    }

    @Test
    void reservaYGeneraUnaSolaGuiaConLosDatosDelTenant() {
        when(envios.reservarGeneracion(25L, 7L)).thenReturn(1);
        when(ordenes.findById(41L)).thenReturn(Optional.of(orden()));
        EmpresaEnvioConfigEntidad config = new EmpresaEnvioConfigEntidad();
        config.setEmpresaId(25L); config.setHabilitado(true); config.setProveedor("ENVIA"); config.setAmbiente("PRUEBA");
        when(configuraciones.findById(25L)).thenReturn(Optional.of(config));
        when(origenes.findByEmpresaIdOrderBySucursalIdAsc(25L)).thenReturn(List.of(origen()));
        CheckoutOrdenDetalleEntidad linea = new CheckoutOrdenDetalleEntidad();
        linea.setProductoId(12L); linea.setCantidad(BigDecimal.ONE); linea.setTotal(new BigDecimal("480.00"));
        when(detalles.findByEmpresaIdAndCheckoutOrdenIdOrderByIdAsc(25L, 41L)).thenReturn(List.of(linea));
        when(productos.findByEmpresaIdAndIdIn(eq(25L), any())).thenReturn(List.of(producto()));
        when(credenciales.resolver(config)).thenReturn("token-tenant");
        when(proveedor.generar(eq("token-tenant"), eq("PRUEBA"), any())).thenReturn(
                new ProveedorGuiasEnvio.Guia("shipment-9", "TRACK-123", "https://label.test/123.pdf",
                        "https://track.test/123", new BigDecimal("115.50"), "Created"));

        servicio.generar(25L, 7L);

        verify(proveedor, times(1)).generar(eq("token-tenant"), eq("PRUEBA"), argThat(solicitud ->
                solicitud.referencia().equals("orden-publica") && solicitud.paqueteria().equals("fedex")
                        && solicitud.paquetes().size() == 1));
        assertThat(envio.getEstado()).isEqualTo("GUIA_GENERADA");
        assertThat(envio.getNumeroGuia()).isEqualTo("TRACK-123");
        assertThat(envio.getUrlRastreo()).isEqualTo("https://track.test/123");
        assertThat(envio.getCostoProveedor()).isEqualByComparingTo("115.50");
    }

    @Test
    void noCompraOtraGuiaCuandoYaExisteNumeroDeRastreo() {
        envio.setNumeroGuia("TRACK-EXISTENTE");
        when(envios.reservarGeneracion(25L, 7L)).thenReturn(0);

        servicio.generar(25L, 7L);

        verify(proveedor, never()).generar(any(), any(), any());
    }

    @Test
    void aplicaRastreoDelWebhookSoloALaGuiaDelTenant() {
        envio.setEstado("GUIA_GENERADA");
        envio.setNumeroGuia("TRACK-123");
        when(envios.findByEmpresaIdAndProveedorAndNumeroGuia(25L, "ENVIA", "TRACK-123"))
                .thenReturn(Optional.of(envio));

        servicio.aplicarEventoWebhook(25L, "ENVIA", null, "TRACK-123", "delivered",
                "Entregado al destinatario", "delivery-9");

        assertThat(envio.getEstado()).isEqualTo("ENTREGADO");
        assertThat(envio.getEstadoRastreo()).isEqualTo("delivered");
        verify(envios).save(envio);
    }

    private CheckoutOrdenEntidad orden() {
        CheckoutOrdenEntidad orden = new CheckoutOrdenEntidad();
        orden.setId(41L); orden.setPublicId("orden-publica"); orden.setEmpresaId(25L); orden.setSucursalId(3L);
        orden.setEstado("PAGADA"); orden.setMoneda("MXN"); orden.setNombreDestinatario("Ana Pérez");
        orden.setCorreoCliente("ana@example.com"); orden.setTelefonoDestinatario("2221234567");
        orden.setCalle("3 Poniente"); orden.setNumeroExterior("303"); orden.setColonia("La Libertad");
        orden.setMunicipio("Puebla"); orden.setEstadoDireccion("Puebla"); orden.setCodigoPais("MX");
        orden.setCodigoPostal("72130"); return orden;
    }

    private SucursalEnvioOrigenEntidad origen() {
        SucursalEnvioOrigenEntidad origen = new SucursalEnvioOrigenEntidad();
        origen.setEmpresaId(25L); origen.setSucursalId(3L); origen.setActivo(true); origen.setNombreContacto("Nail Art");
        origen.setTelefono("2220000000"); origen.setCalle("Reforma"); origen.setNumeroExterior("10");
        origen.setColonia("Centro"); origen.setMunicipio("Puebla"); origen.setEstado("Puebla");
        origen.setCodigoPais("MX"); origen.setCodigoPostal("72000"); return origen;
    }

    private ProductoEntidad producto() {
        ProductoEntidad producto = new ProductoEntidad(); producto.setId(12L); producto.setNombre("Kit de uñas");
        producto.setPesoKg(new BigDecimal("0.500")); producto.setLargoCm(new BigDecimal("20"));
        producto.setAnchoCm(new BigDecimal("15")); producto.setAltoCm(new BigDecimal("8")); return producto;
    }
}
