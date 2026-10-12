package com.techprotech.agenda.modulos.productos.aplicacion;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
/** Una misma evaluación para catálogo público y checkout. Las reglas llegan ordenadas por prioridad. */
public final class CalculadorDescuentosProductos {
 private CalculadorDescuentosProductos() {}
 public static boolean vigente(ReglaDescuentoProductoEntidad regla,java.time.LocalDate hoy){
  return regla.isActiva() && (regla.getFechaInicio()==null || !hoy.isBefore(regla.getFechaInicio()))
      && (regla.getFechaFin()==null || !hoy.isAfter(regla.getFechaFin()));
 }
 public record Resultado(BigDecimal precio,String etiqueta) {}
 public static Resultado calcular(ProductoEntidad producto, ZonaPrecioProductoEntidad zona, Long sucursalId,
     BigDecimal cantidad, BigDecimal inicial, String moneda, List<ReglaDescuentoProductoEntidad> reglas) {
  BigDecimal precio=inicial;var nombres=new ArrayList<String>();
  for(var regla:reglas){
   if(regla.getZonaPrecioId()!=null && (zona==null || !regla.getZonaPrecioId().equals(zona.getId())))continue;
   if(regla.getSucursalId()!=null && !regla.getSucursalId().equals(sucursalId))continue;
   boolean aplica=switch(regla.getAlcance()) {case "GLOBAL"->true;case "PRODUCTO"->Objects.equals(regla.getProductoId(),producto.getId());case "CATEGORIA"->Objects.equals(regla.getCategoriaId(),producto.getCategoriaId());default->false;};
   if(!aplica || cantidad.compareTo(regla.getCantidadMinima())<0)continue;
   if(!"PORCENTAJE".equals(regla.getTipoDescuento()) && !moneda.equalsIgnoreCase(regla.getMoneda()))continue;
   precio=switch(regla.getTipoDescuento()){case "PORCENTAJE"->precio.subtract(precio.multiply(regla.getValor()).divide(BigDecimal.valueOf(100),4,RoundingMode.HALF_UP));case "MONTO"->precio.subtract(regla.getValor());case "PRECIO_FIJO"->regla.getValor();default->precio;};
   precio=precio.max(BigDecimal.ZERO).setScale(2,RoundingMode.HALF_UP);nombres.add(regla.getNombre());
   if(!regla.isCombinable())break;
  }
  return new Resultado(precio.setScale(2,RoundingMode.HALF_UP),precio.compareTo(inicial)<0 && !nombres.isEmpty()?String.join(" + ",nombres):null);
 }
}
