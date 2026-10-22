package com.techprotech.agenda.modulos.dominios.api.dto;
import java.time.LocalDateTime;
import java.util.List;
public record DominioResponse(Long id,String hostname,String estado,Long version,boolean raiz,
 List<RegistroDns> registros,String aviso,String ultimoError,LocalDateTime verificadoEn,LocalDateTime activadoEn,
 LocalDateTime ultimoChequeoEn,List<String> acciones) {}
