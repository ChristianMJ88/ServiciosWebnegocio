package com.techprotech.agenda.modulos.autenticacion.social;

import com.techprotech.agenda.modulos.autenticacion.api.dto.EmpresaAccesoAppResponse;
import com.techprotech.agenda.modulos.autenticacion.api.dto.RespuestaAccesoApp;
import com.techprotech.agenda.modulos.autenticacion.aplicacion.ServicioAutenticacion;
import com.techprotech.agenda.modulos.autenticacion.aplicacion.ServicioRolesEmpresa;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class ServicioAccesoSocial {
  private final UsuarioIdentidadExternaRepositorio identidades;
  private final CodigoAccesoSocialRepositorio codigos;
  private final UsuarioRepositorio usuarios;
  private final ServicioAutenticacion autenticacion;
  private final EmpresaRepositorio empresas;
  private final ServicioRolesEmpresa roles;
  private final SecureRandom random = new SecureRandom();

  public ServicioAccesoSocial(UsuarioIdentidadExternaRepositorio identidades,
                              CodigoAccesoSocialRepositorio codigos, UsuarioRepositorio usuarios,
                              ServicioAutenticacion autenticacion, EmpresaRepositorio empresas,
                              ServicioRolesEmpresa roles) {
    this.identidades = identidades;
    this.codigos = codigos;
    this.usuarios = usuarios;
    this.autenticacion = autenticacion;
    this.empresas = empresas;
    this.roles = roles;
  }

  @Transactional
  public String crearCodigo(PerfilRegistroSocial perfil) {
    var identidadesVinculadas = identidades.findAllByProveedorAndSubjectProveedor(
      perfil.proveedor(), perfil.subject());
    if (identidadesVinculadas.isEmpty()) {
      vincularPorCorreoVerificado(perfil);
      identidadesVinculadas = identidades.findAllByProveedorAndSubjectProveedor(
        perfil.proveedor(), perfil.subject());
    }
    if (identidadesVinculadas.isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Esta cuenta aún no está vinculada; crea tu cuenta primero");
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String codigo = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    CodigoAccesoSocialEntidad entidad = new CodigoAccesoSocialEntidad();
    entidad.setProveedor(perfil.proveedor());
    entidad.setSubjectProveedor(perfil.subject());
    entidad.setTokenHash(hash(codigo));
    entidad.setCreadoEn(LocalDateTime.now());
    entidad.setExpiraEn(LocalDateTime.now().plusMinutes(5));
    codigos.save(entidad);
    return codigo;
  }

  private void vincularPorCorreoVerificado(PerfilRegistroSocial perfil) {
    String correo = perfil.correo().trim().toLowerCase();
    LocalDateTime ahora = LocalDateTime.now();
    usuarios.findByCorreoOrderByEmpresaIdAsc(correo).stream()
      .filter(usuario -> usuario.isHabilitado() && !usuario.isBloqueado())
      .filter(usuario -> !identidades.existsByUsuarioIdAndProveedor(usuario.getId(), perfil.proveedor()))
      .forEach(usuario -> {
        UsuarioIdentidadExternaEntidad identidad = new UsuarioIdentidadExternaEntidad();
        identidad.setUsuarioId(usuario.getId());
        identidad.setProveedor(perfil.proveedor());
        identidad.setSubjectProveedor(perfil.subject());
        identidad.setCorreoVerificado(correo);
        identidad.setCreadoEn(ahora);
        identidades.save(identidad);
        if (usuario.getCorreoVerificadoEn() == null) {
          usuario.setCorreoVerificadoEn(ahora);
          usuarios.save(usuario);
        }
      });
  }

  @Transactional
  public RespuestaAccesoApp intercambiar(String codigo, Long empresaId) {
    CodigoAccesoSocialEntidad entidad = codigos.findByTokenHash(hash(codigo))
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código social no válido"));
    if (entidad.getUsadoEn() != null || entidad.getExpiraEn().isBefore(LocalDateTime.now())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El código social expiró o ya fue utilizado");
    }
    java.util.List<UsuarioEntidad> candidatos = identidades
      .findAllByProveedorAndSubjectProveedor(entidad.getProveedor(), entidad.getSubjectProveedor()).stream()
      .map(UsuarioIdentidadExternaEntidad::getUsuarioId)
      .distinct()
      .map(usuarios::findById)
      .flatMap(java.util.Optional::stream)
      .filter(usuario -> usuario.isHabilitado() && !usuario.isBloqueado())
      .toList();
    if (candidatos.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta no está disponible");
    if (empresaId == null && candidatos.size() > 1) {
      return new RespuestaAccesoApp("SELECCION_EMPRESA", "Selecciona la empresa a la que deseas entrar.",
        candidatos.stream().map(this::empresaAcceso).toList(), null);
    }
    UsuarioEntidad usuario = empresaId == null ? candidatos.getFirst() : candidatos.stream()
      .filter(candidato -> candidato.getEmpresaId().equals(empresaId)).findFirst()
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a la empresa seleccionada"));
    entidad.setUsadoEn(LocalDateTime.now());
    codigos.save(entidad);
    return new RespuestaAccesoApp("AUTENTICADO", "Inicio de sesión correcto.", null,
      autenticacion.emitirSesionOnboarding(usuario.getEmpresaId(), usuario.getId()));
  }

  private EmpresaAccesoAppResponse empresaAcceso(UsuarioEntidad usuario) {
    var empresa = empresas.findById(usuario.getEmpresaId())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La empresa ya no existe"));
    return new EmpresaAccesoAppResponse(empresa.getId(), empresa.getSlug(), empresa.getNombre(),
      roles.obtenerCodigosRolUsuario(empresa.getId(), usuario.getId()),
      roles.obtenerPermisosUsuario(empresa.getId(), usuario.getId()));
  }

  private String hash(String valor) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
        .digest(valor.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }
}
