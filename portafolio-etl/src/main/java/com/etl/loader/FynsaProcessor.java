package com.etl.loader;

import com.app.dto.CartolaFynsa;
import com.app.entities.CargaTransaccionEntity;
import com.etl.interfaz.AbstractCargaProcessor; // Asegúrate que el import es correcto
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FynsaProcessor extends AbstractCargaProcessor<CartolaFynsa> {

    private static final Logger logger = LoggerFactory.getLogger(FynsaProcessor.class);

    public FynsaProcessor(EntityManager entityManager) {
        // Llama al constructor de la clase padre (AbstractCargaProcessor)
        super(entityManager); 
    }

    @Override
    public void procesar(CartolaFynsa dto) {
        if (dto == null) {
            return;
        }

        CargaTransaccionEntity entity = dto.toEntity();
        CargaTransaccionEntity existente = entityManager.find(CargaTransaccionEntity.class, entity.getId());

        if (existente == null) {
            entityManager.persist(entity);
        } else {
            logger.info("Registro de Fynsa ya existe, omitiendo: {}", entity.getId());
        }
    }
}