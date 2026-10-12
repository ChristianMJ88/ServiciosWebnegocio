package com.techprotech.agenda.modulos.admin.api.dto;

/** Resultado del alta mediante invitación, con el contrato específico de cada perfil. */
public sealed interface AccesoUsuarioCreadoResponse permits UsuarioInternoAdminResponse, PrestadorAdminResponse {
}
