package com.techprotech.agenda.modulos.admin.api.dto;

import com.techprotech.agenda.modulos.citas.api.dto.CitaClienteResponse;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
public record CitaAgendaAdminResponse(@JsonUnwrapped CitaClienteResponse cita, boolean puedeCerrar, boolean puedeCancelar) {}
