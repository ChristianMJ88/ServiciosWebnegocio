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
@RequestMapping("/api/v1/publico/disponibilidad/franjas")
public class ControladorDisponibilidadPublico {

    private final ServicioConsultaDisponibilidad servicioConsultaDisponibilidad;

    public ControladorDisponibilidadPublico(ServicioConsultaDisponibilidad servicioConsultaDisponibilidad) {
        this.servicioConsultaDisponibilidad = servicioConsultaDisponibilidad;
    }

    @GetMapping
    public List<FranjaDisponibleResponse> listar(
            @RequestParam(required = false) Long empresaId,
            @RequestParam @NotNull Long sucursalId,
            @RequestParam @NotNull Long servicioId,
            @RequestParam(required = false) Long prestadorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        return servicioConsultaDisponibilidad.obtenerFranjasDisponibles(
                empresaId,
                sucursalId,
                servicioId,
                prestadorId,
                fecha
        );
    }
}

