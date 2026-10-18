package com.techprotech.agenda.modulos.pagos.api.dto;

public record TerminalPointResponse(Long id, String terminalId, String posId, String storeId, String externalPosId,
                                    String nombre, String modoOperacion, Long sucursalId, String sucursalNombre,
                                    boolean activa, boolean configurada) {
    @com.fasterxml.jackson.annotation.JsonProperty("metodoPago")
    public com.techprotech.agenda.modulos.caja.api.dto.CatalogoCajaResponse.OpcionCajaResponse metodoPago() {
        return new com.techprotech.agenda.modulos.caja.api.dto.CatalogoCajaResponse.OpcionCajaResponse(
            com.techprotech.agenda.modulos.caja.aplicacion.CatalogosCaja.METODO_PAGO_POINT,"Tarjeta (Point)");
    }
}
