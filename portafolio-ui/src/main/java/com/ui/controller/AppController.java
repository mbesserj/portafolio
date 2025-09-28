package com.ui.controller;

import com.model.dto.ResultadoCargaDto;
import com.model.enums.ListaEnumsCustodios;
import com.ui.factory.AppFacade;
import com.ui.factory.BaseController;
import com.ui.factory.ServiceResult;
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

public class AppController extends BaseController {

    @FXML
    private BorderPane mainPane;

    private final NavigatorService navigatorService;

    public AppController(AppFacade appFacade, NavigatorService navigatorService, ResourceBundle resourceBundle) {
        super(appFacade, resourceBundle);
        this.navigatorService = navigatorService;
        logger.info("AppController inicializado con sus dependencias.");
    }

    @FXML
    public void initialize() {
        if (navigatorService != null) {
            navigatorService.setMainPane(mainPane);
        }
        onInitialize();
    }

    protected void onInitialize() {
        // Carga la vista inicial por defecto (ej. Kardex) en lugar de la ventana principal.
        logger.info("AppController inicializado. Cargando vista por defecto (Kardex)...");
        if (navigatorService != null) {
            navigatorService.cargarVistaKardex();
        }
    }

    // --- MANEJADORES DE NAVEGACIÓN ---
    @FXML private void handleMostrarKardex() { navigatorService.cargarVistaKardex(); }
    @FXML private void handleMostrarSaldos() { navigatorService.cargarVistaSaldos(); }
    @FXML private void handleMostrarSaldosMensuales() { navigatorService.cargarVistaSaldosMensuales(); }
    @FXML private void handleMostrarResumenSaldos() { navigatorService.cargarVistaResumenEmpSaldo(); }
    @FXML private void handleMostrarConfrontaSaldos() { navigatorService.cargarVistaConfrontaSaldo(); }
    @FXML private void handleMostrarResultadosInstrumento() { navigatorService.cargarVistaResultadosInstrumento(); }
    @FXML private void handleMostrarResumenPortafolio() { navigatorService.cargarVistaResumenPortafolio(); }
    @FXML private void handleMostrarResumenHistorico() { navigatorService.cargarVistaResumenHistorico(); }
    @FXML private void handleMostrarTransacciones() { navigatorService.cargarVistaOperacionesTrxs(); }
    @FXML private void handleMostrarTrxsProblemas() { navigatorService.cargarVistaProblemasTrxs(); }
    @FXML private void handleMostrarTiposMovimiento() { navigatorService.mostrarVentanaTiposMovimiento(); }
    @FXML private void handleTransaccionManual() { navigatorService.mostrarVistaTransaccionManual(); }
    @FXML private void handleMostrarCierreContable() { navigatorService.cargarVistaCierreContable(); }
    @FXML private void handleSalir(ActionEvent event) { Platform.exit(); }

    // --- MANEJADORES DE PROCESOS ---
    @FXML
    private void handleCargarArchivos(ActionEvent event) {
        Optional<ListaEnumsCustodios> custodioOpt = pedirCustodio("Selecciona el custodio para la carga.");
        if (custodioOpt.isEmpty()) { return; }

        List<File> archivos = pedirArchivosExcel("Selecciona uno o más archivos para cargar");
        if (archivos == null || archivos.isEmpty()) { return; }

        Task<ServiceResult<ResultadoCargaDto>> task = new Task<>() {
            @Override
            protected ServiceResult<ResultadoCargaDto> call() {
                ServiceResult<ResultadoCargaDto> resultadoFinal = null;
                for (File archivo : archivos) {
                    updateMessage("Procesando: " + archivo.getName());
                    resultadoFinal = facade.ejecutarCargaDiaria(custodioOpt.get(), archivo);
                    if (resultadoFinal.isError()) {
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
        Optional<ListaEnumsCustodios> custodioOpt = pedirCustodio("Selecciona custodio para carga inicial.");
        if (custodioOpt.isEmpty()) { return; }

        List<File> archivos = pedirArchivosExcel("Selecciona archivos para carga inicial");
        if (archivos == null || archivos.isEmpty()) { return; }

        if (confirmarProcesoDestructivo()) {
            Task<ServiceResult<ResultadoCargaDto>> task = new Task<>() {
                @Override
                protected ServiceResult<ResultadoCargaDto> call() {
                    ServiceResult<ResultadoCargaDto> resultadoFinal = null;
                    for (File archivo : archivos) {
                        updateMessage("Procesando: " + archivo.getName());
                        resultadoFinal = facade.ejecutarCargaInicial(custodioOpt.get(), archivo);
                        if (resultadoFinal.isError()) {
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
        Task<ServiceResult<Void>> costeoTask = new Task<>() {
            @Override
            protected ServiceResult<Void> call() {
                updateMessage("Ejecutando proceso de costeo...");
                return facade.iniciarCosteoCompleto();
            }
        };
        ejecutarTareaConDialogo(costeoTask, "Proceso de Costeo General");
    }

    @FXML
    private void handleReprocesarNormalizacion(ActionEvent event) {
        if (confirmarReproceso()) {
            Task<ServiceResult<ResultadoCargaDto>> task = new Task<>() {
                @Override
                protected ServiceResult<ResultadoCargaDto> call() {
                    updateMessage("Reprocesando normalización...");
                    return facade.reprocesarNormalizacion();
                }
            };
            ejecutarTareaConDialogo(task, "Reprocesamiento de Normalización");
        }
    }

    // --- MÉTODOS DE AYUDA (HELPERS) ---
    private boolean confirmarProcesoDestructivo() {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar Proceso Irreversible");
        confirmacion.setHeaderText("¡ATENCIÓN! ESTA ACCIÓN BORRARÁ TODOS LOS DATOS EXISTENTES.");
        confirmacion.setContentText("Se borrarán todas las transacciones, saldos y kárdex.\n\n¿Estás seguro?");
        return confirmacion.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private boolean confirmarReproceso() {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION, "¿Deseas continuar?", ButtonType.YES, ButtonType.NO);
        confirmacion.setTitle("Confirmar Reprocesamiento");
        confirmacion.setHeaderText("Esto procesará todos los registros pendientes de la tabla de carga.");
        return confirmacion.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }

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
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Archivos Excel (*.xlsx, *.xls)", "*.xlsx", "*.xls");
        fileChooser.getExtensionFilters().add(extFilter);
        return fileChooser.showOpenMultipleDialog(mainPane.getScene().getWindow());
    }
    
    private void ejecutarTareaConDialogo(Task<? extends ServiceResult<?>> task, String nombreProceso) {
        Dialog<Void> dialogoEspera = new Dialog<>();
        dialogoEspera.initOwner(mainPane.getScene().getWindow());
        dialogoEspera.setTitle("Proceso en Curso...");
        dialogoEspera.setHeaderText("Ejecutando " + nombreProceso + ", por favor espera.");
        dialogoEspera.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialogoEspera.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(false);
        dialogoEspera.show();

        task.messageProperty().addListener((obs, oldMsg, newMsg) -> dialogoEspera.setHeaderText(newMsg));

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            dialogoEspera.close();
            ServiceResult<?> resultado = task.getValue();

            resultado.ifSuccess(data -> {
                String mensaje = (data instanceof ResultadoCargaDto)
                        ? ((ResultadoCargaDto) data).getMensaje()
                        : nombreProceso + " completado.";
                showSuccess(mensaje);
            }).ifError(errMsg -> showError("Error en Proceso", errMsg));
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            dialogoEspera.close();
            showError("Error en Proceso", "Ocurrió un fallo inesperado.", task.getException());
        }));

        new Thread(task).start();
    }
}