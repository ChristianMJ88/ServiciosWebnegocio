package com.techprotech.agenda.modulos.pagos.aplicacion;
import com.techprotech.agenda.modulos.pagos.api.dto.*;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.OrdenPointCajaEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.OrdenPointCajaRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago.ClienteMercadoPagoPoint;
import com.techprotech.agenda.modulos.caja.aplicacion.ServicioVentasCaja;
import com.techprotech.agenda.modulos.admin.infraestructura.entidad.AuditoriaConfiguracionEmpresaEntidad;
import com.techprotech.agenda.modulos.admin.infraestructura.repositorio.AuditoriaConfiguracionEmpresaRepositorio;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import java.util.*;import java.math.BigDecimal;import java.time.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;import static org.mockito.Mockito.*;
class ServicioResolucionPointTest {
 final ServicioMercadoPagoPoint point=mock(ServicioMercadoPagoPoint.class);final ClienteMercadoPagoPoint cliente=mock(ClienteMercadoPagoPoint.class);
 final OrdenPointCajaRepositorio ordenes=mock(OrdenPointCajaRepositorio.class);final ServicioVentasCaja ventas=mock(ServicioVentasCaja.class);
 final AuditoriaConfiguracionEmpresaRepositorio auditoria=mock(AuditoriaConfiguracionEmpresaRepositorio.class);
 final OrdenPointCajaEntidad orden=new OrdenPointCajaEntidad();
 final UsuarioAutenticado usuario=new UsuarioAutenticado("admin@example.test",11L,7L,"EMPRESA",List.of(),List.of("PAGOS_CONFIGURAR"),List.of(3L));
 final ServicioResolucionPoint servicio;
 ServicioResolucionPointTest(){
  orden.setId(1L);orden.setPublicId("local-test");orden.setEmpresaId(7L);orden.setVentaId(22L);orden.setTerminalId(5L);orden.setEstado("REQUIERE_REVISION");orden.setImporte(new BigDecimal("400"));orden.setMoneda("MXN");
  when(point.buscarConAlcance(7L,List.of(3L),"local-test")).thenReturn(orden);when(ordenes.bloquear(7L,"local-test")).thenReturn(Optional.of(orden));
  when(point.mapear(orden)).thenAnswer(i->new OrdenPointResponse("local-test",22L,"V-22",5L,"Prueba",orden.getEstado(),null,new BigDecimal("400"),"MXN",null,null,null,null,false,new BigDecimal("300"),false,false,false,false,false));
  servicio=new ServicioResolucionPoint(point,cliente,ordenes,ventas,mock(ServicioConciliacionPagos.class),auditoria,Clock.fixed(Instant.parse("2026-10-17T18:00:00Z"),ZoneOffset.UTC),new com.fasterxml.jackson.databind.ObjectMapper(),ServicioMercadoPagoPointTest.gestor());
 }
 @Test void noCobradoLiberaSaldoYAuditaActorMotivoYMomentoUtc(){
  var r=servicio.resolver(usuario,"local-test",new ResolverOrdenPointRequest(false,null,"Verificado y cancelado en panel"));
  assertThat(r.estado()).isEqualTo("CANCELADO");verify(ventas).cancelarPoint(7L,22L,"Verificado y cancelado en panel");verifyNoInteractions(cliente);
  var captor=org.mockito.ArgumentCaptor.forClass(AuditoriaConfiguracionEmpresaEntidad.class);verify(auditoria).save(captor.capture());assertThat(captor.getValue().getUsuarioActorId()).isEqualTo(11L);assertThat(captor.getValue().getDetalleDespuesJson()).contains("2026-10-17T18:00:00Z","Verificado y cancelado en panel");
 }
 @Test void cobradoConReferenciaAjenaOImporteDistintoNoModificaNada(){
  when(cliente.consultar(7L,"ORD_X")).thenReturn(new ClienteMercadoPagoPoint.OrdenRemota("ORD_X","processed",null,"PAY",null,null,null,"point_otra",new BigDecimal("400"),"MXN"));
  assertThatThrownBy(()->servicio.resolver(usuario,"local-test",new ResolverOrdenPointRequest(true,"ORD_X","Revisado en el panel de pagos"))).hasMessageContaining("no corresponde");verify(ordenes,never()).bloquear(any(),any());verifyNoInteractions(ventas,auditoria);
 }
 @Test void cobradoVerificadoAsociaOrdenYUsaConfirmacionNormal(){
  var remota=new ClienteMercadoPagoPoint.OrdenRemota("ORD_X","processed",null,"PAY",null,null,null,"point_localtest",new BigDecimal("400"),"MXN");when(cliente.consultar(7L,"ORD_X")).thenReturn(remota);
  servicio.resolver(usuario,"local-test",new ResolverOrdenPointRequest(true,"ORD_X","Pago verificado en Mercado Pago"));verify(point).aplicarEstado(orden,remota);verify(auditoria).save(any());
 }
 @Test void requierePermisoEfectivoYNoPermiteResolverUnaOrdenVigente(){
  var sinPermiso=new UsuarioAutenticado(null,11L,7L,"EMPRESA",List.of(),List.of("CAJA_ACCESO"),List.of(3L));
  assertThatThrownBy(()->servicio.resolver(sinPermiso,"local-test",new ResolverOrdenPointRequest(false,null,"Verificado en el panel"))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("permiso");
  orden.setEstado("CREADO");assertThatThrownBy(()->servicio.resolver(usuario,"local-test",new ResolverOrdenPointRequest(false,null,"Verificado en el panel"))).hasMessageContaining("ya no necesita");verifyNoInteractions(cliente,ventas,auditoria);
 }
}
