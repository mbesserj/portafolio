package com.app.dao;

import com.app.dto.CargaTransaccion;
import com.app.entities.CargaTransaccionEntity;
import com.app.utiles.Pk;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.time.LocalDate;
import java.util.List;

public class CargaTransaccionDao extends AbstractJpaDao<CargaTransaccionEntity, Pk> {

    public CargaTransaccionDao(EntityManager entityManager) {
        super(entityManager, CargaTransaccionEntity.class);
    }

    /**
     * CORRECCIÓN: Se añade el método que faltaba para buscar las entidades no procesadas.
     * La lógica de NormalizarDatos depende de este método.
     * @return Una lista de CargaTransaccionEntity pendientes de procesar.
     */
    public List<CargaTransaccionEntity> findUnprocessed() {
        return entityManager.createQuery(
            "SELECT c FROM CargaTransaccionEntity c WHERE c.procesado = false", 
            CargaTransaccionEntity.class
        ).getResultList();
    }

    /**
     * Busca un lote de transacciones NO PROCESADAS y las devuelve como DTOs.
     */
    public List<CargaTransaccion> findUnprocessedDtoBatch(int offset, int batchSize) {
        return entityManager.createQuery("""
        SELECT new com.app.dto.CargaTransaccion(
            c.id.transactionDate,
            c.id.rowNum,
            c.id.tipoClase,
            c.razonSocial,
            c.rut,
            c.custodioNombre,
            c.cuenta,
            c.instrumentoNemo,
            c.instrumentoNombre,
            c.tipoMovimiento,
            c.monto,
            c.montoTotal,
            c.moneda,
            c.movimientoCaja,
            c.montoClp,
            c.montoUsd,
            c.cantidad,
            c.precio,
            c.comisiones,
            c.gastos,
            c.iva,
            c.folio
        )
        FROM CargaTransaccionEntity c
        WHERE c.procesado = false
        ORDER BY c.id.transactionDate, c.id.rowNum
        """, CargaTransaccion.class)
                .setFirstResult(offset)
                .setMaxResults(batchSize)
                .getResultList();
    }

    /**
     * Verifica si una transacción ya existe basándose en su clave de negocio.
     */
    public boolean existsByUniqueBusinessFields(String transactionId, LocalDate transactionDate, String fileOrigin) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(c) FROM CargaTransaccionEntity c WHERE c.id.transactionId = :transactionId "
                + "AND c.id.transactionDate = :transactionDate "
                + "AND c.id.fileOrigin = :fileOrigin", Long.class);
        query.setParameter("transactionId", transactionId);
        query.setParameter("transactionDate", transactionDate);
        query.setParameter("fileOrigin", fileOrigin);
        return query.getSingleResult() > 0;
    }

    /**
     * Limpia la tabla de carga.
     */
    public void clearTable() {
        entityManager.createQuery("DELETE FROM CargaTransaccionEntity").executeUpdate();
    }
}

