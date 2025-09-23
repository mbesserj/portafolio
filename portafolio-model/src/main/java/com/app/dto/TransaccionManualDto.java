package com.app.dto; 

import java.math.BigDecimal;
import java.time.LocalDate;

// Usamos 'record' de Java moderno para un DTO inmutable y conciso.
public record TransaccionManualDto(
        Long empresaId,
        Long custodioId,
        Long instrumentoId,
        Long tipoMovimientoId,
        String cuenta,
        String folio,
        LocalDate fecha,
        BigDecimal cantidad,
        BigDecimal precio,
        BigDecimal comisiones,
        BigDecimal gastos,
        BigDecimal iva,
        String glosa,
        String moneda
) {}