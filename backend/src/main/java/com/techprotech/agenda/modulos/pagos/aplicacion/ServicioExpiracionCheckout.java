package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioExpiracionCheckout {
    private final CheckoutOrdenRepositorio ordenes;
    private final IntentoPagoCheckoutRepositorio intentos;
    private final ServicioInventarioCheckout inventario;
    private final ServicioConfirmacionCitasCheckout citas;
    private final EntityManager entityManager;
    private final Clock reloj;

    public ServicioExpiracionCheckout(CheckoutOrdenRepositorio ordenes, IntentoPagoCheckoutRepositorio intentos,
            ServicioInventarioCheckout inventario, ServicioConfirmacionCitasCheckout citas, EntityManager entityManager, Clock reloj) {
        this.ordenes=ordenes; this.intentos=intentos; this.inventario=inventario;
        this.citas=citas; this.entityManager=entityManager; this.reloj=reloj;
    }

    @Transactional
    public void expirarEnProceso(Long empresaId, Long ordenId, Long intentoId, LocalDateTime limite) {
        var orden=ordenes.bloquearPorEmpresaEId(empresaId,ordenId).orElseThrow();
        entityManager.refresh(orden,LockModeType.PESSIMISTIC_WRITE);
        var ultimo=intentos.findFirstByEmpresaIdAndCheckoutOrdenIdOrderByIdDesc(empresaId,ordenId);
        if (ultimo.isEmpty() || !intentoId.equals(ultimo.get().getId())) return;
        var intento=ultimo.get(); entityManager.refresh(intento,LockModeType.PESSIMISTIC_WRITE);
        if (!"PAGO_EN_PROCESO".equals(orden.getEstado()) || !Set.of("PROCESANDO","REQUIERE_ACCION").contains(intento.getEstado())) return;
        var vencimiento=intento.getPagoExpiraEn()==null?limite:intento.getPagoExpiraEn();
        if (vencimiento.isAfter(LocalDateTime.now(reloj))) return;
        inventario.expirar(empresaId,ordenId); citas.liberar(empresaId,ordenId);
        orden.setEstado("EXPIRADA"); ordenes.save(orden);
    }
}
