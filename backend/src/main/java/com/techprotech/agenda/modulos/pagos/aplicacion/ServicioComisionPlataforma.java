package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.pagos.dominio.ProveedorPago;
import com.techprotech.agenda.modulos.pagos.dominio.TipoComisionPlataforma;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.ReglaComisionPlataformaEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.ProveedorPagoRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.ReglaComisionPlataformaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class ServicioComisionPlataforma {
    private static final BigDecimal CERO = new BigDecimal("0.00");
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final ReglaComisionPlataformaRepositorio reglas;
    private final ProveedorPagoRepositorio proveedores;

    public ServicioComisionPlataforma(ReglaComisionPlataformaRepositorio reglas,
                                      ProveedorPagoRepositorio proveedores) {
        this.reglas = reglas;
        this.proveedores = proveedores;
    }

    @Transactional(readOnly = true)
    public BigDecimal calcular(Long empresaId, Long proveedorId, BigDecimal importe, String moneda) {
        validar(empresaId, proveedorId, importe, moneda);
        return reglas.resolver(proveedorId, empresaId, moneda.trim(), LocalDateTime.now(ZoneOffset.UTC)).stream()
                .findFirst().map(regla -> calcular(regla, importe)).orElse(CERO);
    }

    @Transactional(readOnly = true)
    public BigDecimal calcular(Long empresaId, ProveedorPago proveedor, BigDecimal importe, String moneda) {
        Long proveedorId = proveedores.findByCodigoAndActivoTrue(proveedor.name())
                .orElseThrow(() -> new IllegalStateException("Proveedor de pago no disponible"))
                .getId();
        return calcular(empresaId, proveedorId, importe, moneda);
    }

    private BigDecimal calcular(ReglaComisionPlataformaEntidad regla, BigDecimal importe) {
        return calcularConfiguracion(regla.getTipo(), regla.getValor(), regla.getImporteMinimo(), regla.getImporteMaximo(), importe);
    }

    public BigDecimal calcularConfiguracion(TipoComisionPlataforma tipo, BigDecimal valor,
                                             BigDecimal importeMinimo, BigDecimal importeMaximo,
                                             BigDecimal importe) {
        if (tipo == null || valor == null) throw new IllegalArgumentException("Tipo y valor son obligatorios");
        if (importe == null || importe.signum() <= 0) throw new IllegalArgumentException("El importe debe ser mayor a cero");
        BigDecimal comision = switch (tipo) {
            case PORCENTAJE -> importe.multiply(valor).divide(CIEN, 4, RoundingMode.HALF_UP);
            case MONTO_FIJO -> valor;
        };
        if (importeMinimo != null && comision.compareTo(importeMinimo) < 0) comision = importeMinimo;
        if (importeMaximo != null && comision.compareTo(importeMaximo) > 0) comision = importeMaximo;
        if (comision.signum() < 0) throw new IllegalStateException("La comisión de plataforma no puede ser negativa");
        if (comision.compareTo(importe) > 0) throw new IllegalStateException("La comisión de plataforma excede el importe del cobro");
        return comision.setScale(2, RoundingMode.HALF_UP);
    }

    private void validar(Long empresaId, Long proveedorId, BigDecimal importe, String moneda) {
        if (empresaId == null || proveedorId == null) throw new IllegalArgumentException("Empresa y proveedor son obligatorios");
        if (importe == null || importe.signum() <= 0) throw new IllegalArgumentException("El importe debe ser mayor a cero");
        if (moneda == null || moneda.isBlank()) throw new IllegalArgumentException("La moneda es obligatoria");
    }
}
