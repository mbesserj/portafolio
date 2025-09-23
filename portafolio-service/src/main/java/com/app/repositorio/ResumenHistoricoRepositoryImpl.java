package com.app.repositorio;

import com.app.dto.ResumenHistoricoDto;
import com.app.repository.ResumenHistoricoRepository;
import com.app.repository.ResumenHistoricoRepository;
import com.app.utiles.LibraryInitializer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ResumenHistoricoRepositoryImpl implements ResumenHistoricoRepository {

    @Override
    public List<ResumenHistoricoDto> obtenerResumenHistorico(Long empresaId, Long custodioId, String cuenta) {
        EntityManager em = null;        
        String sql = """
        WITH 
        saldos_activos_calculados AS (
            SELECT
                t.instrumento_id AS nemo_id
            FROM
                kardex k
                JOIN transacciones t ON k.transaccion_id = t.id
                JOIN tipo_movimientos tm ON t.movimiento_id = tm.id
                JOIN tipos_contables tc ON tm.movimiento_contable_id = tc.id
            WHERE
                t.empresa_id = :empresaId
                AND t.custodio_id = :custodioId
                AND t.cuenta = :cuenta
            GROUP BY
                t.instrumento_id
            HAVING
                SUM(CASE WHEN tc.tipo_contable = 'INGRESO' THEN k.cantidad ELSE -k.cantidad END) > 0
        ),
        operaciones_base AS (
            SELECT
                t.instrumento_id AS nemo_id,
                i.nemo,
                i.instrumento,
                (ti.precio * dc.cantidad_usada) AS costo_fifo,
                ((t.precio - ti.precio) * dc.cantidad_usada) AS utilidad,
                (coalesce(t.gasto, 0) + coalesce(t.comision, 0) + coalesce(t.iva, 0)) AS gasto
            FROM
                transacciones t
                JOIN instrumentos i ON t.instrumento_id = i.id
                JOIN tipo_movimientos tm ON t.movimiento_id = tm.id
                JOIN detalle_costeos dc ON t.id = dc.egreso_id
                JOIN transacciones ti ON dc.ingreso_id = ti.id
            WHERE
                t.empresa_id = :empresaId
                AND t.custodio_id = :custodioId
                AND t.cuenta = :cuenta
                AND tm.movimiento_contable_id = (SELECT id FROM tipos_contables WHERE tipo_contable = 'EGRESO')
        ),
        operaciones_cerradas AS (
            SELECT 
                nemo_id, nemo, instrumento, costo_fifo, utilidad, gasto, NULL AS dividendo
            FROM operaciones_base op
            WHERE NOT EXISTS (
                SELECT 1 FROM saldos_activos_calculados sac WHERE sac.nemo_id = op.nemo_id
            )
        ),
        dividendos_transacciones AS (
            SELECT
                i.id AS nemo_id,
                i.nemo,
                i.instrumento,
                NULL AS costo_fifo,
                NULL AS utilidad,
                NULL AS gasto,
                t.monto_clp AS dividendo
            FROM
                transacciones t
                JOIN tipo_movimientos tm ON t.movimiento_id = tm.id
                JOIN instrumentos i ON t.instrumento_id = i.id
            WHERE
                t.empresa_id = :empresaId
                AND t.custodio_id = :custodioId
                AND t.cuenta = :cuenta
                AND tm.tipo_movimiento LIKE '%Dividendo%'
        )
        SELECT
            nemo,
            instrumento,
            SUM(coalesce(costo_fifo, 0)) AS costo_fifo,
            SUM(coalesce(gasto, 0)) AS gasto,
            SUM(coalesce(dividendo, 0)) AS dividendo,
            SUM(coalesce(utilidad, 0)) AS utilidad,
            SUM(coalesce(utilidad, 0)) + SUM(coalesce(dividendo, 0)) - SUM(coalesce(gasto, 0)) AS total
        FROM (
            SELECT * FROM operaciones_cerradas
            UNION ALL
            SELECT * FROM dividendos_transacciones
        ) AS resultados_finales
        GROUP BY
            nemo,
            instrumento
        HAVING
            total <> 0 OR costo_fifo <> 0
        ORDER BY
            nemo
        """;

        try {
            em = LibraryInitializer.getEntityManager();
            Query query = em.createNativeQuery(sql);
            query.setParameter("empresaId", empresaId);
            query.setParameter("custodioId", custodioId);
            query.setParameter("cuenta", cuenta);

            List<Object[]> results = query.getResultList();

            return results.stream()
                    .map(row -> {
                        ResumenHistoricoDto dto = new ResumenHistoricoDto();
                        dto.setNemo((String) row[0]);
                        dto.setNombreInstrumento((String) row[1]);
                        dto.setTotalCostoFifo((BigDecimal) row[2]);
                        dto.setTotalGasto((BigDecimal) row[3]);
                        dto.setTotalDividendo((BigDecimal) row[4]);
                        dto.setTotalUtilidad((BigDecimal) row[5]);
                        dto.setTotalTotal((BigDecimal) row[6]);
                        return dto;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }
}