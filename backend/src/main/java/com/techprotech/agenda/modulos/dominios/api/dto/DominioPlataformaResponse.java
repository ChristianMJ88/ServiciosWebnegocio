package com.techprotech.agenda.modulos.dominios.api.dto;
import java.util.List;
public record DominioPlataformaResponse(Long empresaId,DominioResponse dominio,boolean excedeLimite,boolean pendienteRetiroHosting) {}
