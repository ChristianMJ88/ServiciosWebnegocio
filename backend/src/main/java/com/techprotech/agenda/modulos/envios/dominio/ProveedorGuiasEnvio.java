package com.techprotech.agenda.modulos.envios.dominio;

import java.math.BigDecimal;
import java.util.List;

public interface ProveedorGuiasEnvio {
    String codigo();

    Guia generar(String token, String ambiente, Solicitud solicitud);

    Rastreo rastrear(String token, String ambiente, String numeroGuia);

    record Solicitud(Direccion origen, Direccion destino, List<Paquete> paquetes, String moneda,
                     String paqueteria, String servicio, String referencia) {}
    record Direccion(String nombre, String empresa, String correo, String telefono, String calle, String numero,
                     String colonia, String municipio, String estado, String pais, String codigoPostal,
                     String referencias) {}
    record Paquete(String contenido, BigDecimal cantidad, BigDecimal valorDeclarado, BigDecimal pesoKg,
                   BigDecimal largoCm, BigDecimal anchoCm, BigDecimal altoCm) {}
    record Guia(String proveedorEnvioId, String numeroGuia, String urlEtiqueta, String urlRastreo,
                BigDecimal costo, String estadoProveedor) {}
    record Rastreo(String estado, String descripcion, String referenciaExterna) {}
}
