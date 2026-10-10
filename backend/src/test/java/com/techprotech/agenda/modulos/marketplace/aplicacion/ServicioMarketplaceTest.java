package com.techprotech.agenda.modulos.marketplace.aplicacion;
import com.techprotech.agenda.modulos.marketplace.api.dto.*;
import com.techprotech.agenda.modulos.marketplace.infraestructura.entidad.ParticipacionMarketplaceEntidad;
import com.techprotech.agenda.modulos.marketplace.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioCapacidadesEmpresa;
import com.techprotech.agenda.modulos.plataforma.api.dto.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class ServicioMarketplaceTest {
 final DirectorioRepositorio directorio=mock(DirectorioRepositorio.class);
 final ParticipacionMarketplaceRepositorio participaciones=mock(ParticipacionMarketplaceRepositorio.class);
 final ServicioCapacidadesEmpresa capacidades=mock(ServicioCapacidadesEmpresa.class);
 final ServicioMarketplace servicio=new ServicioMarketplaceImpl(directorio,participaciones,capacidades);
 @Test void excluyeSuscripcionesSinAccesoAunqueExistanCandidatos(){
  when(directorio.buscar("uñas",0,25)).thenReturn(List.of(new NegocioDirectorioResponse(1L,"negocio","Negocio",null,null,null,null)));
  when(capacidades.obtener(1L)).thenReturn(CapacidadesEmpresaResponse.sinSuscripcion());
  assertTrue(servicio.buscar("uñas",0).negocios().isEmpty());
 }
 @Test void publicaSoloModuloIncluidoEnCapacidadesDelBackend(){
  var negocio=new NegocioDirectorioResponse(2L,"negocio","Negocio",null,null,null,null);
  when(directorio.buscar("",0,25)).thenReturn(List.of(negocio));
  when(capacidades.obtener(2L)).thenReturn(new CapacidadesEmpresaResponse(true,1L,1L,"PLAN","Plan","ACTIVA",null,null,
    List.of(new CapacidadModuloEmpresaResponse("MARKETPLACE","Marketplace",null)),List.of(),List.of()));
  assertEquals(List.of(negocio),servicio.buscar("",0).negocios());
 }
 @Test void participacionPorDefectoEsVoluntaria(){
  when(participaciones.findById(7L)).thenReturn(Optional.empty());
  when(capacidades.obtener(7L)).thenReturn(CapacidadesEmpresaResponse.sinSuscripcion());
  assertFalse(servicio.obtener(7L).participante());
 }
 @Test void permiteRetirarseSinExigirPlanYUsaEmpresaAutenticada(){
  when(participaciones.findById(7L)).thenReturn(Optional.empty());
  when(capacidades.obtener(7L)).thenReturn(CapacidadesEmpresaResponse.sinSuscripcion());
  servicio.actualizar(7L,false);
  verify(capacidades,never()).exigirModulo(anyLong(),anyString());
  verify(participaciones).save(argThat(p->p.getEmpresaId().equals(7L)&&!p.isParticipante()));
 }
}
