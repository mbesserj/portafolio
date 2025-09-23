package com.app.service;

import com.app.dto.OperacionesTrxsDto;
import com.app.enums.TipoEnumsCosteo;
import com.app.utiles.LibraryInitializer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class OperacionesTrxsService {

    public List<OperacionesTrxsDto> obtenerTransaccionesPorGrupo(String empresa, String custodio, String cuenta, List<String> nemos) {
        EntityManager em = null;
        try {
            em = LibraryInitializer.getEntityManager();

            // La consulta JPQL ahora incluye el nuevo campo al final del constructor
            String jpql = """
                SELECT new com.app.dto.OperacionesTrxsDto(
                    t.id, t.fecha, t.folio, tm.tipoMovimiento, mc.tipoContable,
                    CASE WHEN mc.tipoContable = :tipoIngreso THEN t.cantidad ELSE null END,
                    CASE WHEN mc.tipoContable = :tipoEgreso THEN t.cantidad ELSE null END,
                    t.precio,
                    t.total,
                    t.costeado, t.paraRevision, t.ignorarEnCosteo
                )
                FROM TransaccionEntity t
                JOIN t.empresa e JOIN t.custodio c JOIN t.instrumento i
                JOIN t.tipoMovimiento tm JOIN tm.movimientoContable mc
                WHERE e.razonSocial = :empresa
                  AND c.nombreCustodio = :custodio
                  AND t.cuenta = :cuenta
                  AND i.instrumentoNemo IN (:nemos)
                ORDER BY
                    t.fecha ASC,
                    CASE WHEN tm.esSaldoInicial = true THEN 0 ELSE 1 END ASC,
                    CASE WHEN mc.tipoContable = :tipoIngreso THEN 2 ELSE 3 END ASC,
                    t.id ASC
                """;

            TypedQuery<OperacionesTrxsDto> query = em.createQuery(jpql, OperacionesTrxsDto.class);
            query.setParameter("empresa", empresa);
            query.setParameter("custodio", custodio);
            query.setParameter("cuenta", cuenta);
            query.setParameter("nemos", nemos);
            query.setParameter("tipoIngreso", TipoEnumsCosteo.INGRESO);
            query.setParameter("tipoEgreso", TipoEnumsCosteo.EGRESO);

            List<OperacionesTrxsDto> transaccionesDto = query.getResultList();

            BigDecimal saldoAcumulado = BigDecimal.ZERO;
            for (OperacionesTrxsDto dto : transaccionesDto) {
                BigDecimal compras = dto.getCompras() != null ? dto.getCompras() : BigDecimal.ZERO;
                BigDecimal ventas = dto.getVentas() != null ? dto.getVentas() : BigDecimal.ZERO;
                BigDecimal total = dto.getTotal() != null ? dto.getTotal() : BigDecimal.ZERO;

                if (dto.getTipoContable() == TipoEnumsCosteo.INGRESO) {
                    total = total.abs();
                } else if (dto.getTipoContable() == TipoEnumsCosteo.EGRESO) {
                    total = total.abs().negate();
                }
                dto.setTotal(total);

                saldoAcumulado = saldoAcumulado.add(compras).subtract(ventas);
                dto.setSaldoAcumulado(saldoAcumulado);
            }
            return transaccionesDto;

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
}