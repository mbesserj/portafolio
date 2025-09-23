package com.etl.loader;

import com.etl.interfaz.AbstractCargaProcessor; // Importa la nueva clase abstracta
import com.app.dto.CartolaBanChile;
import com.app.entities.CargaTransaccionEntity;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BanChileProcessor extends AbstractCargaProcessor<CartolaBanChile> {

    private static final Logger logger = LoggerFactory.getLogger(BanChileProcessor.class);
    private final String hojaTipo;

    // El constructor no cambia
    public BanChileProcessor(EntityManager entityManager, String hojaTipo) {
        super(entityManager);
        this.hojaTipo = hojaTipo;
    }

    @Override
    public void procesar(CartolaBanChile dto) {
        if (dto == null) {
            return;
        }

        CargaTransaccionEntity entity = dto.toEntity();
        CargaTransaccionEntity existente = entityManager.find(CargaTransaccionEntity.class, entity.getId());

        if (existente == null) {
            entityManager.persist(entity);
        } else {
            logger.info("Registro de BanChile ya existe, omitiendo: {}", entity.getId());
        }
    }
}