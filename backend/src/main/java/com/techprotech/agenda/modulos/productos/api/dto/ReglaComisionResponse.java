package com.techprotech.agenda.modulos.productos.api.dto;
import java.math.BigDecimal; import java.time.LocalDate;
public record ReglaComisionResponse(Long id,String nombre,String descripcion,String concepto,String alcance,Long servicioId,String servicioNombre,Long productoId,String productoNombre,Long categoriaProductoId,String categoriaProductoNombre,String beneficiario,Long prestadorId,String prestadorNombre,String tipoCalculo,String baseCalculo,BigDecimal valor,String moneda,LocalDate fechaInicio,LocalDate fechaFin,int prioridad,boolean activa,String estado){}
