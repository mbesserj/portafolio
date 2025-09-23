
package com.app.repositorio;

import com.app.utiles.LibraryInitializer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.Optional;

public class TransaccionRepository {

    public Optional<Object[]> getAggregatesForInstrument(Long empresaId, Long custodioId, String cuenta, Long instrumentoId) {
        EntityManager em = LibraryInitializer.getEntityManager();
        try {
            // Consulta SQL Nativa corregida
            String sql = """
                SELECT
                    SUM(CASE WHEN tm.tipo_movimiento LIKE '%Dividendo%' THEN t.monto_clp ELSE 0 END) as total_dividendos,
                    -- CORRECCIÓN: Usamos los nombres de las columnas de la BD (gasto, comision)
                    SUM(COALESCE(t.gasto,0) + COALESCE(t.iva,0) + COALESCE(t.comision,0)) as total_gastos
                FROM transacciones t
                JOIN tipo_movimientos tm ON t.movimiento_id = tm.id
                WHERE t.empresa_id = :empresaId
                  AND t.custodio_id = :custodioId
                  AND t.cuenta = :cuenta
                  AND t.instrumento_id = :instrumentoId
                """;
            
            Query query = em.createNativeQuery(sql);
            query.setParameter("empresaId", empresaId);
            query.setParameter("custodioId", custodioId);
            query.setParameter("cuenta", cuenta);
            query.setParameter("instrumentoId", instrumentoId);
            
            return Optional.ofNullable((Object[]) query.getSingleResult());

        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        } finally {
            if (em != null) em.close();
        }
    }
}