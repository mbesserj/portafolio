package com.app.ui.controller;

import com.app.facade.ServiceFactory;
import com.app.dto.ResultadoCargaDto;
import com.app.enums.ListaEnumsCustodios;
import com.app.service.ProcesoCargaDiariaService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;


/**
 * Controlador principal de la aplicación.
 * Maneja la navegación y los procesos principales del sistema.
 */
public class AppController extends BaseController {

    @FXML private BorderPane mainPane;

    private final NavigatorService navigatorService;

    public AppController(ServiceFactory serviceFactory, NavigatorService navigatorService, ResourceBundle resourceBundle) {
        super(serviceFactory, resourceBundle);
        this.navigatorService = navigatorService;
        logger.info("AppController inicializado");
    }
    
    @FXML
    public void initialize() {
        navigatorService.setMainPane(mainPane);
        onInitialize();
    }
    
    @Override
    protected void onInitialize() {
        // Configuraciones adicionales si son necesarias
        logger.debug("AppController configurado correctamente");
    }
    
    // --- MANEJADORES DE NAVEGACIÓN ---

    @FXML
    private void handleMostrarKardex() {
        try {
            navigatorService.cargarVistaKardex();
            logger.debug("Vista de Kardex cargada");
        } catch (Exception e) {
            showError("Error de Navegación", "No se pudo cargar la vista de Kardex", e);
        }
    }
    
    @FXML
    private void handleMostrarSaldos() {
        try {
            navigatorService.cargarVistaSaldos();
            logger.debug("Vista de Saldos cargada");
        } catch (Exception e) {
            showError("Error de Navegación", "No se pudo cargar la vista de Saldos", e);
        }
    }
    
    @FXML
    private void handleMostrarSaldosMensuales() {
        try {
            navigatorService.cargarVistaSaldosMensuales();
            logger.debug("Vista de Saldos Mensuales cargada");
        } catch (Exception e) {
            showError("Error de Navegación", "No se pudo cargar la vista de Saldos Mensuales", e);
        }
    }

    @FXML
    private void handleMostrarResumenSaldos() {
        try {
            navigatorService.cargarVistaResumenEmpSaldo();
            logger.debug("Vista de Resumen de Saldos cargada");
        } catch (Exception e) {
            showError("Error de Navegación", "No se pudo cargar la vista de Resumen de Saldos", e);
        }
    }

    @FXML
    private void handleMostrarConfrontaSaldos() {
        try {
            navigatorService.cargarVistaConfrontaSaldo();
            logger.debug("Vista de Confronta Saldos cargada");
        } catch (Exception e) {
            showError("Error de Navegación", "No se pudo cargar la vista de Confronta Saldos", e);
        }
    }

    @FXML
    private void handleMostrarResultadosInstrumento() {
        try {
            navigatorService.cargarVistaResultadosInstrumento();
            logger.debug("Vista de Resultados por Instrumento cargada");
        } catch (Exception e) {
            showError("Error de Navegación", "No se pudo cargar la vista de Resultados por Instrumento", e);
        }
    }

    @FXML
    private void handleMostrarResumenPortafolio() {
        try {
            navigatorService.cargarVistaResumenPortafolio();
            logger.debug("Vista de Resumen de Portafolio cargada");
        } catch (Exception e) {
            showError("Error de Navegación", "No se pudo cargar la vista de Resumen de Portafolio", e);
        }
    }

    @FXML
    private void handleMostrarResumenHistorico() {
        try {
            navigatorService.cargarVistaResumenHistorico();
            logger.debug("Vista de Resumen Histórico cargada");
        } catch (Exception e) {
            showError("Error de Navegación", "No se pudo cargar la vista de Resumen Histórico", e);
        }
    }

    @FXML
    private void handleMostrarTransacciones() {
        try {
            navigatorService.cargarVistaOperacionesTrxs();
            logger.debug("Vista de Transacciones cargada");
        } catch (Exception e) {
            showError("Error de Navegación", "No se pudo cargar la vista de Transacciones", e);
        }
    }

    @FXML
    private void handleMostrarTrxsProblemas() {
        try {
            navigatorService.cargarVistaProblemasTrxs();
            logger.debug("Vista de Transacciones con Problemas cargada");
        } catch (Exception e) {
            showError("Error de Navegación", "No se pudo cargar la vista de Transacciones con Problemas", e);
        }
    }

    @FXML
    private void handleMostrarTiposMovimiento() {
        try {
            navigatorService.mostrarVentanaTiposMovimiento();
            logger.debug("Vista de Tipos de Movimiento mostrada");
        } catch (Exception e) {
            showError("Error de Navegación", "No se pudo mostrar la vista de Tipos de Movimiento", e);
        }
    }
    
    @FXML
    private void handleTransaccionManual() {
        try {
            navigatorService.mostrarVistaTransaccionManual();
            logger.debug("Vista de Transacción Manual mostrada");
        } catch (Exception e) {
            showError("Error de Navegación", "No se pudo mostrar la vista de Transacción Manual", e);
        }
    }

    @FXML
    private void handleMostrarCierreContable() {
        try {
            navigatorService.cargarVistaCierreContable();
            logger.debug("Vista de Cierre Contable cargada");
        } catch (Exception e) {
            showError("Error de Navegación", "No se pudo cargar la vista de Cierre Contable", e);
        }
    }

    @FXML
    private void handleSalir(ActionEvent event) {
        logger.info("Cerrando aplicación por solicitud del usuario");
        Platform.exit();
    }
    
    // --- MANEJADORES DE PROCESOS ---

    @FXML
    private void handleCargarArchivos(ActionEvent event) {
        logger.info("Iniciando proceso de carga de archivos");
        
        Optional<ListaEnumsCustodios> custodio = pedirCustodio("Selecciona el custodio para la carga.");
        if (custodio.isEmpty()) {
            logger.debug("Proceso cancelado: no se seleccionó custodio");
            return;
        }

        List<File> archivos = pedirArchivosExcel("Selecciona uno o más archivos para cargar");
        if (archivos == null || archivos.isEmpty()) {
            logger.debug("Proceso cancelado: no se seleccionaron archivos");
            return;
        }

        logger.info("Iniciando carga de {} archivos para custodio {}", archivos.size(), custodio.get());

        Task<ResultadoCargaDto> task = new Task<>() {
            @Override
            protected ResultadoCargaDto call() throws Exception {
                ProcesoCargaDiariaService procesoDiario = new ProcesoCargaDiariaService();
                ResultadoCargaDto resultadoFinal = null;

                for (File archivo : archivos) {
                    updateMessage("Procesando: " + archivo.getName());
                    resultadoFinal = procesoDiario.ejecutar(custodio.get(), archivo);
                    
                    if (resultadoFinal.getRegistrosProcesados() == 0) {
                        updateMessage("Error procesando " + archivo.getName() + ". Abortando.");
                        logger.warn("Error procesando archivo: {}", archivo.getName());
                        break;
                    }
                }
                return resultadoFinal;
            }
        };
        
        ejecutarTareaConDialogo(task, "Carga de Archivos");
    }

    @FXML
    private void handleCargaInicial(ActionEvent event) {
        logger.info("Iniciando proceso de carga inicial");
        
        Optional<ListaEnumsCustodios> custodio = pedirCustodio("Selecciona el custodio para la carga inicial.");
        if (custodio.isEmpty()) {
            logger.debug("Proceso cancelado: no se seleccionó custodio");
            return;
        }

        List<File> archivos = pedirArchivosExcel("Selecciona uno o más archivos para la carga inicial");
        if (archivos == null || archivos.isEmpty()) {
            logger.debug("Proceso cancelado: no se seleccionaron archivos");
            return;
        }

        // Confirmación de proceso destructivo
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar Proceso Irreversible");
        confirmacion.setHeaderText("¡ATENCIÓN! ESTA ACCIÓN BORRARÁ TODOS LOS DATOS EXISTENTES.");
        confirmacion.setContentText("Se borrarán todas las transacciones, saldos y kárdex.\n\n¿Estás seguro de que deseas continuar?");

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            logger.warn("Usuario confirmó carga inicial destructiva");
            
            Task<ResultadoCargaDto> task = new Task<>() {
                @Override
                protected ResultadoCargaDto call() throws Exception {
 
                    ResultadoCargaDto resultadoFinal = null;

                    for (File archivo : archivos) {
                        updateMessage("Procesando: " + archivo.getName());
                        resultadoFinal = serviceFactory.ejecutarCargaInicial(custodio.get(), archivo);
                        
                        if (resultadoFinal.getRegistrosProcesados() == 0) {
                            updateMessage("Error procesando " + archivo.getName() + ". Abortando.");
                            logger.warn("Error procesando archivo: {}", archivo.getName());
                            break;
                        }
                    }
                    return resultadoFinal;
                }
            };
            ejecutarTareaConDialogo(task, "Carga Inicial Completa");
        } else {
            logger.debug("Usuario canceló la carga inicial");
        }
    }
    
    @FXML
    private void handleEjecutarCosteo(ActionEvent event) {
        logger.info("Iniciando proceso de costeo general");
        
        Task<Void> costeoTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Ejecutando proceso de costeo...");
                serviceFactory.iniciarCosteoCompleto();
                return null;
            }
        };
        ejecutarTareaConDialogo(costeoTask, "Proceso de Costeo General");
    }

    @FXML
    private void handleReprocesarNormalizacion(ActionEvent event) {
        logger.info("Iniciando reprocesamiento de normalización");
        
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar Reprocesamiento");
        confirmacion.setHeaderText("Esta acción procesará todos los registros pendientes de la tabla de carga.");
        confirmacion.setContentText("¿Deseas continuar?");

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    updateMessage("Reprocesando normalización...");
                    serviceFactory.getNormalizarService().ejecutar();
                    return null;
                }
            };
            ejecutarTareaConDialogo(task, "Reprocesamiento de Normalización");
        } else {
            logger.debug("Usuario canceló el reprocesamiento");
        }
    }

    // --- MÉTODOS DE AYUDA (HELPERS) ---

    private Optional<ListaEnumsCustodios> pedirCustodio(String headerText) {
        ChoiceDialog<ListaEnumsCustodios> dialogo = new ChoiceDialog<>(ListaEnumsCustodios.Fynsa, ListaEnumsCustodios.values());
        dialogo.setTitle("Selección de Custodio");
        dialogo.setHeaderText(headerText);
        dialogo.initOwner(mainPane.getScene().getWindow());
        return dialogo.showAndWait();
    }

    private List<File> pedirArchivosExcel(String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "Archivos Excel (*.xlsx, *.xls)", "*.xlsx", "*.xls"
        );
        fileChooser.getExtensionFilters().add(extFilter);
        return fileChooser.showOpenMultipleDialog(mainPane.getScene().getWindow());
    }

    private void ejecutarTareaConDialogo(Task<?> task, String nombreProceso) {
        logger.debug("Ejecutando tarea: {}", nombreProceso);
        
        Dialog<Void> dialogoEspera = new Dialog<>();
        dialogoEspera.initOwner(mainPane.getScene().getWindow());
        dialogoEspera.setTitle("Proceso en Curso...");
        dialogoEspera.setHeaderText("Ejecutando " + nombreProceso + ", por favor espera.");
        dialogoEspera.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialogoEspera.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(false);

        dialogoEspera.show();

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            dialogoEspera.close();
            
            Object resultado = task.getValue();
            String mensaje = nombreProceso + " se ha completado exitosamente.";
            
            if (resultado instanceof ResultadoCargaDto) {
                ResultadoCargaDto resultadoCarga = (ResultadoCargaDto) resultado;
                mensaje = resultadoCarga.getMensaje();
                logger.info("Proceso completado: {} - Registros procesados: {}", 
                           nombreProceso, resultadoCarga.getRegistrosProcesados());
            }
            
            showSuccess(mensaje);
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            dialogoEspera.close();
            Throwable ex = task.getException();
            logger.error("Error en proceso {}: {}", nombreProceso, ex.getMessage(), ex);
            showError("Error en el Proceso", 
                     "Ocurrió un error inesperado durante el proceso: " + nombreProceso, ex);
        }));

        new Thread(task).start();
    }
}