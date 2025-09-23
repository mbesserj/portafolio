package com.app.dao;

import com.app.entities.GrupoEmpresaEntity;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrupoEmpresaDao extends AbstractJpaDao<GrupoEmpresaEntity, Long> {

    private static final Logger logger = LoggerFactory.getLogger(GrupoEmpresaEntity.class);

    public GrupoEmpresaDao(final EntityManager entityManager) {
        super(entityManager, GrupoEmpresaEntity.class);
    }
}