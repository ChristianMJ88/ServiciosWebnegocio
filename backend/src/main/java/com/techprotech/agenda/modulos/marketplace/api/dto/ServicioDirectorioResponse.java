package com.techprotech.agenda.modulos.marketplace.api.dto;
import java.math.BigDecimal;
public record ServicioDirectorioResponse(Long id,String nombre,String categoria,BigDecimal precio,String moneda,int duracionMinutos) {}
