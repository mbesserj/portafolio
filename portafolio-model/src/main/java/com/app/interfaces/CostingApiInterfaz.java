package com.app.interfaces;

import com.app.dto.AjustePropuestoDto;
import com.app.dto.CostingGroupDTO;
import com.app.enums.TipoAjuste;
import com.app.exception.CostingException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Interfaz pública y único punto de entrada para el módulo de costeo.
 * Define el contrato que las librerías externas utilizarán.
 */
public interface CostingApiInterfaz {

    /**
     * Ejecuta el proceso de costeo para todas las transacciones pendientes.
     * @throws CostingException si ocurre un error durante el proceso.
     */
    void ejecutarCosteoCompleto() throws CostingException;

    /**
     * Revierte y re-ejecuta el costeo para un grupo específico.
     * @param claveGrupo La clave única del grupo a recostear.
     * @throws CostingException si ocurre un error durante el proceso.
     */
    void recostearGrupo(String claveGrupo) throws CostingException;

    /**
     * Obtiene una lista de todos los grupos de costeo existentes.
     * @return una Lista de DTOs con la información de los grupos.
     * @throws CostingException si ocurre un error al consultar los datos.
     */
    List<CostingGroupDTO> obtenerGruposCosteo() throws CostingException;

    /**
     * Genera una propuesta de ajuste para una transacción marcada para revisión.
     * @param transaccionId El ID de la transacción de referencia.
     * @param tipo El tipo de ajuste a proponer (INGRESO o EGRESO).
     * @return un DTO con los detalles del ajuste propuesto.
     * @throws CostingException si ocurre un error al generar la propuesta.
     */
    AjustePropuestoDto proponerAjuste(Long transaccionId, TipoAjuste tipo) throws CostingException;

    /**
     * Crea una nueva transacción de ajuste manual.
     * @param transaccionId El ID de la transacción original de referencia.
     * @param tipo El tipo de ajuste (INGRESO o EGRESO).
     * @param cantidad La cantidad del ajuste.
     * @param precio El precio del ajuste.
     * @throws CostingException si ocurre un error al crear el ajuste.
     */
    void crearAjuste(Long transaccionId, TipoAjuste tipo, BigDecimal cantidad, BigDecimal precio) throws CostingException;

    /**
     * Elimina una transacción de ajuste manual y sus registros de costeo asociados.
     * @param ajusteId El ID de la transacción de ajuste a eliminar.
     * @throws CostingException si ocurre un error durante la eliminación.
     */
    void eliminarAjuste(Long ajusteId) throws CostingException;
    
    /**
     * 
     * @return 
     */
    List<CostingGroupDTO> obtenerGruposDeCosteo();
}