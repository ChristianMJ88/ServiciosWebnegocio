package com.techprotech.agenda.modulos.politicas.aplicacion;

import com.techprotech.agenda.modulos.politicas.api.dto.*;
import com.techprotech.agenda.modulos.politicas.dominio.ModoNoShow;
import com.techprotech.agenda.modulos.politicas.infraestructura.entidad.PoliticaEmpresaEntidad;
import com.techprotech.agenda.modulos.politicas.infraestructura.repositorio.PoliticaEmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.admin.infraestructura.entidad.AuditoriaConfiguracionEmpresaEntidad;
import com.techprotech.agenda.modulos.admin.infraestructura.repositorio.AuditoriaConfiguracionEmpresaRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioConfigEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServicioPoliticaEmpresa {
    private final PoliticaEmpresaRepositorio politicas;
    private final EmpresaRepositorio empresas;
    private final EmpresaSitioConfigRepositorio sitios;
    private final AuditoriaConfiguracionEmpresaRepositorio auditoria;
    private final PredeterminadosPolitica predeterminados;
    private final Clock clock;
    private final com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio servicios;
    private final com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.EmpresaCheckoutConfigRepositorio checkout;
    private static final String PLANTILLA = "ANTICIPOS_V1";

    public Optional<PoliticaEmpresaEntidad> vigente(Long empresaId) {
        return politicas.findFirstByEmpresaIdOrderByVersionDesc(empresaId);
    }

    public PoliticaEmpresaEntidad exigirPublicada(Long empresaId) {
        return vigente(empresaId).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                "Publica la política de anticipos y cancelación en Parámetros antes de habilitar el cobro online"));
    }

    public PoliticaPublicaResponse publica(String slug) {
        var sitio = sitios.findBySlug(slug.trim().toLowerCase(Locale.ROOT))
                .filter(EmpresaSitioConfigEntidad::isPublicado).orElseThrow(this::noDisponible);
        return respuesta(sitio, vigente(sitio.getEmpresaId()).orElseThrow(this::noDisponible));
    }

    public PoliticaAdminResponse obtener(Long empresaId) {
        if (empresaId == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        var sitio = sitios.findById(empresaId).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Configura primero el sitio público del negocio antes de publicar su política"));
        var actual = vigente(empresaId).orElse(null);
        var borrador = actual == null ? new PublicarPoliticaRequest(0, predeterminados.getModoNoShow(),
                predeterminados.getDiasReagendar(), predeterminados.getDiasAclaracion(), predeterminados.getReagendadosMax(),
                predeterminados.getRecordatorio1Dias(), predeterminados.getRecordatorio2Dias(), null,
                sitio.getCorreo(), sitio.getTelefono()) : solicitud(actual);
        return new PoliticaAdminResponse(actual == null ? null : respuesta(sitio, actual), borrador,
                texto(borrador), Arrays.stream(ModoNoShow.values())
                .map(m -> new PoliticaAdminResponse.Opcion(m.name(), m.etiqueta())).toList(),
                actual == null && (servicios.existsByEmpresaIdAndActivoTrueAndRequiereAnticipoTrue(empresaId)
                    || checkout.findById(empresaId).map(c -> c.isHabilitado()).orElse(false))
                ? "Tus servicios con anticipo no pueden reservarse en línea hasta que publiques tu política" : null);
    }

    public String vistaPrevia(PublicarPoliticaRequest request) {
        validar(request);
        return texto(request);
    }

    @Transactional
    public PoliticaAdminResponse publicar(UsuarioAutenticado actor, PublicarPoliticaRequest request) {
        if (actor.esPlataforma() || actor.empresaId() == null || actor.usuarioId() == null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        validar(request);
        empresas.bloquearPorId(actor.empresaId()).orElseThrow(this::noDisponible);
        var anterior = vigente(actor.empresaId()).orElse(null);
        int version = anterior == null ? 0 : anterior.getVersion();
        if (!Objects.equals(request.versionBase(), version))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La política cambió. Actualiza antes de publicar otra versión");
        String generado = texto(request);
        String huella = huella(request, generado);
        if (anterior != null && huella.equals(anterior.getTextoSha256())) return obtener(actor.empresaId());
        var nueva = new PoliticaEmpresaEntidad();
        nueva.setEmpresaId(actor.empresaId()); nueva.setVersion(version + 1);
        nueva.setModoNoShow(request.modoNoShow().name()); nueva.setDiasAclaracion(request.diasAclaracion());
        boolean credito = request.modoNoShow() == ModoNoShow.CREDITO_REAGENDAR;
        nueva.setDiasReagendar(credito ? request.diasReagendar() : null);
        nueva.setReagendadosMax(credito ? request.reagendadosMax() : null);
        nueva.setRecordatorio1Dias(credito ? request.recordatorio1Dias() : null);
        nueva.setRecordatorio2Dias(credito ? request.recordatorio2Dias() : null);
        nueva.setTextoAdicional(limpiar(request.textoAdicional()));
        nueva.setContactoReclamosCorreo(limpiar(request.contactoReclamosCorreo()));
        nueva.setContactoReclamosTelefono(limpiar(request.contactoReclamosTelefono()));
        nueva.setPlantillaTexto(PLANTILLA); nueva.setTextoGenerado(generado); nueva.setTextoSha256(huella);
        nueva.setPublicadaEn(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        nueva.setPublicadaPorUsuarioId(actor.usuarioId());
        try { politicas.saveAndFlush(nueva); }
        catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La política cambió. Actualiza antes de publicar otra versión", ex);
        }
        var evento = new AuditoriaConfiguracionEmpresaEntidad();
        evento.setEmpresaId(actor.empresaId()); evento.setUsuarioActorId(actor.usuarioId()); evento.setActorCorreo(actor.correo());
        evento.setModulo("POLITICAS"); evento.setAccion("PUBLICAR_VERSION");
        evento.setResumen("Política publicada: versión " + nueva.getVersion());
        evento.setDetalleAntesJson("{\"version\":" + version + "}");
        evento.setDetalleDespuesJson("{\"version\":" + nueva.getVersion() + ",\"sha256\":\"" + huella + "\"}");
        auditoria.save(evento);
        return obtener(actor.empresaId());
    }

    private void validar(PublicarPoliticaRequest r) {
        if (r.versionBase() == null || r.versionBase() < 0 || r.modoNoShow() == null
                || !positivo(r.diasAclaracion()) || (limpiar(r.contactoReclamosCorreo()) == null
                && limpiar(r.contactoReclamosTelefono()) == null))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica plazos válidos y un contacto del negocio para aclaraciones");
        if (r.modoNoShow() == ModoNoShow.CREDITO_REAGENDAR && (!positivo(r.diasReagendar())
                || !positivo(r.reagendadosMax()) || !positivo(r.recordatorio1Dias()) || !positivo(r.recordatorio2Dias())
                || r.diasReagendar() > r.diasAclaracion() || r.recordatorio1Dias() >= r.diasReagendar()
                || r.recordatorio2Dias() >= r.recordatorio1Dias()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El plazo para reagendar no debe superar la aclaración; los recordatorios deben ser positivos, distintos y anteriores al vencimiento");
    }

    private String texto(PublicarPoliticaRequest r) {
        String modo = r.modoNoShow() == ModoNoShow.RETENER
                ? "Si no asistes, el anticipo se retiene y no genera crédito para reagendar."
                : "Si no asistes, puedes solicitar reagendar dentro de " + dias(r.diasReagendar())
                  + " desde la cita, hasta " + r.reagendadosMax()
                  + (Integer.valueOf(1).equals(r.reagendadosMax()) ? " vez." : " veces.")
                  + " El crédito se aplica a servicios de cualquier sucursal del mismo negocio;"
                  + " si el nuevo servicio cuesta más, se cobra la diferencia. El sobrante vence con el plazo.";
        return "Política de anticipos y cancelación.\n\n" + modo
                + "\n\nAnte una inasistencia no hay reembolso automático. Las aclaraciones se atienden directamente"
                + " con el negocio dentro de " + dias(r.diasAclaracion())
                + " desde la cita. Un reembolso excepcional requiere autorización del negocio."
                + "\n\nSi el negocio cancela una cita con anticipo, debe acordar contigo un crédito sin penalización"
                + " o un reembolso. Las aclaraciones sobre el servicio y sus pagos corresponden al negocio.";
    }

    private String huella(PublicarPoliticaRequest r, String texto) {
        // Incluye recordatorios privados: cambiar cualquier regla crea una versión nueva.
        String valor = PLANTILLA + "\n" + texto + "\n" + limpiar(r.textoAdicional()) + "\n"
                + limpiar(r.contactoReclamosCorreo()) + "\n" + limpiar(r.contactoReclamosTelefono())
                + (r.modoNoShow() == ModoNoShow.CREDITO_REAGENDAR ? "\n" + r.recordatorio1Dias() + ":" + r.recordatorio2Dias() : "");
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private PublicarPoliticaRequest solicitud(PoliticaEmpresaEntidad p) {
        return new PublicarPoliticaRequest(p.getVersion(), ModoNoShow.valueOf(p.getModoNoShow()), p.getDiasReagendar(),
                p.getDiasAclaracion(), p.getReagendadosMax(), p.getRecordatorio1Dias(), p.getRecordatorio2Dias(),
                p.getTextoAdicional(), p.getContactoReclamosCorreo(), p.getContactoReclamosTelefono());
    }

    private PoliticaPublicaResponse respuesta(EmpresaSitioConfigEntidad sitio, PoliticaEmpresaEntidad p) {
        return new PoliticaPublicaResponse(sitio.getNombreComercial(), p.getVersion(), p.getPublicadaEn().atOffset(ZoneOffset.UTC),
                ModoNoShow.valueOf(p.getModoNoShow()), p.getDiasReagendar(), p.getDiasAclaracion(), p.getReagendadosMax(),
                p.getTextoGenerado(), p.getTextoAdicional(), new PoliticaPublicaResponse.Contacto(
                p.getContactoReclamosCorreo(), p.getContactoReclamosTelefono()));
    }

    private String dias(int n) { return n + (n == 1 ? " día natural" : " días naturales"); }

    private boolean positivo(Integer valor) { return valor != null && valor > 0; }
    private String limpiar(String valor) { return valor == null || valor.isBlank() ? null : valor.trim(); }
    private ResponseStatusException noDisponible() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Política no disponible"); }
}
