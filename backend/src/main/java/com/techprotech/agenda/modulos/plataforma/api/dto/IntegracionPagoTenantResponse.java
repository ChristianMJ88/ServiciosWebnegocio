package com.techprotech.agenda.modulos.plataforma.api.dto;

import java.time.LocalDateTime;

public record IntegracionPagoTenantResponse(Long proveedorId, String proveedorNombre, String estado,
                                            String ambiente, LocalDateTime conectadoEn,
                                            LocalDateTime ultimaSincronizacionEn) {}
