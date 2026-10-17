package com.techprotech.agenda.modulos.politicas.api.dto;

import java.util.List;

public record PoliticaAdminResponse(PoliticaPublicaResponse vigente,PublicarPoliticaRequest borrador,
        String textoBorrador,List<Opcion> modos, String avisoCobro) {
    public record Opcion(String codigo,String etiqueta) {}
}
