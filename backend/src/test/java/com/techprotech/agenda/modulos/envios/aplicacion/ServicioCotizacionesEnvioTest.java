package com.techprotech.agenda.modulos.envios.aplicacion;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ServicioCotizacionesEnvioTest {
    private EnvioCotizacionRepositorio cotizaciones;
    private ProveedorCotizacionesEnvio proveedor;
    private ServicioCotizacionesEnvio servicio;

    @BeforeEach
    void preparar() {
        EmpresaSitioConfigRepositorio sitios = mock(EmpresaSitioConfigRepositorio.class);
        EmpresaEnvioConfigRepositorio configuraciones = mock(EmpresaEnvioConfigRepositorio.class);
        SucursalEnvioOrigenRepositorio origenes = mock(SucursalEnvioOrigenRepositorio.class);
        ProductoRepositorio productos = mock(ProductoRepositorio.class);
        ServicioCotizacionProductos cotizador = mock(ServicioCotizacionProductos.class);
        cotizaciones = mock(EnvioCotizacionRepositorio.class);
        proveedor = mock(ProveedorCotizacionesEnvio.class);
        ResolutorCredencialEnvio credenciales = mock(ResolutorCredencialEnvio.class);
        PropiedadesEnvia propiedades = new PropiedadesEnvia("", "", "", "", "", "", "", "", "", "",
                15, false, "", "");
        servicio = new ServicioCotizacionesEnvio(sitios, configuraciones, origenes, productos, cotizador,
                cotizaciones, proveedor, credenciales, propiedades);

        EmpresaSitioConfigEntidad sitio = new EmpresaSitioConfigEntidad();
        sitio.setEmpresaId(25L); sitio.setSlug("nail-art"); sitio.setNombreComercial("Nail Art"); sitio.setPublicado(true);
        when(sitios.findBySlug("nail-art")).thenReturn(Optional.of(sitio));
        EmpresaEnvioConfigEntidad config = new EmpresaEnvioConfigEntidad();
        config.setEmpresaId(25L); config.setHabilitado(true); config.setAmbiente("PRUEBA");
        config.setTipoRecargo("PORCENTAJE"); config.setValorRecargo(new BigDecimal("10"));
        when(configuraciones.findById(25L)).thenReturn(Optional.of(config));
        when(credenciales.resolver(config)).thenReturn("token-seguro");
        SucursalEnvioOrigenEntidad origen = new SucursalEnvioOrigenEntidad();
        origen.setEmpresaId(25L); origen.setSucursalId(3L); origen.setActivo(true);
        origen.setNombreContacto("Almacén"); origen.setTelefono("2221234567"); origen.setCalle("Reforma");
        origen.setNumeroExterior("1"); origen.setColonia("Centro"); origen.setMunicipio("Puebla");
        origen.setEstado("PU"); origen.setCodigoPais("MX"); origen.setCodigoPostal("72000");
        when(origenes.findByEmpresaIdOrderBySucursalIdAsc(25L)).thenReturn(List.of(origen));
        ProductoEntidad producto = new ProductoEntidad(); producto.setId(8L); producto.setNombre("Aceite");
        producto.setPesoKg(new BigDecimal("0.500")); producto.setLargoCm(new BigDecimal("20"));
        producto.setAnchoCm(new BigDecimal("10")); producto.setAltoCm(new BigDecimal("8"));
        when(productos.findByEmpresaIdAndIdIn(eq(25L), any())).thenReturn(List.of(producto));
        when(cotizador.cotizar(eq(25L), eq(3L), isNull(), any())).thenReturn(
                new ServicioCotizacionProductos.ResultadoCotizacion(null, List.of(
                        new ServicioCotizacionProductos.CotizacionProducto(8L, "Aceite", BigDecimal.ONE,
                                new BigDecimal("200.00"), new BigDecimal("200.00"), BigDecimal.ZERO,
                                new BigDecimal("200.00"), "MXN"))));
    }

    @Test
    void conservaTarifaDelProveedorYAplicaRecargoDelTenant() {
        when(proveedor.cotizar(anyString(), eq("PRUEBA"), any())).thenReturn(List.of(
                new ProveedorCotizacionesEnvio.Opcion("dhl", "DHL", "ground", "Terrestre",
                        "2-3 días", new BigDecimal("100.00"), "MXN")));
        when(cotizaciones.saveAll(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        var respuesta = servicio.cotizar("nail-art", solicitud());

        assertThat(respuesta.opciones()).singleElement().satisfies(opcion -> {
            assertThat(opcion.costo()).isEqualByComparingTo("110.00");
            assertThat(opcion.paqueteria()).isEqualTo("dhl");
        });
        verify(cotizaciones).saveAll(argThat(lista -> {
            var iterator = lista.iterator();
            if (!iterator.hasNext()) return false;
            var primera = iterator.next();
            return !iterator.hasNext()
                    && primera.getCostoProveedor().compareTo(new BigDecimal("100.00")) == 0
                    && primera.getCostoCliente().compareTo(new BigDecimal("110.00")) == 0;
        }));
    }

    @Test
    void rechazaUnaTarifaSiCambioLaDireccion() {
        EnvioCotizacionEntidad guardada = new EnvioCotizacionEntidad();
        guardada.setId("quote-1"); guardada.setEmpresaId(25L); guardada.setSucursalId(3L);
        guardada.setCarritoHash(servicio.hashCarrito(Map.of(8L, BigDecimal.ONE)));
        guardada.setDestinoHash(servicio.hashDestino(solicitud().direccion()));
        guardada.setCostoCliente(new BigDecimal("110.00")); guardada.setExpiraEn(LocalDateTime.now().plusMinutes(5));
        when(cotizaciones.findByIdAndEmpresaId("quote-1", 25L)).thenReturn(Optional.of(guardada));
        var otra = new CotizarEnvioPublicoRequest.Direccion("Otra calle", "303", null, "Centro",
                "Puebla", "PU", "72130", "MX", null);

        assertThatThrownBy(() -> servicio.validar("quote-1", 25L, 3L, otra, Map.of(8L, BigDecimal.ONE)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("ya no corresponde");
    }

    private CotizarEnvioPublicoRequest solicitud() {
        return new CotizarEnvioPublicoRequest(3L, null, "Ana", "ana@test.mx", "2221234567",
                new CotizarEnvioPublicoRequest.Direccion("3 Poniente", "303", null, "La Libertad",
                        "Puebla", "PU", "72130", "MX", null),
                List.of(new CotizarEnvioPublicoRequest.Producto(8L, BigDecimal.ONE)));
    }
}
