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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServicioConfiguracionCheckoutTest {
    private EmpresaCheckoutConfigRepositorio configuraciones;
    private EmpresaProveedorPagoRepositorio conexiones;
    private EmpresaSitioConfigRepositorio sitios;
    private ServicioConfiguracionCheckout servicio;

    @BeforeEach
    void preparar() {
        configuraciones = mock(EmpresaCheckoutConfigRepositorio.class);
        ProveedorPagoRepositorio proveedores = mock(ProveedorPagoRepositorio.class);
        conexiones = mock(EmpresaProveedorPagoRepositorio.class);
        sitios = mock(EmpresaSitioConfigRepositorio.class);
        EmpresaEnvioConfigRepositorio configuracionesEnvio = mock(EmpresaEnvioConfigRepositorio.class);
        ProveedorPagoEntidad proveedor = new ProveedorPagoEntidad();
        proveedor.setId(7L);
        proveedor.setCodigo("MERCADO_PAGO");
        when(proveedores.findByCodigoAndActivoTrue("MERCADO_PAGO")).thenReturn(Optional.of(proveedor));
        when(configuraciones.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        servicio = new ServicioConfiguracionCheckout(configuraciones, proveedores, conexiones, sitios,
                new PropiedadesMercadoPago(true, "PRUEBA", "", "", "", "", "", "", ""),
                configuracionesEnvio);
    }

    @Test
    void noPermitePublicarSinUnaConexionDelMismoTenant() {
        when(configuraciones.findById(25L)).thenReturn(Optional.empty());
        when(conexiones.findByEmpresaIdAndProveedorPagoIdAndAmbiente(25L, 7L, "PRUEBA"))
                .thenReturn(Optional.empty());

        ConfiguracionCheckoutRequest request = requestHabilitado();

        assertThatThrownBy(() -> servicio.actualizar(25L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Conecta Mercado Pago");
        verify(configuraciones, never()).save(any());
    }

    @Test
    void publicaSoloLaLlavePublicaDeLaConexionDelTenant() {
        EmpresaSitioConfigEntidad sitio = new EmpresaSitioConfigEntidad();
        sitio.setEmpresaId(25L);
        sitio.setSlug("nail-art");
        sitio.setPublicado(true);
        when(sitios.findBySlug("nail-art")).thenReturn(Optional.of(sitio));
        when(configuraciones.findById(25L)).thenReturn(Optional.of(configuracionHabilitada()));
        when(conexiones.findByEmpresaIdAndProveedorPagoIdAndAmbiente(25L, 7L, "PRUEBA"))
                .thenReturn(Optional.of(conexion("public-key-tenant-25")));

        ConfiguracionCheckoutPublicaResponse respuesta = servicio.obtenerPublica("NAIL-ART");

        assertThat(respuesta.habilitado()).isTrue();
        assertThat(respuesta.llavePublica()).isEqualTo("public-key-tenant-25");
        assertThat(respuesta.proveedor()).isEqualTo("MERCADO_PAGO");
        assertThat(respuesta.ambiente()).isEqualTo("PRUEBA");
    }

    @Test
    void guardaReglasDeProductosYServiciosSinCredencialesEnElRequest() {
        when(configuraciones.findById(25L)).thenReturn(Optional.empty());
        when(conexiones.findByEmpresaIdAndProveedorPagoIdAndAmbiente(25L, 7L, "PRUEBA"))
                .thenReturn(Optional.of(conexion("public-key")));

        ConfiguracionCheckoutResponse respuesta = servicio.actualizar(25L, requestHabilitado());

        assertThat(respuesta.habilitado()).isTrue();
        assertThat(respuesta.permitirPagoProductos()).isTrue();
        assertThat(respuesta.permitirPagoServicios()).isTrue();
        verify(configuraciones).save(argThat(config -> config.getEmpresaId().equals(25L)
                && config.getProveedorPredeterminadoId().equals(7L)
                && "MXN".equals(config.getMoneda())));
    }

    private ConfiguracionCheckoutRequest requestHabilitado() {
        return new ConfiguracionCheckoutRequest(true, true, false, false, true, true,
                true, false, BigDecimal.ZERO, false, BigDecimal.ZERO,
                "PAGO_COMPLETO", null, null, "MXN");
    }

    private EmpresaCheckoutConfigEntidad configuracionHabilitada() {
        EmpresaCheckoutConfigEntidad configuracion = new EmpresaCheckoutConfigEntidad();
        configuracion.setEmpresaId(25L);
        configuracion.setProveedorPredeterminadoId(7L);
        configuracion.setHabilitado(true);
        configuracion.setAceptarTarjeta(true);
        configuracion.setPermitirPagoProductos(true);
        configuracion.setPermitirPagoServicios(true);
        configuracion.setModalidadServicios("PAGO_COMPLETO");
        configuracion.setMoneda("MXN");
        return configuracion;
    }

    private EmpresaProveedorPagoEntidad conexion(String llavePublica) {
        EmpresaProveedorPagoEntidad conexion = new EmpresaProveedorPagoEntidad();
        conexion.setEmpresaId(25L);
        conexion.setProveedorPagoId(7L);
        conexion.setAmbiente("PRUEBA");
        conexion.setEstado("CONECTADO");
        conexion.setLlavePublica(llavePublica);
        return conexion;
    }
}
