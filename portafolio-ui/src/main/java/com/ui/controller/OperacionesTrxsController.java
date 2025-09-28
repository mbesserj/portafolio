package com.ui.controller;

import com.model.dto.OperacionesTrxsDto;
import com.model.entities.InstrumentoEntity;
import com.model.enums.TipoAjuste;
import com.model.enums.TipoMovimientoEspecial;
import com.ui.factory.AppFacade;
import com.ui.factory.BaseController;
import com.ui.util.Alertas;
import com.ui.util.MainPaneAware;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ResourceBundle;

import static com.ui.util.FormatUtils.createNumericCellFactory;
import java.util.List;

public class OperacionesTrxsController extends BaseController implements MainPaneAware {

    // --- Componentes FXML ---
    @FXML
    private ComboBox<InstrumentoEntity> cmbNemoNuevo;
    @FXML
    private FiltroGrupo filtroGrupo;
    @FXML
    private Button btnBuscar, btnFusionar, btnCrearAjusteIngreso, btnCrearAjusteEgreso, btnEliminarAjuste, btnRecostearGrupo, btnCrearTrxsManual, btnIgnorarTrx;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private TableView<OperacionesTrxsDto> tablaTransacciones;

    // --- Columnas ---
    @FXML
    private TableColumn<OperacionesTrxsDto, Long> colId;
    @FXML
    private TableColumn<OperacionesTrxsDto, LocalDate> colFecha;
    @FXML
    private TableColumn<OperacionesTrxsDto, String> colFolio;
    @FXML
    private TableColumn<OperacionesTrxsDto, String> colTipo;
    @FXML
    private TableColumn<OperacionesTrxsDto, String> colContable;
    @FXML
    private TableColumn<OperacionesTrxsDto, BigDecimal> colCompras;
    @FXML
    private TableColumn<OperacionesTrxsDto, BigDecimal> colVentas;
    @FXML
    private TableColumn<OperacionesTrxsDto, BigDecimal> colPrecio;
    @FXML
    private TableColumn<OperacionesTrxsDto, BigDecimal> colTotal;
    @FXML
    private TableColumn<OperacionesTrxsDto, Boolean> colCosteado;
    @FXML
    private TableColumn<OperacionesTrxsDto, BigDecimal> colSaldoAcumulado;

    private BorderPane mainPane;
    private NavigatorService navigatorService;

    public OperacionesTrxsController(AppFacade facade, ResourceBundle bundle) {
        super(facade, bundle);
    }

    public void setNavigatorService(NavigatorService navigatorService) {
        this.navigatorService = navigatorService;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        setupFiltroListeners();
        setupButtonBindings();
    }

    private void setupFiltroListeners() {
        facade.obtenerEmpresasConTransacciones().ifSuccess(filtroGrupo::setEmpresas);

        filtroGrupo.empresaProperty().addListener((obs, o, n) -> {
            filtroGrupo.limpiarCustodios();
            if (n != null) {
                facade.obtenerCustodiosConTransacciones(n.getId()).ifSuccess(filtroGrupo::setCustodios);
            }
        });

        filtroGrupo.custodioProperty().addListener((obs, o, n) -> {
            filtroGrupo.limpiarCuentas();
            if (n != null) {
                facade.obtenerCuentasConTransacciones(filtroGrupo.getEmpresaId(), n.getId()).ifSuccess(filtroGrupo::setCuentas);
            }
        });

        filtroGrupo.cuentaProperty().addListener((obs, o, n) -> {
            filtroGrupo.limpiarInstrumentos();
            if (n != null && !n.isBlank()) {
                facade.obtenerInstrumentosConTransacciones(filtroGrupo.getEmpresaId(), filtroGrupo.getCustodioId(), n)
                        .ifSuccess(instrumentos -> {
                            filtroGrupo.setInstrumentos(instrumentos);
                            cmbNemoNuevo.setItems(FXCollections.observableArrayList(instrumentos));
                        });
            }
        });

        filtroGrupo.nemoValueProperty().addListener((obs, o, n) -> {
            if (n != null) {
                handleBuscar();
            } else {
                tablaTransacciones.getItems().clear();
            }
        });
    }

    @FXML
    void handleBuscar() {
        if (!filtroGrupo.isValidSelection()) {
            return;
        }

        Task<List<OperacionesTrxsDto>> task = new Task<>() {
            @Override
            protected List<OperacionesTrxsDto> call() throws Exception {
                Long instrumentoNuevoId = (cmbNemoNuevo.getValue() != null) ? cmbNemoNuevo.getValue().getId() : null;
                return facade.obtenerOperacionesPorGrupo(filtroGrupo.getEmpresaId(), filtroGrupo.getCustodioId(), filtroGrupo.getCuenta(), filtroGrupo.getInstrumentoId(), instrumentoNuevoId).getData();
            }
        };
        progressIndicator.visibleProperty().bind(task.runningProperty());
        task.setOnSucceeded(e -> tablaTransacciones.setItems(FXCollections.observableArrayList(task.getValue())));
        task.setOnFailed(e -> showError("Error de Búsqueda", "No se pudieron cargar las transacciones.", task.getException()));
        new Thread(task).start();
    }

    @FXML
    private void handleCrearTrxsManual() {
        if (navigatorService == null) {
            return;
        }
        navigatorService.mostrarVentanaTransaccionManual(filtroGrupo.getEmpresaId(), filtroGrupo.getCustodioId(), filtroGrupo.getCuenta())
                .ifPresent(guardadoExitoso -> {
                    if (guardadoExitoso) {
                        handleBuscar();
                    }
                });
    }

    @FXML
    void handleFusionar() {
        Long idAntiguo = filtroGrupo.getInstrumentoId();
        InstrumentoEntity nuevo = cmbNemoNuevo.getValue();
        if (idAntiguo == null || nuevo == null || idAntiguo.equals(nuevo.getId())) {
            Alertas.mostrarAlertaAdvertencia("Selección Inválida", "Debe seleccionar dos instrumentos diferentes para fusionar.");
            return;
        }

        Alertas.mostrarConfirmacion("¿Está seguro de fusionar?", String.format("Se moverán todas las transacciones de %s al nuevo instrumento %s. Esta operación es irreversible.", filtroGrupo.nemoValueProperty().get().getInstrumentoNemo(), nuevo.getInstrumentoNemo()))
                .ifPresent(b -> {
                    facade.fusionarInstrumentos(idAntiguo, nuevo.getId())
                            .ifSuccess(v -> {
                                Alertas.mostrarAlertaExito("Fusión Exitosa", "Fusión completada. Se recomienda ejecutar el costeo general.");
                                handleBuscar();
                            })
                            .ifError(errMsg -> showError("Error de Fusión", errMsg));
                });
    }

    @FXML
    private void handleCrearAjusteIngreso() {
        lanzarVentanaAjuste(TipoAjuste.INGRESO);
    }

    @FXML
    private void handleCrearAjusteEgreso() {
        lanzarVentanaAjuste(TipoAjuste.EGRESO);
    }

    @FXML
    private void handleEliminarAjuste() {
        OperacionesTrxsDto selectedItem = tablaTransacciones.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            return;
        }

        TipoMovimientoEspecial tipoEspecial = TipoMovimientoEspecial.fromString(selectedItem.getTipoMovimiento());
        if (tipoEspecial != TipoMovimientoEspecial.OTRO) {
            Alertas.mostrarConfirmacion("¿Está seguro de eliminar este ajuste?", "Esta acción es irreversible...")
                    .ifPresent(b -> {
                        facade.eliminarAjuste(selectedItem.getId())
                                .ifSuccess(v -> {
                                    handleBuscar();
                                    Alertas.mostrarAlertaExito("Movimiento Eliminado", "La transacción ha sido eliminada.");
                                })
                                // La lambda recibe el 'errorMessage' de ifError y se lo pasa a showError
                                // junto con un título genérico.
                                .ifError(errorMessage -> showError("Error al Eliminar", errorMessage));
                    });
        } else {
            Alertas.mostrarAlertaAdvertencia("Transacción Inválida", "No es un tipo de ajuste que se pueda eliminar.");
        }
    }

    @FXML
    private void handleTableDoubleClick(MouseEvent event) {
        if (event.getClickCount() == 2 && navigatorService != null) {
            OperacionesTrxsDto dto = tablaTransacciones.getSelectionModel().getSelectedItem();
            if (dto != null) {
                navigatorService.mostrarVentanaDetallesTransaccion(dto.getId());
            }
        }
    }

    @FXML
    private void handleRecostearGrupo() {
        OperacionesTrxsDto dto = tablaTransacciones.getSelectionModel().getSelectedItem();
        if (dto == null) {
            return;
        }

        Alertas.mostrarConfirmacion("¿Resetear y recostear este grupo?", "Esta acción puede tardar y desmarcará todas las transacciones 'para revisión' del grupo.")
                .ifPresent(b -> {
                    Task<Void> task = new Task<>() {
                        @Override
                        protected Void call() throws Exception {
                            updateMessage("Reseteando y recosteando grupo...");
                            facade.recostearGrupoPorTransaccion(dto.getId()).getData();
                            return null;
                        }
                    };
                    ejecutarTareaConDialogo(task, "Recosteo de Grupo");
                });
    }

    @FXML
    private void handleIgnorarTrx() {
        OperacionesTrxsDto selectedItem = tablaTransacciones.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            return;
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                facade.toggleIgnorarEnCosteo(selectedItem.getId()).getData();
                return null;
            }
        };
        ejecutarTareaConDialogo(task, "Actualizando Transacción");
    }

    private void lanzarVentanaAjuste(TipoAjuste tipo) {
        OperacionesTrxsDto dto = tablaTransacciones.getSelectionModel().getSelectedItem();
        if (dto == null || navigatorService == null) {
            return;
        }
        if (tipo == TipoAjuste.INGRESO && !dto.isParaRevision()) {
            Alertas.mostrarAlertaInfo("Acción No Requerida", "El ajuste de ingreso es para transacciones marcadas para revisión (en rojo).");
            return;
        }

        navigatorService.mostrarVentanaAjusteManual(dto.getId(), tipo)
                .ifPresent(aprobado -> {
                    if (aprobado) {
                        Alertas.mostrarAlertaExito("Éxito", "La transacción de ajuste ha sido creada.");
                        handleBuscar();
                    }
                });
    }

    private void ejecutarTareaConDialogo(Task<?> task, String title) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(mainPane.getScene().getWindow());
        dialog.setTitle(title);

        ProgressIndicator progress = new ProgressIndicator();
        dialog.getDialogPane().setContent(progress);
        progress.progressProperty().bind(task.progressProperty());

        dialog.headerTextProperty().bind(task.messageProperty());
        dialog.show();

        task.setOnSucceeded(e -> dialog.close());
        task.setOnFailed(e -> {
            dialog.close();
            showError("Error en Proceso", "Ocurrió un fallo inesperado.", task.getException());
        });

        new Thread(task).start();
    }

    @Override
    public void setMainPane(BorderPane mainPane) {
        this.mainPane = mainPane;
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colFolio.setCellValueFactory(new PropertyValueFactory<>("folio"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipoMovimiento"));
        colContable.setCellValueFactory(new PropertyValueFactory<>("tipoContable"));
        colCompras.setCellValueFactory(new PropertyValueFactory<>("compras"));
        colVentas.setCellValueFactory(new PropertyValueFactory<>("ventas"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colCosteado.setCellValueFactory(new PropertyValueFactory<>("costeado"));
        colSaldoAcumulado.setCellValueFactory(new PropertyValueFactory<>("saldoAcumulado"));

        colCompras.setCellFactory(createNumericCellFactory("#,##0"));
        colVentas.setCellFactory(createNumericCellFactory("#,##0"));
        colSaldoAcumulado.setCellFactory(createNumericCellFactory("#,##0.00"));
        colPrecio.setCellFactory(createNumericCellFactory("#,##0.00"));
        colTotal.setCellFactory(createNumericCellFactory("#,##0"));

        colCosteado.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item ? "Sí" : "No"));
                TableRow<OperacionesTrxsDto> currentRow = getTableRow();
                if (currentRow != null && !currentRow.isEmpty() && currentRow.getItem() != null) {
                    OperacionesTrxsDto dto = currentRow.getItem();
                    // Limpiar estilos anteriores para evitar conflictos
                    currentRow.getStyleClass().removeAll("fila-ignorada", "fila-revision");

                    if (dto.isIgnorarEnCosteo()) {
                        currentRow.getStyleClass().add("fila-ignorada");
                    } else if (dto.isParaRevision()) {
                        currentRow.getStyleClass().add("fila-revision");
                    }
                }
            }
        });
    }

    private void setupButtonBindings() {
        BooleanBinding buscarInvalido = filtroGrupo.validSelectionProperty().not();
        btnBuscar.disableProperty().bind(buscarInvalido.or(progressIndicator.visibleProperty()));

        BooleanBinding fusionInvalido = filtroGrupo.validSelectionProperty().not()
                .or(cmbNemoNuevo.getSelectionModel().selectedItemProperty().isNull())
                .or(Bindings.createBooleanBinding(() -> {
                    Long id1 = filtroGrupo.getInstrumentoId();
                    InstrumentoEntity nemo2 = cmbNemoNuevo.getValue();
                    return nemo2 != null && nemo2.getId().equals(id1);
                }, filtroGrupo.nemoValueProperty(), cmbNemoNuevo.valueProperty()));
        btnFusionar.disableProperty().bind(fusionInvalido.or(progressIndicator.visibleProperty()));

        BooleanBinding noHaySeleccion = tablaTransacciones.getSelectionModel().selectedItemProperty().isNull();
        btnRecostearGrupo.disableProperty().bind(noHaySeleccion.or(progressIndicator.visibleProperty()));
        btnEliminarAjuste.disableProperty().bind(noHaySeleccion.or(progressIndicator.visibleProperty()));
        btnCrearAjusteEgreso.disableProperty().bind(noHaySeleccion.or(progressIndicator.visibleProperty()));
        btnIgnorarTrx.disableProperty().bind(noHaySeleccion.or(progressIndicator.visibleProperty()));

        BooleanBinding noNecesitaAjusteIngreso = noHaySeleccion.or(Bindings.createBooleanBinding(() -> {
            OperacionesTrxsDto dto = tablaTransacciones.getSelectionModel().getSelectedItem();
            return dto == null || !dto.isParaRevision();
        }, tablaTransacciones.getSelectionModel().selectedItemProperty()));
        btnCrearAjusteIngreso.disableProperty().bind(noNecesitaAjusteIngreso.or(progressIndicator.visibleProperty()));

        BooleanBinding grupoInvalido = filtroGrupo.empresaProperty().isNull()
                .or(filtroGrupo.custodioProperty().isNull())
                .or(filtroGrupo.cuentaProperty().isNull());
        btnCrearTrxsManual.disableProperty().bind(grupoInvalido.or(progressIndicator.visibleProperty()));
    }
}
