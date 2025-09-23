package com.app.service;

import com.app.dto.ConfrontaSaldoDto;
import com.app.repository.ConfrontaRepository; 
import com.app.utiles.LibraryInitializer;
import com.app.repositorio.ConfrontaRepositoryImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio que orquesta la confrontación de saldos.
 * Delega la ejecución de la consulta al ConfrontaRepository.
 */
public class ConfrontaService {

    private static final Logger logger = LoggerFactory.getLogger(ConfrontaService.class);
    private final ConfrontaRepository confrontaRepository;

    public ConfrontaService() {
        this.confrontaRepository = new ConfrontaRepositoryImpl();
    }

    public List<ConfrontaSaldoDto> obtenerDiferenciasDeSaldos() {
        EntityManager em = null; 
        try {
            em = LibraryInitializer.getEntityManager(); 

            LocalDate fechaCorte = obtenerUltimaFechaDeSaldos(em); 
            if (fechaCorte == null) {
                logger.warn("No se encontraron registros en la tabla de saldos para determinar una fecha de corte.");
                return Collections.emptyList();
            }

            logger.info("Iniciando confronta de saldos con fecha de corte: {}", fechaCorte);
 
            return confrontaRepository.obtenerDiferenciasDeSaldos(fechaCorte);
            
        } catch (Exception e) {
            logger.error("Error en el servicio de confronta.", e);
            return Collections.emptyList();
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    private LocalDate obtenerUltimaFechaDeSaldos(EntityManager em) {
        try {
            return (LocalDate) em.createQuery("SELECT MAX(s.fecha) FROM SaldoEntity s").getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}