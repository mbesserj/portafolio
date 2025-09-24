package com.app.service;

import com.app.dto.ProblemasTrxsDto;
import com.app.enums.TipoEnumsCosteo;
import jakarta.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;

public class ProblemasTrxsService extends AbstractRepository {

    private static final String PROBLEMAS_QUERY = """
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

    /**
     * Obtiene las transacciones marcadas para revisión de una empresa y custodio específicos.
     * 
     * @param razonSocialEmpresa La razón social de la empresa
     * @param nombreCustodio El nombre del custodio
     * @return Lista de transacciones con problemas
     */
    public List<ProblemasTrxsDto> obtenerTransaccionesConProblemas(String razonSocialEmpresa, String nombreCustodio) {
        // Validaciones
        if (razonSocialEmpresa == null || razonSocialEmpresa.trim().isEmpty()) {
            throw new IllegalArgumentException("La razón social de la empresa no puede ser nula o vacía");
        }
        if (nombreCustodio == null || nombreCustodio.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del custodio no puede ser nulo o vacío");
        }

        return executeReadOnly(em -> {
            try {
                TypedQuery<ProblemasTrxsDto> query = em.createQuery(PROBLEMAS_QUERY, ProblemasTrxsDto.class);
                query.setParameter("empresa", razonSocialEmpresa.trim());
                query.setParameter("custodio", nombreCustodio.trim());
                query.setParameter("tipoIngreso", TipoEnumsCosteo.INGRESO);
                query.setParameter("tipoEgreso", TipoEnumsCosteo.EGRESO);
                
                List<ProblemasTrxsDto> problemas = query.getResultList();
                logger.debug("Se encontraron {} transacciones con problemas para empresa '{}' y custodio '{}'", 
                           problemas.size(), razonSocialEmpresa, nombreCustodio);
                return problemas;
                
            } catch (Exception e) {
                logger.error("Error al obtener transacciones con problemas para empresa '{}' y custodio '{}'", 
                           razonSocialEmpresa, nombreCustodio, e);
                return Collections.emptyList();
            }
        });
    }
}