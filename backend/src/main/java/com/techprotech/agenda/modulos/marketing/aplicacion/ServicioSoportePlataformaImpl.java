package com.techprotech.agenda.modulos.marketing.aplicacion;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.marketing.api.dto.*;
import com.techprotech.agenda.modulos.marketing.infraestructura.entidad.ContactoPlataformaEntidad;
import com.techprotech.agenda.modulos.marketing.infraestructura.repositorio.ContactoPlataformaRepositorio;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioAuditoriaPlataforma;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@Service
public class ServicioSoportePlataformaImpl implements ServicioSoportePlataforma {
    private static final byte ID=1;
    private static final String CONFLICTO="La configuración de contacto cambió. Actualiza antes de guardar";
    private final ContactoPlataformaRepositorio repo;
    private final ObjectMapper json;
    private final ServicioAuditoriaPlataforma auditoria;
    private final Clock clock;
    public ServicioSoportePlataformaImpl(ContactoPlataformaRepositorio repo,ObjectMapper json,ServicioAuditoriaPlataforma auditoria,Clock clock) {
        this.repo=repo;this.json=json;this.auditoria=auditoria;this.clock=clock;
    }
    @Transactional(readOnly=true) public SoportePlataformaResponse obtener(){return dto(fila());}
    @Transactional(readOnly=true) public SoportePublicoResponse publico(){
        var e=fila();if(!e.isVisible())throw noDisponible();
        return new SoportePublicoResponse(e.getCorreo(),e.getTelefono(),dias(e),e.getHoraInicio(),e.getHoraFin(),e.getZonaHoraria());
    }
    @Transactional(readOnly=true) public ContactoLegalResponse contactoLegal(){return new ContactoLegalResponse(fila().getCorreoLegal());}
    @Transactional public SoportePlataformaResponse guardar(SoportePlataformaSolicitud s,Long actor){
        String correo=correo(s.correo()),legal=correo(s.correoLegal());
        String telefono=s.telefono()==null?"":s.telefono().replaceAll("[\\s().-]","");
        if(!telefono.matches("\\+[1-9]\\d{7,14}"))throw invalido("Incluye el código internacional del teléfono, comenzando con +");
        if(s.dias()==null||s.dias().isEmpty()||s.dias().size()>7||s.dias().stream().anyMatch(d->d==null||d<1||d>7))throw invalido("Selecciona al menos un día válido");
        if(s.horaInicio()==null||s.horaFin()==null||!s.horaFin().isAfter(s.horaInicio())||s.horaInicio().getSecond()!=0||s.horaFin().getSecond()!=0||s.horaInicio().getNano()!=0||s.horaFin().getNano()!=0)throw invalido("El horario debe usar horas y minutos; el fin debe ser posterior al inicio");
        if(s.zonaHoraria()==null||!ZoneId.getAvailableZoneIds().contains(s.zonaHoraria()))throw invalido("Selecciona una zona horaria IANA válida");
        var e=repo.findById(ID).orElseGet(ContactoPlataformaEntidad::new);
        if(e.getId()!=null&&!Objects.equals(s.version(),e.getVersion())||e.getId()==null&&s.version()!=null)throw conflicto();
        var anterior=e.getId()==null?null:dto(e);
        e.setId(ID);e.setCorreo(correo);e.setCorreoLegal(legal);e.setTelefono(telefono);
        try{e.setDias(json.writeValueAsString(s.dias().stream().distinct().sorted().toList()));}catch(Exception ex){throw new IllegalStateException("No se pudo guardar el horario",ex);}
        e.setHoraInicio(s.horaInicio());e.setHoraFin(s.horaFin());e.setZonaHoraria(s.zonaHoraria());e.setVisible(s.visible());
        e.setActualizadoEn(LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC));e.setActualizadoPor(actor);
        try{e=repo.saveAndFlush(e);}catch(ObjectOptimisticLockingFailureException|DataIntegrityViolationException ex){throw conflicto();}
        var nuevo=dto(e);var detalle=new LinkedHashMap<String,Object>();detalle.put("anterior",anterior);detalle.put("nuevo",nuevo);
        auditoria.registrar(actor,"SOPORTE_ACTUALIZAR","SOPORTE",Byte.toString(ID),detalle);
        return nuevo;
    }
    private ContactoPlataformaEntidad fila(){return repo.findById(ID).orElseThrow(this::noDisponible);}
    private ResponseStatusException noDisponible(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Contacto no disponible");}
    private ResponseStatusException conflicto(){return new ResponseStatusException(HttpStatus.CONFLICT,CONFLICTO);}
    private ResponseStatusException invalido(String mensaje){return new ResponseStatusException(HttpStatus.BAD_REQUEST,mensaje);}
    private String correo(String valor){
        String correo=valor==null?"":valor.trim().toLowerCase(Locale.ROOT);
        if(correo.length()>150||!correo.matches("^[a-z0-9._+-]+@[a-z0-9-]+(\\.[a-z0-9-]+)*\\.[a-z]{2,}$"))throw invalido("Ingresa un correo válido sin parámetros adicionales");
        return correo;
    }
    private List<Integer> dias(ContactoPlataformaEntidad e){try{return json.readValue(e.getDias(),new TypeReference<List<Integer>>(){});}catch(Exception ex){throw new IllegalStateException("Horario almacenado inválido",ex);}}
    private SoportePlataformaResponse dto(ContactoPlataformaEntidad e){return new SoportePlataformaResponse(e.getVersion(),e.getCorreo(),e.getCorreoLegal(),e.getTelefono(),dias(e),e.getHoraInicio(),e.getHoraFin(),e.getZonaHoraria(),e.isVisible(),e.getActualizadoEn().atOffset(ZoneOffset.UTC),e.getActualizadoPor());}
}
