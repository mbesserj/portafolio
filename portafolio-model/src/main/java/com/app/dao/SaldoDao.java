
package com.app.dao;

import com.app.entities.SaldoEntity;
import com.app.utiles.Pk;
import com.app.entities.InstrumentoEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

/**
 * Data Access Object para la entidad SaldoEntity.
 * Extiende AbstractJpaDao para funcionalidades CRUD genéricas.
 * Se ha añadido un método para buscar saldos por sus claves de negocio
 * para la validación de duplicados.
 */
public class SaldoDao extends AbstractJpaDao<SaldoEntity, Pk> {

    private static final Logger logger = LoggerFactory.getLogger(SaldoDao.class);

    public SaldoDao(EntityManager entityManager) {
        super(entityManager, SaldoEntity.class);
    }

    @Override
    public boolean exists(Pk id) {
        try {
            TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(s) FROM SaldoEntity s WHERE s.idAndMonth = :id", Long.class);
            query.setParameter("id", id);
            return query.getSingleResult() > 0;
        } catch (NoResultException e) {
            return false;
        }
    }

    /**
     * Busca un SaldoEntity por su fecha, cuenta e instrumento.
     * Esta combinación de campos se utiliza para detectar registros duplicados.
     *
     * @param fecha La fecha del saldo.
     * @param cuenta El número de cuenta.
     * @param instrumento La entidad de instrumento asociada.
     * @return El SaldoEntity si se encuentra, o null si no existe.
     */
    public SaldoEntity findByFechaAndCuentaAndInstrumento(LocalDate fecha, String cuenta, InstrumentoEntity instrumento) {
        try {
            TypedQuery<SaldoEntity> query = entityManager.createQuery(
                "SELECT s FROM SaldoEntity s WHERE s.fecha = :fecha AND s.cuenta = :cuenta AND s.instrumentoEntity = :instrumento",
                SaldoEntity.class
            );
            query.setParameter("fecha", fecha);
            query.setParameter("cuenta", cuenta);
            query.setParameter("instrumento", instrumento);
            return query.getSingleResult();
        } catch (NoResultException e) {
            logger.debug("No se encontró SaldoEntity para fecha {}, cuenta {}, instrumento {}", fecha, cuenta, instrumento.getInstrumentoNemo());
            return null;
        }
    }
}