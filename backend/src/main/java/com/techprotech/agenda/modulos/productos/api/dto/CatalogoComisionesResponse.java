package com.techprotech.agenda.modulos.productos.api.dto;
import java.util.List;
public record CatalogoComisionesResponse(String monedaPredeterminada,List<Opcion> servicios,List<Opcion> productos,List<Opcion> categoriasProducto,List<Opcion> prestadores,List<ReglaComisionResponse> reglas){public record Opcion(Long id,String nombre){}}
