package com.service.config;

import java.util.regex.Pattern;

/**
 * Configuración centralizada de seguridad para contraseñas.
 * Define políticas de seguridad y configuraciones robustas.
 */
public final class PasswordSecurityConfig {
    
    // CONFIGURACIÓN DE BCRYPT - FUERZA ALTA
    public static final int BCRYPT_STRENGTH = 12; // Más seguro que el default (10)
    
    // POLÍTICAS DE CONTRASEÑAS
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 128;
    public static final int MAX_LOGIN_ATTEMPTS = 3;
    public static final long LOCKOUT_DURATION_MINUTES = 15;
    
    // PATRONES DE VALIDACIÓN
    private static final String PASSWORD_PATTERN = 
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,128}$";
    
    private static final Pattern PASSWORD_REGEX = Pattern.compile(PASSWORD_PATTERN);
    
    // MENSAJES DE ERROR SEGUROS (no revelan detalles)
    public static final String INVALID_CREDENTIALS_MSG = "Credenciales inválidas";
    public static final String ACCOUNT_LOCKED_MSG = "Cuenta temporalmente bloqueada";
    public static final String WEAK_PASSWORD_MSG = "La contraseña no cumple los requisitos de seguridad";
    
    private PasswordSecurityConfig() {
        // Clase utilitaria - constructor privado
    }
    
    /**
     * Valida que una contraseña cumpla con las políticas de seguridad.
     * 
     * @param password Contraseña a validar
     * @return Resultado de la validación
     */
    public static PasswordValidationResult validatePassword(String password) {
        if (password == null) {
            return PasswordValidationResult.failure("La contraseña no puede ser nula");
        }
        
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return PasswordValidationResult.failure(
                "La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
        }
        
        if (password.length() > MAX_PASSWORD_LENGTH) {
            return PasswordValidationResult.failure(
                "La contraseña no puede exceder " + MAX_PASSWORD_LENGTH + " caracteres");
        }
        
        if (!PASSWORD_REGEX.matcher(password).matches()) {
            return PasswordValidationResult.failure(
                "La contraseña debe contener al menos: 1 mayúscula, 1 minúscula, 1 número y 1 carácter especial");
        }
        
        // Verificar contraseñas comunes (opcional)
        if (isCommonPassword(password)) {
            return PasswordValidationResult.failure(
                "La contraseña es demasiado común. Elija una más segura");
        }
        
        return PasswordValidationResult.success();
    }
    
    /**
     * Verifica si una contraseña está en la lista de contraseñas comunes.
     */
    private static boolean isCommonPassword(String password) {
        // Lista básica de contraseñas comunes
        String[] commonPasswords = {
            "password", "123456", "password123", "admin", "qwerty",
            "letmein", "welcome", "monkey", "dragon", "password1"
        };
        
        String lowerPassword = password.toLowerCase();
        for (String common : commonPasswords) {
            if (lowerPassword.equals(common)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Resultado de validación de contraseña.
     */
    public static class PasswordValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        private PasswordValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static PasswordValidationResult success() {
            return new PasswordValidationResult(true, null);
        }
        
        public static PasswordValidationResult failure(String errorMessage) {
            return new PasswordValidationResult(false, errorMessage);
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }
}
