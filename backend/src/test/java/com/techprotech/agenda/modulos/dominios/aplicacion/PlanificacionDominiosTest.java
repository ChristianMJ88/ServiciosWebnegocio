package com.techprotech.agenda.modulos.dominios.aplicacion;
import com.techprotech.agenda.modulos.dominios.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.dominios.infraestructura.repositorio.EmpresaDominioRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;import java.util.*;
import static org.mockito.Mockito.*;import static org.assertj.core.api.Assertions.*;
class PlanificacionDominiosTest {
 @Test void sesentaSinCapacidadNoDejanSinComprobarAlSiguiente(){
  var repo=mock(EmpresaDominioRepositorio.class);var servicio=mock(ServicioDominios.class);var rows=new ArrayList<EmpresaDominioEntidad>();var reloj=Clock.fixed(Instant.parse("2026-10-20T15:00:00Z"),ZoneOffset.UTC);
  for(long id=1;id<=61;id++){var d=new EmpresaDominioEntidad();d.setId(id);d.setEmpresaId(id);d.setEstado(EstadoDominio.PENDIENTE_VERIFICACION);d.setVersion(0L);rows.add(d);if(id<=60)doThrow(new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED)).when(servicio).verificar(id,null,id,0L);}
  when(repo.findById(anyLong())).thenAnswer(i->rows.stream().filter(d->d.getId().equals(i.getArgument(0))).findFirst());
  when(repo.candidatos(anyList(),any(),any())).thenAnswer(i->rows.stream().filter(d->d.getProximoChequeoEn()==null).limit(50).map(EmpresaDominioEntidad::getId).toList());
  when(repo.planificar(anyLong(),any())).thenAnswer(i->{rows.stream().filter(d->d.getId().equals(i.getArgument(0))).findFirst().orElseThrow().setProximoChequeoEn(i.getArgument(1));return 1;});
  var tarea=new TareaComprobacionDominios(repo,servicio,new PlanificadorChequeosDominio(repo,reloj,300,86400),reloj,50);
  tarea.ejecutar();verify(servicio,never()).verificar(61L,null,61L,0L);tarea.ejecutar();verify(servicio).verificar(61L,null,61L,0L);
  assertThat(rows).allMatch(d->d.getProximoChequeoEn()!=null);
 }
 @Test void frecuenciaNoAumentaIntentosYErrorInesperadoNoInterrumpeElLote(){
  var repo=mock(EmpresaDominioRepositorio.class);var servicio=mock(ServicioDominios.class);var planificador=mock(PlanificadorChequeosDominio.class);var reloj=Clock.systemUTC();
  when(repo.candidatos(anyList(),any(),any())).thenReturn(List.of(1L,2L,3L));
  for(long id=1;id<=3;id++){var row=new EmpresaDominioEntidad();row.setId(id);row.setEmpresaId(id);row.setVersion(0L);when(repo.findById(id)).thenReturn(Optional.of(row));}
  doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS)).when(servicio).verificar(1L,null,1L,0L);
  doThrow(new IllegalStateException("secreto que no debe registrarse")).when(servicio).verificar(2L,null,2L,0L);
  new TareaComprobacionDominios(repo,servicio,planificador,reloj,50).ejecutar();
  verify(planificador).porFrecuencia(1L);verify(planificador,never()).porFrecuencia(2L);verify(servicio).verificar(3L,null,3L,0L);
 }
 @Test void descuentoDeFrecuenciaEsPersistente(){var repo=mock(EmpresaDominioRepositorio.class);new PlanificadorChequeosDominio(repo,Clock.systemUTC(),300,86400).porFrecuencia(7L);verify(repo).descontarIntento(7L);}
}
