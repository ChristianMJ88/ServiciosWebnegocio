package com.techprotech.agenda.modulos.productos.aplicacion;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ReglaComisionEntidad;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.mockito.Mockito.*;

class ServicioComisionesDevengadasTest {
 private final EntityManager em=mock(EntityManager.class);
 private final ReglaComisionRepositorio reglas=mock(ReglaComisionRepositorio.class);
 private final CitaRepositorio citas=mock(CitaRepositorio.class);
 private final ServicioComisionesDevengadas servicio=new ServicioComisionesDevengadas(em,reglas,citas,mock(ProductoRepositorio.class),mock(ServicioComisiones.class));
 private VentaEntidad venta(String estado){var v=new VentaEntidad();v.setId(1L);v.setEmpresaId(2L);v.setMoneda("MXN");v.setEstado(estado);return v;}
 @Test void pagoPendienteNoDevenga(){servicio.registrar(venta("PENDIENTE_PAGO"),List.of());verifyNoInteractions(em,reglas,citas);}
 @Test void reintentoNoDuplica(){var q=mock(Query.class);when(em.createNativeQuery(startsWith("select count"))).thenReturn(q);when(q.setParameter(anyString(),any())).thenReturn(q);when(q.getSingleResult()).thenReturn(1L);servicio.registrar(venta("PAGADA"),List.of());verifyNoInteractions(reglas,citas);}
 @Test void conservaBaseNetaYValorAlPagar(){
  var count=mock(Query.class);when(em.createNativeQuery(startsWith("select count"))).thenReturn(count);when(count.setParameter(anyString(),any())).thenReturn(count);when(count.getSingleResult()).thenReturn(0L);
  var insert=mock(Query.class);when(em.createNativeQuery(startsWith("insert"))).thenReturn(insert);when(insert.setParameter(anyString(),any())).thenReturn(insert);
  var regla=new ReglaComisionEntidad();regla.setId(8L);regla.setActiva(true);regla.setConcepto("SERVICIO");regla.setAlcance("GLOBAL");regla.setBeneficiario("PRESTADOR");regla.setBaseCalculo("NETO_DESCUENTOS");regla.setTipoCalculo("PORCENTAJE");regla.setValor(new BigDecimal("10"));when(reglas.findByEmpresaIdOrderByPrioridadDescNombreAsc(2L)).thenReturn(List.of(regla));
  var cita=new CitaEntidad();cita.setPrestadorId(9L);when(citas.findByIdAndEmpresaId(3L,2L)).thenReturn(Optional.of(cita));
  var d=new VentaDetalleEntidad();d.setId(4L);d.setCitaId(3L);d.setTipoConcepto("SERVICIO");d.setTotal(new BigDecimal("80.00"));d.setImpuestos(BigDecimal.ZERO);servicio.registrar(venta("PAGADA"),List.of(d));
  verify(insert).setParameter("persona",9L);verify(insert).setParameter("importe",new BigDecimal("8.00"));verify(insert).setParameter("valor",new BigDecimal("10"));verify(insert).executeUpdate();
 }
}
