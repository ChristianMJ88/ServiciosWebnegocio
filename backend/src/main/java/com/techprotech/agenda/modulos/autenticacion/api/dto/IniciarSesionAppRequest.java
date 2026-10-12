package com.techprotech.agenda.modulos.autenticacion.api.dto;
import jakarta.validation.constraints.*;
public record IniciarSesionAppRequest(@NotBlank @Size(max=255) String correo,@NotBlank String contrasena,Long empresaId,@Size(min=8,max=100) String nuevaContrasena) {
 public IniciarSesionAppRequest(String correo,String contrasena,Long empresaId){this(correo,contrasena,empresaId,null);}
}
