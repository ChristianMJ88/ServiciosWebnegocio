package com.techprotech.agenda.modulos.onboarding.aplicacion;

import com.techprotech.agenda.compartido.correo.*;
import com.techprotech.agenda.modulos.autenticacion.aplicacion.ServicioProteccionAcceso;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.onboarding.api.dto.ConfirmarCorreoOnboardingResponse;
import com.techprotech.agenda.modulos.onboarding.infraestructura.entidad.VerificacionCorreoOnboardingEntidad;
import com.techprotech.agenda.modulos.onboarding.infraestructura.repositorio.VerificacionCorreoOnboardingRepositorio;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@Service
public class ServicioVerificacionCorreoOnboarding {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioVerificacionCorreoOnboarding.class);
    private final VerificacionCorreoOnboardingRepositorio repositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final EmpresaRepositorio empresaRepositorio;
    private final ClienteCorreoSaliente correo;
    private final ServicioConfiguracionCorreoEmpresa configuracionCorreo;
    private final PropiedadesCorreo propiedadesCorreo;
    private final ServicioOutboxCorreoBienvenida bienvenida;
    private final ServicioProteccionAcceso proteccionAcceso;
    private final SecureRandom random = new SecureRandom();
    private final String url;
    private final int minutos;

    public ServicioVerificacionCorreoOnboarding(VerificacionCorreoOnboardingRepositorio repositorio, UsuarioRepositorio usuarioRepositorio, EmpresaRepositorio empresaRepositorio,
            ClienteCorreoSaliente correo, ServicioConfiguracionCorreoEmpresa configuracionCorreo, PropiedadesCorreo propiedadesCorreo,
            ServicioOutboxCorreoBienvenida bienvenida, ServicioProteccionAcceso proteccionAcceso,
            @Value("${aplicacion.onboarding.verificacion-correo.url}") String url,
            @Value("${aplicacion.onboarding.verificacion-correo.minutos-vigencia:30}") int minutos) {
        this.repositorio=repo(repositorio); this.usuarioRepositorio=usuarioRepositorio; this.empresaRepositorio=empresaRepositorio; this.correo=correo; this.configuracionCorreo=configuracionCorreo;
        this.propiedadesCorreo=propiedadesCorreo; this.bienvenida=bienvenida; this.proteccionAcceso=proteccionAcceso; this.url=url; this.minutos=minutos;
    }
    private VerificacionCorreoOnboardingRepositorio repo(VerificacionCorreoOnboardingRepositorio r){ return r; }

    @Transactional
    public boolean enviar(EmpresaEntidad empresa, UsuarioEntidad usuario, String nombre) {
        repositorio.findByEmpresaIdAndUsuarioIdAndEstado(empresa.getId(), usuario.getId(), "PENDIENTE")
                .forEach(pendiente -> {
                    pendiente.setEstado("REEMPLAZADA");
                    repositorio.save(pendiente);
                });
        byte[] bytes=new byte[32]; random.nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        VerificacionCorreoOnboardingEntidad v=new VerificacionCorreoOnboardingEntidad();
        v.setEmpresaId(empresa.getId()); v.setUsuarioId(usuario.getId()); v.setCorreo(usuario.getCorreo()); v.setTokenHash(hash(token));
        v.setEstado("PENDIENTE"); v.setCreadoEn(LocalDateTime.now()); v.setExpiraEn(LocalDateTime.now().plusMinutes(minutos)); repositorio.save(v);
        String enlace=url+"?token="+token;
        String texto="Hola "+nombre+",\n\nConfirma tu correo para activar tu cuenta en "+empresa.getNombre()+": "+enlace+"\n\nEl enlace vence en "+minutos+" minutos.";
        ConfiguracionCorreoResolvida cfg=configuracionCorreo.resolver(empresa.getId());
        try {
            correo.enviar(cfg,new MensajeCorreoSaliente(usuario.getCorreo(),"Confirma tu correo en "+empresa.getNombre(),texto,
                    "<p>Hola "+escapar(nombre)+",</p><p>Confirma tu correo para activar tu cuenta en <strong>"+escapar(empresa.getNombre())+"</strong>.</p><p><a href=\""+enlace+"\">Confirmar correo</a></p>",cfg.responderA(), List.of()));
            return true;
        } catch (RuntimeException ex) {
            LOGGER.error("No se pudo enviar la verificación de correo para empresa {} y usuario {}. El enlace puede reenviarse desde el panel.",
                    empresa.getId(), usuario.getId(), ex);
            return false;
        }
    }

    @Transactional
    public ConfirmarCorreoOnboardingResponse confirmar(String token) {
        VerificacionCorreoOnboardingEntidad v=repositorio.buscarParaConfirmar(hash(token)).orElseThrow(()->new ResponseStatusException(NOT_FOUND,"Enlace de verificación no válido"));
        if(!"PENDIENTE".equals(v.getEstado()) || v.getExpiraEn().isBefore(LocalDateTime.now())) throw new ResponseStatusException(NOT_FOUND,"Enlace de verificación expirado");
        UsuarioEntidad u=usuarioRepositorio.findById(v.getUsuarioId()).orElseThrow(()->new ResponseStatusException(NOT_FOUND,"Cuenta no encontrada"));
        u.setHabilitado(true); u.setBloqueado(false); proteccionAcceso.limpiar(u);
        u.setCorreoVerificadoEn(LocalDateTime.now());
        usuarioRepositorio.save(u);
        v.setEstado("CONFIRMADA"); v.setConfirmadoEn(LocalDateTime.now()); repositorio.save(v);
        EmpresaEntidad empresa=empresaRepositorio.findById(v.getEmpresaId()).orElseThrow(()->new ResponseStatusException(NOT_FOUND,"Negocio no encontrado"));
        bienvenida.programar(new BienvenidaEmpresaCorreo(empresa.getId(),empresa.getNombre(),u.getCorreo(),u.getCorreo(),empresa.getSlug()));
        return new ConfirmarCorreoOnboardingResponse("Correo confirmado. Ya puedes iniciar sesión.","/acceso");
    }

    @Transactional
    public void reenviar(Long empresaId, Long usuarioId) {
        UsuarioEntidad usuario = usuarioRepositorio.findByIdAndEmpresaId(usuarioId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Cuenta no encontrada"));
        if (usuario.getCorreoVerificadoEn() != null) return;
        repositorio.findFirstByEmpresaIdAndUsuarioIdOrderByCreadoEnDesc(empresaId, usuarioId).ifPresent(ultimo -> {
            if (ultimo.getCreadoEn().isAfter(LocalDateTime.now().minusSeconds(60))) {
                throw new ResponseStatusException(TOO_MANY_REQUESTS, "Espera un minuto antes de solicitar otro enlace");
            }
        });
        EmpresaEntidad empresa = empresaRepositorio.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Negocio no encontrado"));
        if (!enviar(empresa, usuario, usuario.getCorreo())) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "No pudimos enviar el correo en este momento; inténtalo más tarde");
        }
    }
    private String hash(String value){ try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);} }
    private String escapar(String valor){ return valor.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;"); }
}
