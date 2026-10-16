package com.techprotech.agenda.compartido.tiempo;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import java.time.DateTimeException;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Resuelve la zona configurada, sin depender de la JVM ni mantener otro reloj. */
@Component
public class ZonaNegocio {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZonaNegocio.class);
    private static final ZoneId RESPALDO = ZoneId.of("America/Mexico_City");
    private final EmpresaRepositorio empresas;
    private final SucursalRepositorio sucursales;

    public ZonaNegocio(EmpresaRepositorio empresas, SucursalRepositorio sucursales) {
        this.empresas = empresas;
        this.sucursales = sucursales;
    }

    public ZoneId de(Long empresaId, Long sucursalId) {
        if (empresaId == null) return de((EmpresaEntidad) null, (SucursalEntidad) null);
        SucursalEntidad sucursal = sucursalId == null ? null
                : sucursales.findByIdAndEmpresaId(sucursalId, empresaId).orElse(null);
        ZoneId zona = sucursal == null ? null : validar(sucursal.getZonaHoraria());
        if (zona != null) return zona;
        return de(empresas.findById(empresaId).orElse(null), sucursal);
    }

    public ZoneId de(EmpresaEntidad empresa, SucursalEntidad sucursal) {
        if (empresa != null && sucursal != null && empresa.getId().equals(sucursal.getEmpresaId())) {
            ZoneId zona = validar(sucursal.getZonaHoraria());
            if (zona != null) return zona;
        }
        ZoneId zona = empresa == null ? null : validar(empresa.getZonaHoraria());
        if (zona != null) return zona;
        LOGGER.warn("Sin zona horaria válida para empresa {} y sucursal {}; se usa {}",
                empresa == null ? null : empresa.getId(), sucursal == null ? null : sucursal.getId(), RESPALDO);
        return RESPALDO;
    }

    private ZoneId validar(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return ZoneId.of(valor.trim());
        } catch (DateTimeException ex) {
            return null;
        }
    }
}
