package com.techprotech.agenda.modulos.sitio.dominio;

public enum TipoBloqueSitio {
    PORTADA("Portada", "Primer mensaje y acciones principales", "bi bi-image", true),
    SERVICIOS_DESTACADOS("Servicios destacados", "Selección principal del catálogo", "bi bi-grid", true),
    BENEFICIOS("Beneficios", "Razones para elegir el negocio", "bi bi-stars", true),
    EQUIPO("Equipo", "Personas que atienden a los clientes", "bi bi-people", true),
    GALERIA("Galería", "Imágenes del trabajo y las instalaciones", "bi bi-images", true),
    PROMOCIONES("Promociones", "Ofertas con vigencia y servicios asociados", "bi bi-tag", true),
    RESENAS("Reseñas", "Opiniones y señales de confianza", "bi bi-chat-quote", true),
    PREGUNTAS_FRECUENTES("Preguntas frecuentes", "Respuestas para reducir dudas", "bi bi-question-circle", true),
    CONTACTO("Contacto", "Acciones y datos para comunicarse", "bi bi-chat-dots", true),
    MAPA("Mapa", "Ubicación de sucursales", "bi bi-map", true);

    private final String nombre;
    private final String descripcion;
    private final String icono;
    private final boolean disponible;

    TipoBloqueSitio(String nombre, String descripcion, String icono, boolean disponible) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.icono = icono;
        this.disponible = disponible;
    }

    public String nombre() { return nombre; }
    public String descripcion() { return descripcion; }
    public String icono() { return icono; }
    public boolean disponible() { return disponible; }
}
