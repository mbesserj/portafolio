package com.serv.service;

import com.model.dto.OperacionesTrxsDto;
import com.model.enums.TipoEnumsCosteo;
import com.model.interfaces.AbstractRepository;
import com.app.sql.QueryRepository;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OperacionesTrxsService extends AbstractRepository {

    private static final Logger logger = LoggerFactory.getLogger(OperacionesTrxsService.class);

    /**
     * Constructor.
     */
    public OperacionesTrxsService() {
        super();
    }
    
    /**
     * Obtiene las transacciones de un grupo específico con cálculos de saldos acumulados.
     * 
     * @param empresa Razón social de la empresa
     * @param custodio Nombre del custodio
     * @param cuenta Nombre de la cuenta
     * @param nemos Lista de nemos de instrumentos
     * @return Lista de operaciones con saldos calculados
     */
    public List<OperacionesTrxsDto> obtenerTransaccionesPorGrupo(String empresa, String custodio, 
                                                                String cuenta, List<String> nemos) {
        // Validaciones
        if (empresa == null || empresa.trim().isEmpty()) {
            throw new IllegalArgumentException("La empresa no puede ser nula o vacía");
        }
        if (custodio == null || custodio.trim().isEmpty()) {
            throw new IllegalArgumentException("El custodio no puede ser nulo o vacío");
        }
        if (cuenta == null || cuenta.trim().isEmpty()) {
            throw new IllegalArgumentException("La cuenta no puede ser nula o vacía");
        }
        if (nemos == null || nemos.isEmpty()) {
            logger.debug("Lista de nemos vacía para grupo [empresa: {}, custodio: {}, cuenta: {}]", 
                        empresa, custodio, cuenta);
            return Collections.emptyList();
        }

        return executeReadOnly(em -> {
            try {
                String sql_operaciones = QueryRepository.getOperacionesQuery(QueryRepository.OperacionesQueries.OPERACIONES_QUERY);
                TypedQuery<OperacionesTrxsDto> query = em.createQuery(sql_operaciones, OperacionesTrxsDto.class);
                query.setParameter("empresa", empresa.trim());
                query.setParameter("custodio", custodio.trim());
                query.setParameter("cuenta", cuenta.trim());
                query.setParameter("nemos", nemos);
                query.setParameter("tipoIngreso", TipoEnumsCosteo.INGRESO);
                query.setParameter("tipoEgreso", TipoEnumsCosteo.EGRESO);

                List<OperacionesTrxsDto> transaccionesDto = query.getResultList();
                logger.debug("Se obtuvieron {} transacciones para el grupo", transaccionesDto.size());

                // Calcular saldos acumulados
                calcularSaldosAcumulados(transaccionesDto);
                
                return transaccionesDto;
                
            } catch (Exception e) {
                logger.error("Error al obtener transacciones por grupo [empresa: {}, custodio: {}, cuenta: {}]", 
                           empresa, custodio, cuenta, e);
                return Collections.emptyList();
            }
        });
    }

    private void calcularSaldosAcumulados(List<OperacionesTrxsDto> transacciones) {
        BigDecimal saldoAcumulado = BigDecimal.ZERO;
        
        for (OperacionesTrxsDto dto : transacciones) {
            BigDecimal compras = dto.getCompras() != null ? dto.getCompras() : BigDecimal.ZERO;
            BigDecimal ventas = dto.getVentas() != null ? dto.getVentas() : BigDecimal.ZERO;
            BigDecimal total = dto.getTotal() != null ? dto.getTotal() : BigDecimal.ZERO;

            // Normalizar el total según el tipo contable
            if (dto.getTipoContable() == TipoEnumsCosteo.INGRESO) {
                total = total.abs();
            } else if (dto.getTipoContable() == TipoEnumsCosteo.EGRESO) {
                total = total.abs().negate();
            }
            dto.setTotal(total);

            saldoAcumulado = saldoAcumulado.add(compras).subtract(ventas);
            dto.setSaldoAcumulado(saldoAcumulado);
        }
    }
}