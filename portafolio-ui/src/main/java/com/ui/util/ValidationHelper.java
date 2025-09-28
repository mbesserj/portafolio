package com.ui.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utilidad para validaciones comunes en la interfaz de usuario
 */
public class ValidationHelper {
    
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    
    /**
     * Resultado de una validación
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        private ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, new ArrayList<>());
        }
        
        public static ValidationResult error(String message) {
            List<String> errors = new ArrayList<>();
            errors.add(message);
            return new ValidationResult(false, errors);
        }
        
        public static ValidationResult errors(List<String> messages) {
            return new ValidationResult(false, new ArrayList<>(messages));
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }
        
        public String getFirstError() {
            return errors.isEmpty() ? "" : errors.get(0);
        }
        
        public String getAllErrors() {
            return String.join("\n", errors);
        }
        
        /**
         * Combina este resultado con otro
         */
        public ValidationResult and(ValidationResult other) {
            if (this.valid && other.valid) {
                return success();
            }
            
            List<String> combinedErrors = new ArrayList<>(this.errors);
            combinedErrors.addAll(other.errors);
            return errors(combinedErrors);
        }
    }
    
    /**
     * Valida que un campo requerido no esté vacío
     */
    public static ValidationResult validateRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return ValidationResult.error(fieldName + " es requerido");
        }
        return ValidationResult.success();
    }
    
    /**
     * Valida que un objeto requerido no sea null
     */
    public static ValidationResult validateRequired(Object value, String fieldName) {
        if (value == null) {
            return ValidationResult.error(fieldName + " es requerido");
        }
        return ValidationResult.success();
    }
    
    /**
     * Valida que un string sea un BigDecimal válido
     */
    public static ValidationResult validateBigDecimal(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return ValidationResult.success(); // Opcional por defecto
        }
        
        try {
            new BigDecimal(value.trim());
            return ValidationResult.success();
        } catch (NumberFormatException e) {
            return ValidationResult.error(fieldName + " debe ser un número válido");
        }
    }
    
    /**
     * Valida que un BigDecimal sea positivo
     */
    public static ValidationResult validatePositive(BigDecimal value, String fieldName) {
        if (value == null) {
            return ValidationResult.success(); // Se asume que ya se validó si es requerido
        }
        
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.error(fieldName + " debe ser mayor a cero");
        }
        return ValidationResult.success();
    }
    
    /**
     * Valida que un BigDecimal sea no negativo
     */
    public static ValidationResult validateNonNegative(BigDecimal value, String fieldName) {
        if (value == null) {
            return ValidationResult.success();
        }
        
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return ValidationResult.error(fieldName + " no puede ser negativo");
        }
        return ValidationResult.success();
    }
    
    /**
     * Valida formato de email
     */
    public static ValidationResult validateEmail(String email, String fieldName) {
        if (email == null || email.trim().isEmpty()) {
            return ValidationResult.success(); // Opcional por defecto
        }
        
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return ValidationResult.error(fieldName + " debe tener un formato válido");
        }
        return ValidationResult.success();
    }
    
    /**
     * Valida longitud mínima
     */
    public static ValidationResult validateMinLength(String value, int minLength, String fieldName) {
        if (value == null || value.length() < minLength) {
            return ValidationResult.error(fieldName + " debe tener al menos " + minLength + " caracteres");
        }
        return ValidationResult.success();
    }
    
    /**
     * Valida longitud máxima
     */
    public static ValidationResult validateMaxLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            return ValidationResult.error(fieldName + " no puede tener más de " + maxLength + " caracteres");
        }
        return ValidationResult.success();
    }
    
    /**
     * Valida que una fecha no sea futura
     */
    public static ValidationResult validateNotFuture(LocalDate date, String fieldName) {
        if (date == null) {
            return ValidationResult.success();
        }
        
        if (date.isAfter(LocalDate.now())) {
            return ValidationResult.error(fieldName + " no puede ser una fecha futura");
        }
        return ValidationResult.success();
    }
    
    /**
     * Valida que una fecha no sea muy antigua (más de 10 años)
     */
    public static ValidationResult validateNotTooOld(LocalDate date, String fieldName) {
        if (date == null) {
            return ValidationResult.success();
        }
        
        LocalDate tenYearsAgo = LocalDate.now().minusYears(10);
        if (date.isBefore(tenYearsAgo)) {
            return ValidationResult.error(fieldName + " no puede ser anterior a " + tenYearsAgo);
        }
        return ValidationResult.success();
    }
    
    /**
     * Valida un BigDecimal requerido y positivo
     */
    public static ValidationResult validateRequiredPositiveBigDecimal(String value, String fieldName) {
        ValidationResult requiredResult = validateRequired(value, fieldName);
        if (!requiredResult.isValid()) {
            return requiredResult;
        }
        
        ValidationResult decimalResult = validateBigDecimal(value, fieldName);
        if (!decimalResult.isValid()) {
            return decimalResult;
        }
        
        try {
            BigDecimal decimal = new BigDecimal(value.trim());
            return validatePositive(decimal, fieldName);
        } catch (NumberFormatException e) {
            return ValidationResult.error(fieldName + " debe ser un número válido");
        }
    }
    
    /**
     * Builder para validaciones complejas
     */
    public static class ValidationBuilder {
        private ValidationResult result = ValidationResult.success();
        
        public ValidationBuilder required(String value, String fieldName) {
            result = result.and(validateRequired(value, fieldName));
            return this;
        }
        
        public ValidationBuilder required(Object value, String fieldName) {
            result = result.and(validateRequired(value, fieldName));
            return this;
        }
        
        public ValidationBuilder bigDecimal(String value, String fieldName) {
            result = result.and(validateBigDecimal(value, fieldName));
            return this;
        }
        
        public ValidationBuilder positive(BigDecimal value, String fieldName) {
            result = result.and(validatePositive(value, fieldName));
            return this;
        }
        
        public ValidationBuilder email(String email, String fieldName) {
            result = result.and(validateEmail(email, fieldName));
            return this;
        }
        
        public ValidationBuilder minLength(String value, int minLength, String fieldName) {
            result = result.and(validateMinLength(value, minLength, fieldName));
            return this;
        }
        
        public ValidationBuilder custom(ValidationResult customValidation) {
            result = result.and(customValidation);
            return this;
        }
        
        public ValidationResult build() {
            return result;
        }
    }
    
    /**
     * Crea un nuevo builder para validaciones
     */
    public static ValidationBuilder validate() {
        return new ValidationBuilder();
    }
}