package com.techprotech.agenda.modulos.plataforma.api.dto;

import java.math.BigDecimal;

public record SimulacionComisionPlataformaResponse(BigDecimal importeBase, BigDecimal comisionFluora,
                                                    BigDecimal netoAntesComisionProveedor, String moneda) {}
