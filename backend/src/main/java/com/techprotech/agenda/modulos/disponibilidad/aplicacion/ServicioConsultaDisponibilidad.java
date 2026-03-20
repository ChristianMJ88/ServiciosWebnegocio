package com.techprotech.agenda.modulos.disponibilidad.aplicacion;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ServicioConsultaDisponibilidad {

    public List<FranjaDisponibleResponse> obtenerFranjasDisponibles(
            Long empresaId,
            Long sucursalId,
            Long servicioId,
            Long prestadorId,
            LocalDate fecha
    ) {
        throw new UnsupportedOperationException(
                "El calculo real de disponibilidad aun no esta implementado. " +
                "El siguiente paso es leer reglas, excepciones y citas desde base de datos."
        );
    }
}

