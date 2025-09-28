package com.costing.repositorios;

import com.model.interfaces.AbstractRepository;
import com.model.interfaces.ResetCosteoFlag;

public class ResetCosteoServiceImpl extends AbstractRepository implements ResetCosteoFlag {

    /**
     * Resetea los datos para que se puedan recostear.
     */
    public ResetCosteoServiceImpl() {
        super();
    }
        
    @Override
    public int resetCosteoFlagsByGrupo(Long empresaId, String cuenta, Long custodioId, Long instrumentoId) {
        // Se usa la versión transaccional que devuelve un valor (int)
        return executeInTransaction(em -> {
            return em.createQuery("""
                UPDATE TransaccionEntity t SET t.costeado = false, t.paraRevision = false
                WHERE t.empresa.id = :empresaId
                  AND t.cuenta = :cuenta
                  AND t.custodio.id = :custodioId
                  AND t.instrumento.id = :instrumentoId
                """)
                .setParameter("empresaId", empresaId)
                .setParameter("cuenta", cuenta)
                .setParameter("custodioId", custodioId)
                .setParameter("instrumentoId", instrumentoId)
                .executeUpdate();
        });
    }
}