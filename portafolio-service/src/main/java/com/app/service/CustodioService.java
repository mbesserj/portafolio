package com.app.service;

import com.app.dao.CustodioDao;
import com.app.entities.CustodioEntity;
import com.app.entities.EmpresaEntity;
import com.app.interfaces.AbstractRepository;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustodioService extends AbstractRepository {

    private static final String QUERY_EMPRESA_CON_CUSTODIOS = 
        "SELECT e FROM EmpresaEntity e LEFT JOIN FETCH e.custodios WHERE e.id = :empresaId";
    
    private static final String QUERY_CUENTAS_POR_CUSTODIO_EMPRESA = """
        SELECT c.cuenta 
        FROM CuentaEntity c
        WHERE c.custodio.id = :custodioId 
          AND c.empresa.id = :empresaId
        ORDER BY c.cuenta ASC
        """;

    private static final Logger logger = LoggerFactory.getLogger(CustodioService.class);

    /**
     * Constructor custodioService.
     */
    public CustodioService() {
        super();
    }
    
    /**
     * Obtiene todos los custodios ordenados por nombre.
     * 
     * @return Lista de custodios o lista vacía si hay error
     */
    public List<CustodioEntity> obtenerTodos() {
        return executeReadOnly(em -> {
            try {
                return new CustodioDao(em).findAll();
            } catch (Exception e) {
                logger.error("Error al obtener todos los custodios", e);
                return Collections.emptyList();
            }
        });
    }

    /**
     * Obtiene los custodios asociados a una empresa específica.
     * 
     * @param empresaId El ID de la empresa
     * @return Lista de custodios ordenados por nombre, o lista vacía si no hay datos o error
     */
    public List<CustodioEntity> obtenerCustodiosPorEmpresa(Long empresaId) {
        if (empresaId == null) {
            logger.debug("ID de empresa es null, retornando lista vacía");
            return Collections.emptyList();
        }
        
        return executeReadOnly(em -> {
            try {
                TypedQuery<EmpresaEntity> query = em.createQuery(QUERY_EMPRESA_CON_CUSTODIOS, EmpresaEntity.class);
                query.setParameter("empresaId", empresaId);

                EmpresaEntity empresa = query.getSingleResult();

                if (empresa != null && empresa.getCustodios() != null) {
                    List<CustodioEntity> custodiosList = new ArrayList<>(empresa.getCustodios());
                    custodiosList.sort(Comparator.comparing(CustodioEntity::getNombreCustodio));
                    logger.debug("Se encontraron {} custodios para la empresa {}", custodiosList.size(), empresaId);
                    return custodiosList;
                }

                logger.debug("No se encontraron custodios para la empresa {}", empresaId);
                return Collections.emptyList();
                
            } catch (NoResultException e) {
                logger.debug("Empresa no encontrada con ID: {}", empresaId);
                return Collections.emptyList();
            } catch (Exception e) {
                logger.error("Error al obtener custodios para la empresa con ID: {}", empresaId, e);
                return Collections.emptyList();
            }
        });
    }

    /**
     * Obtiene las cuentas asociadas a un custodio y empresa específicos.
     * 
     * @param custodioId El ID del custodio
     * @param empresaId El ID de la empresa
     * @return Lista de cuentas ordenadas, o lista vacía si no hay datos o error
     */
    public List<String> obtenerCuentasPorCustodioYEmpresa(Long custodioId, Long empresaId) {
        if (custodioId == null || empresaId == null) {
            logger.debug("ID de custodio o empresa es null, retornando lista vacía");
            return Collections.emptyList();
        }
        
        return executeReadOnly(em -> {
            try {
                TypedQuery<String> query = em.createQuery(QUERY_CUENTAS_POR_CUSTODIO_EMPRESA, String.class);
                query.setParameter("custodioId", custodioId);
                query.setParameter("empresaId", empresaId);

                List<String> cuentas = query.getResultList();
                logger.debug("Se encontraron {} cuentas para custodio {} y empresa {}", 
                           cuentas.size(), custodioId, empresaId);
                return cuentas;
                
            } catch (Exception e) {
                logger.error("Error al obtener cuentas para custodio {} y empresa {}", custodioId, empresaId, e);
                return Collections.emptyList();
            }
        });
    }

    /**
     * Obtiene un custodio por su ID.
     * 
     * @param id El ID del custodio
     * @return El custodio encontrado o null si no existe
     */
    public CustodioEntity obtenerPorId(Long id) {
        if (id == null) {
            logger.debug("ID de custodio es null");
            return null;
        }
        return executeReadOnly(em -> {
            try {
                CustodioEntity custodio = em.find(CustodioEntity.class, id);
                if (custodio == null) {
                    logger.debug("No se encontró custodio con ID: {}", id);
                }
                return custodio;
            } catch (Exception e) {
                logger.error("Error al buscar custodio con ID: {}", id, e);
                return null;
            }
        });
    }
}