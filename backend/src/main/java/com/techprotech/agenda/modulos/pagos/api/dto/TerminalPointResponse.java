package com.techprotech.agenda.modulos.pagos.api.dto;

public record TerminalPointResponse(Long id, String terminalId, String posId, String storeId, String externalPosId,
                                    String nombre, String modoOperacion, Long sucursalId, String sucursalNombre,
                                    boolean activa, boolean configurada) {}
