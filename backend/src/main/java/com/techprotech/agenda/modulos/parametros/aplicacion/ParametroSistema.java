package com.techprotech.agenda.modulos.parametros.aplicacion;
import java.time.LocalDateTime;
import java.util.List;
public record ParametroSistema(String clave, String nombre, String descripcion, String tipo, String valor,
                               String valorPredeterminado, String categoria, List<String> opciones,
                               boolean personalizado, boolean editable, LocalDateTime actualizadoEn) {}
