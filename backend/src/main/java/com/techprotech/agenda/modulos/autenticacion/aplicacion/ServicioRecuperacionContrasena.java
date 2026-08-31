package com.techprotech.agenda.modulos.autenticacion.aplicacion;

import com.techprotech.agenda.compartido.correo.*;
import com.techprotech.agenda.modulos.autenticacion.api.ConfirmarRecuperacionRequest;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ServicioRecuperacionContrasena {
    private final UsuarioRepositorio usuarios; private final EmpresaRepositorio empresas;
    private final RecuperacionContrasenaRepositorio recuperaciones;
    private final ServicioConfiguracionCorreoEmpresa configuracionCorreo; private final ClienteCorreoSaliente correo;
    private final PasswordEncoder encoder; private final SecureRandom random = new SecureRandom(); private final String url;
    public ServicioRecuperacionContrasena(UsuarioRepositorio usuarios, EmpresaRepositorio empresas,
            RecuperacionContrasenaRepositorio recuperaciones, ServicioConfiguracionCorreoEmpresa configuracionCorreo,
            ClienteCorreoSaliente correo, PasswordEncoder encoder,
            @Value("${aplicacion.recuperacion-contrasena.url:https://app.refluora.com/recuperar-contrasena/confirmar}") String url) {
        this.usuarios=usuarios; this.empresas=empresas; this.recuperaciones=recuperaciones;
        this.configuracionCorreo=configuracionCorreo; this.correo=correo; this.encoder=encoder; this.url=url;
    }
    @Transactional
    public void solicitar(String correoSolicitado) {
        String correoNormalizado = correoSolicitado.trim().toLowerCase(); LocalDateTime ahora=LocalDateTime.now();
        usuarios.findByCorreoOrderByEmpresaIdAsc(correoNormalizado).stream()
                .filter(UsuarioEntidad::isHabilitado).filter(u -> !u.isBloqueado()).forEach(usuario -> {
            if (recuperaciones.existsByUsuarioIdAndEstadoAndCreadoEnAfter(usuario.getId(), "PENDIENTE", ahora.minusMinutes(5))) return;
            recuperaciones.findByUsuarioIdAndEstado(usuario.getId(), "PENDIENTE").forEach(vieja -> { vieja.setEstado("CANCELADA"); recuperaciones.save(vieja); });
            String token=token(); RecuperacionContrasenaEntidad r=new RecuperacionContrasenaEntidad();
            r.setUsuarioId(usuario.getId()); r.setEmpresaId(usuario.getEmpresaId()); r.setTokenHash(hash(token));
            r.setEstado("PENDIENTE"); r.setExpiraEn(ahora.plusMinutes(30)); r.setCreadoEn(ahora); recuperaciones.save(r);
            enviar(usuario, token);
        });
    }
    @Transactional
    public void confirmar(ConfirmarRecuperacionRequest request) {
        RecuperacionContrasenaEntidad r=recuperaciones.findByTokenHash(hash(request.token()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"El enlace no es válido"));
        if (!"PENDIENTE".equals(r.getEstado())) throw new ResponseStatusException(HttpStatus.CONFLICT,"El enlace ya fue utilizado");
        if (r.getExpiraEn().isBefore(LocalDateTime.now())) throw new ResponseStatusException(HttpStatus.GONE,"El enlace expiró");
        UsuarioEntidad usuario=usuarios.findByIdAndEmpresaId(r.getUsuarioId(),r.getEmpresaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"El usuario no existe"));
        usuario.setContrasenaHash(encoder.encode(request.contrasena())); usuario.setBloqueado(false);
        usuario.setIntentosLoginFallidos(0); usuario.setLoginBloqueadoHasta(null); usuarios.save(usuario);
        r.setEstado("UTILIZADA"); r.setUtilizadaEn(LocalDateTime.now()); recuperaciones.save(r);
        recuperaciones.findByUsuarioIdAndEstado(usuario.getId(),"PENDIENTE").stream().filter(x -> !x.getId().equals(r.getId()))
                .forEach(x -> {x.setEstado("CANCELADA"); recuperaciones.save(x);});
    }
    private void enviar(UsuarioEntidad usuario,String token) {
        EmpresaEntidad empresa=empresas.findById(usuario.getEmpresaId()).orElseThrow(); ConfiguracionCorreoResolvida config=configuracionCorreo.resolver(empresa.getId());
        if(!config.habilitado()) throw new IllegalStateException("No hay correo disponible"); String enlace=url+"?token="+token;
        String texto="Solicitaste recuperar tu contraseña de "+empresa.getNombre()+" en Fluora.\n\n"+enlace+"\n\nEl enlace vence en 30 minutos y solo puede usarse una vez.";
        String html="<div style=\"font-family:Arial,sans-serif;max-width:600px;margin:auto;color:#173b57\"><h1>Recupera tu acceso</h1><p>Recibimos una solicitud para cambiar tu contraseña de <strong>"+esc(empresa.getNombre())+"</strong>.</p><p><a style=\"background:#6c4cff;color:#fff;padding:12px 20px;border-radius:8px;text-decoration:none;display:inline-block\" href=\""+enlace+"\">Crear nueva contraseña</a></p><p>El enlace vence en 30 minutos y solo puede usarse una vez. Si no hiciste la solicitud, ignora este mensaje.</p></div>";
        correo.enviar(config,new MensajeCorreoSaliente(usuario.getCorreo(),"Recupera tu acceso a "+empresa.getNombre()+" | Fluora",texto,html,config.responderA(),List.of()));
    }
    private String token(){byte[] b=new byte[32];random.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    private String hash(String t){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(t.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private String esc(String v){return v.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
}
