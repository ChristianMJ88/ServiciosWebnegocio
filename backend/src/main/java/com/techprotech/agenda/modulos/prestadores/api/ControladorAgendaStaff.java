package com.techprotech.agenda.modulos.prestadores.api;

import com.techprotech.agenda.modulos.prestadores.api.dto.CitaAgendaResponse;
import com.techprotech.agenda.modulos.prestadores.aplicacion.ServicioAgendaStaff;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/staff")
@PreAuthorize("hasRole('STAFF')")
public class ControladorAgendaStaff {

    private final ServicioAgendaStaff servicioAgendaStaff;

    public ControladorAgendaStaff(ServicioAgendaStaff servicioAgendaStaff) {
        this.servicioAgendaStaff = servicioAgendaStaff;
    }

    @GetMapping("/agenda")
    public List<CitaAgendaResponse> agenda(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return servicioAgendaStaff.obtenerAgenda(usuario.empresaId(), usuario.usuarioId(), desde, hasta);
    }

    @PatchMapping("/citas/{citaId}/confirmar")
    public ResponseEntity<Void> confirmar(@PathVariable Long citaId, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        servicioAgendaStaff.cambiarEstado(usuario.empresaId(), usuario.usuarioId(), citaId, "CONFIRMADA");
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/citas/{citaId}/finalizar")
    public ResponseEntity<Void> finalizar(@PathVariable Long citaId, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        servicioAgendaStaff.cambiarEstado(usuario.empresaId(), usuario.usuarioId(), citaId, "FINALIZADA");
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/citas/{citaId}/no-asistio")
    public ResponseEntity<Void> noAsistio(@PathVariable Long citaId, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        servicioAgendaStaff.cambiarEstado(usuario.empresaId(), usuario.usuarioId(), citaId, "NO_ASISTIO");
        return ResponseEntity.noContent().build();
    }
}

