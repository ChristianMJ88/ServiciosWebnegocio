package com.techprotech.agenda.modulos.dominios.aplicacion;
import com.techprotech.agenda.modulos.admin.infraestructura.entidad.AuditoriaConfiguracionEmpresaEntidad;
import com.techprotech.agenda.modulos.admin.infraestructura.repositorio.AuditoriaConfiguracionEmpresaRepositorio;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioAuditoriaPlataforma;
import com.techprotech.agenda.modulos.dominios.infraestructura.entidad.EmpresaDominioEntidad;
import org.springframework.stereotype.Service;
import java.util.Map;
@Service
public class ServicioAuditoriaDominios {
 private final AuditoriaConfiguracionEmpresaRepositorio tenant;private final ServicioAuditoriaPlataforma plataforma;
 public ServicioAuditoriaDominios(AuditoriaConfiguracionEmpresaRepositorio t,ServicioAuditoriaPlataforma p){tenant=t;plataforma=p;}
 public void registrar(EmpresaDominioEntidad d,Long actor,boolean operador,String accion){
  if(operador){plataforma.registrar(actor,accion,"DOMINIO",d.getId().toString(),Map.of("empresaId",d.getEmpresaId(),"hostname",d.getHostname(),"estado",d.getEstado().name()));return;}
  var a=new AuditoriaConfiguracionEmpresaEntidad();a.setEmpresaId(d.getEmpresaId());a.setUsuarioActorId(actor);a.setModulo("DOMINIOS");a.setAccion(accion);a.setResumen(d.getHostname()+": "+d.getEstado().name());tenant.save(a);
 }
}
