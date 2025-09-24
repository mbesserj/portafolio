package com.app.ui.controller;

import javafx.concurrent.Task;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utilitario mejorado para ejecutar tareas en background y mantener la UI responsiva
 */
public class TaskManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);
    
    /**
     * Ejecuta una operación en background con callbacks completos
     */
    public static <T> void executeAsync(
            Supplier<T> operation,
            Consumer<T> onSuccess,
            Consumer<Throwable> onError,
            Runnable onFinally) {
        
        Task<T> task = new Task<T>() {
            @Override
            protected T call() throws Exception {
                try {
                    return operation.get();
                } catch (Exception e) {
                    logger.error("Error en tarea asíncrona", e);
                    throw e;
                }
            }
        };
        
        task.setOnSucceeded(event -> {
            try {
                T result = task.getValue();
                Platform.runLater(() -> onSuccess.accept(result));
            } catch (Exception e) {
                logger.error("Error en callback de éxito", e);
                Platform.runLater(() -> onError.accept(e));
            } finally {
                if (onFinally != null) {
                    Platform.runLater(onFinally);
                }
            }
        });
        
        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            logger.error("Falla en tarea asíncrona", exception);
            Platform.runLater(() -> onError.accept(exception));
            if (onFinally != null) {
                Platform.runLater(onFinally);
            }
        });
        
        // Ejecutar en thread separado
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.setName("PortafolioTask-" + System.currentTimeMillis());
        thread.start();
    }
    
    /**
     * Versión simplificada para operaciones sin retorno
     */
    public static void executeAsync(
            Runnable operation,
            Runnable onSuccess,
            Consumer<Throwable> onError) {
        
        executeAsync(
            () -> { 
                operation.run(); 
                return null; 
            },
            result -> onSuccess.run(),
            onError,
            null
        );
    }
    
    /**
     * Para operaciones largas con progreso
     */
    public static <T> Task<T> createProgressTask(
            Supplier<T> operation,
            Consumer<Double> progressCallback) {
        
        return new Task<T>() {
            @Override
            protected T call() throws Exception {
                updateProgress(0, 100);
                T result = operation.get();
                updateProgress(100, 100);
                return result;
            }
            
            @Override
            protected void updateProgress(double workDone, double max) {
                super.updateProgress(workDone, max);
                if (progressCallback != null) {
                    Platform.runLater(() -> progressCallback.accept(workDone / max));
                }
            }
        };
    }
    
    /**
     * Ejecuta una tarea con indicador de progreso
     */
    public static <T> void executeWithProgress(
            Supplier<T> operation,
            Consumer<T> onSuccess,
            Consumer<Throwable> onError,
            Consumer<Double> onProgress) {
        
        Task<T> task = createProgressTask(operation, onProgress);
        
        task.setOnSucceeded(e -> Platform.runLater(() -> onSuccess.accept(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> onError.accept(task.getException())));
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}