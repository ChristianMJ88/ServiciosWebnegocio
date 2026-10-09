package com.techprotech.agenda.modulos.plataforma.infraestructura.stripe;

import com.fasterxml.jackson.databind.JsonNode;

public interface ConsultaSuscripcionStripe {
    record Resultado(JsonNode suscripcion, JsonNode factura) {}
    Resultado consultar(String id) throws Exception;
}
