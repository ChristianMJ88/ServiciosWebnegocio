package com.techprotech.agenda.modulos.plataforma.api.dto;

public record SeccionAdminPlataformaResponse(Long id,String codigo,String nombre,String descripcion,boolean requiereModulo,int orden) {}
