
package com.app.dao;

import com.app.entities.CustodioEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustodioDao extends AbstractJpaDao<CustodioEntity, Long> {

    private static final Logger logger = LoggerFactory.getLogger(CustodioEntity.class);
    private static final String CUSTODIO_POR_DEFINIR = "POR DEFINIR";

    public CustodioDao(final EntityManager entityManager) {
        super(entityManager, CustodioEntity.class);
    }

    public Optional<CustodioEntity> findByNombre(String nombre) {
        try {
            TypedQuery<CustodioEntity> query = entityManager.createQuery(
                    "SELECT c FROM CustodioEntity c WHERE c.nombreCustodio = :nombre", CustodioEntity.class);
            query.setParameter("nombre", nombre);
            return Optional.ofNullable(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Error al buscar custodio por nombre: {} - {}", nombre, e.getMessage());
            throw new RuntimeException("Fallo al buscar custodio por nombre.", e);
        }
    }

    public CustodioEntity findOrCreateByNombre(String nombre) {
        String normalizedNombre = (nombre == null || nombre.trim().isEmpty())
                ? CUSTODIO_POR_DEFINIR
                : nombre.trim();

        Optional<CustodioEntity> existingCustodio = findByNombre(normalizedNombre);

        if (existingCustodio.isPresent()) {
            return existingCustodio.get();
        } else {
            CustodioEntity newCustodio = new CustodioEntity();
            newCustodio.setNombreCustodio(normalizedNombre);
            newCustodio.setFechaCreacion(LocalDate.now());

            try {
                create(newCustodio);
                entityManager.flush();
                return newCustodio;
            } catch (Exception e) {
                logger.error("Error al crear y persistir nuevo custodio: {} - {}", normalizedNombre, e.getMessage());
                throw new RuntimeException("Fallo al crear y persistir nuevo custodio.", e);
            }
        }
    }
}