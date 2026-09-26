package com.techprotech.agenda.modulos.sitio.aplicacion;

import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioConfigEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import com.techprotech.agenda.modulos.productos.aplicacion.ServicioConfiguracionTienda;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ServicioSitioPublico {

    private final EmpresaSitioConfigRepositorio empresaSitioConfigRepositorio;
    private final ServicioConstructorSitio servicioConstructorSitio;
    private final ServicioServiciosDestacadosSitio servicioServiciosDestacadosSitio;
    private final ServicioPromocionesSitio servicioPromocionesSitio;
    private final ServicioGaleriaSitio servicioGaleriaSitio;
    private final ServicioBeneficiosSitio servicioBeneficiosSitio;
    private final ServicioEquipoSitio servicioEquipoSitio;
    private final ServicioResenasSitio servicioResenasSitio;
    private final ServicioPreguntasFrecuentesSitio servicioPreguntasFrecuentesSitio;
    private final ServicioMapaSitio servicioMapaSitio;
    private final ServicioConfiguracionTienda servicioConfiguracionTienda;

    public ServicioSitioPublico(
            EmpresaSitioConfigRepositorio empresaSitioConfigRepositorio,
            ServicioConstructorSitio servicioConstructorSitio,
            ServicioServiciosDestacadosSitio servicioServiciosDestacadosSitio,
            ServicioPromocionesSitio servicioPromocionesSitio,
            ServicioGaleriaSitio servicioGaleriaSitio,
            ServicioBeneficiosSitio servicioBeneficiosSitio,
            ServicioEquipoSitio servicioEquipoSitio,
            ServicioResenasSitio servicioResenasSitio,
            ServicioPreguntasFrecuentesSitio servicioPreguntasFrecuentesSitio,
            ServicioMapaSitio servicioMapaSitio,
            ServicioConfiguracionTienda servicioConfiguracionTienda
    ) {
        this.empresaSitioConfigRepositorio = empresaSitioConfigRepositorio;
        this.servicioConstructorSitio = servicioConstructorSitio;
        this.servicioServiciosDestacadosSitio = servicioServiciosDestacadosSitio;
        this.servicioPromocionesSitio = servicioPromocionesSitio;
        this.servicioGaleriaSitio = servicioGaleriaSitio;
        this.servicioBeneficiosSitio = servicioBeneficiosSitio;
        this.servicioEquipoSitio = servicioEquipoSitio;
        this.servicioResenasSitio = servicioResenasSitio;
        this.servicioPreguntasFrecuentesSitio = servicioPreguntasFrecuentesSitio;
        this.servicioMapaSitio = servicioMapaSitio;
        this.servicioConfiguracionTienda = servicioConfiguracionTienda;
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
                sitio.getHeroTipoMedio(),
                sitio.getHeroVideoUrl(),
                sitio.getHeroAltura(),
                sitio.getHeroEtiqueta(),
                sitio.getHeroAlineacion(),
                sitio.getHeroOverlay(),
                sitio.getHeroTextoCta(),
                sitio.getHeroAccionCta(),
                sitio.getHeroEnlaceCta(),
                sitio.getHeroTextoSecundario(),
                sitio.getHeroAccionSecundaria(),
                sitio.getHeroEnlaceSecundario(),
                sitio.isHeroMostrarSecundario(),
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
                servicioConstructorSitio.obtenerParaSitioPublico(sitio.getEmpresaId(), sitio),
                servicioServiciosDestacadosSitio.obtenerParaSitioPublico(sitio.getEmpresaId()),
                servicioGaleriaSitio.obtenerParaSitioPublico(sitio.getEmpresaId()),
                servicioPromocionesSitio.obtenerParaSitioPublico(sitio.getEmpresaId()),
                servicioBeneficiosSitio.obtenerParaSitioPublico(sitio.getEmpresaId()),
                servicioEquipoSitio.obtenerParaSitioPublico(sitio.getEmpresaId()),
                servicioResenasSitio.obtenerParaSitioPublico(sitio.getEmpresaId()),
                servicioPreguntasFrecuentesSitio.obtenerParaSitioPublico(sitio.getEmpresaId()),
                servicioMapaSitio.obtenerParaSitioPublico(sitio.getEmpresaId()),
                servicioConfiguracionTienda.obtener(sitio.getEmpresaId()),
                sitio.isPublicado()
        );
    }
}
