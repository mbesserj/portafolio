package com.serv.service;

import com.model.dto.ProblemasTrxsDto;
import com.model.enums.TipoEnumsCosteo;
import com.model.interfaces.AbstractRepository;
import com.app.sql.QueryRepository;
import jakarta.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProblemasTrxsService extends AbstractRepository {

    private static final Logger logger = LoggerFactory.getLogger(ProblemasTrxsService.class);

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
                String sql_problemas = QueryRepository.getProblemasQuery(QueryRepository.ProblemasQueries.PROBLEMAS_QUERY);
                TypedQuery<ProblemasTrxsDto> query = em.createQuery(sql_problemas, ProblemasTrxsDto.class);
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