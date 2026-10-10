package com.techprotech.agenda.modulos.marketplace.api.dto;
import java.util.List;
public record NegocioDirectorioResponse(Long empresaId,String slug,String nombre,String descripcion,
 String imagenUrl,String direccion,String telefono,String portadaUrl,List<ServicioDirectorioResponse> servicios) {
 public NegocioDirectorioResponse(Long empresaId,String slug,String nombre,String descripcion,String imagenUrl,String direccion,String telefono){
  this(empresaId,slug,nombre,descripcion,imagenUrl,direccion,telefono,null,List.of());
 }
 public NegocioDirectorioResponse conServicios(List<ServicioDirectorioResponse> servicios){
  return new NegocioDirectorioResponse(empresaId,slug,nombre,descripcion,imagenUrl,direccion,telefono,portadaUrl,List.copyOf(servicios));
 }
}
