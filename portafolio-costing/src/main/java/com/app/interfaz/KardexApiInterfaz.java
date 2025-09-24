package com.app.interfaz;

import com.app.dto.InventarioCostoDto;
import com.app.dto.KardexReporteDto;
import com.app.entities.KardexEntity;
import com.app.entities.TransaccionEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface KardexApiInterfaz {
        
    /**
     * 
     * @param claveAgrupacion
     * @return 
     */
    int deleteKardexByClaveAgrupacion(String claveAgrupacion);
    
    /**
     * 
     * @param claveAgrupacion
     * @return 
     */
    int deleteDetalleCosteoByClaveAgrupacion(String claveAgrupacion);
    
    /**
     * 
     * @param empresaId             Id de la empresa
     * @param custodioId            Id de custodio
     * @param cuenta                Cuenta del custodio
     * @param instrumentoId         Id instrumento
     * @return                      Entrega los movimientos del kardex para estos filtros.
     */
    List<KardexReporteDto> obtenerMovimientosPorGrupo(Long empresaId, Long custodioId, String cuenta, Long instrumentoId);
    
    /**
     * 
     * @param empresaId             Id de la empresa
     * @param custodioId            Id del custodii
     * @return                      Entrega el costo de los saldos finales del articulo.
     */
    List<InventarioCostoDto> obtenerSaldosFinalesPorGrupo(Long empresaId, Long custodioId);
    
    /**
     * 
     * @param transaccion
     * @return 
     */
    Optional<KardexEntity> obtenerUltimoSaldoAntesDe(TransaccionEntity transaccion);
    
    /**
     * 
     * @param empresaId
     * @param cuenta
     * @param custodioId
     * @param instrumentoId
     * @return 
     */
    Optional<KardexEntity> findLastByGroup(Long empresaId, String cuenta, Long custodioId, Long instrumentoId);
    
    /**
     * 
     * @param empresaId
     * @param custodioId
     * @param instrumentoId
     * @param cuenta
     * @param fechaInicio
     * @param fechaFin
     * @return 
     */
    List<KardexEntity> findByGroupAndDateRange(Long empresaId, Long custodioId, Long instrumentoId, String cuenta, LocalDate fechaInicio, LocalDate fechaFin);
    
    /**
     * 
     * @param empresaId
     * @param cuenta
     * @param custodioId
     * @param instrumentoId
     * @param fecha
     * @return 
     */
    Optional<KardexEntity> findLastByGroupBeforeDate(Long empresaId, String cuenta, Long custodioId, Long instrumentoId, LocalDate fecha);
    
    /**
     * 
     * @param empresaId
     * @param custodioId
     * @param instrumentoId
     * @param cuenta
     * @return 
     */
    int deleteSaldoKardexByGrupo(Long empresaId, Long custodioId, Long instrumentoId, String cuenta);
    
    /**
     * 
     * @param empresaId
     * @param custodioId
     * @param cuenta
     * @return 
     */
    List<InventarioCostoDto> obtenerSaldosFinalesPorGrupoYCuenta(Long empresaId, Long custodioId, String cuenta);
    
}