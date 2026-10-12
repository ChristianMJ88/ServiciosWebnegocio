package com.techprotech.agenda.modulos.admin.aplicacion;

import com.techprotech.agenda.modulos.admin.api.dto.PlantillaWhatsappAdminResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class FiltroPlantillasWhatsapp {
    private FiltroPlantillasWhatsapp() {}

    public static List<PlantillaWhatsappAdminResponse> filtrar(List<PlantillaWhatsappAdminResponse> plantillas,
            String buscar, String tipo, String orden) {
        String texto = normalizar(buscar);
        Comparator<PlantillaWhatsappAdminResponse> comparador = Comparator.comparing(
                p -> normalizar(p.nombre()));
        if ("desc".equals(orden)) comparador = comparador.reversed();
        return plantillas.stream()
                .filter(p -> texto.isEmpty() || normalizar(p.nombre()).contains(texto)
                        || normalizar(p.cuerpo()).contains(texto))
                .filter(p -> tipo == null || tipo.isBlank() || tipo.equals(p.tipoPlantilla()))
                .sorted(comparador.thenComparing(PlantillaWhatsappAdminResponse::sid))
                .toList();
    }

    private static String normalizar(String valor) {
        return valor == null ? "" : valor.strip().toLowerCase(Locale.ROOT);
    }
}
