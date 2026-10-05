package com.techprotech.agenda.modulos.plataforma.aplicacion;

import com.techprotech.agenda.modulos.plataforma.api.dto.*;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class ServicioCapacidadesEmpresa {
    private static final List<String> ESTADOS_CON_ACCESO=List.of("ACTIVA","PRUEBA","GRACIA");
    private final SuscripcionPlataformaRepositorio suscripciones;
    private final PlanPlataformaRepositorio planes;
    private final PlanModuloPlataformaRepositorio planModulos;
    private final PlanLimitePlataformaRepositorio planLimites;
    private final SeccionAdminPlataformaRepositorio seccionesAdmin;

    public ServicioCapacidadesEmpresa(SuscripcionPlataformaRepositorio suscripciones,PlanPlataformaRepositorio planes,
            PlanModuloPlataformaRepositorio planModulos,PlanLimitePlataformaRepositorio planLimites,
            SeccionAdminPlataformaRepositorio seccionesAdmin) {
        this.suscripciones=suscripciones;this.planes=planes;this.planModulos=planModulos;
        this.planLimites=planLimites;this.seccionesAdmin=seccionesAdmin;
    }

    @Transactional(readOnly=true)
    public CapacidadesEmpresaResponse obtener(Long empresaId) {
        LocalDateTime ahora=LocalDateTime.now(ZoneOffset.UTC);
        SuscripcionPlataformaEntidad suscripcion=suscripciones
                .findByEmpresaIdAndEstadoInOrderByCreadaEnDesc(empresaId,ESTADOS_CON_ACCESO).stream()
                .filter(s->estaVigente(s,ahora)).findFirst().orElse(null);
        if(suscripcion==null)return CapacidadesEmpresaResponse.sinSuscripcion(seccionesAdmin.findCodigosSinModulo());
        PlanPlataformaEntidad plan=planes.findById(suscripcion.getPlanId()).orElse(null);
        if(plan==null)return CapacidadesEmpresaResponse.sinSuscripcion(seccionesAdmin.findCodigosSinModulo());

        List<CapacidadModuloEmpresaResponse> capacidades=planModulos.findCapacidadesByPlanId(plan.getId());
        List<CapacidadLimiteEmpresaResponse> cotas=planLimites.findCapacidadesByPlanId(plan.getId());
        List<String> secciones=seccionesAdmin.findCodigosDisponiblesByPlanId(plan.getId());

        return new CapacidadesEmpresaResponse(true,suscripcion.getId(),plan.getId(),plan.getCodigo(),plan.getNombre(),
                suscripcion.getEstado(),suscripcion.getPruebaHasta(),suscripcion.getPeriodoActualHasta(),capacidades,cotas,secciones);
    }

    @Transactional(readOnly=true)
    public void exigirModulo(Long empresaId,String codigo) {
        CapacidadesEmpresaResponse capacidades=obtener(empresaId);
        if(!capacidades.suscripcionVigente())
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,"El negocio no tiene una suscripción vigente");
        boolean incluido=capacidades.modulos().stream().anyMatch(m->m.codigo().equalsIgnoreCase(codigo));
        if(!incluido)
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,"El plan actual no incluye el módulo solicitado");
    }

    @Transactional(readOnly=true)
    public void exigirLimite(Long empresaId,String codigo,long usoActual) {
        CapacidadesEmpresaResponse capacidades=obtener(empresaId);
        if(!capacidades.suscripcionVigente())
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,"El negocio no tiene una suscripción vigente");
        capacidades.limites().stream()
                .filter(limite->limite.codigo().equalsIgnoreCase(codigo))
                .findFirst()
                .ifPresent(limite->{
                    if(usoActual>=limite.valor())
                        throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                                "El plan alcanzó el límite de "+limite.valor()+" "+limite.unidad());
                });
    }

    private boolean estaVigente(SuscripcionPlataformaEntidad suscripcion,LocalDateTime ahora) {
        if(suscripcion.getTerminaEn()!=null&&!suscripcion.getTerminaEn().isAfter(ahora))return false;
        return !"PRUEBA".equals(suscripcion.getEstado())
                ||(suscripcion.getPruebaHasta()!=null&&suscripcion.getPruebaHasta().isAfter(ahora));
    }
}
