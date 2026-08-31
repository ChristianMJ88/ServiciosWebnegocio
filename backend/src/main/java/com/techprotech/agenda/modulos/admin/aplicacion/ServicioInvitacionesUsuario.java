package com.techprotech.agenda.modulos.admin.aplicacion;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.compartido.correo.*;
import com.techprotech.agenda.modulos.admin.api.dto.*;
import com.techprotech.agenda.modulos.admin.infraestructura.entidad.InvitacionUsuarioEmpresaEntidad;
import com.techprotech.agenda.modulos.admin.infraestructura.repositorio.InvitacionUsuarioEmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.social.PerfilRegistroSocial;
import com.techprotech.agenda.modulos.autenticacion.social.ServicioAutenticacionSocialGoogle;
import com.techprotech.agenda.modulos.autenticacion.social.UsuarioIdentidadExternaEntidad;
import com.techprotech.agenda.modulos.autenticacion.social.UsuarioIdentidadExternaRepositorio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.HexFormat;

@Service
public class ServicioInvitacionesUsuario {
    private final InvitacionUsuarioEmpresaRepositorio invitaciones;
    private final EmpresaRepositorio empresas;
    private final ServicioAdminCitas admin;
    private final ServicioConfiguracionCorreoEmpresa configuracionCorreo;
    private final ClienteCorreoSaliente clienteCorreo;
    private final ServicioAutenticacionSocialGoogle socialGoogle;
    private final UsuarioIdentidadExternaRepositorio identidades;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String urlAceptacion;
    private final long horasVigencia;
    private final PropiedadesCorreo propiedadesCorreo;

    public ServicioInvitacionesUsuario(
            InvitacionUsuarioEmpresaRepositorio invitaciones,
            EmpresaRepositorio empresas,
            ServicioAdminCitas admin,
            ServicioConfiguracionCorreoEmpresa configuracionCorreo,
            ClienteCorreoSaliente clienteCorreo,
            ServicioAutenticacionSocialGoogle socialGoogle,
            UsuarioIdentidadExternaRepositorio identidades,
            ObjectMapper objectMapper,
            PropiedadesCorreo propiedadesCorreo,
            @Value("${aplicacion.invitaciones.url-aceptacion}") String urlAceptacion,
            @Value("${aplicacion.invitaciones.horas-vigencia}") long horasVigencia
    ) {
        this.invitaciones = invitaciones;
        this.empresas = empresas;
        this.admin = admin;
        this.configuracionCorreo = configuracionCorreo;
        this.clienteCorreo = clienteCorreo;
        this.socialGoogle = socialGoogle;
        this.identidades = identidades;
        this.objectMapper = objectMapper;
        this.propiedadesCorreo = propiedadesCorreo;
        this.urlAceptacion = urlAceptacion;
        this.horasVigencia = horasVigencia;
    }

    @Transactional
    public InvitacionUsuarioResponse invitar(Long empresaId, Long actorId, UsuarioInternoAdminRequest request) {
        UsuarioInternoAdminRequest solicitud = new UsuarioInternoAdminRequest(
                request.sucursalId(), request.sucursalIds(), request.correo(), null, request.nombreCompleto(),
                request.telefono(), request.puesto(), request.rolEmpresaId(), request.permisosDirectos(), true, request.notas());
        admin.validarInvitacionUsuarioInterno(empresaId, solicitud);
        EmpresaEntidad empresa = empresas.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La empresa no existe"));
        String correo = request.correo().trim().toLowerCase();
        LocalDateTime ahora = LocalDateTime.now();
        invitaciones.findByEmpresaIdAndCorreoAndEstado(empresaId, correo, "PENDIENTE").forEach(anterior -> {
            anterior.setEstado("CANCELADA");
            anterior.setCanceladaEn(ahora);
            anterior.setActualizadoEn(ahora);
            invitaciones.save(anterior);
        });

        String token = generarToken();
        InvitacionUsuarioEmpresaEntidad invitacion = new InvitacionUsuarioEmpresaEntidad();
        invitacion.setEmpresaId(empresaId);
        invitacion.setCorreo(correo);
        invitacion.setNombreCompleto(request.nombreCompleto().trim());
        invitacion.setTelefono(request.telefono());
        invitacion.setPuesto(request.puesto());
        invitacion.setRolEmpresaId(request.rolEmpresaId());
        invitacion.setSucursalId(request.sucursalId());
        invitacion.setSucursalIdsJson(json(request.sucursalIds()));
        invitacion.setPermisosDirectosJson(json(request.permisosDirectos()));
        invitacion.setNotas(request.notas());
        invitacion.setTokenHash(hash(token));
        invitacion.setEstado("PENDIENTE");
        invitacion.setExpiraEn(ahora.plusHours(horasVigencia));
        invitacion.setCreadaPorUsuarioId(actorId);
        invitacion.setCreadoEn(ahora);
        invitacion.setActualizadoEn(ahora);
        invitacion = invitaciones.save(invitacion);
        enviarCorreo(empresa, invitacion, token);
        return respuesta(invitacion);
    }

    @Transactional(readOnly = true)
    public DetalleInvitacionUsuarioResponse consultar(String token) {
        InvitacionUsuarioEmpresaEntidad invitacion = validarVigencia(invitaciones.findFirstByTokenHash(hash(token))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La invitación no existe")));
        EmpresaEntidad empresa = empresas.findById(invitacion.getEmpresaId()).orElseThrow();
        return new DetalleInvitacionUsuarioResponse(empresa.getNombre(), invitacion.getCorreo(),
                invitacion.getNombreCompleto(), invitacion.getPuesto(), invitacion.getExpiraEn());
    }

    @Transactional
    public UsuarioInternoAdminResponse aceptar(AceptarInvitacionUsuarioRequest request) {
        InvitacionUsuarioEmpresaEntidad invitacion = resolverVigente(request.token());
        PerfilRegistroSocial perfilSocial = null;
        if (request.registroSocialToken() != null && !request.registroSocialToken().isBlank()) {
            perfilSocial = socialGoogle.validarTokenRegistro(request.registroSocialToken());
            if (!invitacion.getCorreo().equalsIgnoreCase(perfilSocial.correo())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cuenta social no corresponde al correo invitado");
            }
        }
        UsuarioInternoAdminRequest alta = new UsuarioInternoAdminRequest(
                invitacion.getSucursalId(), listaLong(invitacion.getSucursalIdsJson()), invitacion.getCorreo(),
                request.contrasena(), invitacion.getNombreCompleto(), invitacion.getTelefono(), invitacion.getPuesto(),
                invitacion.getRolEmpresaId(), listaString(invitacion.getPermisosDirectosJson()), true, invitacion.getNotas());
        UsuarioInternoAdminResponse usuario = admin.crearUsuarioInterno(
                invitacion.getEmpresaId(), invitacion.getCreadaPorUsuarioId(), alta);
        if (perfilSocial != null) {
            UsuarioIdentidadExternaEntidad identidad = new UsuarioIdentidadExternaEntidad();
            identidad.setUsuarioId(usuario.usuarioId());
            identidad.setProveedor(perfilSocial.proveedor());
            identidad.setSubjectProveedor(perfilSocial.subject());
            identidad.setCorreoVerificado(perfilSocial.correo());
            identidad.setCreadoEn(LocalDateTime.now());
            identidades.save(identidad);
        }
        invitacion.setEstado("ACEPTADA");
        invitacion.setAceptadaEn(LocalDateTime.now());
        invitacion.setActualizadoEn(LocalDateTime.now());
        invitaciones.save(invitacion);
        return usuario;
    }

    private InvitacionUsuarioEmpresaEntidad resolverVigente(String token) {
        InvitacionUsuarioEmpresaEntidad invitacion = invitaciones.findByTokenHash(hash(token))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La invitación no existe"));
        return validarVigencia(invitacion);
    }

    private InvitacionUsuarioEmpresaEntidad validarVigencia(InvitacionUsuarioEmpresaEntidad invitacion) {
        if (!"PENDIENTE".equals(invitacion.getEstado()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La invitación ya fue utilizada o cancelada");
        if (invitacion.getExpiraEn().isBefore(LocalDateTime.now()))
            throw new ResponseStatusException(HttpStatus.GONE, "La invitación expiró; solicita una nueva");
        return invitacion;
    }

    private void enviarCorreo(EmpresaEntidad empresa, InvitacionUsuarioEmpresaEntidad invitacion, String token) {
        ConfiguracionCorreoResolvida config = configuracionCorreo.resolver(empresa.getId());
        if (!config.habilitado()) throw new IllegalStateException("No hay un correo canónico o del tenant disponible");
        String enlace = urlAceptacion + "?token=" + token;
        String plataforma = propiedadesCorreo.nombreRemitentePorDefecto();
        String soporte = propiedadesCorreo.responderAPorDefecto();
        String texto = "Hola " + invitacion.getNombreCompleto() + ",\n\n" + empresa.getNombre()
                + " te invitó a formar parte de su equipo en " + plataforma + ".\nCrea tu contraseña y acepta la invitación aquí:\n"
                + enlace + "\n\nEl enlace vence en " + horasVigencia + " horas y solo puede utilizarse una vez.\n\n¿Dudas? " + soporte;
        String html = "<div style=\"font-family:Arial,sans-serif;max-width:600px;margin:auto;color:#173b57\">"
                + "<h1>Te damos la bienvenida a " + escapar(empresa.getNombre()) + "</h1><p>Hola "
                + escapar(invitacion.getNombreCompleto()) + ",</p><p>Has recibido una invitación para formar parte del equipo en " + escapar(plataforma) + ".</p>"
                + "<p><a style=\"background:#6c4cff;color:white;padding:12px 20px;border-radius:8px;text-decoration:none;display:inline-block\" href=\""
                + enlace + "\">Aceptar invitación</a></p><p>Este enlace vence en " + horasVigencia + " horas y solo puede utilizarse una vez.</p>"
                + "<p style=\"color:#607080\">Si no esperabas esta invitación, ignora este mensaje.<br>¿Dudas? " + escapar(soporte) + "</p></div>";
        clienteCorreo.enviar(config, new MensajeCorreoSaliente(invitacion.getCorreo(),
                "Invitación para unirte a " + empresa.getNombre() + " en " + plataforma, texto, html, config.responderA(), List.of()));
    }

    private String generarToken() { byte[] bytes = new byte[32]; secureRandom.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private String hash(String token) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); } catch (Exception ex) { throw new IllegalStateException(ex); } }
    private String json(Object valor) { try { return objectMapper.writeValueAsString(valor == null ? List.of() : valor); } catch (Exception ex) { throw new IllegalArgumentException(ex); } }
    private List<Long> listaLong(String valor) { try { return objectMapper.readValue(valor, new TypeReference<>() {}); } catch (Exception ex) { throw new IllegalStateException(ex); } }
    private List<String> listaString(String valor) { try { return objectMapper.readValue(valor, new TypeReference<>() {}); } catch (Exception ex) { throw new IllegalStateException(ex); } }
    private InvitacionUsuarioResponse respuesta(InvitacionUsuarioEmpresaEntidad i) { return new InvitacionUsuarioResponse(i.getId(), i.getCorreo(), i.getNombreCompleto(), i.getEstado(), i.getExpiraEn()); }
    private String escapar(String valor) { return valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
}
