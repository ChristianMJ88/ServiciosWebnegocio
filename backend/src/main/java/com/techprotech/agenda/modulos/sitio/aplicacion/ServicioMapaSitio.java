package com.techprotech.agenda.modulos.sitio.aplicacion;

import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

@Service
public class ServicioMapaSitio {
    private final SucursalRepositorio sucursalRepositorio;

    public ServicioMapaSitio(SucursalRepositorio sucursalRepositorio) {
        this.sucursalRepositorio = sucursalRepositorio;
    }

    public List<UbicacionMapaSitioPublicaResponse> obtenerParaSitioPublico(Long empresaId) {
        return sucursalRepositorio.findByEmpresaIdAndActivaTrue(empresaId).stream()
                .filter(sucursal -> sucursal.getDireccion() != null && !sucursal.getDireccion().isBlank())
                .sorted(Comparator.comparing(SucursalEntidad::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(this::mapear)
                .toList();
    }

    private UbicacionMapaSitioPublicaResponse mapear(SucursalEntidad sucursal) {
        String direccion = sucursal.getDireccion().trim();
        String consulta = URLEncoder.encode(direccion, StandardCharsets.UTF_8);
        return new UbicacionMapaSitioPublicaResponse(
                sucursal.getId(),
                sucursal.getNombre(),
                direccion,
                normalizarOpcional(sucursal.getTelefono()),
                "https://www.google.com/maps?q=" + consulta + "&output=embed",
                "https://www.google.com/maps/search/?api=1&query=" + consulta
        );
    }

    private String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
