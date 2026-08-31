package com.techprotech.agenda.modulos.autenticacion.aplicacion;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class ServicioProteccionAcceso {
    private static final int MAX_INTENTOS = 5;
    private static final int MINUTOS_BLOQUEO = 15;
    private final UsuarioRepositorio usuarios;
    public ServicioProteccionAcceso(UsuarioRepositorio usuarios) { this.usuarios = usuarios; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarIntentoFallido(Long usuarioId) {
        usuarios.findById(usuarioId).ifPresent(usuario -> {
            int intentos = usuario.getIntentosLoginFallidos() + 1;
            usuario.setIntentosLoginFallidos(intentos);
            if (intentos >= MAX_INTENTOS) {
                usuario.setLoginBloqueadoHasta(LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO));
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
