package com.serv.service;

import com.model.dto.ConfrontaSaldoDto;
import com.model.interfaces.AbstractRepository;
import com.model.interfaces.ConfrontaRepository;
import jakarta.persistence.NoResultException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.app.sql.QueryRepository;

/**
 * Servicio que orquesta la confrontación de saldos.
 */
public class ConfrontaService extends AbstractRepository {

    private static final Logger logger = LoggerFactory.getLogger(ConfrontaService.class);

    private final ConfrontaRepository confrontaRepository;

    /**
     * Constructor que recibe el repositorio por inyección de dependencias.
     * 
     * @param confrontaRepository El repositorio de confronta
     * @throws IllegalArgumentException si confrontaRepository es null
     */
    public ConfrontaService(ConfrontaRepository confrontaRepository) {
        if (confrontaRepository == null) {
            throw new IllegalArgumentException("ConfrontaRepository no puede ser null");
        }
        this.confrontaRepository = confrontaRepository;
    }

    /**
     * Obtiene las diferencias de saldos para la fecha de corte más reciente.
     * 
     * @return Lista de diferencias de saldos, o lista vacía si no hay datos
     */
    public List<ConfrontaSaldoDto> obtenerDiferenciasDeSaldos() {
        return executeReadOnly(em -> {
            try {
                LocalDate fechaCorte = obtenerUltimaFechaDeSaldos(em);
                if (fechaCorte == null) {
                    logger.warn("No se encontraron registros en la tabla de saldos para determinar una fecha de corte");
                    return Collections.emptyList();
                }
                List<ConfrontaSaldoDto> diferencias = confrontaRepository.obtenerDiferenciasDeSaldos(fechaCorte);
                logger.info("Se encontraron {} diferencias de saldos", diferencias.size());
                return diferencias;
                
            } catch (Exception e) {
                logger.error("Error en el servicio de confronta", e);
                return Collections.emptyList();
            }
        });
    }

    private LocalDate obtenerUltimaFechaDeSaldos(jakarta.persistence.EntityManager em) {
        try {
            String sql_ultima_feche_saldos = QueryRepository.getConfrontaQuery(QueryRepository.ConfrontaQueries.ULTIMA_FECHA_SALDOS_QUERY);
            return (LocalDate) em.createQuery(sql_ultima_feche_saldos).getSingleResult();
        } catch (NoResultException e) {
            logger.debug("No se encontraron registros de saldos en la base de datos");
            return null;
        }
    }
}