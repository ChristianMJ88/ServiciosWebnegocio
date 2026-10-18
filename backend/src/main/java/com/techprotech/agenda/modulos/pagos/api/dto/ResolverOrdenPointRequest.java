package com.techprotech.agenda.modulos.pagos.api.dto;
public record ResolverOrdenPointRequest(@jakarta.validation.constraints.NotNull Boolean cobrado,
        @jakarta.validation.constraints.Size(max=180) @jakarta.validation.constraints.Pattern(regexp="[A-Za-z0-9_-]*") String proveedorOrdenId,
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min=10,max=500) String motivo) {}
