package com.techprotech.agenda.modulos.marketing.aplicacion;
import com.techprotech.agenda.modulos.marketing.api.dto.*;
public interface ServicioSoportePlataforma {
    SoportePlataformaResponse obtener();
    SoportePlataformaResponse guardar(SoportePlataformaSolicitud solicitud,Long actor);
    SoportePublicoResponse publico();
    ContactoLegalResponse contactoLegal();
}
