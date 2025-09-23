package com.app.repositorio;

import com.app.repository.SincronizacionRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SincronizacionRepositoryImpl implements SincronizacionRepository {
    private static final Logger logger = LoggerFactory.getLogger(SincronizacionRepositoryImpl.class);

    @Override
    public void sincronizarSaldosKardexDesdeSaldos(EntityManager em) {
        logger.info("Iniciando sincronización de 'saldos_kardex' desde la tabla 'saldos'.");

        // El TRUNCATE ahora se maneja en LimpiezaService, aquí solo insertamos.
        logger.debug("Insertando saldos consolidados desde la tabla 'saldos'...");
        String sqlInsert = """
            INSERT INTO saldos_kardex 
                (empresa_id, custodio_id, instrumento_id, cuenta, saldo_cantidad, costo_total, costo_promedio, fecha_ultima_actualizacion, fecha_creacion, creado_por)
            WITH UltimosSaldos AS (
                SELECT 
                    s.empresa_id, s.custodio_id, s.instrumento_id, s.cuenta,
                    s.fecha, s.cantidad, s.monto_clp, s.precio,
                    ROW_NUMBER() OVER(PARTITION BY s.empresa_id, s.custodio_id, s.instrumento_id, s.cuenta ORDER BY s.fecha DESC, s.id DESC) as rn
                FROM saldos s
            )
            SELECT 
                us.empresa_id,
                us.custodio_id,
                us.instrumento_id,
                us.cuenta,
                us.cantidad AS saldo_cantidad,
                us.monto_clp AS costo_total,
                CASE WHEN us.cantidad <> 0 THEN us.monto_clp / us.cantidad ELSE 0 END AS costo_promedio,
                us.fecha AS fecha_ultima_actualizacion,
                CURDATE() AS fecha_creacion,
                'CARGA_INICIAL' AS creado_por
            FROM UltimosSaldos us
            WHERE us.rn = 1 AND us.cantidad <> 0
        """;
        
        int registrosInsertados = em.createNativeQuery(sqlInsert).executeUpdate();
        logger.info("Sincronización completada. Se insertaron {} registros en saldos_kardex.", registrosInsertados);
    }
}
