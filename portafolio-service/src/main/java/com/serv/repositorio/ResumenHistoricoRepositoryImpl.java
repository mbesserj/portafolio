package com.serv.repositorio;

import com.model.dto.ResumenHistoricoDto;
import com.model.interfaces.AbstractRepository;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import com.serv.sql.QueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.model.interfaces.ResumenHistorico;

public class ResumenHistoricoRepositoryImpl extends AbstractRepository implements ResumenHistorico {

    private static final Logger logger = LoggerFactory.getLogger(ResumenHistoricoRepositoryImpl.class);

    @Override
    public List<ResumenHistoricoDto> obtenerResumenHistorico(Long empresaId, Long custodioId, String cuenta) {
        return executeReadOnly(em -> {
            try {
                String sql_resumen_historico_query = QueryRepository.getResumenHistoricoQuery(QueryRepository.ResumenHistoricoQueries.RESUMEN_HISTORICO_QUERY);
                Query query = em.createNativeQuery(sql_resumen_historico_query);
                query.setParameter("empresaId", empresaId);
                query.setParameter("custodioId", custodioId);
                query.setParameter("cuenta", cuenta);

                List<Object[]> results = query.getResultList();
                return mapToResumenHistoricoDto(results);
            } catch (Exception e) {
                logger.error("Error al obtener resumen histórico", e);
                return Collections.emptyList();
            }
        });
    }
    
    private List<ResumenHistoricoDto> mapToResumenHistoricoDto(List<Object[]> results) {
        return results.stream()
                .map(row -> {
                    ResumenHistoricoDto dto = new ResumenHistoricoDto();
                    dto.setNemo((String) row[0]);
                    dto.setNombreInstrumento((String) row[1]);
                    dto.setTotalCostoFifo((BigDecimal) row[2]);
                    dto.setTotalGasto((BigDecimal) row[3]);
                    dto.setTotalDividendo((BigDecimal) row[4]);
                    dto.setTotalUtilidad((BigDecimal) row[5]);
                    dto.setTotalTotal((BigDecimal) row[6]);
                    return dto;
                })
                .collect(Collectors.toList());
    }
}