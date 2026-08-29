package com.techprotech.agenda.compartido.whatsapp;

import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class ContextoEmpresaWhatsapp {

    private final ThreadLocal<Long> empresaActual = new ThreadLocal<>();

    public <T> T ejecutar(Long empresaId, Supplier<T> operacion) {
        Long anterior = empresaActual.get();
        empresaActual.set(empresaId);
        try {
            return operacion.get();
        } finally {
            restaurar(anterior);
        }
    }

    public void ejecutar(Long empresaId, Runnable operacion) {
        ejecutar(empresaId, () -> {
            operacion.run();
            return null;
        });
    }

    public Long requerirEmpresaId() {
        Long empresaId = empresaActual.get();
        if (empresaId == null) {
            throw new IllegalStateException("No hay un tenant de WhatsApp asociado a la operacion actual");
        }
        return empresaId;
    }

    private void restaurar(Long anterior) {
        if (anterior == null) {
            empresaActual.remove();
        } else {
            empresaActual.set(anterior);
        }
    }
}
