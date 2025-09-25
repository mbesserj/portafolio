package com.app.service;

import com.app.entities.UsuarioEntity;
import com.app.interfaces.AbstractRepository;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Fachada para gestionar las operaciones de autenticación.
 */
public class AuthenticationService extends AbstractRepository {

    private static final String QUERY_USUARIO_BY_USERNAME = 
        "SELECT u FROM UsuarioEntity u WHERE u.usuario = :user";

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthenticationService() {
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Autentica un usuario validando sus credenciales.
     * 
     * @param usuario El nombre de usuario
     * @param password La contraseña en texto plano
     * @return true si las credenciales son válidas, false en caso contrario
     * @throws IllegalArgumentException si usuario o password son nulos o vacíos
     */
    public boolean autenticar(String usuario, String password) {
        if (usuario == null || usuario.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario no puede ser nulo o vacío");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede ser nula o vacía");
        }

        return executeReadOnly(em -> {
            try {
                TypedQuery<UsuarioEntity> query = em.createQuery(QUERY_USUARIO_BY_USERNAME, UsuarioEntity.class);
                query.setParameter("user", usuario.trim());
                UsuarioEntity user = query.getSingleResult();
                
                boolean matches = passwordEncoder.matches(password, user.getPassword());
                logger.debug("Intento de autenticación para usuario: {} - Resultado: {}", usuario, matches);
                return matches;
                
            } catch (NoResultException e) {
                logger.debug("Usuario no encontrado: {}", usuario);
                return false;
            } catch (Exception e) {
                logger.error("Error durante la autenticación del usuario: {}", usuario, e);
                return false;
            }
        });
    }
}