
package com.app.engine;

/**
 * Excepción que se lanza cuando no hay saldo de cantidad suficiente para realizar un egreso.
 */
public class InsufficientBalanceException extends Exception {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}