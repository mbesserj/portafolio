package com.app.service;

import com.app.dao.UsuarioDao;
import com.app.entities.PerfilEntity;
import com.app.entities.UsuarioEntity;
import com.app.utiles.LibraryInitializer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class UsuarioService {

    private final PerfilService perfilService = new PerfilService();
    // Creamos una instancia del codificador de Spring.
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UsuarioEntity registrarNuevoUsuario(String username, String password, String email) throws Exception {
        EntityManager em = LibraryInitializer.getEntityManager();
        UsuarioDao usuarioDao = new UsuarioDao(em);

        if (usuarioDao.findByUsername(username) != null) {
            throw new Exception("El nombre de usuario '" + username + "' ya está en uso.");
        }

        UsuarioEntity nuevoUsuario = new UsuarioEntity();
        nuevoUsuario.setUsuario(username);
        nuevoUsuario.setCorreo(email);
        nuevoUsuario.setPassword(passwordEncoder.encode(password));

        PerfilEntity perfilAdmin = perfilService.buscarOCrearPorNombre("ADMINISTRADOR");
        nuevoUsuario.getPerfiles().add(perfilAdmin);

        try {
            em.getTransaction().begin();
            usuarioDao.create(nuevoUsuario);
            em.getTransaction().commit();
            return nuevoUsuario;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo registrar el usuario.", e);
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Verifica si existe al menos un usuario en la base de datos.
     * @return true si hay uno o más usuarios, false en caso contrario.
     */
    public boolean hayUsuariosRegistrados() {
        EntityManager em = LibraryInitializer.getEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery("SELECT COUNT(u.id) FROM UsuarioEntity u", Long.class);
            Long userCount = query.getSingleResult();
            return userCount > 0;
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    public List<UsuarioEntity> obtenerUsuariosPorPerfil(PerfilEntity perfil) {
        EntityManager em = LibraryInitializer.getEntityManager();
        try {
            TypedQuery<UsuarioEntity> query = em.createQuery(
                    "SELECT u FROM UsuarioEntity u WHERE :perfil MEMBER OF u.perfiles AND u.fechaInactivo IS NULL",
                    UsuarioEntity.class);
            query.setParameter("perfil", perfil);
            return query.getResultList();
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    public void desactivarUsuario(Long usuarioId) {
        EntityManager em = LibraryInitializer.getEntityManager();
        try {
            em.getTransaction().begin();
            UsuarioDao usuarioDao = new UsuarioDao(em);
            UsuarioEntity usuario = usuarioDao.findById(usuarioId);
            if (usuario != null) {
                usuario.setFechaInactivo(LocalDate.now());
                usuarioDao.update(usuario);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al desactivar usuario", e);
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    public void cambiarPerfilDeUsuario(Long usuarioId, PerfilEntity perfilOrigen, PerfilEntity perfilDestino) {
        EntityManager em = LibraryInitializer.getEntityManager();
        try {
            em.getTransaction().begin();
            UsuarioDao usuarioDao = new UsuarioDao(em);
            UsuarioEntity usuario = usuarioDao.findById(usuarioId);
            if (usuario != null) {
                usuario.getPerfiles().remove(perfilOrigen);
                usuario.getPerfiles().add(perfilDestino);
                usuarioDao.update(usuario);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al cambiar perfil de usuario", e);
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }
}