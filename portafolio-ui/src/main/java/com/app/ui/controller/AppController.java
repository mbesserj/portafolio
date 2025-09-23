package com.app.ui.controller;

import com.app.dto.ResultadoCargaDto;
import com.app.enums.ListaEnumsCustodios;
import com.app.service.ProcesoCargaDiaria;
import com.app.service.ProcesoCargaInicial;
import com.app.ui.service.NavigatorService;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
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
import java.time.Duration;

public class AppController {

    @FXML
    private BorderPane mainPane;

    private final ServiceFactory serviceFactory;
    private final NavigatorService navigatorService;
    private final ResourceBundle resourceBundle;

    public AppController(ServiceFactory serviceFactory, NavigatorService navigatorService, ResourceBundle resourceBundle) {
        this.serviceFactory = serviceFactory;
        this.navigatorService = navigatorService;
        this.resourceBundle = resourceBundle;
    }
    
    @FXML
    public void initialize() {
        navigatorService.setMainPane(mainPane);
    }
    
    // --- MANEJADORES DE NAVEGACIÓN (COMPLETOS) ---

    @FXML
    private void handleMostrarKardex() {
        navigatorService.cargarVistaKardex();
    }
    
    @FXML
    private void handleMostrarSaldos() {
        navigatorService.cargarVistaSaldos();
    }
    
    @FXML
    private void handleMostrarSaldosMensuales() {
        navigatorService.cargarVistaSaldosMensuales();
    }

    /**
     * CORRECCIÓN: Se añade el método que faltaba.
     */
    @FXML
    private void handleMostrarResumenSaldos() {
        navigatorService.cargarVistaResumenEmpSaldo();
    }

    @FXML
    private void handleMostrarConfrontaSaldos() {
        navigatorService.cargarVistaConfrontaSaldo();
    }

    @FXML
    private void handleMostrarResultadosInstrumento() {
        navigatorService.cargarVistaResultadosInstrumento();
    }

    @FXML
    private void handleMostrarResumenPortafolio() {
        navigatorService.cargarVistaResumenPortafolio();
    }

    @FXML
    private void handleMostrarResumenHistorico() {
        navigatorService.cargarVistaResumenHistorico();
    }

    @FXML
    private void handleMostrarTransacciones() {
        navigatorService.cargarVistaOperacionesTrxs();
    }

    @FXML
    private void handleMostrarTrxsProblemas() {
        navigatorService.cargarVistaProblemasTrxs();
    }

    @FXML
    private void handleMostrarTiposMovimiento() {
        navigatorService.mostrarVentanaTiposMovimiento();
    }
    
    @FXML
    private void handleTransaccionManual() {
        navigatorService.mostrarVistaTransaccionManual();
    }

    @FXML
    private void handleMostrarCierreContable() {
        navigatorService.cargarVistaCierreContable();
    }

    @FXML
    private void handleSalir(ActionEvent event) {
        Platform.exit();
    }
    
    // --- MANEJADORES DE PROCESOS (COMPLETOS) ---

    @FXML
    private void handleCargarArchivos(ActionEvent event) {
        Optional<ListaEnumsCustodios> custodio = pedirCustodio("Selecciona el custodio para la carga.");
        if (custodio.isEmpty()) {
            return;
        }

        List<File> archivos = pedirArchivosExcel("Selecciona uno o más archivos para cargar");
        if (archivos == null || archivos.isEmpty()) {
            return;
        }

        Task<ResultadoCargaDto> task = new Task<>() {
            @Override
            protected ResultadoCargaDto call() throws Exception {
                ProcesoCargaDiaria procesoDiario = new ProcesoCargaDiaria();
                ResultadoCargaDto resultadoFinal = null;

                for (File archivo : archivos) {
                    resultadoFinal = procesoDiario.ejecutar(custodio.get(), archivo);
                    if (resultadoFinal.getRegistrosProcesados() == 0) {
                        updateMessage("Error procesando " + archivo.getName() + ". Abortando.");
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
        Optional<ListaEnumsCustodios> custodio = pedirCustodio("Selecciona el custodio para la carga inicial.");
        if (custodio.isEmpty()) {
            return;
        }

        List<File> archivos = pedirArchivosExcel("Selecciona uno o más archivos para la carga inicial");
        if (archivos == null || archivos.isEmpty()) {
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar Proceso Irreversible");
        confirmacion.setHeaderText("¡ATENCIÓN! ESTA ACCIÓN BORRARÁ TODOS LOS DATOS EXISTENTES.");
        confirmacion.setContentText("Se borrarán todas las transacciones, saldos y kárdex.\n\n¿Estás seguro de que deseas continuar?");

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            Task<ResultadoCargaDto> task = new Task<>() {
                @Override
                protected ResultadoCargaDto call() throws Exception {
                    ProcesoCargaInicial procesoInicial = new ProcesoCargaInicial();
                    ResultadoCargaDto resultadoFinal = null;

                    for (File archivo : archivos) {
                        resultadoFinal = procesoInicial.ejecutar(custodio.get(), archivo);
                        if (resultadoFinal.getRegistrosProcesados() == 0) {
                            updateMessage("Error procesando " + archivo.getName() + ". Abortando.");
                             break;
                        }
                    }
                    return resultadoFinal;
                }
            };
            ejecutarTareaConDialogo(task, "Carga Inicial Completa");
        }
    }
    
    @FXML
    private void handleEjecutarCosteo(ActionEvent event) {
        Task<Void> costeoTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                serviceFactory.getCostService().ejecutarProcesoDeCosteo();
                return null;
            }
        };
        ejecutarTareaConDialogo(costeoTask, "Proceso de Costeo General");
    }

    @FXML
    private void handleReprocesarNormalizacion(ActionEvent event) {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar Reprocesamiento");
        confirmacion.setHeaderText("Esta acción procesará todos los registros pendientes de la tabla de carga.");
        confirmacion.setContentText("¿Deseas continuar?");

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    serviceFactory.getNormalizarService().ejecutar();
                    return null;
                }
            };
            ejecutarTareaConDialogo(task, "Reprocesamiento de Normalización");
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
        Dialog<Void> dialogoEspera = new Dialog<>();
        dialogoEspera.initOwner(mainPane.getScene().getWindow());
        dialogoEspera.setTitle("Proceso en Curso...");
        dialogoEspera.setHeaderText("Ejecutando " + nombreProceso + ", por favor espera.");
        dialogoEspera.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialogoEspera.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(false);

        dialogoEspera.show();

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            dialogoEspera.close();
            Alert alertExito = new Alert(Alert.AlertType.INFORMATION);
            alertExito.setTitle("Proceso Finalizado");
            alertExito.setContentText(nombreProceso + " se ha completado exitosamente.");
            
            Object resultado = task.getValue();
            if (resultado instanceof ResultadoCargaDto) {
                 alertExito.setHeaderText(((ResultadoCargaDto) resultado).getMensaje());
            }
            alertExito.showAndWait();
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            dialogoEspera.close();
            Throwable ex = task.getException();
            ex.printStackTrace();
            mostrarAlertaError("Ocurrió un error inesperado durante el proceso: " + nombreProceso, ex);
        }));

        new Thread(task).start();
    }

    private void mostrarAlertaError(String header, Throwable e) {
        Alert alertError = new Alert(Alert.AlertType.ERROR);
        alertError.setTitle("Error en el Proceso");
        alertError.setHeaderText(header);
        alertError.setContentText("El error fue: " + e.getMessage());
        alertError.showAndWait();
    }
}