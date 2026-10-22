package com.techprotech.agenda.modulos.dominios.aplicacion;
import com.techprotech.agenda.modulos.dominios.infraestructura.entidad.EstadoDominio;
import com.techprotech.agenda.modulos.dominios.infraestructura.repositorio.EmpresaDominioRepositorio;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioCapacidadesEmpresa;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class ServicioConsultaDominios {
 private final EmpresaDominioRepositorio dominios;
 private final ServicioCapacidadesEmpresa capacidades;
 private final ValidadorHostname hosts;
 public ServicioConsultaDominios(EmpresaDominioRepositorio d,ServicioCapacidadesEmpresa c,ValidadorHostname h){dominios=d;capacidades=c;hosts=h;}
 public boolean incluido(Long empresaId){var c=capacidades.obtener(empresaId);return c.suscripcionVigente()&&c.modulos().stream().anyMatch(m->m.codigo().equals("DOMINIO_PROPIO"));}
 @Transactional(readOnly=true) public Long empresaActiva(String hostname){
  String host;
  try{host=hosts.normalizar(hostname);}catch(ResponseStatusException e){throw noExiste();}
  var d=dominios.findByHostnameAndEstado(host,EstadoDominio.ACTIVO).orElseThrow(ServicioConsultaDominios::noExiste);
  if(!incluido(d.getEmpresaId()))throw noExiste();
  return d.getEmpresaId();
 }
 @Transactional(readOnly=true) public String dominioPublico(Long empresaId,String derivado){
  if(derivado==null||!incluido(empresaId))return null;
  return dominios.findByHostnameAndEstado(derivado,EstadoDominio.ACTIVO).filter(d->d.getEmpresaId().equals(empresaId)).map(d->d.getHostname()).orElse(null);
 }
 private static ResponseStatusException noExiste(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"No existe un sitio publicado para ese dominio");}
}
