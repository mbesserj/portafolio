package com.app.repositorio;

import com.app.dto.ConfrontaSaldoDto;
import com.app.interfaces.AbstractRepository;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import com.app.interfaces.ConfrontaRepInterfaz;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfrontaRepositoryImpl extends AbstractRepository implements ConfrontaRepInterfaz {

    private static final Logger logger = LoggerFactory.getLogger(ConfrontaRepositoryImpl.class);

    private static final String CONFRONTA_QUERY = """
        WITH 
        UltimoSaldoMercado AS (
            SELECT 
                s.empresa_id, s.custodio_id, s.instrumento_id, s.cuenta,
                s.fecha, s.cantidad, s.monto_clp, s.precio,
                ROW_NUMBER() OVER(PARTITION BY s.empresa_id, s.custodio_id, s.instrumento_id, s.cuenta ORDER BY s.fecha DESC, s.id DESC) as rn
            FROM saldos s
            WHERE s.fecha <= :fechaCorte
        ),
        TodosLosGrupos AS (
            SELECT empresa_id, custodio_id, instrumento_id, cuenta FROM saldos_kardex
            UNION
            SELECT empresa_id, custodio_id, instrumento_id, cuenta FROM UltimoSaldoMercado WHERE rn = 1
        )
        SELECT 
            g.empresa_id, g.custodio_id, g.instrumento_id,
            e.razonSocial AS empresa_nombre, c.custodio AS custodio_nombre, i.nemo AS instrumento_nemo, g.cuenta,
            sk.fecha_ultima_actualizacion AS ultima_fecha_kardex,
            COALESCE(sk.saldo_cantidad, 0) AS cantidad_kardex,
            COALESCE(sk.costo_total, 0) AS valor_kardex,
            sm.fecha AS ultima_fecha_saldos,
            COALESCE(sm.cantidad, 0) AS cantidad_mercado,
            COALESCE(sm.monto_clp, 0) AS valor_mercado,
            (COALESCE(sm.cantidad, 0) - COALESCE(sk.saldo_cantidad, 0)) AS diferencia_cantidad,
            sm.precio AS precio_mercado
        FROM TodosLosGrupos g
        LEFT JOIN saldos_kardex sk ON g.empresa_id = sk.empresa_id AND g.custodio_id = sk.custodio_id AND g.instrumento_id = sk.instrumento_id AND g.cuenta = sk.cuenta
        LEFT JOIN (SELECT * FROM UltimoSaldoMercado WHERE rn = 1) sm ON g.empresa_id = sm.empresa_id AND g.custodio_id = sm.custodio_id AND g.instrumento_id = sm.instrumento_id AND g.cuenta = sm.cuenta
        LEFT JOIN empresas e ON g.empresa_id = e.id
        LEFT JOIN custodios c ON g.custodio_id = c.id
        LEFT JOIN instrumentos i ON g.instrumento_id = i.id
        HAVING ABS(diferencia_cantidad) > 0.0001
        ORDER BY e.razonSocial, c.custodio, g.cuenta, i.nemo
        """;

    @Override
    @SuppressWarnings("unchecked")
    public List<ConfrontaSaldoDto> obtenerDiferenciasDeSaldos(LocalDate fechaCorte) {
        return executeReadOnly(em -> {
            try {
                Query query = em.createNativeQuery(CONFRONTA_QUERY, "ConfrontaSaldoMapping");
                query.setParameter("fechaCorte", fechaCorte);
                return query.getResultList();
            } catch (Exception e) {
                logger.error("Error al ejecutar la consulta de confronta de saldos", e);
                return Collections.emptyList();
            }
        });
    }
}