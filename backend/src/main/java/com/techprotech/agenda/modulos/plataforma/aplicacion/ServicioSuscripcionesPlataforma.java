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
    private final LimitePlataformaRepositorio limites;
    private final PlanLimitePlataformaRepositorio planLimites;
    private final SuscripcionPlataformaRepositorio suscripciones;
    private final EmpresaRepositorio empresas;
    private final ServicioAuditoriaPlataforma auditoria;

    public ServicioSuscripcionesPlataforma(ModuloPlataformaRepositorio modulos, PlanPlataformaRepositorio planes,
            PlanModuloPlataformaRepositorio planModulos, LimitePlataformaRepositorio limites,
            PlanLimitePlataformaRepositorio planLimites, SuscripcionPlataformaRepositorio suscripciones,
            EmpresaRepositorio empresas, ServicioAuditoriaPlataforma auditoria) {
        this.modulos=modulos; this.planes=planes; this.planModulos=planModulos; this.limites=limites; this.planLimites=planLimites;
        this.suscripciones=suscripciones; this.empresas=empresas; this.auditoria=auditoria;
    }

    @Transactional(readOnly=true)
    public List<ModuloPlataformaResponse> modulos() {
        return modulos.findByActivoTrueOrderByOrdenAscNombreAsc().stream()
                .map(m->new ModuloPlataformaResponse(m.getId(),m.getCodigo(),m.getNombre(),m.getDescripcion(),m.getOrden())).toList();
    }

    @Transactional(readOnly=true)
    public List<LimitePlataformaResponse> limites() {
        return limites.findByActivoTrueOrderByOrdenAscNombreAsc().stream()
                .map(l->new LimitePlataformaResponse(l.getId(),l.getCodigo(),l.getNombre(),l.getDescripcion(),l.getUnidad(),l.getOrden())).toList();
    }

    @Transactional(readOnly=true)
    public List<PlanPlataformaResponse> planes() {
        List<PlanPlataformaEntidad> lista=planes.findAllByOrderByActivoDescNombreAsc();
        Map<Long,ModuloPlataformaEntidad> catalogo=modulos.findAll().stream()
                .collect(Collectors.toMap(ModuloPlataformaEntidad::getId,Function.identity()));
        return lista.stream().map(p->respuesta(p,catalogo)).toList();
    }

    @Transactional(readOnly=true)
    public List<PlanPlataformaResponse> planesPublicos() {
        LocalDateTime ahora=LocalDateTime.now(ZoneOffset.UTC);
        Map<Long,ModuloPlataformaEntidad> catalogo=mapaModulos();
        return planes.findAllByOrderByActivoDescNombreAsc().stream()
                .filter(PlanPlataformaEntidad::isActivo).filter(PlanPlataformaEntidad::isPublico)
                .filter(p->p.getVigenteDesde()==null||!p.getVigenteDesde().isAfter(ahora))
                .filter(p->p.getVigenteHasta()==null||p.getVigenteHasta().isAfter(ahora))
                .sorted(Comparator.comparing(PlanPlataformaEntidad::isDestacado).reversed().thenComparing(PlanPlataformaEntidad::getPrecioMensual))
                .map(p->respuesta(p,catalogo)).toList();
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
        PlanPlataformaEntidad plan=planes.findById(s.planId()).filter(this::estaVigente)
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

    @Transactional
    public SuscripcionPlataformaEntidad crearSuscripcionRegistro(Long empresaId,Long planId) {
        PlanPlataformaEntidad plan=planes.findById(planId)
                .filter(PlanPlataformaEntidad::isPublico).filter(this::estaVigente)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"El plan seleccionado no está disponible"));
        LocalDateTime ahora=LocalDateTime.now(ZoneOffset.UTC);
        SuscripcionPlataformaEntidad nueva=new SuscripcionPlataformaEntidad();
        boolean ofrecePrueba=plan.getDiasPrueba()>0;
        nueva.setEmpresaId(empresaId); nueva.setPlanId(plan.getId()); nueva.setEstado(ofrecePrueba?"PRUEBA":"PENDIENTE_PAGO"); nueva.setCiclo("MENSUAL");
        nueva.setPrecioAcordado(ofrecePrueba?BigDecimal.ZERO:plan.getPrecioMensual()); nueva.setMoneda(plan.getMoneda()); nueva.setIniciaEn(ahora);
        nueva.setPruebaHasta(ofrecePrueba?ahora.plusDays(plan.getDiasPrueba()):null); nueva.setReferenciaExterna("ONBOARDING");
        return suscripciones.save(nueva);
    }

    private PlanPlataformaEntidad guardarPlan(PlanPlataformaEntidad plan,GuardarPlanPlataformaRequest s,String codigo) {
        if(s.vigenteHasta()!=null&&s.vigenteDesde()!=null&&!s.vigenteHasta().isAfter(s.vigenteDesde()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La vigencia final debe ser posterior al inicio");
        Set<Long> ids=s.modulos().stream().map(ModuloPlanRequest::moduloId).collect(Collectors.toSet());
        if(ids.size()!=s.modulos().size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No repitas módulos dentro del plan");
        Map<Long,ModuloPlataformaEntidad> disponibles=modulos.findAllById(ids).stream().filter(ModuloPlataformaEntidad::isActivo)
                .collect(Collectors.toMap(ModuloPlataformaEntidad::getId,Function.identity()));
        if(disponibles.size()!=ids.size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Uno de los módulos no está disponible");
        Set<Long> idsLimites=s.limites().stream().map(LimitePlanRequest::limiteId).collect(Collectors.toSet());
        if(idsLimites.size()!=s.limites().size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No repitas límites dentro del plan");
        long limitesDisponibles=limites.findAllById(idsLimites).stream().filter(LimitePlataformaEntidad::isActivo).count();
        if(limitesDisponibles!=idsLimites.size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Uno de los límites no está disponible");
        plan.setCodigo(codigo); plan.setNombre(s.nombre().trim()); plan.setDescripcion(textoOpcional(s.descripcion()));
        plan.setMoneda(normalizarMoneda(s.moneda())); plan.setPrecioMensual(s.precioMensual());
        plan.setPrecioAnual(s.precioAnual()); plan.setActivo(s.activo()); plan.setPublico(s.publico()); plan.setDestacado(s.destacado());
        plan.setDiasPrueba(s.diasPrueba()); plan.setDiasGracia(s.diasGracia()); plan.setVigenteDesde(s.vigenteDesde()); plan.setVigenteHasta(s.vigenteHasta());
        plan=planes.save(plan);
        planModulos.deleteByPlanId(plan.getId());
        planLimites.deleteByPlanId(plan.getId());
        Long planId=plan.getId();
        planModulos.saveAll(s.modulos().stream().map(m->{PlanModuloPlataformaEntidad relacion=new PlanModuloPlataformaEntidad();
            relacion.setPlanId(planId); relacion.setModuloId(m.moduloId()); relacion.setLimiteUso(m.limiteUso()); return relacion;}).toList());
        planLimites.saveAll(s.limites().stream().map(l->{PlanLimitePlataformaEntidad relacion=new PlanLimitePlataformaEntidad();
            relacion.setPlanId(planId); relacion.setLimiteId(l.limiteId()); relacion.setValor(l.valor()); return relacion;}).toList());
        return plan;
    }

    private PlanPlataformaResponse respuesta(PlanPlataformaEntidad p,Map<Long,ModuloPlataformaEntidad> catalogo) {
        List<ModuloPlanResponse> incluidos=planModulos.findByPlanId(p.getId()).stream().map(pm->{ModuloPlataformaEntidad m=catalogo.get(pm.getModuloId());
            return new ModuloPlanResponse(pm.getModuloId(),m==null?null:m.getCodigo(),m==null?null:m.getNombre(),pm.getLimiteUso());}).toList();
        Map<Long,LimitePlataformaEntidad> catalogoLimites=limites.findAll().stream().collect(Collectors.toMap(LimitePlataformaEntidad::getId,Function.identity()));
        List<LimitePlanResponse> cotas=planLimites.findByPlanId(p.getId()).stream().map(pl->{LimitePlataformaEntidad l=catalogoLimites.get(pl.getLimiteId());
            return new LimitePlanResponse(pl.getLimiteId(),l==null?null:l.getCodigo(),l==null?null:l.getNombre(),l==null?null:l.getUnidad(),pl.getValor());}).toList();
        return new PlanPlataformaResponse(p.getId(),p.getCodigo(),p.getNombre(),p.getDescripcion(),p.getMoneda(),
                p.getPrecioMensual(),p.getPrecioAnual(),p.isActivo(),p.isPublico(),p.isDestacado(),p.getDiasPrueba(),p.getDiasGracia(),
                p.getVigenteDesde(),p.getVigenteHasta(),incluidos,cotas);
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
    private boolean estaVigente(PlanPlataformaEntidad plan) {
        if(!plan.isActivo()) return false;
        LocalDateTime ahora=LocalDateTime.now(ZoneOffset.UTC);
        return (plan.getVigenteDesde()==null||!plan.getVigenteDesde().isAfter(ahora))
                &&(plan.getVigenteHasta()==null||plan.getVigenteHasta().isAfter(ahora));
    }
    private Map<Long,ModuloPlataformaEntidad> mapaModulos(){return modulos.findAll().stream().collect(Collectors.toMap(ModuloPlataformaEntidad::getId,Function.identity()));}
    private static String normalizarCodigo(String valor){return valor.trim().toUpperCase(Locale.ROOT);}
    private static String normalizarMoneda(String valor){return valor.trim().toUpperCase(Locale.ROOT);}
    private static String textoOpcional(String valor){return valor==null||valor.isBlank()?null:valor.trim();}
}
