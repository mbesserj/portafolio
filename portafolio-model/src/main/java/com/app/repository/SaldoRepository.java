package com.app.repository;

import com.app.dto.ResumenSaldoDto;
import com.app.entities.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Contrato UNIFICADO para TODAS las operaciones de consulta sobre Saldos.
 * Consolida las responsabilidades de Saldo, SaldosDiarios y SaldoKardex.
 */
public interface SaldoRepository {

    // --- Métodos de SaldosDiarios (del SaldosDiariosRepositoryImpl original) ---
    Optional<SaldosDiariosEntity> findLastBeforeDate(EmpresaEntity empresa, CustodioEntity custodio, InstrumentoEntity instrumento, String cuenta, LocalDate fecha);
    List<SaldosDiariosEntity> findAllByGroupAndDateRange(EmpresaEntity empresa, CustodioEntity custodio, InstrumentoEntity instrumento, String cuenta, LocalDate fechaInicio, LocalDate fechaFin);
    
    // --- Métodos de Saldo General (del SaldoRepositoryImpl original) ---
    Optional<SaldoEntity> buscarSaldoMasCercanoAFecha(Long empresaId, Long custodioId, Long instrumentoId, String cuenta, LocalDate fechaObjetivo);
    Optional<SaldoEntity> obtenerUltimoSaldo(Long empresaId, Long custodioId, String cuenta, Long instrumentoId);
    List<ResumenSaldoDto> obtenerResumenSaldosAgregados(Long empresaId, Long custodioId);
    
    Optional<SaldoKardexEntity> findByGrupo(Long empresaId, Long custodioId, Long instrumentoId, String cuenta);
}