package com.techprotech.agenda.modulos.sitio.aplicacion;

import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioConfigEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ServicioSitioPublico {

    private final EmpresaSitioConfigRepositorio empresaSitioConfigRepositorio;

    public ServicioSitioPublico(EmpresaSitioConfigRepositorio empresaSitioConfigRepositorio) {
        this.empresaSitioConfigRepositorio = empresaSitioConfigRepositorio;
    }

    public SitioPublicoResponse obtenerPorSlug(String slug) {
        EmpresaSitioConfigEntidad sitio = empresaSitioConfigRepositorio.findBySlug(slug)
                .filter(EmpresaSitioConfigEntidad::isPublicado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un sitio publicado para ese tenant"));

        return mapearSitio(sitio);
    }

    public SitioPublicoResponse obtenerPorHostname(String hostname) {
        EmpresaSitioConfigEntidad sitio = empresaSitioConfigRepositorio.findByDominioPrincipalIgnoreCase(hostname)
                .filter(EmpresaSitioConfigEntidad::isPublicado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un sitio publicado para ese dominio"));

        return mapearSitio(sitio);
    }

    public IndiceSitiosPublicosResponse obtenerIndicePublicado() {
        return new IndiceSitiosPublicosResponse(List.copyOf(empresaSitioConfigRepositorio.findSlugsPublicados()));
    }

    private SitioPublicoResponse mapearSitio(EmpresaSitioConfigEntidad sitio) {
        return new SitioPublicoResponse(
                sitio.getEmpresaId(),
                sitio.getSlug(),
                sitio.getNombreComercial(),
                sitio.getDominioPrincipal(),
                sitio.getLogoUrl(),
                sitio.getDescripcionCorta(),
                sitio.getColorPrimario(),
                sitio.getColorSecundario(),
                sitio.getFuenteTitulos(),
                sitio.getFuenteCuerpo(),
                sitio.getHeroTitulo(),
                sitio.getHeroSubtitulo(),
                sitio.getHeroImagenUrl(),
                sitio.getHeroEtiqueta(),
                sitio.getHeroAlineacion(),
                sitio.getHeroOverlay(),
                sitio.getHeroTextoCta(),
                sitio.getHeroAccionCta(),
                sitio.isHeroMostrarWhatsapp(),
                sitio.getInicioTituloServicios(),
                sitio.getInicioDescripcionServicios(),
                sitio.getInicioEtiquetaServicios(),
                sitio.getInicioTextoVerServicios(),
                sitio.getInicioTextoReservar(),
                sitio.getInicioTituloContacto(),
                sitio.getInicioDescripcionContacto(),
                sitio.getInicioEtiquetaContacto(),
                sitio.getInicioTextoAgenda(),
                sitio.getInicioTextoWhatsapp(),
                sitio.isInicioMostrarContacto(),
                sitio.isInicioMostrarServicios(),
                sitio.getInicioOrdenSecciones(),
                sitio.getSeoTitulo(),
                sitio.getSeoDescripcion(),
                sitio.getWhatsapp(),
                sitio.getWhatsappMensaje(),
                sitio.getTelefono(),
                sitio.getCorreo(),
                sitio.getDireccion(),
                sitio.getInstagramUrl(),
                sitio.getFacebookUrl(),
                sitio.getXUrl(),
                sitio.getTiktokUrl(),
                sitio.isWhatsappHabilitado(),
                sitio.isInstagramHabilitado(),
                sitio.isFacebookHabilitado(),
                sitio.isXHabilitado(),
                sitio.isTiktokHabilitado(),
                sitio.isChatHabilitado(),
                sitio.getChatTitulo(),
                sitio.getChatSaludo(),
                sitio.getTema(),
                sitio.getMenuEstilo(),
                sitio.isMenuMostrarInicio(),
                sitio.isMenuMostrarServicios(),
                sitio.isMenuMostrarContacto(),
                sitio.isMenuMostrarRegistro(),
                sitio.isMenuMostrarDescripcion(),
                sitio.getMenuTextoCta(),
                sitio.getMenuAccionCta(),
                sitio.getFooterTexto(),
                sitio.isFooterMostrarNavegacion(),
                sitio.isFooterMostrarContacto(),
                sitio.isPublicado()
        );
    }
}
