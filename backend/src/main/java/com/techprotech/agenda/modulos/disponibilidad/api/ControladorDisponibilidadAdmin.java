package com.techprotech.agenda.modulos.disponibilidad.api;

import com.techprotech.agenda.modulos.disponibilidad.api.dto.ExcepcionDisponibilidadRequest;
import com.techprotech.agenda.modulos.disponibilidad.api.dto.ExcepcionDisponibilidadResponse;
import com.techprotech.agenda.modulos.disponibilidad.api.dto.ReglaDisponibilidadRequest;
import com.techprotech.agenda.modulos.disponibilidad.api.dto.ReglaDisponibilidadResponse;
import com.techprotech.agenda.modulos.disponibilidad.api.dto.MetadatosDisponibilidadResponse;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.CatalogosDisponibilidad;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioGestionDisponibilidad;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/v1/admin/disponibilidad")
@PreAuthorize("hasAuthority('PANEL_ADMIN_ACCESO')")
public class ControladorDisponibilidadAdmin {

    private final ServicioGestionDisponibilidad servicioGestionDisponibilidad;

    public ControladorDisponibilidadAdmin(ServicioGestionDisponibilidad servicioGestionDisponibilidad) {
        this.servicioGestionDisponibilidad = servicioGestionDisponibilidad;
    }

    @GetMapping("/metadatos")
    @PreAuthorize("hasAuthority('PRESTADORES_GESTIONAR')")
    public MetadatosDisponibilidadResponse metadatos() {
        return new MetadatosDisponibilidadResponse(
                CatalogosDisponibilidad.TIPOS_SUJETO.stream()
                        .map(opcion -> new MetadatosDisponibilidadResponse.OpcionTextoResponse(opcion.valor(), opcion.etiqueta()))
                        .toList(),
                IntStream.rangeClosed(1, 7)
                        .mapToObj(dia -> new MetadatosDisponibilidadResponse.OpcionNumeroResponse(dia, nombreDia(dia)))
                        .toList(),
                CatalogosDisponibilidad.TIPOS_BLOQUEO.stream()
                        .map(opcion -> new MetadatosDisponibilidadResponse.OpcionTextoResponse(opcion.valor(), opcion.etiqueta()))
                        .toList(),
                CatalogosDisponibilidad.INTERVALO_MINIMO_MINUTOS
        );
    }

    private String nombreDia(int dia) {
        String nombre = DayOfWeek.of(dia).getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-MX"));
        return Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1);
    }

    @GetMapping("/reglas")
    @PreAuthorize("hasAuthority('PRESTADORES_GESTIONAR')")
    public List<ReglaDisponibilidadResponse> reglas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioGestionDisponibilidad.listarReglasAdmin(usuario.empresaId());
    }

    @PostMapping("/reglas")
    @PreAuthorize("hasAuthority('PRESTADORES_GESTIONAR')")
    public ReglaDisponibilidadResponse crearRegla(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ReglaDisponibilidadRequest request
    ) {
        return servicioGestionDisponibilidad.crearReglaAdmin(usuario.empresaId(), request);
    }

    @PatchMapping("/reglas/{id}")
    @PreAuthorize("hasAuthority('PRESTADORES_GESTIONAR')")
    public ReglaDisponibilidadResponse actualizarRegla(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody ReglaDisponibilidadRequest request
    ) {
        return servicioGestionDisponibilidad.actualizarReglaAdmin(usuario.empresaId(), id, request);
    }

    @GetMapping("/excepciones")
    @PreAuthorize("hasAuthority('PRESTADORES_GESTIONAR')")
    public List<ExcepcionDisponibilidadResponse> excepciones(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicioGestionDisponibilidad.listarExcepcionesAdmin(usuario.empresaId());
    }

    @PostMapping("/excepciones")
    @PreAuthorize("hasAuthority('PRESTADORES_GESTIONAR')")
    public ExcepcionDisponibilidadResponse crearExcepcion(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ExcepcionDisponibilidadRequest request
    ) {
        return servicioGestionDisponibilidad.crearExcepcionAdmin(usuario.empresaId(), request);
    }

    @PatchMapping("/excepciones/{id}")
    @PreAuthorize("hasAuthority('PRESTADORES_GESTIONAR')")
    public ExcepcionDisponibilidadResponse actualizarExcepcion(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody ExcepcionDisponibilidadRequest request
    ) {
        return servicioGestionDisponibilidad.actualizarExcepcionAdmin(usuario.empresaId(), id, request);
    }
}
