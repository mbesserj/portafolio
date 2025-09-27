package com.app.service;


public class AuthenticationResult {
    private final boolean success;
    private final String message;
    private final String username;
    
    public AuthenticationResult(boolean success, String message) {
        this(success, message, null);
    }
    
    public AuthenticationResult(boolean success, String message, String username) {
        this.success = success;
        this.message = message;
        this.username = username;
    }
    
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getUsername() { return username; }
    
}