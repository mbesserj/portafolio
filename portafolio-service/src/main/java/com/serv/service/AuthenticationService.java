package com.serv.service;

import com.serv.config.PasswordSecurityConfig;
import com.serv.config.PasswordSecurityConfig.PasswordValidationResult;
import com.model.entities.UsuarioEntity;
import com.model.interfaces.AbstractRepository;
import com.app.sql.QueryRepository;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Servicio de autenticación con seguridad robusta. Incluye protección contra
 * ataques de fuerza bruta y validación segura de contraseñas.
 */
public class AuthenticationService extends AbstractRepository {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    // ENCODER CON CONFIGURACIÓN SEGURA
    private final BCryptPasswordEncoder passwordEncoder;

    // CONTROL DE INTENTOS DE LOGIN (EN MEMORIA - EN PRODUCCIÓN USAR REDIS/BD)
    private final ConcurrentMap<String, LoginAttemptInfo> loginAttempts = new ConcurrentHashMap<>();

    /**
     * Constructor con configuración de seguridad explícita.
     */
    public AuthenticationService(BCryptPasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Autentica un usuario con protección contra ataques de fuerza bruta.
     *
     * @param usuario El nombre de usuario
     * @param password La contraseña en texto plano
     * @return Resultado de la autenticación
     */
    public AuthenticationResult autenticar(String usuario, String password) {
        // VALIDACIÓN DE PARÁMETROS
        if (usuario == null || usuario.trim().isEmpty()) {
            logger.warn("Intento de login con usuario vacío desde IP: [implementar IP tracking]");
            return AuthenticationResult.failure(PasswordSecurityConfig.INVALID_CREDENTIALS_MSG);
        }

        if (password == null || password.trim().isEmpty()) {
            logger.warn("Intento de login con contraseña vacía para usuario: {}", usuario);
            return AuthenticationResult.failure(PasswordSecurityConfig.INVALID_CREDENTIALS_MSG);
        }

        String normalizedUser = usuario.trim().toLowerCase();

        // VERIFICAR SI LA CUENTA ESTÁ BLOQUEADA
        if (isAccountLocked(normalizedUser)) {
            logger.warn("Intento de login en cuenta bloqueada: {}", normalizedUser);
            return AuthenticationResult.failure(PasswordSecurityConfig.ACCOUNT_LOCKED_MSG);
        }

        return executeReadOnly(em -> {
            try {
                // BUSCAR USUARIO EN BD
                String sql_usuario_es_activo = QueryRepository.getAuthenticationQuery(
                        QueryRepository.AuthenticationQueries.USUARIO_ES_ACTIVO_QUERY);

                TypedQuery<UsuarioEntity> query = em.createQuery(sql_usuario_es_activo, UsuarioEntity.class);
                query.setParameter("user", normalizedUser);
                UsuarioEntity user = query.getSingleResult();

                // VERIFICAR CONTRASEÑA
                boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());

                if (passwordMatches) {
                    // LOGIN EXITOSO - LIMPIAR INTENTOS
                    clearLoginAttempts(normalizedUser);
                    logger.info("Login exitoso para usuario: {}", normalizedUser);
                    return AuthenticationResult.success(user);
                } else {
                    // PASSWORD INCORRECTO - REGISTRAR INTENTO FALLIDO
                    recordFailedAttempt(normalizedUser);
                    logger.warn("Password incorrecto para usuario: {}", normalizedUser);
                    return AuthenticationResult.failure(PasswordSecurityConfig.INVALID_CREDENTIALS_MSG);
                }

            } catch (NoResultException e) {
                // USUARIO NO ENCONTRADO - REGISTRAR INTENTO FALLIDO
                recordFailedAttempt(normalizedUser);
                logger.warn("Intento de login con usuario inexistente: {}", normalizedUser);
                return AuthenticationResult.failure(PasswordSecurityConfig.INVALID_CREDENTIALS_MSG);
            } catch (Exception e) {
                logger.error("Error durante autenticación del usuario: {}", normalizedUser, e);
                return AuthenticationResult.failure("Error interno del sistema");
            }
        });
    }

    /**
     * Valida y codifica una contraseña de forma segura.
     *
     * @param plainPassword Contraseña en texto plano
     * @return Resultado con la contraseña codificada o error
     */
    public PasswordEncodingResult encodePassword(String plainPassword) {
        // VALIDAR POLÍTICA DE CONTRASEÑAS
        PasswordValidationResult validation = PasswordSecurityConfig.validatePassword(plainPassword);

        if (!validation.isValid()) {
            logger.debug("Contraseña rechazada por política de seguridad");
            return PasswordEncodingResult.failure(validation.getErrorMessage());
        }

        try {
            // CODIFICAR CON BCRYPT SEGURO
            String encodedPassword = passwordEncoder.encode(plainPassword);
            logger.debug("Contraseña codificada exitosamente");
            return PasswordEncodingResult.success(encodedPassword);

        } catch (Exception e) {
            logger.error("Error al codificar contraseña", e);
            return PasswordEncodingResult.failure("Error al procesar la contraseña");
        }
    }

    /**
     * Verifica si una cuenta está bloqueada por intentos fallidos.
     */
    private boolean isAccountLocked(String username) {
        LoginAttemptInfo attemptInfo = loginAttempts.get(username);

        if (attemptInfo == null) {
            return false;
        }

        // VERIFICAR SI EL BLOQUEO HA EXPIRADO
        if (attemptInfo.isLockExpired()) {
            loginAttempts.remove(username);
            return false;
        }

        return attemptInfo.isLocked();
    }

    /**
     * Registra un intento de login fallido.
     */
    private void recordFailedAttempt(String username) {
        loginAttempts.compute(username, (key, attemptInfo) -> {
            if (attemptInfo == null) {
                return new LoginAttemptInfo();
            }
            attemptInfo.recordFailedAttempt();
            return attemptInfo;
        });
    }

    /**
     * Limpia los intentos de login fallidos para un usuario.
     */
    private void clearLoginAttempts(String username) {
        loginAttempts.remove(username);
    }

    /**
     * Información de intentos de login para un usuario.
     */
    private static class LoginAttemptInfo {

        private int attempts = 0;
        private LocalDateTime lastAttempt = LocalDateTime.now();
        private LocalDateTime lockTime = null;

        void recordFailedAttempt() {
            attempts++;
            lastAttempt = LocalDateTime.now();

            if (attempts >= PasswordSecurityConfig.MAX_LOGIN_ATTEMPTS) {
                lockTime = LocalDateTime.now().plusMinutes(PasswordSecurityConfig.LOCKOUT_DURATION_MINUTES);
            }
        }

        boolean isLocked() {
            return lockTime != null && LocalDateTime.now().isBefore(lockTime);
        }

        boolean isLockExpired() {
            return lockTime != null && LocalDateTime.now().isAfter(lockTime);
        }
    }

    /**
     * Resultado de autenticación.
     */
    public static class AuthenticationResult {

        private final boolean success;
        private final String errorMessage;
        private final UsuarioEntity user;

        private AuthenticationResult(boolean success, String errorMessage, UsuarioEntity user) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.user = user;
        }

        public static AuthenticationResult success(UsuarioEntity user) {
            return new AuthenticationResult(true, null, user);
        }

        public static AuthenticationResult failure(String errorMessage) {
            return new AuthenticationResult(false, errorMessage, null);
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
     * Resultado de codificación de contraseña.
     */
    public static class PasswordEncodingResult {

        private final boolean success;
        private final String encodedPassword;
        private final String errorMessage;

        private PasswordEncodingResult(boolean success, String encodedPassword, String errorMessage) {
            this.success = success;
            this.encodedPassword = encodedPassword;
            this.errorMessage = errorMessage;
        }

        public static PasswordEncodingResult success(String encodedPassword) {
            return new PasswordEncodingResult(true, encodedPassword, null);
        }

        public static PasswordEncodingResult failure(String errorMessage) {
            return new PasswordEncodingResult(false, null, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getEncodedPassword() {
            return encodedPassword;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
