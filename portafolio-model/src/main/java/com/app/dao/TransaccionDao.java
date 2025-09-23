package com.app.dao;

import com.app.entities.TransaccionEntity;
import com.app.utiles.Pk;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

/**
 * Data Access Object (DAO) para la entidad TransaccionEntity. Proporciona
 * métodos para interactuar con la tabla de transacciones. Se ha añadido un
 * método para buscar transacciones por sus claves de negocio para la validación
 * de duplicados.
 */
public class TransaccionDao extends AbstractJpaDao<TransaccionEntity, Pk> {

    private static final Logger logger = LoggerFactory.getLogger(TransaccionDao.class);

    public TransaccionDao(EntityManager entityManager) {
        super(entityManager, TransaccionEntity.class);
    }

    /**
     * Busca una TransaccionEntity por su folio, fecha y cuenta. Esta
     * combinación de campos se utiliza para detectar registros duplicados.
     *
     * @param folio El folio de la transacción.
     * @param fecha La fecha de la transacción.
     * @param cuenta El número de cuenta.
     * @return La TransaccionEntity si se encuentra, o null si no existe.
     */
    public TransaccionEntity findByFolioAndFechaAndCuenta(String folio, LocalDate fecha, String cuenta) {
        try {
            TypedQuery<TransaccionEntity> query = entityManager.createQuery(
                    "SELECT t FROM TransaccionEntity t WHERE t.folio = :folio AND t.fecha = :fecha AND t.cuenta = :cuenta",
                    TransaccionEntity.class
            );
            query.setParameter("folio", folio);
            query.setParameter("fecha", fecha);
            query.setParameter("cuenta", cuenta);
            return query.getSingleResult();
        } catch (NoResultException e) {
            logger.debug("No se encontró TransaccionEntity para folio {}, fecha {}, cuenta {}", folio, fecha, cuenta);
            return null;
        }
    }

    /**
     * Verifica si existe una transacción con la clave primaria dada.
     *
     * @param id La clave primaria compuesta (Pk) de la transacción.
     * @return true si la transacción existe, false en caso contrario.
     */
    @Override
    public boolean exists(Pk id) {
        try {
            TypedQuery<Long> query = entityManager.createQuery(
                    "SELECT COUNT(t) FROM TransaccionEntity t WHERE t.idAndMonth = :id", Long.class);
            query.setParameter("id", id);
            return query.getSingleResult() > 0;
        } catch (NoResultException e) {
            return false;
        }
    }

    /**
     * Obtiene todas las transacciones ordenadas por fecha.
     *
     * @return Una lista de todas las entidades de transacción.
     */
    @Override
    public List<TransaccionEntity> findAll() {
        return entityManager.createQuery("SELECT t FROM TransaccionEntity t ORDER BY t.fecha", TransaccionEntity.class).getResultList();
    }

    /**
     * Busca todas las transacciones que aún no han sido costeadas (costeado =
     * false). Las transacciones se ordenan por fecha en orden ascendente para
     * procesar el Kardex en el orden correcto.
     *
     * @return Una lista de {@code TransaccionEntity} no costeadas.
     */
    public List<TransaccionEntity> findUncostedTransactionsOrderedByDate() {
        return entityManager.createQuery(
                "SELECT t FROM TransaccionEntity t WHERE t.costeado = false ORDER BY t.fecha ASC",
                TransaccionEntity.class
        ).getResultList();
    }
    
    public List<TransaccionEntity> findTransaccionesParaRevision(String razonSocialEmpresa, String nombreCustodio) {
            return entityManager.createQuery(
                "SELECT t FROM TransaccionEntity t WHERE t.paraRevision = true ORDER BY t.fecha ASC",
                TransaccionEntity.class
        ).getResultList();   
    }
}