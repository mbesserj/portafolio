package com.service.service;

import com.app.dao.PerfilDao;
import com.app.entities.PerfilEntity;
import com.app.interfaces.AbstractRepository;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio para gestionar la lógica de negocio de los perfiles.
 */
public class PerfilService extends AbstractRepository {

    private static final Logger logger = LoggerFactory.getLogger(PerfilService.class);

    /**
     * Busca un perfil por nombre o lo crea si no existe.
     * 
     * @param nombre El nombre del perfil
     * @return El perfil encontrado o creado
     * @throws IllegalArgumentException si el nombre es nulo o vacío
     */
    public PerfilEntity buscarOCrearPorNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del perfil no puede ser nulo o vacío");
        }

        return executeInTransaction(em -> {
            try {
                PerfilDao perfilDao = new PerfilDao(em);
                PerfilEntity perfil = perfilDao.findByName(nombre.trim());
                
                if (perfil == null) {
                    logger.info("Perfil '{}' no encontrado. Creándolo...", nombre);
                    perfil = new PerfilEntity(nombre.trim());
                    perfilDao.create(perfil);
                    logger.info("Perfil '{}' creado exitosamente con ID: {}", nombre, perfil.getId());
                } else {
                    logger.debug("Perfil '{}' encontrado con ID: {}", nombre, perfil.getId());
                }
                return perfil;
                
            } catch (Exception e) {
                logger.error("Error al buscar o crear perfil '{}'", nombre, e);
                throw new RuntimeException("Error al procesar perfil: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Obtiene todos los perfiles del sistema.
     * 
     * @return Lista de perfiles o lista vacía si hay error
     */
    public List<PerfilEntity> obtenerTodos() {
        return executeReadOnly(em -> {
            try {
                PerfilDao perfilDao = new PerfilDao(em);
                List<PerfilEntity> perfiles = perfilDao.findAll();
                logger.debug("Se obtuvieron {} perfiles", perfiles.size());
                return perfiles;
            } catch (Exception e) {
                logger.error("Error al obtener todos los perfiles", e);
                return Collections.emptyList();
            }
        });
    }
}