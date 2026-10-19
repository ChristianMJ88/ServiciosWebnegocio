package com.techprotech.agenda.modulos.marketing.api.dto;
import java.time.LocalTime;
import java.util.List;
public record SoportePublicoResponse(String correo,String telefono,List<Integer> dias,
    LocalTime horaInicio,LocalTime horaFin,String zonaHoraria) {}
