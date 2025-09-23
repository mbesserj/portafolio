
package com.app.portafolio;

import com.app.entities.PortafolioEntity;
import com.app.entities.TransaccionEntity;
import com.app.utiles.LibraryInitializer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

public class PortafolioService {

    public void manejarNuevaTransaccion(TransaccionEntity transaccion, PortafolioEntity portafolio) {
        
        EntityManager entityManager = LibraryInitializer.getEntityManager();
        PortafolioTransaccionService service = new PortafolioTransaccionService(entityManager);
        EntityTransaction transaction = entityManager.getTransaction();

        try {
            transaction.begin();
            service.crearTransaccionEnPortafolio(transaccion, portafolio);

            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Error en la transacción de la base de datos", e);
        } finally {
            entityManager.close();
        }
    }
}