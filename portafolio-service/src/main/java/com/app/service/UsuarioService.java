package com.app.service;

import com.app.entities.PerfilEntity;
import com.app.entities.UsuarioEntity;
import com.app.interfaces.AbstractRepository; // O la ubicación correcta de tu clase abstracta
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio para la gestión de usuarios.
 * Sigue el patrón de Inyección de Dependencias para obtener sus colaboradores (PerfilService, PasswordEncoder)
 * y extiende AbstractRepository para la gestión de la persistencia.
 */
public class UsuarioService extends AbstractRepository {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioService.class);
    
    // Las dependencias son finales y se inyectan en el constructor.
    private final PerfilService perfilService;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Constructor único que declara las dependencias del servicio.
     * El ServiceContainer se encargará de proveer estas dependencias.
     *
     * @param perfilService El servicio para gestionar perfiles.
     * @param passwordEncoder El codificador para contraseñas.
     */
    public UsuarioService(PerfilService perfilService, BCryptPasswordEncoder passwordEncoder) {
        super();
        this.perfilService = perfilService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registra un nuevo usuario en el sistema.
     */
    public UsuarioEntity registrarNuevoUsuario(String username, String password, String email) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty() || email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Usuario, contraseña y email no pueden ser nulos o vacíos");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres");
        }

        return executeInTransaction(em -> {
            long count = em.createQuery("SELECT COUNT(u) FROM UsuarioEntity u WHERE u.usuario = :username", Long.class)
                           .setParameter("username", username.trim())
                           .getSingleResult();
            
            if (count > 0) {
                throw new IllegalStateException("El nombre de usuario '" + username + "' ya está en uso");
            }

            UsuarioEntity nuevoUsuario = new UsuarioEntity();
            nuevoUsuario.setUsuario(username.trim());
            nuevoUsuario.setCorreo(email.trim());
            nuevoUsuario.setPassword(passwordEncoder.encode(password));

            PerfilEntity perfilAdmin = perfilService.buscarOCrearPorNombre("ADMINISTRADOR");
            nuevoUsuario.getPerfiles().add(perfilAdmin);

            em.persist(nuevoUsuario);
            
            logger.info("Usuario '{}' registrado exitosamente con ID: {}", username, nuevoUsuario.getId());
            return nuevoUsuario;
        });
    }

    /**
     * Verifica si existe al menos un usuario en la base de datos.
     */
    public boolean hayUsuariosRegistrados() {
        return executeReadOnly(em -> {
            Long userCount = em.createQuery("SELECT COUNT(u.id) FROM UsuarioEntity u", Long.class)
                               .getSingleResult();
            return userCount != null && userCount > 0;
        });
    }

    /**
     * Obtiene todos los usuarios activos que tienen un perfil específico.
     */
    public List<UsuarioEntity> obtenerUsuariosPorPerfil(PerfilEntity perfil) {
        if (perfil == null) {
            return Collections.emptyList();
        }

        return executeReadOnly(em -> 
            em.createQuery("SELECT u FROM UsuarioEntity u WHERE :perfil MEMBER OF u.perfiles AND u.fechaInactivo IS NULL", UsuarioEntity.class)
              .setParameter("perfil", perfil)
              .getResultList()
        );
    }

    /**
     * Desactiva un usuario estableciendo su fecha de inactividad.
     */
    public void desactivarUsuario(Long usuarioId) {
        if (usuarioId == null) {
            throw new IllegalArgumentException("El ID del usuario no puede ser null");
        }

        executeInTransaction(em -> {
            UsuarioEntity usuario = em.find(UsuarioEntity.class, usuarioId);
            
            if (usuario == null) {
                logger.warn("Intento de desactivar usuario no existente con ID: {}", usuarioId);
                return;
            }

            if (usuario.getFechaInactivo() != null) {
                logger.info("Usuario con ID {} ya estaba desactivado.", usuarioId);
                return;
            }

            usuario.setFechaInactivo(LocalDate.now());
            em.merge(usuario);
            logger.info("Usuario '{}' (ID: {}) desactivado exitosamente", usuario.getUsuario(), usuarioId);
        });
    }

    /**
     * Cambia el perfil de un usuario, removiendo el perfil origen y añadiendo el destino.
     */
    public void cambiarPerfilDeUsuario(Long usuarioId, PerfilEntity perfilOrigen, PerfilEntity perfilDestino) {
        if (usuarioId == null || perfilOrigen == null || perfilDestino == null) {
            throw new IllegalArgumentException("Usuario ID, perfil origen y perfil destino no pueden ser null");
        }

        executeInTransaction(em -> {
            UsuarioEntity usuario = em.find(UsuarioEntity.class, usuarioId);
            if (usuario == null) {
                throw new IllegalStateException("No se encontró usuario con ID: " + usuarioId);
            }

            boolean teniaPerfil = usuario.getPerfiles().remove(perfilOrigen);
            if (teniaPerfil) {
                usuario.getPerfiles().add(perfilDestino);
                em.merge(usuario);
                logger.info("Perfil cambiado para usuario '{}' de '{}' a '{}'",
                    usuario.getUsuario(), perfilOrigen.getPerfil(), perfilDestino.getPerfil());
            } else {
                logger.warn("El usuario '{}' no tenía el perfil '{}' para ser cambiado.",
                    usuario.getUsuario(), perfilOrigen.getPerfil());
            }
        });
    }
}