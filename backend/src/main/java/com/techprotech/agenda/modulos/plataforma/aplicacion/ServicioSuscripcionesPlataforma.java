package com.techprotech.agenda.modulos.plataforma.aplicacion;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.plataforma.api.dto.*;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServicioSuscripcionesPlataforma {
    private static final List<String> ESTADOS_VIGENTES=List.of("ACTIVA","PRUEBA");
    private final ModuloPlataformaRepositorio modulos;
    private final PlanPlataformaRepositorio planes;
    private final PlanModuloPlataformaRepositorio planModulos;
    private final SuscripcionPlataformaRepositorio suscripciones;
    private final EmpresaRepositorio empresas;
    private final ServicioAuditoriaPlataforma auditoria;

    public ServicioSuscripcionesPlataforma(ModuloPlataformaRepositorio modulos, PlanPlataformaRepositorio planes,
            PlanModuloPlataformaRepositorio planModulos, SuscripcionPlataformaRepositorio suscripciones,
            EmpresaRepositorio empresas, ServicioAuditoriaPlataforma auditoria) {
        this.modulos=modulos; this.planes=planes; this.planModulos=planModulos;
        this.suscripciones=suscripciones; this.empresas=empresas; this.auditoria=auditoria;
    }

    @Transactional(readOnly=true)
    public List<ModuloPlataformaResponse> modulos() {
        return modulos.findByActivoTrueOrderByOrdenAscNombreAsc().stream()
                .map(m->new ModuloPlataformaResponse(m.getId(),m.getCodigo(),m.getNombre(),m.getDescripcion(),m.getOrden())).toList();
    }

    @Transactional(readOnly=true)
    public List<PlanPlataformaResponse> planes() {
        List<PlanPlataformaEntidad> lista=planes.findAllByOrderByActivoDescNombreAsc();
        Map<Long,ModuloPlataformaEntidad> catalogo=modulos.findAll().stream()
                .collect(Collectors.toMap(ModuloPlataformaEntidad::getId,Function.identity()));
        return lista.stream().map(p->respuesta(p,catalogo)).toList();
    }

    @Transactional
    public PlanPlataformaResponse crearPlan(Long usuarioId, GuardarPlanPlataformaRequest s) {
        String codigo=normalizarCodigo(s.codigo());
        if(planes.existsByCodigoIgnoreCase(codigo))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Ya existe un plan con ese código");
        PlanPlataformaEntidad plan=guardarPlan(new PlanPlataformaEntidad(),s,codigo);
        auditoria.registrar(usuarioId,"CREAR","PLAN_SAAS",String.valueOf(plan.getId()),
                Map.of("codigo",plan.getCodigo(),"activo",plan.isActivo(),"modulos",s.modulos().size()));
        return respuesta(plan,mapaModulos());
    }

    @Transactional
    public PlanPlataformaResponse actualizarPlan(Long usuarioId,Long id,GuardarPlanPlataformaRequest s) {
        PlanPlataformaEntidad plan=planes.findById(id)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"El plan no existe"));
        String codigo=normalizarCodigo(s.codigo());
        planes.findByCodigoIgnoreCase(codigo).filter(otro->!otro.getId().equals(id)).ifPresent(otro->{
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Ya existe un plan con ese código");
        });
        guardarPlan(plan,s,codigo);
        auditoria.registrar(usuarioId,"ACTUALIZAR","PLAN_SAAS",String.valueOf(plan.getId()),
                Map.of("codigo",plan.getCodigo(),"activo",plan.isActivo(),"modulos",s.modulos().size()));
        return respuesta(plan,mapaModulos());
    }

    @Transactional(readOnly=true)
    public List<SuscripcionPlataformaResponse> suscripciones() {
        List<SuscripcionPlataformaEntidad> lista=suscripciones.findAllByOrderByCreadaEnDesc();
        Map<Long,EmpresaEntidad> mapaEmpresas=empresas.findAllById(lista.stream().map(SuscripcionPlataformaEntidad::getEmpresaId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(EmpresaEntidad::getId,Function.identity()));
        Map<Long,PlanPlataformaEntidad> mapaPlanes=planes.findAllById(lista.stream().map(SuscripcionPlataformaEntidad::getPlanId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(PlanPlataformaEntidad::getId,Function.identity()));
        return lista.stream().map(s->respuesta(s,mapaEmpresas.get(s.getEmpresaId()),mapaPlanes.get(s.getPlanId()))).toList();
    }

    @Transactional
    public SuscripcionPlataformaResponse asignar(Long usuarioId,AsignarSuscripcionPlataformaRequest s) {
        EmpresaEntidad empresa=empresas.findById(s.empresaId())
                .orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"El negocio seleccionado no existe"));
        PlanPlataformaEntidad plan=planes.findById(s.planId()).filter(PlanPlataformaEntidad::isActivo)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"El plan seleccionado no está activo"));
        validarFechas(s);
        LocalDateTime ahora=LocalDateTime.now(ZoneOffset.UTC);
        List<SuscripcionPlataformaEntidad> anteriores=suscripciones.findByEmpresaIdAndEstadoIn(s.empresaId(),ESTADOS_VIGENTES);
        anteriores.forEach(anterior->{anterior.setEstado("REEMPLAZADA"); anterior.setTerminaEn(ahora);});
        suscripciones.saveAll(anteriores);
        SuscripcionPlataformaEntidad nueva=new SuscripcionPlataformaEntidad();
        nueva.setEmpresaId(empresa.getId()); nueva.setPlanId(plan.getId()); nueva.setEstado(s.estado());
        nueva.setCiclo(s.ciclo()); nueva.setMoneda(normalizarMoneda(s.moneda()==null?plan.getMoneda():s.moneda()));
        BigDecimal precio=s.precioAcordado()!=null?s.precioAcordado():("ANUAL".equals(s.ciclo())?plan.getPrecioAnual():plan.getPrecioMensual());
        if(precio==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El plan no tiene precio para el ciclo seleccionado");
        nueva.setPrecioAcordado(precio); nueva.setIniciaEn(s.iniciaEn()); nueva.setTerminaEn(s.terminaEn());
        nueva.setPruebaHasta(s.pruebaHasta()); nueva=suscripciones.save(nueva);
        auditoria.registrar(usuarioId,"ASIGNAR","SUSCRIPCION_SAAS",String.valueOf(nueva.getId()),
                Map.of("empresaId",empresa.getId(),"planId",plan.getId(),"estado",nueva.getEstado(),"ciclo",nueva.getCiclo()));
        return respuesta(nueva,empresa,plan);
    }

    private PlanPlataformaEntidad guardarPlan(PlanPlataformaEntidad plan,GuardarPlanPlataformaRequest s,String codigo) {
        Set<Long> ids=s.modulos().stream().map(ModuloPlanRequest::moduloId).collect(Collectors.toSet());
        if(ids.size()!=s.modulos().size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No repitas módulos dentro del plan");
        Map<Long,ModuloPlataformaEntidad> disponibles=modulos.findAllById(ids).stream().filter(ModuloPlataformaEntidad::isActivo)
                .collect(Collectors.toMap(ModuloPlataformaEntidad::getId,Function.identity()));
        if(disponibles.size()!=ids.size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Uno de los módulos no está disponible");
        plan.setCodigo(codigo); plan.setNombre(s.nombre().trim()); plan.setDescripcion(textoOpcional(s.descripcion()));
        plan.setMoneda(normalizarMoneda(s.moneda())); plan.setPrecioMensual(s.precioMensual());
        plan.setPrecioAnual(s.precioAnual()); plan.setActivo(s.activo()); plan.setPublico(s.publico()); plan=planes.save(plan);
        planModulos.deleteByPlanId(plan.getId());
        Long planId=plan.getId();
        planModulos.saveAll(s.modulos().stream().map(m->{PlanModuloPlataformaEntidad relacion=new PlanModuloPlataformaEntidad();
            relacion.setPlanId(planId); relacion.setModuloId(m.moduloId()); relacion.setLimiteUso(m.limiteUso()); return relacion;}).toList());
        return plan;
    }

    private PlanPlataformaResponse respuesta(PlanPlataformaEntidad p,Map<Long,ModuloPlataformaEntidad> catalogo) {
        List<ModuloPlanResponse> incluidos=planModulos.findByPlanId(p.getId()).stream().map(pm->{ModuloPlataformaEntidad m=catalogo.get(pm.getModuloId());
            return new ModuloPlanResponse(pm.getModuloId(),m==null?null:m.getCodigo(),m==null?null:m.getNombre(),pm.getLimiteUso());}).toList();
        return new PlanPlataformaResponse(p.getId(),p.getCodigo(),p.getNombre(),p.getDescripcion(),p.getMoneda(),
                p.getPrecioMensual(),p.getPrecioAnual(),p.isActivo(),p.isPublico(),incluidos);
    }
    private SuscripcionPlataformaResponse respuesta(SuscripcionPlataformaEntidad s,EmpresaEntidad e,PlanPlataformaEntidad p) {
        return new SuscripcionPlataformaResponse(s.getId(),s.getEmpresaId(),e==null?null:e.getNombre(),s.getPlanId(),
                p==null?null:p.getNombre(),s.getEstado(),s.getCiclo(),s.getPrecioAcordado(),s.getMoneda(),
                s.getIniciaEn(),s.getTerminaEn(),s.getPruebaHasta(),s.getProveedorPagoId(),s.getReferenciaExterna());
    }
    private void validarFechas(AsignarSuscripcionPlataformaRequest s) {
        if(s.terminaEn()!=null&&!s.terminaEn().isAfter(s.iniciaEn()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La fecha final debe ser posterior al inicio");
        if("PRUEBA".equals(s.estado())&&(s.pruebaHasta()==null||!s.pruebaHasta().isAfter(s.iniciaEn())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Una suscripción de prueba requiere una fecha final de prueba");
    }
    private Map<Long,ModuloPlataformaEntidad> mapaModulos(){return modulos.findAll().stream().collect(Collectors.toMap(ModuloPlataformaEntidad::getId,Function.identity()));}
    private static String normalizarCodigo(String valor){return valor.trim().toUpperCase(Locale.ROOT);}
    private static String normalizarMoneda(String valor){return valor.trim().toUpperCase(Locale.ROOT);}
    private static String textoOpcional(String valor){return valor==null||valor.isBlank()?null:valor.trim();}
}
