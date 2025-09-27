package com.app.repositorio;

import com.app.interfaces.AbstractRepository;
import com.app.sql.QueryRepository;
import static com.app.sql.QueryRepository.AggregatesQueries.AGGREGATES_QUERY;
import jakarta.persistence.Query;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AggregatesForInstrument extends AbstractRepository {

    private static final Logger logger = LoggerFactory.getLogger(AggregatesForInstrument.class);

    public Optional<Object[]> getAggregatesForInstrument(Long empresaId, Long custodioId, String cuenta, Long instrumentoId) {
        return executeReadOnly(em -> {
            try {
                String sql = QueryRepository.getAggregatesQuery(AGGREGATES_QUERY);
                Query query = em.createNativeQuery(sql);
                query.setParameter("empresaId", empresaId);
                query.setParameter("custodioId", custodioId);
                query.setParameter("cuenta", cuenta);
                query.setParameter("instrumentoId", instrumentoId);

                return Optional.ofNullable((Object[]) query.getSingleResult());
            } catch (Exception e) {
                logger.error("Error al obtener agregados para instrumento", e);
                return Optional.empty();
            }
        });
    }
}
