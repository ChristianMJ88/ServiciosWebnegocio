package com.techprotech.agenda.modulos.productos.aplicacion;

import com.techprotech.agenda.modulos.productos.api.dto.ConfiguracionTiendaRequest;
import com.techprotech.agenda.modulos.productos.api.dto.ConfiguracionTiendaResponse;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ConfiguracionTiendaEntidad;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.ConfiguracionTiendaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ServicioConfiguracionTienda {
    private final ConfiguracionTiendaRepositorio repositorio;

    public ServicioConfiguracionTienda(ConfiguracionTiendaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public ConfiguracionTiendaResponse obtener(Long empresaId) {
        return repositorio.findById(empresaId).map(this::mapear).orElseGet(this::predeterminada);
    }

    @Transactional
    public ConfiguracionTiendaResponse actualizar(Long empresaId, ConfiguracionTiendaRequest request) {
        ConfiguracionTiendaEntidad entidad = repositorio.findById(empresaId).orElseGet(() -> {
            ConfiguracionTiendaEntidad nueva = new ConfiguracionTiendaEntidad();
            nueva.setEmpresaId(empresaId);
            return nueva;
        });
        entidad.setDiasNuevo(request.diasNuevo());
        entidad.setHabilitada(request.habilitada());
        entidad.setMenuMostrarProductos(request.menuMostrarProductos());
        entidad.setTitulo(texto(request.titulo()));
        entidad.setDescripcion(texto(request.descripcion()));
        entidad.setSeoTitulo(texto(request.seoTitulo()));
        entidad.setSeoDescripcion(texto(request.seoDescripcion()));
        entidad.setTextoAgregar(texto(request.textoAgregar()));
        entidad.setCarritoHabilitado(request.carritoHabilitado());
        entidad.setMostrarStock(request.mostrarStock());
        entidad.setPermitirPedidoWhatsapp(request.permitirPedidoWhatsapp());
        return mapear(repositorio.save(entidad));
    }

    private ConfiguracionTiendaResponse mapear(ConfiguracionTiendaEntidad entidad) {
        return new ConfiguracionTiendaResponse(
                entidad.isHabilitada(), entidad.isMenuMostrarProductos(), entidad.getTitulo(), entidad.getDescripcion(),
                entidad.getSeoTitulo(), entidad.getSeoDescripcion(), entidad.getTextoAgregar(),
                entidad.isCarritoHabilitado(), entidad.isMostrarStock(), entidad.isPermitirPedidoWhatsapp(), entidad.getDiasNuevo()
        );
    }

    private ConfiguracionTiendaResponse predeterminada() {
        return new ConfiguracionTiendaResponse(false, true, null, null, null, null, null, true, false, true,0);
    }

    private String texto(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }
}
