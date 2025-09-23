package com.app.service;

import com.app.dto.ProblemasTrxsDto;
import com.app.enums.TipoEnumsCosteo;
import com.app.utiles.LibraryInitializer;
import jakarta.persistence.EntityManager;
import java.util.Collections;
import java.util.List;

public class ProblemasTrxsService {

    public List<ProblemasTrxsDto> obtenerTransaccionesConProblemas(String razonSocialEmpresa, String nombreCustodio) {
        EntityManager em = null;
        try {
            em = LibraryInitializer.getEntityManager();

            String hql = """
                SELECT new com.app.dto.ProblemasTrxsDto(
                    t.fecha,
                    t.folio,
                    tm.tipoMovimiento,
                    i.instrumentoNemo,
                    CASE WHEN mc.tipoContable = :tipoIngreso THEN t.cantidad ELSE null END,
                    CASE WHEN mc.tipoContable = :tipoEgreso THEN t.cantidad ELSE null END,
                    t.precio,
                    -t.total,
                    t.costeado
                )
                FROM TransaccionEntity t
                JOIN t.empresa e
                JOIN t.custodio c
                JOIN t.instrumento i
                JOIN t.tipoMovimiento tm
                JOIN tm.movimientoContable mc
                WHERE t.paraRevision = true
                  AND e.razonSocial = :empresa
                  AND c.nombreCustodio = :custodio
                ORDER BY t.fecha ASC, t.id ASC
                """;
            return em.createQuery(hql, ProblemasTrxsDto.class)
                    .setParameter("empresa", razonSocialEmpresa)
                    .setParameter("custodio", nombreCustodio)
                    .setParameter("tipoIngreso", TipoEnumsCosteo.INGRESO)
                    .setParameter("tipoEgreso", TipoEnumsCosteo.EGRESO)
                    .getResultList();
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
