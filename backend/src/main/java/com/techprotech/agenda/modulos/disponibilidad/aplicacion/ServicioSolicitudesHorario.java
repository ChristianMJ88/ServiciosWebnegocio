package com.techprotech.agenda.modulos.disponibilidad.aplicacion;
import com.techprotech.agenda.modulos.disponibilidad.api.dto.*;
import com.techprotech.agenda.modulos.disponibilidad.infraestructura.entidad.SolicitudHorarioEntidad;
import com.techprotech.agenda.modulos.disponibilidad.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;
import java.time.LocalDateTime;
import java.util.*;
@Service
public class ServicioSolicitudesHorario {
 private final SolicitudHorarioRepositorio solicitudes;
 private final ReglaDisponibilidadRepositorio reglas;
 private final ServicioGestionDisponibilidad gestion;
 private final ServicioHorarioSemanal semanal;
 private final UsuarioRepositorio usuarios;
 private final PasswordEncoder encoder;
 private final ObjectMapper json;
 public ServicioSolicitudesHorario(SolicitudHorarioRepositorio solicitudes,ReglaDisponibilidadRepositorio reglas,ServicioGestionDisponibilidad gestion,ServicioHorarioSemanal semanal,UsuarioRepositorio usuarios,PasswordEncoder encoder,ObjectMapper json){this.solicitudes=solicitudes;this.reglas=reglas;this.gestion=gestion;this.semanal=semanal;this.usuarios=usuarios;this.encoder=encoder;this.json=json;}
 @Transactional
 public SolicitudHorarioResponse solicitar(Long empresa,Long staff,SolicitudHorarioRequest request){
  var h=request.propuesta().horario();
  var propio=new ReglaDisponibilidadRequest("PRESTADOR",staff,h.diaSemana(),h.horaInicio(),h.horaFin(),h.intervaloMinutos(),h.vigenteDesde(),h.vigenteHasta());
  gestion.validarReglaAdmin(empresa,propio);
  if(request.reglaId()!=null) reglas.findByIdAndEmpresaIdAndTipoSujetoAndSujetoId(request.reglaId(),empresa,"PRESTADOR",staff).orElseThrow(()->new ResponseStatusException(NOT_FOUND,"Horario no encontrado"));
  var e=new SolicitudHorarioEntidad();e.setEmpresaId(empresa);e.setPrestadorId(staff);e.setReglaId(request.reglaId());e.setEstado("PENDIENTE");e.setCreadaEn(LocalDateTime.now());
  try{e.setPropuesta(json.writeValueAsString(new HorarioSemanalRequest(propio,request.propuesta().diasSemana(),request.propuesta().reemplazar())));}catch(Exception ex){throw new IllegalStateException(ex);}
  return respuesta(solicitudes.save(e));
 }
 @Transactional(readOnly=true)
 public List<SolicitudHorarioResponse> pendientes(Long empresa){return solicitudes.findByEmpresaIdAndEstadoOrderByCreadaEnAsc(empresa,"PENDIENTE").stream().map(this::respuesta).toList();}
 @Transactional
 public void resolver(Long empresa,Long admin,Long id,ResolverHorarioRequest request){
  var usuario=usuarios.findByIdAndEmpresaId(admin,empresa).orElseThrow(()->new ResponseStatusException(FORBIDDEN));
  if(!usuario.isHabilitado()||usuario.isBloqueado()||!encoder.matches(request.contrasena(),usuario.getContrasenaHash()))throw new ResponseStatusException(FORBIDDEN,"No se pudo confirmar tu contraseña");
  var e=solicitudes.findByIdAndEmpresaId(id,empresa).orElseThrow(()->new ResponseStatusException(NOT_FOUND));
  if(!"PENDIENTE".equals(e.getEstado()))throw new ResponseStatusException(CONFLICT,"Solicitud ya resuelta");
  var p=respuesta(e).propuesta();
  if(request.aprobar()){
   if(e.getReglaId()!=null){reglas.findByIdAndEmpresaIdAndTipoSujetoAndSujetoId(e.getReglaId(),empresa,"PRESTADOR",e.getPrestadorId()).orElseThrow(()->new ResponseStatusException(CONFLICT,"El horario original ya no existe"));gestion.actualizarReglaAdmin(empresa,e.getReglaId(),p.horario());}
   else semanal.guardar(empresa,p);
  }
  e.setEstado(request.aprobar()?"APROBADA":"RECHAZADA");e.setResueltaEn(LocalDateTime.now());e.setResueltaPor(admin);solicitudes.save(e);
 }
 private SolicitudHorarioResponse respuesta(SolicitudHorarioEntidad e){try{
  var nombre=usuarios.findByIdAndEmpresaId(e.getPrestadorId(),e.getEmpresaId()).map(u -> u.getCorreo()).orElse("Usuario no disponible");
  return new SolicitudHorarioResponse(e.getId(),e.getPrestadorId(),nombre,e.getReglaId(),json.readValue(e.getPropuesta(),HorarioSemanalRequest.class),e.getEstado(),e.getCreadaEn());
 }catch(Exception ex){throw new IllegalStateException(ex);}}
}
