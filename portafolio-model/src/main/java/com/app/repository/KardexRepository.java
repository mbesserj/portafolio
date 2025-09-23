package com.app.repository;

import com.app.dto.InventarioCostoDto;
import com.app.dto.KardexReporteDto;
import com.app.entities.KardexEntity;
import com.app.entities.SaldoKardexEntity;
import com.app.entities.TransaccionEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface KardexRepository {
    
    // Métodos para el recosteo
    int deleteKardexByClaveAgrupacion(String claveAgrupacion);
    int deleteDetalleCosteoByClaveAgrupacion(String claveAgrupacion);

    // Métodos de consulta
    List<KardexReporteDto> obtenerMovimientosPorGrupo(Long empresaId, Long custodioId, String cuenta, Long instrumentoId);
    List<InventarioCostoDto> obtenerSaldosFinalesPorGrupo(Long empresaId, Long custodioId);
    Optional<KardexEntity> obtenerUltimoSaldoAntesDe(TransaccionEntity transaccion);
    Optional<KardexEntity> findLastByGroup(Long empresaId, String cuenta, Long custodioId, Long instrumentoId);
    List<KardexEntity> findByGroupAndDateRange(Long empresaId, Long custodioId, Long instrumentoId, String cuenta, LocalDate fechaInicio, LocalDate fechaFin);
    Optional<KardexEntity> findLastByGroupBeforeDate(Long empresaId, String cuenta, Long custodioId, Long instrumentoId, LocalDate fecha);
    int deleteSaldoKardexByGrupo(Long empresaId, Long custodioId, Long instrumentoId, String cuenta);
   
}