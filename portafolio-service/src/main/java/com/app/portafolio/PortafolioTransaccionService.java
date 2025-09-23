package com.app.portafolio;

import com.app.dao.PortafolioTransaccionDao;
import com.app.dao.TransaccionDao;
import com.app.entities.PortafolioEntity;
import com.app.entities.PortafolioTransaccionEntity;
import com.app.entities.TransaccionEntity;
import jakarta.persistence.EntityManager;

public class PortafolioTransaccionService {

    private final TransaccionDao transaccionDao;
    private final PortafolioTransaccionDao portafolioTransaccionDao;

    // El constructor recibe el EntityManager y crea las instancias de los DAOs
    public PortafolioTransaccionService(EntityManager entityManager) {
        this.transaccionDao = new TransaccionDao(entityManager);
        this.portafolioTransaccionDao = new PortafolioTransaccionDao(entityManager);
    }

    // Este método ya no gestiona la transacción, solo ejecuta la lógica
    public void crearTransaccionEnPortafolio(TransaccionEntity transaccion, PortafolioEntity portafolio) {
        // Paso 1: Guardar la transacción original
        TransaccionEntity nuevaTransaccion = transaccionDao.create(transaccion);
        
        // Paso 2: Crear y persistir la nueva instancia de mapeo para la relación
        PortafolioTransaccionEntity relacion = new PortafolioTransaccionEntity();
        relacion.setPortafolio(portafolio);
        relacion.setTransaccion(nuevaTransaccion);
        
        portafolioTransaccionDao.create(relacion);
    }

    // Este método tampoco gestiona la transacción
    public void relacionarTransaccionEnPortafolio(TransaccionEntity transaccion, PortafolioEntity portafolio) {
        // Paso 1: Crear la nueva instancia de mapeo para la relación
        PortafolioTransaccionEntity relacion = new PortafolioTransaccionEntity();
        relacion.setPortafolio(portafolio);
        relacion.setTransaccion(transaccion);
        
        // Paso 2: Persistir la instancia de mapeo usando el nuevo DAO
        portafolioTransaccionDao.create(relacion);
    }
}