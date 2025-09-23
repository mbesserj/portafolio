package com.app.dao;

import com.app.entities.EmpresaEntity;
import com.app.entities.GrupoEmpresaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmpresaDao extends AbstractJpaDao<EmpresaEntity, Long> {

    private static final Logger logger = LoggerFactory.getLogger(EmpresaDao.class);

    public EmpresaDao(final EntityManager entityManager) {
        super(entityManager, EmpresaEntity.class);
    }

    /**
     * Método búsqueda y creación de empresa entity.
     * Busca una empresa por su RUT. Si existe, actualiza su razón social si ha cambiado.
     * Si no existe, la crea.
     */
    public EmpresaEntity findOrCreateByRazonSocial(String razonSocial, String rut) {
        String rutNormalizado = normalizarRut(rut);
        if (rutNormalizado.isEmpty()) {
            logger.warn("Se intentó procesar una empresa con RUT vacío o nulo.");
            return null;
        }

        TypedQuery<EmpresaEntity> query = entityManager.createQuery(
            "SELECT e FROM EmpresaEntity e WHERE e.rut = :rut", EmpresaEntity.class);
        query.setParameter("rut", rutNormalizado);

        try {
            EmpresaEntity empresaExistente = query.getSingleResult();
            
            String razonSocialNormalizada = (razonSocial == null || razonSocial.trim().isEmpty()) ? "Sin Razon Social" : razonSocial.trim();
            
            if (!empresaExistente.getRazonSocial().equalsIgnoreCase(razonSocialNormalizada)) {
                logger.info("Actualizando razón social para RUT {}: '{}' -> '{}'", rutNormalizado, empresaExistente.getRazonSocial(), razonSocialNormalizada);
                empresaExistente.setRazonSocial(razonSocialNormalizada);
                update(empresaExistente);
            }
            
            return empresaExistente;

        } catch (NoResultException e) {
            logger.info("Creando nueva empresa con RUT: {}", rutNormalizado);
            EmpresaEntity nuevaEmpresa = new EmpresaEntity();
            nuevaEmpresa.setRut(rutNormalizado);
            String razon = (razonSocial == null || razonSocial.trim().isEmpty()) ? "Sin Razon Social" : razonSocial.trim();
            nuevaEmpresa.setRazonSocial(razon);
            nuevaEmpresa.setFechaCreado(LocalDate.now());
            
            create(nuevaEmpresa);
            return nuevaEmpresa;
        }
    }

    /**
     * Función auxiliar para limpiar y estandarizar un RUT.
     */
    private String normalizarRut(String rut) {
        if (rut == null || rut.trim().isEmpty()) {
            return "";
        }
        return rut.replace(".", "").replace("-", "").trim().toUpperCase();
    }

    /**
     * Asigna un grupo a una empresa, buscándola o creándola primero.
     * Llama al método principal para mantener la lógica unificada.
     */
    public EmpresaEntity addGrupoEmpresa(String razonSocial, String rut, GrupoEmpresaEntity grupo) {
        EmpresaEntity empresa = findOrCreateByRazonSocial(razonSocial, rut);
        
        if (empresa != null) {
            empresa.setGrupoEmpresa(grupo);
            update(empresa);
        }
        return empresa;
    }

    /**
     * Sobrecarga para encontrar/crear y asignar un grupo.
     * Llama al método addGrupoEmpresa para evitar duplicar código.
     */
    public EmpresaEntity findOrCreateByRazonSocial(String razonSocial, String rut, GrupoEmpresaEntity grupo) {
        return addGrupoEmpresa(razonSocial, rut, grupo);
    }
}