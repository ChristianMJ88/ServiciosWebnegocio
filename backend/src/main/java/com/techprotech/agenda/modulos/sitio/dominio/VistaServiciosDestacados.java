package com.techprotech.agenda.modulos.sitio.dominio;

public enum VistaServiciosDestacados {
    TARJETAS("Tarjetas", "Cuadrícula visual para comparar opciones"),
    CARRUSEL("Carrusel", "Recorrido horizontal, ideal para móvil"),
    LISTA("Lista", "Presentación compacta con lectura rápida");

    private final String nombre;
    private final String descripcion;

    VistaServiciosDestacados(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public String nombre() { return nombre; }
    public String descripcion() { return descripcion; }
}
