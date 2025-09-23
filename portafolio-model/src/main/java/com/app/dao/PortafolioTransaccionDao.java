package com.app.dao;

import com.app.entities.PortafolioTransaccionEntity;
import jakarta.persistence.EntityManager;

public class PortafolioTransaccionDao extends AbstractJpaDao<PortafolioTransaccionEntity, Long> {

    public PortafolioTransaccionDao(EntityManager entityManager) {
        super(entityManager, PortafolioTransaccionEntity.class);
    }
}
