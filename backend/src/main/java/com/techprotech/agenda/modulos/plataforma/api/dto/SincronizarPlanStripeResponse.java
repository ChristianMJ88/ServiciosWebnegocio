package com.techprotech.agenda.modulos.plataforma.api.dto;
public record SincronizarPlanStripeResponse(Long planId, String productoId, String precioMensualId,
        String precioAnualId, boolean actualizado) {}
