package com.techprotech.agenda.compartido.correo;

public record AdjuntoCorreoSaliente(
        String nombreArchivo,
        String tipoContenido,
        byte[] contenido
) {
}
