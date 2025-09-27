package com.service.service;

import com.app.dto.InventarioCostoDto;
import com.app.dto.ResumenInstrumentoDto;
import com.app.entities.SaldoEntity;
import com.app.interfaces.AbstractRepository;
import com.app.interfaces.KardexApiInterfaz;
import com.app.interfaces.SaldoApiInterfaz;
import com.service.repositorio.AggregatesForInstrument;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ResumenPortafolioService extends AbstractRepository {

    private final SaldoApiInterfaz saldoService;
    private final KardexApiInterfaz kardexService;
    private final AggregatesForInstrument aggregatesOpt;

    public ResumenPortafolioService(SaldoApiInterfaz saldoService, KardexApiInterfaz kardexService, AggregatesForInstrument aggregatesOpt) {
        this.saldoService = saldoService;
        this.kardexService = kardexService;
        this.aggregatesOpt = aggregatesOpt;
    }

    public List<ResumenInstrumentoDto> obtenerResumenPortafolio(Long empresaId, Long custodioId, String cuenta) {
        List<ResumenInstrumentoDto> resumenCompleto = new ArrayList<>();
        
        List<InventarioCostoDto> inventario = kardexService.obtenerSaldosFinalesPorGrupoYCuenta(empresaId, custodioId, cuenta);

        for (InventarioCostoDto item : inventario) {
            if (item.getSaldoCantidadFinal() == null || item.getSaldoCantidadFinal().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            ResumenInstrumentoDto dto = new ResumenInstrumentoDto();
            dto.setInstrumentoId(item.getInstrumentoId());
            dto.setNemo(item.getInstrumentoNemo());
            dto.setNombreInstrumento(item.getInstrumentoNombre());
            dto.setSaldoDisponible(item.getSaldoCantidadFinal());
            dto.setCostoFifo(item.getCostoTotalFifo());

            Optional<SaldoEntity> ultimoSaldoOpt = this.saldoService.obtenerUltimoSaldo(empresaId, custodioId, cuenta, item.getInstrumentoId());
            if (ultimoSaldoOpt.isPresent()) {
                BigDecimal precioMercado = ultimoSaldoOpt.get().getPrecio();
                BigDecimal valorMercado = precioMercado.multiply(item.getSaldoCantidadFinal());
                dto.setValorDeMercado(valorMercado);
                dto.setUtilidadNoRealizada(valorMercado.subtract(item.getCostoTotalFifo()));
            }

            Optional<Object[]> aggregatesOpt = this.aggregatesOpt.getAggregatesForInstrument(empresaId, custodioId, cuenta, item.getInstrumentoId());
            if (aggregatesOpt.isPresent()) {
                Object[] aggregates = aggregatesOpt.get();
                dto.setTotalDividendos((BigDecimal) aggregates[0]);
                dto.setTotalGastos((BigDecimal) aggregates[1]);
            }

            /**
             * Por implementar método calcularUtilidadRealizadaParaInstrumento.
             */
            BigDecimal utilidadRealizada = calcularUtilidadRealizadaParaInstrumento(empresaId, custodioId, cuenta, item.getInstrumentoId());
            dto.setUtilidadRealizada(utilidadRealizada);
            
            // --- CÁLCULO DE RENTABILIDAD CORREGIDO (POR INSTRUMENTO) ---
            
            // Aseguramos que los valores no sean nulos para el cálculo
            BigDecimal ur = dto.getUtilidadRealizada() != null ? dto.getUtilidadRealizada() : BigDecimal.ZERO;
            BigDecimal unr = dto.getUtilidadNoRealizada() != null ? dto.getUtilidadNoRealizada() : BigDecimal.ZERO;
            BigDecimal dividendos = dto.getTotalDividendos() != null ? dto.getTotalDividendos() : BigDecimal.ZERO;
            BigDecimal gastos = dto.getTotalGastos() != null ? dto.getTotalGastos() : BigDecimal.ZERO;
            BigDecimal costo = dto.getCostoFifo();

            if (costo != null && costo.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal numerador = ur.add(unr).add(dividendos).subtract(gastos);
                BigDecimal rentabilidad = numerador.divide(costo, 4, RoundingMode.HALF_UP);
                dto.setRentabilidad(rentabilidad);
            }

            resumenCompleto.add(dto);
        }
        
        if (!resumenCompleto.isEmpty()) {
            ResumenInstrumentoDto totales = new ResumenInstrumentoDto();
            totales.setNemo("TOTALES");

            // Sumamos cada columna
            totales.setCostoFifo(resumenCompleto.stream().map(ResumenInstrumentoDto::getCostoFifo).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
            totales.setValorDeMercado(resumenCompleto.stream().map(ResumenInstrumentoDto::getValorDeMercado).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
            totales.setTotalDividendos(resumenCompleto.stream().map(ResumenInstrumentoDto::getTotalDividendos).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
            totales.setTotalGastos(resumenCompleto.stream().map(ResumenInstrumentoDto::getTotalGastos).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
            totales.setUtilidadRealizada(resumenCompleto.stream().map(ResumenInstrumentoDto::getUtilidadRealizada).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
            totales.setUtilidadNoRealizada(resumenCompleto.stream().map(ResumenInstrumentoDto::getUtilidadNoRealizada).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
            
            // --- CÁLCULO DE RENTABILIDAD CORREGIDO (PARA TOTALES) ---
            BigDecimal costoTotal = totales.getCostoFifo();
            if (costoTotal != null && costoTotal.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal numeradorTotal = totales.getUtilidadRealizada()
                                                 .add(totales.getUtilidadNoRealizada())
                                                 .add(totales.getTotalDividendos())
                                                 .subtract(totales.getTotalGastos());
                
                BigDecimal rentabilidadTotal = numeradorTotal.divide(costoTotal, 4, RoundingMode.HALF_UP);
                totales.setRentabilidad(rentabilidadTotal);
            }
            
            resumenCompleto.add(totales);
        }
        return resumenCompleto;
    }
    
    private BigDecimal calcularUtilidadRealizadaParaInstrumento(Long empresaId, Long custodioId, String cuenta, Long instrumentoId) {
        return BigDecimal.ZERO;
    }
}