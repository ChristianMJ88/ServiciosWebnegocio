package com.techprotech.agenda.modulos.plataforma.api.dto;
import java.util.List;
public record ModuloPlataformaResponse(Long id,String codigo,String nombre,String descripcion,int orden,List<String> seccionesAdmin) {}
