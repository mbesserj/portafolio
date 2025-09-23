package com.app.repository;

import com.app.dto.AjustePropuestoDto;
import com.app.dto.CostingGroupDTO;
import com.app.enums.TipoAjuste;
import com.app.exception.CostingException;
import java.math.BigDecimal;
import java.util.List;

public interface CostingService {

    // --- Métodos de Costeo ---
    void runFullCosting() throws CostingException;
    void reCostGroup(String groupKey) throws CostingException;
    List<CostingGroupDTO> getCostingGroups() throws CostingException;

    // --- MÉTODOS PARA AJUSTES ---
    AjustePropuestoDto proponerAjusteManual(Long txReferenciaId, TipoAjuste tipo);
    void crearAjusteManual(Long txReferenciaId, TipoAjuste tipo, BigDecimal cantidadFinal, BigDecimal precioFinal);
    void eliminarAjusteManual(Long idAjuste) throws CostingException;
    
    // --- METODOS PARA COSTEO POR GRUPOS ---
    List<CostingGroupDTO> obtenerGruposDeCosteo();

}