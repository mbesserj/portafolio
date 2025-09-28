package com.serv.service;

import com.model.entities.PerfilEntity;
import com.model.entities.UsuarioEntity;
import com.model.interfaces.AbstractRepository;
import com.serv.service.AuthenticationService.PasswordEncodingResult;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio de usuarios actualizado con validación segura de contraseñas.
 */
public class UsuarioService extends AbstractRepository {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioService.class);

    private final PerfilService perfilService;
    private final AuthenticationService authenticationService;

    public UsuarioService(PerfilService perfilService, AuthenticationService authenticationService) {
        super();
        this.perfilService = perfilService;
        this.authenticationService = authenticationService;
    }

    /**
     * Registra un nuevo usuario con validación segura de contraseña.
     */
    public UserRegistrationResult registrarNuevoUsuario(String username, String password, String email) {
        // VALIDACIÓN BÁSICA
        if (username == null || username.trim().isEmpty()) {
            return UserRegistrationResult.failure("El nombre de usuario no puede estar vacío");
        }
        if (email == null || email.trim().isEmpty()) {
            return UserRegistrationResult.failure("El email no puede estar vacío");
        }

        // VALIDAR Y CODIFICAR CONTRASEÑA
        PasswordEncodingResult encodingResult = authenticationService.encodePassword(password);
        if (!encodingResult.isSuccess()) {
            return UserRegistrationResult.failure(encodingResult.getErrorMessage());
        }

        return executeInTransaction(em -> {
            try {
                // VERIFICAR QUE EL USERNAME NO EXISTE
                long count = em.createQuery("SELECT COUNT(u) FROM UsuarioEntity u WHERE LOWER(u.usuario) = LOWER(:username)", Long.class)
                        .setParameter("username", username.trim())
                        .getSingleResult();

                if (count > 0) {
                    logger.warn("Intento de registro con username duplicado: {}", username);
                    return UserRegistrationResult.failure("El nombre de usuario ya está en uso");
                }

                // CREAR NUEVO USUARIO
                UsuarioEntity nuevoUsuario = new UsuarioEntity();
                nuevoUsuario.setUsuario(username.trim().toLowerCase());
                nuevoUsuario.setCorreo(email.trim().toLowerCase());
                nuevoUsuario.setPassword(encodingResult.getEncodedPassword());

                // ASIGNAR PERFIL
                PerfilEntity perfilAdmin = perfilService.buscarOCrearPorNombre("ADMINISTRADOR");
                nuevoUsuario.getPerfiles().add(perfilAdmin);

                em.persist(nuevoUsuario);

                logger.info("Usuario '{}' registrado exitosamente con ID: {}", username, nuevoUsuario.getId());
                return UserRegistrationResult.success(nuevoUsuario);

            } catch (Exception e) {
                logger.error("Error al registrar usuario '{}'", username, e);
                return UserRegistrationResult.failure("Error interno al crear el usuario");
            }
        });
    }

    public UserRegistrationResult crearUsuarioAdmin(String username, String password) {
        return registrarNuevoUsuario(username, password, "");
    }

    /**
     * Cambia la contraseña de un usuario con validación segura.
     */
    public PasswordChangeResult cambiarContrasena(Long usuarioId, String currentPassword, String newPassword) {
        if (usuarioId == null) {
            return PasswordChangeResult.failure("ID de usuario inválido");
        }

        return executeInTransaction(em -> {
            try {
                UsuarioEntity usuario = em.find(UsuarioEntity.class, usuarioId);
                if (usuario == null) {
                    return PasswordChangeResult.failure("Usuario no encontrado");
                }

                // VERIFICAR CONTRASEÑA ACTUAL
                AuthenticationService.AuthenticationResult authResult
                        = authenticationService.autenticar(usuario.getUsuario(), currentPassword);

                if (!authResult.isSuccess()) {
                    logger.warn("Intento de cambio de contraseña con contraseña actual incorrecta para usuario: {}",
                            usuario.getUsuario());
                    return PasswordChangeResult.failure("Contraseña actual incorrecta");
                }

                // VALIDAR Y CODIFICAR NUEVA CONTRASEÑA
                PasswordEncodingResult encodingResult = authenticationService.encodePassword(newPassword);
                if (!encodingResult.isSuccess()) {
                    return PasswordChangeResult.failure(encodingResult.getErrorMessage());
                }

                // ACTUALIZAR CONTRASEÑA
                usuario.setPassword(encodingResult.getEncodedPassword());
                em.merge(usuario);

                logger.info("Contraseña cambiada exitosamente para usuario: {}", usuario.getUsuario());
                return PasswordChangeResult.success();

            } catch (Exception e) {
                logger.error("Error al cambiar contraseña para usuario ID: {}", usuarioId, e);
                return PasswordChangeResult.failure("Error interno al cambiar la contraseña");
            }
        });
    }

    /**
     * Resultado de registro de usuario.
     */
    public static class UserRegistrationResult {

        private final boolean success;
        private final String errorMessage;
        private final UsuarioEntity user;

        private UserRegistrationResult(boolean success, String errorMessage, UsuarioEntity user) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.user = user;
        }

        public static UserRegistrationResult success(UsuarioEntity user) {
            return new UserRegistrationResult(true, null, user);
        }

        public static UserRegistrationResult failure(String errorMessage) {
            return new UserRegistrationResult(false, errorMessage, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public UsuarioEntity getUser() {
            return user;
        }
    }

    /**
     * Resultado de cambio de contraseña.
     */
    public static class PasswordChangeResult {

        private final boolean success;
        private final String errorMessage;

        private PasswordChangeResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static PasswordChangeResult success() {
            return new PasswordChangeResult(true, null);
        }

        public static PasswordChangeResult failure(String errorMessage) {
            return new PasswordChangeResult(false, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
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

        return executeReadOnly(em
                -> em.createQuery("SELECT u FROM UsuarioEntity u WHERE :perfil MEMBER OF u.perfiles AND u.fechaInactivo IS NULL", UsuarioEntity.class)
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
     * Cambia el perfil de un usuario, removiendo el perfil origen y añadiendo
     * el destino.
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
