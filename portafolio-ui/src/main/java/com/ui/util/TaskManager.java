package com.ui.util;

import com.ui.factory.ServiceResult;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utilitario mejorado para ejecutar tareas en background y mantener la UI
 * responsiva
 */
public class TaskManager {

    private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);

    public static <T> void executeAsync(
            Supplier<ServiceResult<T>> operation, // Acepta ServiceResult
            Consumer<T> onSuccess,
            Consumer<Throwable> onError,
            Runnable onFinally) {

        Task<T> task = new Task<T>() {
            @Override
            protected T call() throws Exception {
                try {
                    ServiceResult<T> result = operation.get();
                    if (result.isError()) {
                        throw new RuntimeException(result.getMessage());
                    }
                    return result.getData();
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
}
