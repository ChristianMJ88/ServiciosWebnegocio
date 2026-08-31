package com.techprotech.agenda.modulos.admin.api.dto;

import java.time.LocalDateTime;

public record DetalleInvitacionUsuarioResponse(String empresa, String correo, String nombreCompleto, String puesto, LocalDateTime expiraEn) {}
