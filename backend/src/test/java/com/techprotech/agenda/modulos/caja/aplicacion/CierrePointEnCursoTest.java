package com.techprotech.agenda.modulos.caja.aplicacion;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class CierrePointEnCursoTest {
 @Test void noAlteraElCorteMientrasHayPointPendiente() {
  var repos=mock(com.techprotech.agenda.modulos.caja.infraestructura.repositorio.CajaSesionRepositorio.class);
  var s=new com.techprotech.agenda.modulos.caja.infraestructura.entidad.CajaSesionEntidad();s.setId(6L);s.setSucursalId(3L);s.setEmpresaId(7L);s.setEstado("ABIERTA");s.setMontoEsperado(new java.math.BigDecimal("100"));
  when(repos.bloquearPorId(6L,7L)).thenReturn(java.util.Optional.of(s));when(repos.tienePointEnCurso(7L,6L)).thenReturn(true);
  var caja=mock(ServicioCaja.class,CALLS_REAL_METHODS);org.springframework.test.util.ReflectionTestUtils.setField(caja,"cajaSesionRepositorio",repos);
  assertThatThrownBy(()->caja.cerrarSesion(7L,11L,java.util.List.of(3L),6L,new com.techprotech.agenda.modulos.caja.api.dto.CerrarCajaRequest(new java.math.BigDecimal("100"),null))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("Point en curso");
  assertThat(s.getMontoEsperado()).isEqualByComparingTo("100");assertThat(s.getEstado()).isEqualTo("ABIERTA");verify(repos,never()).save(any());
 }
}
