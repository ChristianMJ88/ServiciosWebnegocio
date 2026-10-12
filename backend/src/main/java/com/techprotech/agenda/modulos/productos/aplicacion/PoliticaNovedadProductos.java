package com.techprotech.agenda.modulos.productos.aplicacion;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoEntidad;
import java.time.LocalDate;
public final class PoliticaNovedadProductos {
 private PoliticaNovedadProductos() {}
 public static boolean esNuevo(ProductoEntidad producto,int dias,LocalDate hoy){
  if(producto.getNuevoDesde()!=null && producto.getNuevoHasta()!=null)
   return !hoy.isBefore(producto.getNuevoDesde()) && !hoy.isAfter(producto.getNuevoHasta());
  if(dias<=0 || producto.getCreadoEn()==null)return false;
  var creado=producto.getCreadoEn().toLocalDate();
  return !hoy.isBefore(creado) && hoy.isBefore(creado.plusDays(dias));
 }
}
