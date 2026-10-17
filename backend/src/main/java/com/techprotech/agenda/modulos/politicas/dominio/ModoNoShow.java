package com.techprotech.agenda.modulos.politicas.dominio;

public enum ModoNoShow {
    CREDITO_REAGENDAR("Crédito para reagendar"), RETENER("Retener el anticipo");
    private final String etiqueta;
    ModoNoShow(String etiqueta) { this.etiqueta = etiqueta; }
    public String etiqueta() { return etiqueta; }
}
