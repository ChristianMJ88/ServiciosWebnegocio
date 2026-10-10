package com.techprotech.agenda.modulos.marketplace.api.dto;
import java.util.List;
public record DirectorioResponse(List<NegocioDirectorioResponse> negocios,Long siguienteCursor) {}
