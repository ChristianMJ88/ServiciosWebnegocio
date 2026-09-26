package com.techprotech.agenda.modulos.caja.aplicacion;

import com.techprotech.agenda.modulos.caja.api.dto.RegistrarVentaRequest;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.productos.aplicacion.ServicioInventarioProductos;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoEntidad;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.ProductoExistenciaRepositorio;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.ProductoRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServicioVentasCajaTest {
    @Test
    void registraServicioYProductosComoUnaVentaYDescuentaInventario() {
        VentaRepositorio ventas = mock(VentaRepositorio.class);
        VentaDetalleRepositorio detalles = mock(VentaDetalleRepositorio.class);
        PagoVentaRepositorio pagos = mock(PagoVentaRepositorio.class);
        CajaSesionRepositorio sesiones = mock(CajaSesionRepositorio.class);
        MovimientoCajaRepositorio movimientos = mock(MovimientoCajaRepositorio.class);
        PagoCitaRepositorio pagosCita = mock(PagoCitaRepositorio.class);
        CitaRepositorio citas = mock(CitaRepositorio.class);
        ClienteRepositorio clientes = mock(ClienteRepositorio.class);
        ServicioRepositorio servicios = mock(ServicioRepositorio.class);
        ProductoRepositorio productos = mock(ProductoRepositorio.class);
        ProductoExistenciaRepositorio existencias = mock(ProductoExistenciaRepositorio.class);
        SucursalRepositorio sucursales = mock(SucursalRepositorio.class);
        ServicioInventarioProductos inventario = mock(ServicioInventarioProductos.class);
        ServicioVentasCaja servicio = new ServicioVentasCaja(ventas, detalles, pagos, sesiones, movimientos, pagosCita,
                citas, clientes, servicios, productos, existencias, sucursales, inventario);

        SucursalEntidad sucursal = new SucursalEntidad(); sucursal.setId(5L); sucursal.setEmpresaId(1L); sucursal.setActiva(true);
        CajaSesionEntidad sesion = new CajaSesionEntidad(); sesion.setId(8L); sesion.setEmpresaId(1L); sesion.setSucursalId(5L); sesion.setEstado("ABIERTA");
        CitaEntidad cita = new CitaEntidad(); cita.setId(10L); cita.setEmpresaId(1L); cita.setSucursalId(5L); cita.setServicioId(20L);
        cita.setClienteId(30L); cita.setPrecio(new BigDecimal("100.00")); cita.setMoneda("MXN"); cita.setEstado("FINALIZADA"); cita.setCheckInEn(LocalDateTime.now());
        ServicioEntidad conceptoServicio = new ServicioEntidad(); conceptoServicio.setId(20L); conceptoServicio.setEmpresaId(1L); conceptoServicio.setNombre("Manicure");
        ProductoEntidad producto = new ProductoEntidad(); producto.setId(40L); producto.setEmpresaId(1L); producto.setActivo(true);
        producto.setControlaInventario(true); producto.setNombre("Crema"); producto.setSku("CREMA-1"); producto.setPrecioBase(new BigDecimal("50.00")); producto.setMoneda("MXN");

        when(sucursales.findByIdAndEmpresaIdAndActivaTrue(5L, 1L)).thenReturn(Optional.of(sucursal));
        when(sesiones.findByEmpresaIdAndSucursalIdAndEstado(1L, 5L, "ABIERTA")).thenReturn(Optional.of(sesion));
        when(productos.findAllById(any())).thenReturn(List.of(producto));
        when(citas.findByIdAndEmpresaId(10L, 1L)).thenReturn(Optional.of(cita));
        when(servicios.findByIdAndEmpresaId(20L, 1L)).thenReturn(Optional.of(conceptoServicio));
        when(pagosCita.sumarPagadoPorCita(1L, 10L)).thenReturn(BigDecimal.ZERO);
        when(detalles.sumarServiciosPagadosPorCita(1L, 10L)).thenReturn(BigDecimal.ZERO);
        when(ventas.save(any())).thenAnswer(invocation -> { VentaEntidad item = invocation.getArgument(0); item.setId(50L); return item; });
        when(detalles.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(pagos.save(any())).thenAnswer(invocation -> { PagoVentaEntidad item = invocation.getArgument(0); item.setId(60L); return item; });

        var response = servicio.registrar(1L, 2L, List.of(5L), new RegistrarVentaRequest(5L, 10L, null,
                List.of(new RegistrarVentaRequest.ProductoVentaRequest(40L, new BigDecimal("2"))), "TARJETA", "AUTH-1", null));

        assertThat(response.total()).isEqualByComparingTo("200.00");
        assertThat(response.detalles()).extracting(item -> item.tipoConcepto()).containsExactly("SERVICIO", "PRODUCTO");
        verify(inventario).descontarVenta(eq(1L), eq(2L), eq(List.of(5L)), eq(5L), eq(java.util.Map.of(40L, new BigDecimal("2"))), any());
        verify(movimientos, never()).save(any());
    }
}
