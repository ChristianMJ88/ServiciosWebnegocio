package com.techprotech.agenda.modulos.autenticacion.aplicacion;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class ServicioProteccionAcceso {
    private final UsuarioRepositorio usuarios;
    private final int maxIntentos;
    private final int minutosBloqueo;
    public ServicioProteccionAcceso(UsuarioRepositorio usuarios,
            @Value("${aplicacion.seguridad-acceso.max-intentos}") int maxIntentos,
            @Value("${aplicacion.seguridad-acceso.minutos-bloqueo}") int minutosBloqueo) {
        this.usuarios = usuarios; this.maxIntentos = maxIntentos; this.minutosBloqueo = minutosBloqueo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarIntentoFallido(Long usuarioId) {
        usuarios.findById(usuarioId).ifPresent(usuario -> {
            int intentos = usuario.getIntentosLoginFallidos() + 1;
            usuario.setIntentosLoginFallidos(intentos);
            if (intentos >= maxIntentos) {
                usuario.setLoginBloqueadoHasta(LocalDateTime.now().plusMinutes(minutosBloqueo));
                usuario.setIntentosLoginFallidos(0);
            }
            usuarios.save(usuario);
        });
    }

    public boolean estaBloqueadoTemporalmente(UsuarioEntidad usuario) {
        return usuario.getLoginBloqueadoHasta() != null && usuario.getLoginBloqueadoHasta().isAfter(LocalDateTime.now());
    }

    public void limpiar(UsuarioEntidad usuario) {
        usuario.setIntentosLoginFallidos(0);
        usuario.setLoginBloqueadoHasta(null);
    }
}
