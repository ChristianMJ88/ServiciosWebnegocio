package com.techprotech.agenda.modulos.marketing.api.dto;
import java.time.*;
import java.util.List;
public record SoportePlataformaResponse(Long version,String correo,String correoLegal,String telefono,
    List<Integer> dias,LocalTime horaInicio,LocalTime horaFin,String zonaHoraria,
    boolean visible,OffsetDateTime actualizadoEn,Long actualizadoPor) {}
