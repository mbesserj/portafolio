package com.app.service;

import com.app.entities.*;
import com.app.interfaces.AbstractRepository;
import jakarta.persistence.TypedQuery;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FiltroService extends AbstractRepository {

    // Constantes para queries reutilizables
    private static final String QUERY_EMPRESAS_CON_TRANSACCIONES = 
        "SELECT DISTINCT t.empresa FROM TransaccionEntity t ORDER BY t.empresa.razonSocial ASC";
    
    private static final String QUERY_CUSTODIOS_CON_TRANSACCIONES = 
        "SELECT DISTINCT t.custodio FROM TransaccionEntity t WHERE t.empresa.id = :empresaId ORDER BY t.custodio.nombreCustodio ASC";
    
    private static final String QUERY_CUENTAS_CON_TRANSACCIONES = 
        "SELECT DISTINCT t.cuenta FROM TransaccionEntity t WHERE t.empresa.id = :empresaId AND t.custodio.id = :custodioId AND t.cuenta IS NOT NULL ORDER BY t.cuenta ASC";
    
    private static final String QUERY_INSTRUMENTOS_CON_TRANSACCIONES = """
        SELECT DISTINCT t.instrumento FROM TransaccionEntity t
        WHERE t.empresa.id = :empresaId AND t.custodio.id = :custodioId AND t.cuenta = :cuenta
        ORDER BY t.instrumento.instrumentoNemo ASC
        """;
    
    private static final Logger logger = LoggerFactory.getLogger(FiltroService.class);

    /**
     * Constructor vacio.
     */
    public FiltroService() {
        super();
    }

    /**
     * Obtiene todas las empresas que tienen transacciones registradas.
     * 
     * @return Lista de empresas con transacciones, ordenadas por razón social
     */
    public List<EmpresaEntity> obtenerEmpresasConTransacciones() {
        return executeReadOnly(em -> {
            try {
                List<EmpresaEntity> empresas = em.createQuery(QUERY_EMPRESAS_CON_TRANSACCIONES, EmpresaEntity.class)
                    .getResultList();
                logger.debug("Se encontraron {} empresas con transacciones para filtros", empresas.size());
                return empresas;
            } catch (Exception e) {
                logger.error("Error al obtener empresas con transacciones para filtros", e);
                return Collections.emptyList();
            }
        });
    }

    /**
     * Obtiene todos los custodios que tienen transacciones para una empresa específica.
     * 
     * @param empresaId El ID de la empresa
     * @return Lista de custodios con transacciones para la empresa, ordenados por nombre
     */
    public List<CustodioEntity> obtenerCustodiosConTransacciones(Long empresaId) {
        if (empresaId == null) {
            logger.debug("ID de empresa es null para obtener custodios con transacciones");
            return Collections.emptyList();
        }
        
        return executeReadOnly(em -> {
            try {
                TypedQuery<CustodioEntity> query = em.createQuery(QUERY_CUSTODIOS_CON_TRANSACCIONES, CustodioEntity.class);
                query.setParameter("empresaId", empresaId);
                List<CustodioEntity> custodios = query.getResultList();
                logger.debug("Se encontraron {} custodios con transacciones para empresa {}", 
                           custodios.size(), empresaId);
                return custodios;
            } catch (Exception e) {
                logger.error("Error al obtener custodios con transacciones para empresa {}", empresaId, e);
                return Collections.emptyList();
            }
        });
    }

    /**
     * Obtiene todas las cuentas que tienen transacciones para una empresa y custodio específicos.
     * 
     * @param empresaId El ID de la empresa
     * @param custodioId El ID del custodio
     * @return Lista de cuentas con transacciones, ordenadas alfabéticamente
     */
    public List<String> obtenerCuentasConTransacciones(Long empresaId, Long custodioId) {
        if (empresaId == null || custodioId == null) {
            logger.debug("ID de empresa o custodio es null para obtener cuentas con transacciones");
            return Collections.emptyList();
        }
        
        return executeReadOnly(em -> {
            try {
                TypedQuery<String> query = em.createQuery(QUERY_CUENTAS_CON_TRANSACCIONES, String.class);
                query.setParameter("empresaId", empresaId);
                query.setParameter("custodioId", custodioId);
                List<String> cuentas = query.getResultList();
                logger.debug("Se encontraron {} cuentas con transacciones para empresa {} y custodio {}", 
                           cuentas.size(), empresaId, custodioId);
                return cuentas;
            } catch (Exception e) {
                logger.error("Error al obtener cuentas con transacciones para empresa {} y custodio {}", 
                           empresaId, custodioId, e);
                return Collections.emptyList();
            }
        });
    }

    /**
     * Obtiene todos los instrumentos que tienen transacciones para empresa, custodio y cuenta específicos.
     * 
     * @param empresaId El ID de la empresa
     * @param custodioId El ID del custodio
     * @param cuenta El nombre de la cuenta
     * @return Lista de instrumentos con transacciones, ordenados por nemo
     */
    public List<InstrumentoEntity> obtenerInstrumentosConTransacciones(Long empresaId, Long custodioId, String cuenta) {
        if (empresaId == null || custodioId == null || cuenta == null || cuenta.trim().isEmpty()) {
            logger.debug("Parámetros inválidos para obtener instrumentos con transacciones");
            return Collections.emptyList();
        }
        
        return executeReadOnly(em -> {
            try {
                TypedQuery<InstrumentoEntity> query = em.createQuery(QUERY_INSTRUMENTOS_CON_TRANSACCIONES, InstrumentoEntity.class);
                query.setParameter("empresaId", empresaId);
                query.setParameter("custodioId", custodioId);
                query.setParameter("cuenta", cuenta.trim());
                List<InstrumentoEntity> instrumentos = query.getResultList();
                logger.debug("Se encontraron {} instrumentos con transacciones para empresa {}, custodio {} y cuenta {}", 
                           instrumentos.size(), empresaId, custodioId, cuenta);
                return instrumentos;
            } catch (Exception e) {
                logger.error("Error al obtener instrumentos con transacciones para empresa {}, custodio {} y cuenta {}", 
                           empresaId, custodioId, cuenta, e);
                return Collections.emptyList();
            }
        });
    }
}