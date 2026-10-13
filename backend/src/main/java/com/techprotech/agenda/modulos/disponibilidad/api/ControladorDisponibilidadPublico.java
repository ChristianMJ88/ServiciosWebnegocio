package com.techprotech.agenda.modulos.disponibilidad.api;

import com.techprotech.agenda.modulos.disponibilidad.aplicacion.FranjaDisponibleResponse;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioConsultaDisponibilidad;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/publico/disponibilidad")
public class ControladorDisponibilidadPublico {

    private final com.techprotech.agenda.modulos.sitio.aplicacion.ResolutorTenantPublico tenant;

    private final ServicioConsultaDisponibilidad servicioConsultaDisponibilidad;

    public ControladorDisponibilidadPublico(ServicioConsultaDisponibilidad servicioConsultaDisponibilidad,
            com.techprotech.agenda.modulos.sitio.aplicacion.ResolutorTenantPublico tenant) {
        this.tenant = tenant;
        this.servicioConsultaDisponibilidad = servicioConsultaDisponibilidad;
    }

    @GetMapping("/franjas")
    public List<FranjaDisponibleResponse> listar(
            @org.springframework.web.bind.annotation.RequestHeader("X-Tenant-Slug") String slug,
            @RequestParam(required = false) Long empresaId,
            @RequestParam @NotNull Long sucursalId,
            @RequestParam @NotNull Long servicioId,
            @RequestParam(required = false) Long prestadorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        Long empresa = tenant.resolver(slug,empresaId);
        tenant.validarServicio(empresa,sucursalId,servicioId,prestadorId);
        return servicioConsultaDisponibilidad.obtenerFranjasDisponibles(
                empresa,
                sucursalId,
                servicioId,
                prestadorId,
                fecha
        );
    }@GetMapping("/dias")
public com.techprotech.agenda.modulos.disponibilidad.aplicacion.DiasDisponiblesResponse dias(
        @org.springframework.web.bind.annotation.RequestHeader("X-Tenant-Slug") String slug,
        @RequestParam(required=false) Long empresaId,@RequestParam Long sucursalId,
        @RequestParam List<Long> servicioIds,@RequestParam(required=false) List<Long> prestadorIds,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta) {
    Long empresa=tenant.resolver(slug,empresaId);
    if(prestadorIds!=null && prestadorIds.size()!=servicioIds.size()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Los prestadores deben corresponder a los servicios");
    for(int i=0;i<servicioIds.size();i++) tenant.validarServicio(empresa,sucursalId,servicioIds.get(i),prestadorIds==null || prestadorIds.get(i)==0?null:prestadorIds.get(i));
    return servicioConsultaDisponibilidad.obtenerDias(empresa,sucursalId,servicioIds,prestadorIds,desde,hasta);
}

}

