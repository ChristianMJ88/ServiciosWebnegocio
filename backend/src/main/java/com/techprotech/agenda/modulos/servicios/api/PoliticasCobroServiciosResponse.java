package com.techprotech.agenda.modulos.servicios.api;
import java.math.BigDecimal;
import java.util.List;
public record PoliticasCobroServiciosResponse(List<Opcion> opciones,Predeterminado predeterminado) {
    public record Opcion(String codigo,String etiqueta) {}
    public record Predeterminado(boolean requiereAnticipo,String anticipoTipo,BigDecimal anticipoValor) {}
}
