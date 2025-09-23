package com.app.service;

import com.app.dao.PerfilDao;
import com.app.entities.PerfilEntity;
import com.app.utiles.LibraryInitializer;
import jakarta.persistence.EntityManager;
import java.util.List;

/**
 * Servicio para gestionar la lógica de negocio de los perfiles.
 */
public class PerfilService {

    /**
     * Busca un perfil por su nombre. Si no lo encuentra, lo crea dentro de la
     * misma transacción. Esto asegura que el perfil siempre exista cuando se
     * necesite.
     *
     * @param nombre El nombre del perfil a buscar o crear (ej:
     * "ADMINISTRADOR").
     * @return La entidad PerfilEntity encontrada o recién creada.
     */
    public PerfilEntity buscarOCrearPorNombre(String nombre) {
        EntityManager em = LibraryInitializer.getEntityManager();
        PerfilDao perfilDao = new PerfilDao(em);
        PerfilEntity perfil;

        try {
            em.getTransaction().begin();

            perfil = perfilDao.findByName(nombre);
            if (perfil == null) {
                System.out.println("Perfil '" + nombre + "' no encontrado. Creándolo...");
                perfil = new PerfilEntity(nombre);
                perfilDao.create(perfil);
            }
            em.getTransaction().commit();

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al buscar o crear el perfil: " + nombre, e);
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
        return perfil;
    }

    public List<PerfilEntity> obtenerTodos() {
        EntityManager em = LibraryInitializer.getEntityManager();
        try {
            PerfilDao perfilDao = new PerfilDao(em);
            return perfilDao.findAll(); // Usa el método heredado de AbstractJpaDao
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }
}
