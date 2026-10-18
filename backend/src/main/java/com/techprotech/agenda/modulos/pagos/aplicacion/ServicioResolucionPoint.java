package com.techprotech.agenda.modulos.pagos.aplicacion;
import com.techprotech.agenda.modulos.pagos.api.dto.*;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.OrdenPointCajaRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago.ClienteMercadoPagoPoint;
import com.techprotech.agenda.modulos.caja.aplicacion.ServicioVentasCaja;
import com.techprotech.agenda.modulos.admin.infraestructura.entidad.AuditoriaConfiguracionEmpresaEntidad;
import com.techprotech.agenda.modulos.admin.infraestructura.repositorio.AuditoriaConfiguracionEmpresaRepositorio;
import com.techprotech.agenda.modulos.pagos.seguridad.PermisosPagos;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;

/** Requiere verificación humana. Nunca supone que un timeout equivale a no haber cobrado. */
@Service
public class ServicioResolucionPoint {
 private final ServicioMercadoPagoPoint point;private final ClienteMercadoPagoPoint cliente;
 private final OrdenPointCajaRepositorio ordenes;private final ServicioVentasCaja ventas;
 private final ServicioConciliacionPagos conciliacion;private final AuditoriaConfiguracionEmpresaRepositorio auditoria;
 private final Clock reloj;private final com.fasterxml.jackson.databind.ObjectMapper mapper;private final TransactionTemplate tx;
 public ServicioResolucionPoint(ServicioMercadoPagoPoint point,ClienteMercadoPagoPoint cliente,OrdenPointCajaRepositorio ordenes,
     ServicioVentasCaja ventas,ServicioConciliacionPagos conciliacion,AuditoriaConfiguracionEmpresaRepositorio auditoria,Clock reloj,
     com.fasterxml.jackson.databind.ObjectMapper mapper,org.springframework.transaction.PlatformTransactionManager gestor) {
  this.point=point;this.cliente=cliente;this.ordenes=ordenes;this.ventas=ventas;this.conciliacion=conciliacion;this.auditoria=auditoria;this.reloj=reloj;this.mapper=mapper;
  this.tx=new TransactionTemplate(gestor);tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
 }
 @Transactional(propagation=Propagation.NOT_SUPPORTED)
 public OrdenPointResponse resolver(UsuarioAutenticado usuario,String publicId,ResolverOrdenPointRequest request) {
  if(usuario.esPlataforma() || !usuario.tienePermiso(PermisosPagos.CONFIGURAR)) throw error(HttpStatus.FORBIDDEN,"No tienes permiso para resolver este cobro");
  if(request.cobrado()==null || request.motivo()==null || request.motivo().trim().length()<10 || request.motivo().length()>500) throw error(HttpStatus.BAD_REQUEST,"Documenta cómo verificaste el cobro (10 a 500 caracteres)");
  var orden=point.buscarConAlcance(usuario.empresaId(),usuario.sucursalesPermitidas(),publicId);validarEstado(orden.getEstado(),orden.getProveedorOrdenId());
  ClienteMercadoPagoPoint.OrdenRemota remota;
  if(request.cobrado()) {
   if(request.proveedorOrdenId()==null || request.proveedorOrdenId().isBlank() || request.proveedorOrdenId().length()>180 || !request.proveedorOrdenId().trim().matches("[A-Za-z0-9_-]+")) throw error(HttpStatus.BAD_REQUEST,"Indica la orden de Mercado Pago que verificaste");
   remota=cliente.consultar(usuario.empresaId(),request.proveedorOrdenId().trim());
   if(!request.proveedorOrdenId().trim().equals(remota.ordenId()) || !("point_"+orden.getPublicId().replace("-","")).equals(remota.externalReference()) || remota.importe()==null || remota.importe().compareTo(orden.getImporte())!=0
       || remota.moneda()==null || !remota.moneda().equalsIgnoreCase(orden.getMoneda())) throw error(HttpStatus.CONFLICT,"La orden de Mercado Pago no corresponde a este cobro o su importe");
   var otra=ordenes.findByProveedorOrdenId(remota.ordenId());
   if(otra.isPresent() && !otra.get().getPublicId().equals(publicId)) throw error(HttpStatus.CONFLICT,"La orden del proveedor ya está vinculada a otro cobro");
  }else remota=null;
  return tx.execute(status -> {
   var actual=ordenes.bloquear(usuario.empresaId(),publicId).orElseThrow(() -> error(HttpStatus.NOT_FOUND,"Orden no encontrada"));validarEstado(actual.getEstado(),actual.getProveedorOrdenId());
   String antes=actual.getEstado();
   if(remota==null){actual.setEstado("CANCELADO");actual.setDetalleEstado("No cobrado: verificado por el negocio");ventas.cancelarPoint(usuario.empresaId(),actual.getVentaId(),request.motivo().trim());}
   else{point.aplicarEstado(actual,remota);conciliacion.registrarPoint(actual,remota);}
   ordenes.save(actual);
   var registro=new AuditoriaConfiguracionEmpresaEntidad();registro.setEmpresaId(usuario.empresaId());registro.setUsuarioActorId(usuario.usuarioId());registro.setActorCorreo(usuario.correo());registro.setModulo("PAGOS");registro.setAccion("RESOLVER_POINT");registro.setResumen("Resolución manual de orden Point "+publicId);
   try {registro.setDetalleAntesJson(mapper.writeValueAsString(Map.of("estado",antes,"orden",publicId)));
    registro.setDetalleDespuesJson(mapper.writeValueAsString(Map.of("estado",actual.getEstado(),"motivo",request.motivo().trim(),"momento",reloj.instant().toString(),"cobrado",request.cobrado(),"estadoRemoto",remota==null?"NO_COBRADO_VERIFICADO":String.valueOf(remota.estado()))));
   }catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("No se pudo auditar la resolución",e);}
   auditoria.save(registro);return point.mapear(actual).conAcciones(usuario);
  });
 }
 private void validarEstado(String estado,String remoto){if(remoto!=null || !"REQUIERE_REVISION".equals(estado)) throw error(HttpStatus.CONFLICT,"Esta orden ya no necesita resolución manual");}
 private ResponseStatusException error(HttpStatus estado,String mensaje){return new ResponseStatusException(estado,mensaje);}
}
