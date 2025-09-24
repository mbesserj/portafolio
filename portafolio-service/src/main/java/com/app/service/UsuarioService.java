package com.app.service;

import com.app.dao.UsuarioDao;
import com.app.entities.PerfilEntity;
import com.app.entities.UsuarioEntity;
import jakarta.persistence.TypedQuery;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class UsuarioService extends AbstractRepository {

    private static final String QUERY_COUNT_USUARIOS = "SELECT COUNT(u.id) FROM UsuarioEntity u";
    private static final String QUERY_USUARIOS_POR_PERFIL = 
        "SELECT u FROM UsuarioEntity u WHERE :perfil MEMBER OF u.perfiles AND u.fechaInactivo IS NULL";

    private final PerfilService perfilService;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Constructor con inyección de dependencias.
     */
    public UsuarioService() {
        this.perfilService = new PerfilService();
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Constructor alternativo para facilitar testing con inyección de dependencias.
     * @param perfilService
     * @param passwordEncoder
     */
    public UsuarioService(PerfilService perfilService, BCryptPasswordEncoder passwordEncoder) {
        this.perfilService = perfilService != null ? perfilService : new PerfilService();
        this.passwordEncoder = passwordEncoder != null ? passwordEncoder : new BCryptPasswordEncoder();
    }

    /**
     * Registra un nuevo usuario en el sistema.
     * 
     * @param username Nombre de usuario único
     * @param password Contraseña en texto plano
     * @param email Correo electrónico
     * @return El usuario creado
     * @throws IllegalArgumentException si los parámetros son inválidos
     * @throws RuntimeException si el usuario ya existe o falla la creación
     */
    public UsuarioEntity registrarNuevoUsuario(String username, String password, String email) {
        // Validaciones
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario no puede ser nulo o vacío");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede ser nula o vacía");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El email no puede ser nulo o vacío");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres");
        }

        return executeInTransaction(em -> {
            try {
                UsuarioDao usuarioDao = new UsuarioDao(em);

                // Verificar si el usuario ya existe
                if (usuarioDao.findByUsername(username.trim()) != null) {
                    throw new RuntimeException("El nombre de usuario '" + username + "' ya está en uso");
                }

                // Crear nuevo usuario
                UsuarioEntity nuevoUsuario = new UsuarioEntity();
                nuevoUsuario.setUsuario(username.trim());
                nuevoUsuario.setCorreo(email.trim());
                nuevoUsuario.setPassword(passwordEncoder.encode(password));

                // Asignar perfil de administrador
                PerfilEntity perfilAdmin = perfilService.buscarOCrearPorNombre("ADMINISTRADOR");
                nuevoUsuario.getPerfiles().add(perfilAdmin);

                usuarioDao.create(nuevoUsuario);
                
                logger.info("Usuario '{}' registrado exitosamente con ID: {}", username, nuevoUsuario.getId());
                return nuevoUsuario;
                
            } catch (RuntimeException e) {
                logger.error("Error al registrar usuario '{}'", username, e);
                throw e;
            } catch (Exception e) {
                logger.error("Error inesperado al registrar usuario '{}'", username, e);
                throw new RuntimeException("Error al registrar usuario: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Verifica si existe al menos un usuario en la base de datos.
     * 
     * @return true si hay usuarios registrados, false en caso contrario
     */
    public boolean hayUsuariosRegistrados() {
        return executeReadOnly(em -> {
            try {
                TypedQuery<Long> query = em.createQuery(QUERY_COUNT_USUARIOS, Long.class);
                Long userCount = query.getSingleResult();
                boolean hayUsuarios = userCount != null && userCount > 0;
                logger.debug("Usuarios registrados en el sistema: {}", userCount);
                return hayUsuarios;
            } catch (Exception e) {
                logger.error("Error al verificar si hay usuarios registrados", e);
                return false;
            }
        });
    }

    /**
     * Obtiene todos los usuarios activos que tienen un perfil específico.
     * 
     * @param perfil El perfil a buscar
     * @return Lista de usuarios con el perfil especificado
     */
    public List<UsuarioEntity> obtenerUsuariosPorPerfil(PerfilEntity perfil) {
        if (perfil == null) {
            logger.debug("Perfil es null, retornando lista vacía");
            return Collections.emptyList();
        }

        return executeReadOnly(em -> {
            try {
                TypedQuery<UsuarioEntity> query = em.createQuery(QUERY_USUARIOS_POR_PERFIL, UsuarioEntity.class);
                query.setParameter("perfil", perfil);
                
                List<UsuarioEntity> usuarios = query.getResultList();
                logger.debug("Se encontraron {} usuarios con perfil '{}'", usuarios.size(), perfil.getPerfil());
                return usuarios;
                
            } catch (Exception e) {
                logger.error("Error al obtener usuarios por perfil '{}'", perfil.getPerfil(), e);
                return Collections.emptyList();
            }
        });
    }

    /**
     * Desactiva un usuario estableciendo su fecha de inactividad.
     * 
     * @param usuarioId El ID del usuario a desactivar
     * @throws IllegalArgumentException si usuarioId es null
     * @throws RuntimeException si falla la desactivación
     */
    public void desactivarUsuario(Long usuarioId) {
        if (usuarioId == null) {
            throw new IllegalArgumentException("El ID del usuario no puede ser null");
        }

        executeInTransaction(em -> {
            try {
                UsuarioDao usuarioDao = new UsuarioDao(em);
                UsuarioEntity usuario = usuarioDao.findById(usuarioId);
                
                if (usuario == null) {
                    logger.warn("No se encontró usuario con ID: {}", usuarioId);
                    return;
                }
                
                if (usuario.getFechaInactivo() != null) {
                    logger.info("Usuario con ID {} ya estaba desactivado desde: {}", usuarioId, usuario.getFechaInactivo());
                    return;
                }

                usuario.setFechaInactivo(LocalDate.now());
                usuarioDao.update(usuario);
                
                logger.info("Usuario '{}' (ID: {}) desactivado exitosamente", usuario.getUsuario(), usuarioId);
                
            } catch (Exception e) {
                logger.error("Error al desactivar usuario con ID: {}", usuarioId, e);
                throw new RuntimeException("Error al desactivar usuario: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Cambia el perfil de un usuario, removiendo el perfil origen y añadiendo el destino.
     * 
     * @param usuarioId El ID del usuario
     * @param perfilOrigen El perfil a remover
     * @param perfilDestino El perfil a añadir
     * @throws IllegalArgumentException si algún parámetro es null
     * @throws RuntimeException si falla el cambio de perfil
     */
    public void cambiarPerfilDeUsuario(Long usuarioId, PerfilEntity perfilOrigen, PerfilEntity perfilDestino) {
        if (usuarioId == null) {
            throw new IllegalArgumentException("El ID del usuario no puede ser null");
        }
        if (perfilOrigen == null) {
            throw new IllegalArgumentException("El perfil origen no puede ser null");
        }
        if (perfilDestino == null) {
            throw new IllegalArgumentException("El perfil destino no puede ser null");
        }

        executeInTransaction(em -> {
            try {
                UsuarioDao usuarioDao = new UsuarioDao(em);
                UsuarioEntity usuario = usuarioDao.findById(usuarioId);
                
                if (usuario == null) {
                    throw new RuntimeException("No se encontró usuario con ID: " + usuarioId);
                }

                boolean teniaPerfil = usuario.getPerfiles().remove(perfilOrigen);
                boolean agregoPerfil = usuario.getPerfiles().add(perfilDestino);
                
                if (teniaPerfil && agregoPerfil) {
                    usuarioDao.update(usuario);
                    logger.info("Perfil cambiado para usuario '{}' - De '{}' a '{}'", 
                               usuario.getUsuario(), perfilOrigen.getPerfil(), perfilDestino.getPerfil());
                } else if (!teniaPerfil) {
                    logger.warn("Usuario '{}' no tenía el perfil origen '{}'", 
                               usuario.getUsuario(), perfilOrigen.getPerfil());
                }
                
            } catch (Exception e) {
                logger.error("Error al cambiar perfil de usuario con ID: {}", usuarioId, e);
                throw new RuntimeException("Error al cambiar perfil de usuario: " + e.getMessage(), e);
            }
        });
    }
}