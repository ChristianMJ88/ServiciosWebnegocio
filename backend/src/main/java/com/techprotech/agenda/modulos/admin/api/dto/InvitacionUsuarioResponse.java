package com.techprotech.agenda.modulos.admin.api.dto;

import java.time.LocalDateTime;

public record InvitacionUsuarioResponse(Long id, String correo, String nombreCompleto, String estado, LocalDateTime expiraEn) {}
