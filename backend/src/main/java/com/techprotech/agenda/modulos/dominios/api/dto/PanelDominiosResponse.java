package com.techprotech.agenda.modulos.dominios.api.dto;
import java.util.List;
public record PanelDominiosResponse(boolean incluido,boolean puedeAgregar,Long limite,long uso,List<DominioResponse> dominios) {}
