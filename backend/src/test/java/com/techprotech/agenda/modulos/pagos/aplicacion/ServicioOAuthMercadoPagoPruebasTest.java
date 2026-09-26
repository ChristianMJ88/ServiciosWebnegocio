package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.pagos.api.dto.ConexionProveedorPagoResponse;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.EmpresaProveedorPagoEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.ProveedorPagoEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago.PropiedadesMercadoPago;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.EmpresaProveedorPagoRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.ProveedorPagoRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.seguridad.ProtectorSecretosPago;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServicioOAuthMercadoPagoPruebasTest {
    private ProtectorSecretosPago protector;
    private EmpresaProveedorPagoRepositorio conexiones;
    private ServicioOAuthMercadoPago servicio;

    @BeforeEach
    void preparar() {
        protector = mock(ProtectorSecretosPago.class);
        ProveedorPagoRepositorio proveedores = mock(ProveedorPagoRepositorio.class);
        conexiones = mock(EmpresaProveedorPagoRepositorio.class);
        ProveedorPagoEntidad proveedor = mock(ProveedorPagoEntidad.class);
        when(proveedor.getId()).thenReturn(7L);
        when(proveedores.findByCodigoAndActivoTrue("MERCADO_PAGO")).thenReturn(Optional.of(proveedor));
        when(conexiones.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(protector.encriptar("access-token-prueba")).thenReturn("token-cifrado");
        servicio = new ServicioOAuthMercadoPago(
                new PropiedadesMercadoPago(true, "PRUEBA", "", "", "redirect", "frontend", "webhook",
                        "public-key-prueba", "access-token-prueba"),
                protector, proveedores, conexiones, RestClient.builder(), new ObjectMapper());
    }

    @Test
    void activaCredencialesDePruebaSoloParaLaEmpresaSolicitante() {
        when(conexiones.findByEmpresaIdAndProveedorPagoIdAndAmbiente(25L, 7L, "PRUEBA"))
                .thenReturn(Optional.empty());

        ConexionProveedorPagoResponse respuesta = servicio.conectarPruebas(25L);

        assertThat(respuesta.estado()).isEqualTo("CONECTADO");
        assertThat(respuesta.ambiente()).isEqualTo("PRUEBA");
        verify(conexiones).save(argThat(conexion -> conexion.getEmpresaId().equals(25L)
                && conexion.getProveedorPagoId().equals(7L)
                && conexion.getLlavePublica().equals("public-key-prueba")
                && conexion.getTokenAccesoCifrado().equals("token-cifrado")));
    }

    @Test
    void obtieneElTokenCifradoDeLaConexionDePruebaDelTenant() {
        EmpresaProveedorPagoEntidad conexion = new EmpresaProveedorPagoEntidad();
        conexion.setEstado("CONECTADO");
        conexion.setTokenAccesoCifrado("token-cifrado");
        when(conexiones.findByEmpresaIdAndProveedorPagoIdAndAmbiente(25L, 7L, "PRUEBA"))
                .thenReturn(Optional.of(conexion));
        when(protector.desencriptar("token-cifrado")).thenReturn("access-token-prueba");

        assertThat(servicio.obtenerAccessToken(25L)).isEqualTo("access-token-prueba");
        verify(conexiones).findByEmpresaIdAndProveedorPagoIdAndAmbiente(25L, 7L, "PRUEBA");
    }
}
