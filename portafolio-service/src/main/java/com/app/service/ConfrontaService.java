package com.app.service;

import com.app.dto.ConfrontaSaldoDto;
import com.app.interfaces.AbstractRepository;
import jakarta.persistence.NoResultException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import com.app.interfaces.ConfrontaRepInterfaz;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio que orquesta la confrontación de saldos.
 */
public class ConfrontaService extends AbstractRepository {

    private static final String QUERY_ULTIMA_FECHA_SALDOS = 
        "SELECT MAX(s.fecha) FROM SaldoEntity s";
    
    private static final Logger logger = LoggerFactory.getLogger(ConfrontaService.class);

    private final ConfrontaRepInterfaz confrontaRepository;

    /**
     * Constructor que recibe el repositorio por inyección de dependencias.
     * 
     * @param confrontaRepository El repositorio de confronta
     * @throws IllegalArgumentException si confrontaRepository es null
     */
    public ConfrontaService(ConfrontaRepInterfaz confrontaRepository) {
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

                logger.info("Iniciando confronta de saldos con fecha de corte: {}", fechaCorte);
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
            return (LocalDate) em.createQuery(QUERY_ULTIMA_FECHA_SALDOS).getSingleResult();
        } catch (NoResultException e) {
            logger.debug("No se encontraron registros de saldos en la base de datos");
            return null;
        }
    }
}
