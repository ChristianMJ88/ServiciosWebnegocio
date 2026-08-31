package com.techprotech.agenda.modulos.autenticacion.api;
import com.techprotech.agenda.modulos.autenticacion.aplicacion.ServicioRecuperacionContrasena;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/v1/publico/recuperacion-contrasena")
public class ControladorRecuperacionContrasena {
    private final ServicioRecuperacionContrasena servicio;
    public ControladorRecuperacionContrasena(ServicioRecuperacionContrasena servicio){this.servicio=servicio;}
    @PostMapping("/solicitar") public Map<String,String> solicitar(@Valid @RequestBody SolicitarRecuperacionRequest r){
        servicio.solicitar(r.correo()); return Map.of("mensaje","Si el correo corresponde a una cuenta activa, enviaremos instrucciones para recuperar el acceso.");}
    @PostMapping("/confirmar") public Map<String,String> confirmar(@Valid @RequestBody ConfirmarRecuperacionRequest r){
        servicio.confirmar(r); return Map.of("mensaje","La contraseña fue actualizada.");}
}
