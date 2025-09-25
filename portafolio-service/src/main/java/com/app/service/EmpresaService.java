package com.app.service;

import com.app.dao.EmpresaDao;
import com.app.entities.EmpresaEntity;
import com.app.interfaces.AbstractRepository;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmpresaService extends AbstractRepository {

    private static final String QUERY_EMPRESAS_CON_TRANSACCIONES = 
        "SELECT DISTINCT t.empresa FROM TransaccionEntity t ORDER BY t.empresa.razonSocial ASC";

    private static final Logger logger = LoggerFactory.getLogger(EmpresaService.class);

    public EmpresaService() {
        super();
    }
    
    /**
     * Obtiene todas las empresas del sistema.
     * 
     * @return Lista de empresas o lista vacía si hay error
     */
    public List<EmpresaEntity> obtenerTodas() {
        return executeReadOnly(em -> {
            try {
                return new EmpresaDao(em).findAll();
            } catch (Exception e) {
                logger.error("Error al obtener todas las empresas", e);
                return Collections.emptyList();
            }
        });
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
                logger.debug("Se encontraron {} empresas con transacciones", empresas.size());
                return empresas;
            } catch (Exception e) {
                logger.error("Error al obtener empresas con transacciones", e);
                return Collections.emptyList();
            }
        });
    }

    /**
     * Obtiene una empresa por su ID.
     * 
     * @param id El ID de la empresa
     * @return La empresa encontrada o null si no existe
     */
    public EmpresaEntity obtenerPorId(Long id) {
        if (id == null) {
            logger.debug("ID de empresa es null");
            return null;
        }
        
        return executeReadOnly(em -> {
            try {
                EmpresaEntity empresa = em.find(EmpresaEntity.class, id);
                if (empresa == null) {
                    logger.debug("No se encontró empresa con ID: {}", id);
                }
                return empresa;
            } catch (Exception e) {
                logger.error("Error al buscar empresa con ID: {}", id, e);
                return null;
            }
        });
    }
}