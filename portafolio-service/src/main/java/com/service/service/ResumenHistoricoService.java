package com.service.service;

import com.app.dto.ResumenHistoricoDto;
import com.app.interfaces.AbstractRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import com.app.interfaces.ResumenHistoricoInterfaz;

public class ResumenHistoricoService extends AbstractRepository {
    
    private final ResumenHistoricoInterfaz repository;

    public ResumenHistoricoService(ResumenHistoricoInterfaz repository) {
        this.repository = repository;
    }

    public List<ResumenHistoricoDto> generarReporte(Long empresaId, Long custodioId, String cuenta) {
        List<ResumenHistoricoDto> reporte = repository.obtenerResumenHistorico(empresaId, custodioId, cuenta);

        if (reporte != null && !reporte.isEmpty()) {
            ResumenHistoricoDto totales = new ResumenHistoricoDto("TOTALES");
            
            // Corrección en el cálculo de totales: usar los getters correctos del DTO.
            BigDecimal totalGasto = reporte.stream().map(ResumenHistoricoDto::getTotalGasto).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalDividendo = reporte.stream().map(ResumenHistoricoDto::getTotalDividendo).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalUtilidad = reporte.stream().map(ResumenHistoricoDto::getTotalUtilidad).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalTotal = reporte.stream().map(ResumenHistoricoDto::getTotalTotal).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

            // Usar los setters correspondientes.
            totales.setTotalGasto(totalGasto);
            totales.setTotalDividendo(totalDividendo);
            totales.setTotalUtilidad(totalUtilidad);
            totales.setTotalTotal(totalTotal);
            
            reporte.add(totales);
        }
        return reporte;
    }
}