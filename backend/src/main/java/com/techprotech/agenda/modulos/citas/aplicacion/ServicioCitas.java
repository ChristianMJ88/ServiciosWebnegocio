package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.modulos.citas.api.dto.CitaCreadaResponse;
import com.techprotech.agenda.modulos.citas.api.dto.CrearCitaRequest;
import org.springframework.stereotype.Service;

@Service
public class ServicioCitas {

    public CitaCreadaResponse crearCita(CrearCitaRequest request) {
        throw new UnsupportedOperationException(
                "La creacion real de citas aun no esta implementada. " +
                "El siguiente paso es validar disponibilidad, persistir la cita y registrar historial."
        );
    }
}

