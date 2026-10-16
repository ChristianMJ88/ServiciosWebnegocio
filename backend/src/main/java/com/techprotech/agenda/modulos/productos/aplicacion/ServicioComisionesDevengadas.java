package com.techprotech.agenda.modulos.productos.aplicacion;

import java.time.Clock;
import com.techprotech.agenda.compartido.tiempo.ZonaNegocio;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ReglaComisionEntidad;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;

@Service
public class ServicioComisionesDevengadas {
    private final Clock reloj;
    private final ZonaNegocio zonasNegocio;
 private final EntityManager em; private final ReglaComisionRepositorio reglas; private final CitaRepositorio citas; private final ProductoRepositorio productos; private final ServicioComisiones catalogos;
 public ServicioComisionesDevengadas(EntityManager em,ReglaComisionRepositorio reglas,CitaRepositorio citas,ProductoRepositorio productos,ServicioComisiones catalogos, Clock reloj, ZonaNegocio zonasNegocio){
        this.reloj = reloj;
        this.zonasNegocio = zonasNegocio;this.em=em;this.reglas=reglas;this.citas=citas;this.productos=productos;this.catalogos=catalogos;}
 @Transactional public void registrar(VentaEntidad venta,List<VentaDetalleEntidad> detalles){
  if(!"PAGADA".equals(venta.getEstado()))return;
  em.lock(venta,LockModeType.PESSIMISTIC_WRITE);
  if(((Number)em.createNativeQuery("select count(*) from comision_devengada where empresa_id=:empresa and venta_id=:venta").setParameter("empresa",venta.getEmpresaId()).setParameter("venta",venta.getId()).getSingleResult()).longValue()>0)return;
  LocalDate hoy=LocalDate.now(reloj.withZone(zonasNegocio.de(venta.getEmpresaId(), venta.getSucursalId())));
  var vigentes=reglas.findByEmpresaIdOrderByPrioridadDescNombreAsc(venta.getEmpresaId()).stream().filter(r->r.isActiva()&&(r.getFechaInicio()==null||!hoy.isBefore(r.getFechaInicio()))&&(r.getFechaFin()==null||!hoy.isAfter(r.getFechaFin()))).toList();
  for(var detalle:detalles){
   var personas=new HashSet<Long>();
   for(var regla:vigentes){
    if(!regla.getConcepto().equals(detalle.getTipoConcepto()))continue;
    Long persona="PRESTADOR".equals(regla.getBeneficiario())?(detalle.getCitaId()!=null?citas.findByIdAndEmpresaId(detalle.getCitaId(),venta.getEmpresaId()).map(c->c.getPrestadorId()).orElse(null):detalle.getPrestadorResponsableId()):null;
    if(persona==null||personas.contains(persona)||(regla.getPrestadorId()!=null&&!persona.equals(regla.getPrestadorId())))continue;
    if("ESPECIFICO".equals(regla.getAlcance())&&!Objects.equals("PRODUCTO".equals(regla.getConcepto())?regla.getProductoId():regla.getServicioId(),"PRODUCTO".equals(regla.getConcepto())?detalle.getProductoId():detalle.getServicioId()))continue;
    if("CATEGORIA".equals(regla.getAlcance())&&(detalle.getProductoId()==null||!productos.findByIdAndEmpresaId(detalle.getProductoId(),venta.getEmpresaId()).map(p->Objects.equals(p.getCategoriaId(),regla.getCategoriaProductoId())).orElse(false)))continue;
    if("MONTO_FIJO".equals(regla.getTipoCalculo())&&!venta.getMoneda().equalsIgnoreCase(regla.getMoneda()))continue;
    if("MONTO_FIJO".equals(regla.getTipoCalculo())&&detalle.getCitaId()!=null&&((Number)em.createNativeQuery("select count(*) from comision_devengada c join venta_detalle d on d.id=c.detalle_id and d.empresa_id=c.empresa_id where c.empresa_id=:empresa and c.regla_id=:regla and c.usuario_id=:persona and d.cita_id=:cita").setParameter("empresa",venta.getEmpresaId()).setParameter("regla",regla.getId()).setParameter("persona",persona).setParameter("cita",detalle.getCitaId()).getSingleResult()).longValue()>0)continue;
    BigDecimal base=switch(regla.getBaseCalculo()){case "BRUTO"->detalle.getPrecioUnitario().multiply(detalle.getCantidad());case "NETO_DESCUENTOS"->detalle.getTotal().subtract(detalle.getImpuestos());case "MARGEN"->detalle.getCostoUnitarioHistorico()==null?null:detalle.getTotal().subtract(detalle.getImpuestos()).subtract(detalle.getCostoUnitarioHistorico().multiply(detalle.getCantidad())).max(BigDecimal.ZERO);default->null;};
    BigDecimal importe=base==null?null:"PORCENTAJE".equals(regla.getTipoCalculo())?base.multiply(regla.getValor()).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP):regla.getValor().multiply(detalle.getCantidad()).setScale(2,RoundingMode.HALF_UP);
    em.createNativeQuery("insert into comision_devengada (empresa_id,venta_id,detalle_id,regla_id,usuario_id,moneda,base_calculo,tipo_calculo,valor,base_importe,importe,creada_en) values (:empresa,:venta,:detalle,:regla,:persona,:moneda,:base,:tipo,:valor,:baseImporte,:importe,:fecha)").setParameter("empresa",venta.getEmpresaId()).setParameter("venta",venta.getId()).setParameter("detalle",detalle.getId()).setParameter("regla",regla.getId()).setParameter("persona",persona).setParameter("moneda",venta.getMoneda()).setParameter("base",regla.getBaseCalculo()).setParameter("tipo",regla.getTipoCalculo()).setParameter("valor",regla.getValor()).setParameter("baseImporte",base).setParameter("importe",importe).setParameter("fecha",LocalDateTime.now(reloj)).executeUpdate();
    personas.add(persona);
   }
  }
 }
 public record Ganancia(Long usuarioId,String moneda,BigDecimal importe,long pendientes){}
 @Transactional(readOnly=true) public List<Ganancia> resumen(Long empresa){
  @SuppressWarnings("unchecked") List<Object[]> filas=em.createNativeQuery("select c.usuario_id,c.moneda,coalesce(sum(c.importe),0),sum(case when c.importe is null then 1 else 0 end) from comision_devengada c join venta v on v.id=c.venta_id and v.empresa_id=c.empresa_id where c.empresa_id=:empresa and v.estado='PAGADA' group by c.usuario_id,c.moneda").setParameter("empresa",empresa).getResultList();
  var resultado=new ArrayList<>(filas.stream().map(f->new Ganancia(((Number)f[0]).longValue(),(String)f[1],(BigDecimal)f[2],((Number)f[3]).longValue())).toList());
  var catalogo=catalogos.catalogo(empresa);
  for(var persona:catalogo.prestadores()) if(resultado.stream().noneMatch(g->g.usuarioId().equals(persona.id()))) resultado.add(new Ganancia(persona.id(),catalogo.monedaPredeterminada(),BigDecimal.ZERO.setScale(2),0));
  return resultado;
 }
}
