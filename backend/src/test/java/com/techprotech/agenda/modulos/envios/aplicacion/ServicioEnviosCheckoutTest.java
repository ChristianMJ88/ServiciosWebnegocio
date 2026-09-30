package com.techprotech.agenda.modulos.envios.aplicacion;

import com.techprotech.agenda.modulos.envios.api.dto.ActualizarEnvioManualRequest;
import com.techprotech.agenda.modulos.envios.infraestructura.entidad.EnvioPedidoEntidad;
import com.techprotech.agenda.modulos.envios.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenDetalleRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenRepositorio;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.ProductoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServicioEnviosCheckoutTest {
    private EnvioPedidoRepositorio envios;
    private CheckoutOrdenRepositorio ordenes;
    private EmpresaEnvioConfigRepositorio configuraciones;
    private ServicioEnviosCheckout servicio;

    @BeforeEach
    void preparar() {
        envios = mock(EnvioPedidoRepositorio.class);
        ordenes = mock(CheckoutOrdenRepositorio.class);
        configuraciones = mock(EmpresaEnvioConfigRepositorio.class);
        servicio = new ServicioEnviosCheckout(envios, ordenes, mock(CheckoutOrdenDetalleRepositorio.class),
                configuraciones, mock(SucursalEnvioOrigenRepositorio.class), mock(ProductoRepositorio.class),
                mock(EnvioCotizacionRepositorio.class), mock(EnvioEventoRepositorio.class),
                mock(ServicioTrazabilidadEnvio.class));
        when(envios.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void creaGestionManualCuandoLaIntegracionEstaInactiva() {
        CheckoutOrdenEntidad orden = ordenPagada();
        when(configuraciones.findById(25L)).thenReturn(Optional.empty());
        when(envios.findByEmpresaIdAndCheckoutOrdenId(25L, 41L)).thenReturn(Optional.empty());

        servicio.preparar(orden);

        verify(envios).save(argThat(envio -> "MANUAL".equals(envio.getProveedor())
                && "PENDIENTE_GESTION_MANUAL".equals(envio.getEstado())
                && envio.getCostoCliente().compareTo(new BigDecimal("129.00")) == 0));
    }

    @Test
    void conservaLaGuiaExternaYElCostoRealDelNegocio() {
        EnvioPedidoEntidad envio = new EnvioPedidoEntidad();
        envio.setId(9L); envio.setEmpresaId(25L); envio.setCheckoutOrdenId(41L);
        envio.setEstado("PENDIENTE_GESTION_MANUAL"); envio.setMoneda("MXN");
        envio.setCostoCliente(new BigDecimal("129.00"));
        when(envios.findById(9L)).thenReturn(Optional.of(envio));
        when(ordenes.findById(41L)).thenReturn(Optional.of(ordenPagada()));

        var respuesta = servicio.actualizarManual(25L, 9L, new ActualizarEnvioManualRequest(
                "Estafeta", "Terrestre", "GUIA-123", "https://rastreo.test/GUIA-123",
                new BigDecimal("112.50"), "GUIA_GENERADA"));

        assertThat(respuesta.estado()).isEqualTo("GUIA_GENERADA");
        assertThat(respuesta.numeroGuia()).isEqualTo("GUIA-123");
        assertThat(respuesta.costoProveedor()).isEqualByComparingTo("112.50");
    }

    private CheckoutOrdenEntidad ordenPagada() {
        CheckoutOrdenEntidad orden = new CheckoutOrdenEntidad();
        orden.setId(41L); orden.setPublicId("orden-123"); orden.setEmpresaId(25L); orden.setSucursalId(3L);
        orden.setEstado("PAGADA"); orden.setModalidadEntrega("ENVIO_NACIONAL"); orden.setMoneda("MXN");
        orden.setCostoEnvio(new BigDecimal("129.00")); orden.setTotal(new BigDecimal("609.00"));
        orden.setNombreDestinatario("Ana Pérez"); orden.setDireccionFormateada("3 Poniente 303, Puebla");
        return orden;
    }
}
