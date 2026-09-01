package com.techprotech.agenda.modulos.onboarding.aplicacion;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.admin.infraestructura.repositorio.AuditoriaConfiguracionEmpresaRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.disponibilidad.infraestructura.repositorio.ReglaDisponibilidadRepositorio;
import com.techprotech.agenda.modulos.onboarding.api.dto.EstadoOnboardingResponse;
import com.techprotech.agenda.modulos.onboarding.infraestructura.entidad.EmpresaOnboardingProgresoEntidad;
import com.techprotech.agenda.modulos.onboarding.infraestructura.repositorio.EmpresaOnboardingProgresoRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServicioEstadoOnboarding {
    private final EmpresaOnboardingProgresoRepositorio progresoRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final ServicioVerificacionCorreoOnboarding verificacionCorreo;
    private final ReglaDisponibilidadRepositorio reglas;
    private final ServicioRepositorio servicios;
    private final CitaRepositorio citas;
    private final AuditoriaConfiguracionEmpresaRepositorio auditoriaConfiguracion;

    public ServicioEstadoOnboarding(
            EmpresaOnboardingProgresoRepositorio progresoRepositorio,
            UsuarioRepositorio usuarioRepositorio,
            ServicioVerificacionCorreoOnboarding verificacionCorreo,
            ReglaDisponibilidadRepositorio reglas,
            ServicioRepositorio servicios,
            CitaRepositorio citas,
            AuditoriaConfiguracionEmpresaRepositorio auditoriaConfiguracion
    ) {
        this.progresoRepositorio = progresoRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.verificacionCorreo = verificacionCorreo;
        this.reglas = reglas;
        this.servicios = servicios;
        this.citas = citas;
        this.auditoriaConfiguracion = auditoriaConfiguracion;
    }

    @Transactional
    public EstadoOnboardingResponse obtener(Long empresaId, Long usuarioId) {
        UsuarioEntidad usuario = usuarioRepositorio.findByIdAndEmpresaId(usuarioId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Cuenta no encontrada"));
        EmpresaOnboardingProgresoEntidad progreso = progresoRepositorio.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Onboarding no encontrado"));

        sincronizarConDatosReales(empresaId, progreso);

        List<String> pendientes = new ArrayList<>();
        if (!progreso.isHorarioCompletado()) pendientes.add("CONFIGURAR_HORARIO");
        if (!progreso.isServicioCompletado()) pendientes.add("CREAR_SERVICIO");
        if (!progreso.isPersonalCompletado()) pendientes.add("AGREGAR_PERSONAL");
        if (!progreso.isSitioCompletado()) pendientes.add("PERSONALIZAR_SITIO");
        if (!progreso.isCitaPruebaCompletada()) pendientes.add("CREAR_CITA_PRUEBA");

        return new EstadoOnboardingResponse(
                usuario.getCorreoVerificadoEn() != null,
                usuario.getCorreo(),
                progreso.getCategoria(),
                progreso.getTamanoEquipo(),
                progreso.getPasoRecomendado(),
                5 - pendientes.size(),
                5,
                pendientes
        );
    }

    public void reenviarConfirmacion(Long empresaId, Long usuarioId) {
        verificacionCorreo.reenviar(empresaId, usuarioId);
    }

    private void sincronizarConDatosReales(Long empresaId, EmpresaOnboardingProgresoEntidad progreso) {
        progreso.setHorarioCompletado(reglas.countByEmpresaId(empresaId) > 0);
        progreso.setServicioCompletado(servicios.countByEmpresaId(empresaId) > 0);
        progreso.setPersonalCompletado(usuarioRepositorio.findByEmpresaIdOrderByCorreoAsc(empresaId).size() > 1);
        progreso.setSitioCompletado(auditoriaConfiguracion.existsByEmpresaIdAndAccion(
                empresaId, "CONFIGURACION_SITIO_ACTUALIZADA"));
        progreso.setCitaPruebaCompletada(citas.countByEmpresaId(empresaId) > 0);
        progreso.setPasoRecomendado(resolverPasoRecomendado(progreso));
        progreso.setActualizadoEn(LocalDateTime.now());
        progresoRepositorio.save(progreso);
    }

    private String resolverPasoRecomendado(EmpresaOnboardingProgresoEntidad progreso) {
        if (!progreso.isHorarioCompletado()) return "CONFIGURAR_HORARIO";
        if (!progreso.isServicioCompletado()) return "CREAR_SERVICIO";
        if (!progreso.isPersonalCompletado()) return "AGREGAR_PERSONAL";
        if (!progreso.isSitioCompletado()) return "PERSONALIZAR_SITIO";
        if (!progreso.isCitaPruebaCompletada()) return "CREAR_CITA_PRUEBA";
        return "COMPLETADO";
    }
}
