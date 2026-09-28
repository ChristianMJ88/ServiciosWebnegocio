package com.techprotech.agenda.modulos.envios.dominio;

import java.math.BigDecimal;
import java.util.List;

public interface ProveedorCotizacionesEnvio {
    List<Opcion> cotizar(String token, String ambiente, Solicitud solicitud);

    record Solicitud(Direccion origen, Direccion destino, List<Paquete> paquetes, String moneda) {}
    record Direccion(String nombre, String empresa, String correo, String telefono, String calle, String numero,
                     String colonia, String municipio, String estado, String pais, String codigoPostal,
                     String referencias) {}
    record Paquete(String contenido, BigDecimal cantidad, BigDecimal valorDeclarado, BigDecimal pesoKg,
                   BigDecimal largoCm, BigDecimal anchoCm, BigDecimal altoCm) {}
    record Opcion(String paqueteria, String paqueteriaNombre, String servicio, String servicioNombre,
                  String entregaEstimada, BigDecimal costo, String moneda) {}
}
