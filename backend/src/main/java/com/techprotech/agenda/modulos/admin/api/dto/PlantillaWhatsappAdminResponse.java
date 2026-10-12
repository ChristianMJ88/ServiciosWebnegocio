package com.techprotech.agenda.modulos.admin.api.dto;

public record PlantillaWhatsappAdminResponse(
        String sid,
        String nombre,
        String idioma,
        String categoria,
        String estado,
        String tipoPlantilla,
        String cuerpo
) {
    public PlantillaWhatsappAdminResponse(String sid, String nombre, String idioma, String categoria, String estado, String tipoPlantilla) {
        this(sid, nombre, idioma, categoria, estado, tipoPlantilla, null);
    }
}
