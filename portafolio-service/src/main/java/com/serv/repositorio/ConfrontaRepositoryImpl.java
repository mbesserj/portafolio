package com.serv.repositorio;

import com.model.dto.ConfrontaSaldoDto;
import com.model.interfaces.AbstractRepository;
import com.model.interfaces.ConfrontaRepository;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.app.sql.QueryRepository;

public class ConfrontaRepositoryImpl extends AbstractRepository implements ConfrontaRepository {

    private static final Logger logger = LoggerFactory.getLogger(ConfrontaRepositoryImpl.class);

    @Override
    @SuppressWarnings("unchecked")
    public List<ConfrontaSaldoDto> obtenerDiferenciasDeSaldos(LocalDate fechaCorte) {
        return executeReadOnly(em -> {
            try {
                String sql = QueryRepository.getConfrontaQuery(QueryRepository.ConfrontaQueries.CONFRONTA_SALDOS_QUERY);
                Query query = em.createNativeQuery(sql, "ConfrontaSaldoMapping");
                query.setParameter("fechaCorte", fechaCorte);
                return query.getResultList();
            } catch (Exception e) {
                logger.error("Error al ejecutar la consulta de confronta de saldos", e);
                return Collections.emptyList();
            }
        });
    }
}