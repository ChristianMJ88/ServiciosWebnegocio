package com.techprotech.agenda.modulos.caja.api.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/** Contrato explícito: no expone la implementación interna de Spring Data. */
public record PaginaCajaResponse<T>(List<T> content, int pagina, int tamano,
                                   long totalElements, int totalPages) {
    public static <T> PaginaCajaResponse<T> de(Page<T> pagina) {
        return new PaginaCajaResponse<>(List.copyOf(pagina.getContent()), pagina.getNumber(),
                pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
    }
}
