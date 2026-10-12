package com.techprotech.agenda.modulos.marketplace.aplicacion;
import com.techprotech.agenda.modulos.marketplace.api.dto.*;
import com.techprotech.agenda.modulos.marketplace.infraestructura.entidad.ParticipacionMarketplaceEntidad;
import com.techprotech.agenda.modulos.marketplace.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioCapacidadesEmpresa;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
@Service
public class ServicioMarketplaceImpl implements ServicioMarketplace {
 private final DirectorioRepositorio directorio;
 private final ParticipacionMarketplaceRepositorio participaciones;
 private final ServicioCapacidadesEmpresa capacidades;
 public ServicioMarketplaceImpl(DirectorioRepositorio directorio,ParticipacionMarketplaceRepositorio participaciones,
   ServicioCapacidadesEmpresa capacidades){this.directorio=directorio;this.participaciones=participaciones;this.capacidades=capacidades;}
 private boolean disponible(Long empresaId){
  var c=capacidades.obtener(empresaId);
  return c.suscripcionVigente()&&c.modulos().stream().anyMatch(m->"MARKETPLACE".equals(m.codigo()));
 }
 @Transactional(readOnly=true)
 public DirectorioResponse buscar(String texto,long cursor){
  return buscar(texto,"","",cursor);
 }
 @Transactional(readOnly=true)
 public DirectorioResponse buscar(String texto,String ubicacion,String categoria,long cursor){
  // Cursor over candidates: bounded work per request, including subscriptions that ceased to be eligible.
  var candidatos=directorio.buscar(texto.trim(),ubicacion.trim(),categoria.trim(),cursor,25);
  var resultados=new ArrayList<NegocioDirectorioResponse>();
  for(var candidato:candidatos.subList(0,Math.min(24,candidatos.size())))
   if(disponible(candidato.empresaId()))resultados.add(candidato);
  Long siguiente=candidatos.size()>24?candidatos.get(23).empresaId():null;
  var servicios=directorio.servicios(resultados.stream().map(NegocioDirectorioResponse::empresaId).toList(),texto.trim(),categoria.trim());
  var sucursales=directorio.sucursales(resultados.stream().map(NegocioDirectorioResponse::empresaId).toList());
  return new DirectorioResponse(resultados.stream().map(n->n.conSucursales(sucursales.getOrDefault(n.empresaId(),java.util.List.of())).conServicios(servicios.getOrDefault(n.empresaId(),java.util.List.of()))).toList(),siguiente);
 }
 @Transactional(readOnly=true)
 public ParticipacionMarketplaceResponse obtener(Long empresaId){
  return new ParticipacionMarketplaceResponse(participaciones.findById(empresaId)
    .map(ParticipacionMarketplaceEntidad::isParticipante).orElse(false),disponible(empresaId));
 }
 @Transactional
 public ParticipacionMarketplaceResponse actualizar(Long empresaId,boolean participante){
  if(participante)capacidades.exigirModulo(empresaId,"MARKETPLACE");
  var registro=participaciones.findById(empresaId).orElseGet(ParticipacionMarketplaceEntidad::new);
  registro.setEmpresaId(empresaId);registro.setParticipante(participante);participaciones.save(registro);
  return obtener(empresaId);
 }
}
