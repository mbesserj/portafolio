package com.app.service;

import com.app.entities.UsuarioEntity;
import com.app.utiles.LibraryInitializer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; 

/**
 * Fachada para gestionar las operaciones de autenticación. Este es el único
 * punto de entrada para la validación de usuarios desde cualquier aplicación
 * cliente (como portafolio-ui).
 */
public class AuthenticationService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public boolean autenticar(String usuario, String password) {
        EntityManager em = LibraryInitializer.getEntityManager();
        try {
            TypedQuery<UsuarioEntity> query = em.createQuery(
                    "SELECT u FROM UsuarioEntity u WHERE u.usuario = :user", UsuarioEntity.class);
            query.setParameter("user", usuario);
            UsuarioEntity user = query.getSingleResult();

            return passwordEncoder.matches(password, user.getPassword());

        } catch (NoResultException e) {
            return false; 
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }
}