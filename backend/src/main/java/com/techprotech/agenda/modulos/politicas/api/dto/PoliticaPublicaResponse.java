package com.techprotech.agenda.modulos.politicas.api.dto;

import com.techprotech.agenda.modulos.politicas.dominio.ModoNoShow;
import java.time.OffsetDateTime;

/** Solo datos de la política del negocio: no incluye ids globales, actores ni configuración de cobros. */
public record PoliticaPublicaResponse(String nombreComercial,Integer version,OffsetDateTime vigenteDesde,
        ModoNoShow modoNoShow,Integer diasReagendar,Integer diasAclaracion,Integer reagendadosMax,
        String textoGenerado,String textoAdicional,Contacto contactoReclamos) {
    public record Contacto(String correo,String telefono) {}
}
