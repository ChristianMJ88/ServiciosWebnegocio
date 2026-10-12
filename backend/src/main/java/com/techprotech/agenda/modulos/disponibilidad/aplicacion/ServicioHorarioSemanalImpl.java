package com.techprotech.agenda.modulos.disponibilidad.aplicacion;
import com.techprotech.agenda.modulos.disponibilidad.api.dto.*;
import com.techprotech.agenda.modulos.disponibilidad.infraestructura.entidad.ReglaDisponibilidadEntidad;
import com.techprotech.agenda.modulos.disponibilidad.infraestructura.repositorio.ReglaDisponibilidadRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
import java.time.LocalDate;
@Service
public class ServicioHorarioSemanalImpl implements ServicioHorarioSemanal {
 private final ServicioGestionDisponibilidad gestion;
 private final ReglaDisponibilidadRepositorio reglas;
 public ServicioHorarioSemanalImpl(ServicioGestionDisponibilidad gestion,ReglaDisponibilidadRepositorio reglas){this.gestion=gestion;this.reglas=reglas;}
 @Transactional
 public List<ReglaDisponibilidadResponse> guardarStaff(Long empresaId,Long usuarioId,HorarioSemanalRequest request){
  gestion.exigirSolicitudSiHayHorario(empresaId,usuarioId);
  var horario=request.horario();
  var propio=new ReglaDisponibilidadRequest("PRESTADOR",usuarioId,horario.diaSemana(),horario.horaInicio(),
   horario.horaFin(),horario.intervaloMinutos(),horario.vigenteDesde(),horario.vigenteHasta());
  return guardar(empresaId,new HorarioSemanalRequest(propio,request.diasSemana(),request.reemplazar()));
 }
 @Transactional
 public List<ReglaDisponibilidadResponse> guardar(Long empresaId,HorarioSemanalRequest request){
  var horario=request.horario();
  String tipo=gestion.validarReglaAdmin(empresaId,horario);
  var dias=new TreeSet<>(request.diasSemana());
  var existentes=reglas.findByEmpresaIdAndTipoSujetoAndSujetoIdOrderByDiaSemanaAscHoraInicioAsc(empresaId,tipo,horario.sujetoId());
  var reemplazables=existentes.stream().filter(r->dias.contains(r.getDiaSemana())
   &&Objects.equals(r.getVigenteDesde(),horario.vigenteDesde())&&Objects.equals(r.getVigenteHasta(),horario.vigenteHasta())).toList();
  for(var regla:existentes){
   if(!dias.contains(regla.getDiaSemana())||(request.reemplazar()&&reemplazables.contains(regla)))continue;
   if(coincide(regla,horario))continue;
   if(fechaAntesOIgual(regla.getVigenteDesde(),horario.vigenteHasta())&&fechaAntesOIgual(horario.vigenteDesde(),regla.getVigenteHasta())
    &&regla.getHoraInicio().isBefore(horario.horaFin())&&horario.horaInicio().isBefore(regla.getHoraFin()))
    throw new ResponseStatusException(HttpStatus.CONFLICT,"El horario se cruza con una regla existente. Revisa el día "+regla.getDiaSemana()+" y su vigencia.");
  }
  if(request.reemplazar())reglas.deleteAll(reemplazables);
  var resultado=new ArrayList<ReglaDisponibilidadResponse>();
  for(int dia:dias){
   if(!request.reemplazar()&&existentes.stream().anyMatch(r->r.getDiaSemana()==dia&&coincide(r,horario)))continue;
   resultado.add(gestion.crearReglaAdmin(empresaId,new ReglaDisponibilidadRequest(tipo,horario.sujetoId(),dia,
    horario.horaInicio(),horario.horaFin(),horario.intervaloMinutos(),horario.vigenteDesde(),horario.vigenteHasta())));
  }
  return resultado;
 }
 private boolean fechaAntesOIgual(LocalDate inicio,LocalDate fin){return inicio==null||fin==null||!inicio.isAfter(fin);}
 private boolean coincide(ReglaDisponibilidadEntidad regla,ReglaDisponibilidadRequest horario){
  return regla.getHoraInicio().equals(horario.horaInicio())&&regla.getHoraFin().equals(horario.horaFin())
   &&regla.getIntervaloMinutos()==horario.intervaloMinutos()&&Objects.equals(regla.getVigenteDesde(),horario.vigenteDesde())
   &&Objects.equals(regla.getVigenteHasta(),horario.vigenteHasta());
 }
}
