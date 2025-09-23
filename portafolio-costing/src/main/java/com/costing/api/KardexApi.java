package com.costing.api;

import com.app.dto.InventarioCostoDto;
import com.app.dto.KardexReporteDto;
import com.app.entities.KardexEntity;
import com.app.entities.SaldoEntity;
import com.app.entities.TransaccionEntity;
import com.app.repositorios.KardexRepositoryImpl;
import com.app.repositorios.SaldosRepositoryImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class KardexApi {

    private KardexRepositoryImpl kardexService;
    private SaldosRepositoryImpl saldosService;

    public int deleteKardexByClaveAgrupacion(String claveAgrupacion) {
        return kardexService.deleteKardexByClaveAgrupacion(claveAgrupacion);
    }

    public int deleteDetalleCosteoByClaveAgrupacion(String claveAgrupacion) {
        return kardexService.deleteDetalleCosteoByClaveAgrupacion(claveAgrupacion);
    }

    public List<KardexReporteDto> obtenerMovimientosPorGrupo(Long empresaId, Long custodioId, String cuenta, Long instrumentoId) {
        return kardexService.obtenerMovimientosPorGrupo(empresaId, custodioId, cuenta, instrumentoId);
    }

    public List<InventarioCostoDto> obtenerSaldosFinalesPorGrupo(Long empresaId, Long custodioId) {
        return kardexService.obtenerSaldosFinalesPorGrupo(empresaId, custodioId);
    }

    public Optional<KardexEntity> obtenerUltimoSaldoAntesDe(TransaccionEntity transaccion) {
        return kardexService.obtenerUltimoSaldoAntesDe(transaccion);
    }

    public Optional<KardexEntity> findLastByGroup(Long empresaId, String cuenta, Long custodioId, Long instrumentoId) {
        return kardexService.findLastByGroup(empresaId, cuenta, custodioId, instrumentoId);
    }

    public Optional<KardexEntity> findLastByGroupBeforeDate(Long empresaId, String cuenta, Long custodioId, Long instrumentoId, LocalDate fecha) {
        return kardexService.findLastByGroupBeforeDate(empresaId, cuenta, custodioId, instrumentoId, LocalDate.EPOCH);
    }

    public List<KardexEntity> findByGroupAndDateRange(Long empresaId, Long custodioId, Long instrumentoId, String cuenta, LocalDate fechaInicio, LocalDate fechaFin) {
        return kardexService.findByGroupAndDateRange(empresaId, custodioId, instrumentoId, cuenta, fechaInicio, fechaFin);
    }

    public Optional<SaldoEntity> obtenerUltimoSaldo(Long empresaId, Long custodioId, String cuenta, Long instrumentoId) {
        return saldosService.obtenerUltimoSaldo(empresaId, custodioId, cuenta, instrumentoId);
    }

    public List<InventarioCostoDto> obtenerSaldosFinalesPorGrupoYCuenta(Long empresaId, Long custodioId, String cuenta) {
        return null;
    }
    
    public  BigDecimal calcularUtilidadRealizadaParaInstrumento(Long empresaId, Long custodioId, String cuenta, Long instrumentoId) {
        return null;
    }
}