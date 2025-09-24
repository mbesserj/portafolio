package com.app.ui.controller;

import com.app.ui.util.MainPaneAware;
import com.app.dto.AjustePropuestoDto;
import com.app.dto.OperacionesTrxsDto;
import com.app.entities.InstrumentoEntity;
import com.app.entities.TransaccionEntity;
import com.app.enums.TipoAjuste;
import com.app.enums.TipoMovimientoEspecial;
import com.app.service.CustodioService;
import com.app.service.EmpresaService;
import com.app.service.FiltroService;
import com.app.service.FusionInstrumentoService;
import com.app.service.InstrumentoService;
import com.app.service.OperacionesTrxsService;
import com.app.service.TransaccionService;
import com.app.ui.util.Alertas;
import com.app.ui.util.ReportTask;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static com.app.ui.util.FormatUtils.createComboBoxCellFactory;
import static com.app.ui.util.FormatUtils.createNumericCellFactory;
import com.costing.api.CostingApi;
import java.util.ResourceBundle;
import javafx.util.Callback;

public class OperacionesTrxsController implements MainPaneAware {

    private BorderPane mainPane;
    // --- Servicios ---
    private final OperacionesTrxsService operacionesService;
    private final EmpresaService empresaService;
    private final CustodioService custodioService;
    private final FusionInstrumentoService instrumentoAdminService;
    private final InstrumentoService instrumentoService;
    private final TransaccionService transaccionService;
    private final CostingApi costoService;
    private final FiltroService filtroService;
    private final ServiceFactory serviceFactory;
    private final Callback<Class<?>, Object> controllerFactory;
    private final ResourceBundle resourceBundle;

    // --- Componentes FXML ---
    @FXML
    private ComboBox<InstrumentoEntity> cmbNemoNuevo;
    @FXML
    private FiltroGrupo filtroGrupo;
    @FXML
    private Button btnBuscar;
    @FXML
    private Button btnFusionar;
    @FXML
    private Button btnCrearAjusteIngreso;
    @FXML
    private Button btnCrearAjusteEgreso;
    @FXML
    private Button btnEliminarAjuste;
    @FXML
    private Button btnRecostearGrupo;
    @FXML
    private Button btnCrearTrxsManual;
    @FXML
    private Button btnIgnorarTrx;

    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private TableView<OperacionesTrxsDto> tablaTransacciones;
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

    public OperacionesTrxsController(
            OperacionesTrxsService operacionesService,
            ServiceFactory serviceFactory,
            FiltroService filtroService,
            Callback<Class<?>, Object> controllerFactory,
            ResourceBundle resourceBundle) {

        this.operacionesService = operacionesService;
        this.serviceFactory = serviceFactory;
        this.empresaService = serviceFactory.getEmpresaService();
        this.custodioService = serviceFactory.getCustodioService();
        this.instrumentoService = serviceFactory.getInstrumentoService();
        this.instrumentoAdminService = serviceFactory.getFusionService();
        this.transaccionService = serviceFactory.getTransaccionService();
        this.costoService = serviceFactory.getCosteoService();
        this.filtroService = filtroService;
        this.controllerFactory = controllerFactory;
        this.resourceBundle = resourceBundle;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        filtroGrupo.initializeComponent(
                serviceFactory.getEmpresaService(),
                serviceFactory.getFiltroService()
        );

        cmbNemoNuevo.setCellFactory(createComboBoxCellFactory(
                instrumento -> instrumento.getInstrumentoNemo() + " - " + instrumento.getInstrumentoNombre()
        ));
        cmbNemoNuevo.setButtonCell(createComboBoxCellFactory(InstrumentoEntity::getInstrumentoNemo).call(null));

        // Listener 1: Reacciona a los filtros base para poblar el combo de nemo "destino"
        filtroGrupo.validSelectionProperty().addListener((obs, oldVal, newVal) -> {
            actualizarComboNemoNuevo();
        });

        // Listener 2: Reacciona a cada cambio del instrumento principal para buscar
        filtroGrupo.nemoValueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                handleBuscar();
            } else {
                tablaTransacciones.getItems().clear();
            }
        });

        setupButtonBindings();
    }

    private void actualizarComboNemoNuevo() {
        cmbNemoNuevo.getItems().clear();
        Long empresaId = filtroGrupo.getEmpresaId();
        Long custodioId = filtroGrupo.getCustodioId();
        String cuenta = filtroGrupo.getCuenta();

        if (empresaId != null && custodioId != null && cuenta != null) {
            List<InstrumentoEntity> instrumentos = filtroService.obtenerInstrumentosConTransacciones(empresaId, custodioId, cuenta);
            instrumentos.add(0, null);
            cmbNemoNuevo.setItems(FXCollections.observableArrayList(instrumentos));
        }
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

                    // LÓGICA ACTUALIZADA
                    if (dto.isIgnorarEnCosteo()) {
                        // Prioridad 1: Si está ignorada, la mostramos gris y tachada
                        currentRow.setStyle("-fx-background-color: #e0e0e0; -fx-strikethrough: true;");
                    } else if (dto.isParaRevision()) {
                        // Prioridad 2: Si es para revisión, la mostramos en rojo
                        currentRow.setStyle("-fx-background-color: #ffcccc;");
                    } else {
                        // Si no es ninguna de las anteriores, estilo por defecto
                        currentRow.setStyle("");
                    }
                } else if (currentRow != null) {
                    currentRow.setStyle("");
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
                }, filtroGrupo.validSelectionProperty(), cmbNemoNuevo.valueProperty()));
        btnFusionar.disableProperty().bind(fusionInvalido.or(progressIndicator.visibleProperty()));

        BooleanBinding noHaySeleccion = tablaTransacciones.getSelectionModel().selectedItemProperty().isNull();
        btnRecostearGrupo.disableProperty().bind(noHaySeleccion.or(progressIndicator.visibleProperty()));
        btnEliminarAjuste.disableProperty().bind(noHaySeleccion.or(progressIndicator.visibleProperty()));
        btnCrearAjusteEgreso.disableProperty().bind(noHaySeleccion.or(progressIndicator.visibleProperty()));

        BooleanBinding noNecesitaAjusteIngreso = noHaySeleccion.or(Bindings.createBooleanBinding(() -> {
            OperacionesTrxsDto dto = tablaTransacciones.getSelectionModel().getSelectedItem();
            return dto == null || !dto.isParaRevision();
        }, tablaTransacciones.getSelectionModel().selectedItemProperty()));
        btnCrearAjusteIngreso.disableProperty().bind(noNecesitaAjusteIngreso.or(progressIndicator.visibleProperty()));

        BooleanBinding grupoInvalido = filtroGrupo.empresaProperty().isNull()
                .or(filtroGrupo.custodioProperty().isNull())
                .or(filtroGrupo.cuentaProperty().isNull());
        btnCrearTrxsManual.disableProperty().bind(grupoInvalido.or(progressIndicator.visibleProperty()));

        btnIgnorarTrx.disableProperty().bind(noHaySeleccion.or(progressIndicator.visibleProperty()));

    }

    /**
     * Handler para el botón "Crear Saldo Inicial (Manual)". Orquesta el proceso
     * de calcular la cantidad y pedir el costo al usuario.
     */
    @FXML
    private void handleCrearTrxsManual() {
        // 1. Validamos que tengamos el contexto mínimo (empresa, custodio, cuenta)
        if (filtroGrupo.empresaProperty().isNull().get()
                || filtroGrupo.custodioProperty().isNull().get()
                || filtroGrupo.cuentaProperty().isNull().get()) {
            Alertas.mostrarAlertaAdvertencia("Contexto Requerido",
                    "Por favor, seleccione una empresa, custodio y cuenta.");
            return;
        }

        final Long empresaId = filtroGrupo.getEmpresaId();
        final Long custodioId = filtroGrupo.getCustodioId();
        final String cuenta = filtroGrupo.getCuenta();

        try {
            // Obtenemos la LISTA de instrumentos disponibles para el contexto seleccionado
            List<InstrumentoEntity> instrumentosDisponibles = filtroService.obtenerInstrumentosConTransacciones(empresaId, custodioId, cuenta);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TransaccionManualView.fxml"), this.resourceBundle);
            loader.setControllerFactory(this.controllerFactory);
            Parent root = loader.load();

            TransaccionManualController controller = loader.getController();

            // Y aquí le pasamos la LISTA, no un ID.
            controller.initData(empresaId, custodioId, cuenta, instrumentosDisponibles);

            // --- El resto del código para mostrar la ventana se mantiene igual ---
            Stage stage = new Stage();
            stage.setTitle("Crear Transacción Manual");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(mainPane.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.showAndWait();

            if (controller.isGuardadoExitoso()) {
                handleBuscar();
            }

        } catch (IOException e) {
            e.printStackTrace();
            Alertas.mostrarAlertaError("Error de UI", "No se pudo cargar la ventana para crear transacciones manuales.");
        }
    }

    @FXML
    void handleBuscar() {
        tablaTransacciones.getItems().clear();

        Long empresaId = filtroGrupo.getEmpresaId();
        Long custodioId = filtroGrupo.getCustodioId();
        String cuenta = filtroGrupo.getCuenta();
        Long instrumentoPrincipalId = filtroGrupo.getInstrumentoId();

        if (instrumentoPrincipalId == null) {
            return;
        }

        String nemoPrincipal = instrumentoService.obtenerPorId(instrumentoPrincipalId).getInstrumentoNemo();
        InstrumentoEntity nemoNuevo = cmbNemoNuevo.getValue();

        List<String> nemosSeleccionados = new ArrayList<>();
        nemosSeleccionados.add(nemoPrincipal);

        if (nemoNuevo != null && !nemoNuevo.getInstrumentoNemo().equals(nemoPrincipal)) {
            nemosSeleccionados.add(nemoNuevo.getInstrumentoNemo());
        }

        String razonSocial = empresaService.obtenerPorId(empresaId).getRazonSocial();
        String nombreCustodio = custodioService.obtenerPorId(custodioId).getNombreCustodio();

        Supplier<List<OperacionesTrxsDto>> servicio = () -> operacionesService.obtenerTransaccionesPorGrupo(
                razonSocial, nombreCustodio, cuenta, nemosSeleccionados);
        ReportTask<OperacionesTrxsDto> task = new ReportTask<>(tablaTransacciones, progressIndicator, null, servicio);
        new Thread(task).start();
    }

    @FXML
    void handleFusionar() {
        InstrumentoEntity antiguo = instrumentoService.obtenerPorId(filtroGrupo.getInstrumentoId());
        InstrumentoEntity nuevo = cmbNemoNuevo.getValue();

        if (antiguo == null || nuevo == null || antiguo.getId().equals(nuevo.getId())) {
            Alertas.mostrarAlertaAdvertencia("Selección Inválida", "Debe seleccionar dos instrumentos diferentes para fusionar.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar Fusión");
        confirmacion.setHeaderText("¡Operación Destructiva!");
        String mensaje = String.format("¿Está seguro de fusionar '%s' en '%s'?\n\n"
                + "Todo el historial de '%s' será reasignado y el instrumento será ELIMINADO PERMANENTEMENTE.\n\n"
                + "Esta acción no se puede deshacer.",
                antiguo.getInstrumentoNemo(), nuevo.getInstrumentoNemo(), antiguo.getInstrumentoNemo());
        confirmacion.setContentText(mensaje);

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            ejecutarTareaDeFusion(antiguo.getId(), nuevo.getId());
        }
    }

    @FXML
    private void handleCrearAjusteIngreso() {
        lanzarVentanaAjuste(TipoAjuste.INGRESO);
    }

    @FXML
    private void handleCrearAjusteEgreso() {
        lanzarVentanaAjuste(TipoAjuste.EGRESO);
    }

    private void lanzarVentanaAjuste(TipoAjuste tipo) {
        OperacionesTrxsDto dtoSeleccionado = tablaTransacciones.getSelectionModel().getSelectedItem();
        if (dtoSeleccionado == null) {
            Alertas.mostrarAlertaInfo("Acción Requerida", "Por favor, seleccione una transacción de referencia.");
            return;
        }

        if (tipo == TipoAjuste.INGRESO && !dtoSeleccionado.isParaRevision()) {
            Alertas.mostrarAlertaInfo("Acción No Requerida", "El ajuste de ingreso es solo para transacciones marcadas para revisión (en rojo).");
            return;
        }

        TransaccionEntity txSeleccionada = transaccionService.obtenerTransaccionPorId(dtoSeleccionado.getId());
        if (txSeleccionada == null) {
            Alertas.mostrarAlertaError("Error", "No se pudo encontrar la transacción completa con ID: " + dtoSeleccionado.getId());
            return;
        }
        try {

            AjustePropuestoDto propuesta = costoService.proponerAjusteManual(txSeleccionada, tipo);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjusteManualView.fxml"), this.resourceBundle);
            Parent root = loader.load();
            AjusteManualController controller = loader.getController();
            controller.initData(txSeleccionada, tipo, propuesta);

            Stage stage = new Stage();
            stage.setTitle("Crear Ajuste Manual");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(mainPane.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.showAndWait();

            if (controller.isAprobado()) {
                BigDecimal cantidadFinal = controller.getCantidadFinal();
                BigDecimal precioFinal = controller.getPrecioFinal();
                costoService.crearAjusteManual(txSeleccionada, tipo, cantidadFinal, precioFinal);
                Alertas.mostrarAlertaExito("Éxito", "La transacción de ajuste ha sido creada.");
                handleBuscar();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Alertas.mostrarAlertaError("Error de UI", "No se pudo cargar la ventana de ajuste.");
        }
    }

    @FXML
    private void handleEliminarAjuste() {
        OperacionesTrxsDto selectedItem = tablaTransacciones.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            Alertas.mostrarAlertaAdvertencia("Selección Requerida", "Por favor, seleccione una transacción para eliminar.");
            return;
        }

        // 1. Convertimos el String del DTO a nuestro Enum para trabajar de forma segura.
        TipoMovimientoEspecial tipoEspecial = TipoMovimientoEspecial.fromString(selectedItem.getTipoMovimiento());

        // 2. La validación ahora es mucho más limpia y robusta.
        // Si el tipo no es uno de los especiales, será 'OTRO' y la condición fallará.
        if (tipoEspecial != TipoMovimientoEspecial.OTRO) {

            try {
                Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
                confirmacion.setTitle("Confirmar Eliminación");
                confirmacion.setHeaderText("¿Está seguro de que desea eliminar este movimiento?");

                // 3. Usamos el Enum para personalizar el mensaje de forma segura.
                if (tipoEspecial == TipoMovimientoEspecial.SALDO_INICIAL) {
                    confirmacion.setContentText("¡CUIDADO! Eliminar un Saldo Inicial reseteará TODO el historial de costeo para este grupo.");
                } else {
                    confirmacion.setContentText("Esta acción es irreversible y podría requerir un recosteo del grupo.");
                }

                Optional<ButtonType> resultado = confirmacion.showAndWait();
                if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
                    costoService.eliminarAjusteManual(selectedItem.getId());
                    handleBuscar();
                    Alertas.mostrarAlertaExito("Movimiento Eliminado", "La transacción ha sido eliminada con éxito.");
                }
            } catch (Exception e) {
                Alertas.mostrarAlertaError("Error al Eliminar", e.getMessage());
            }
        } else {
            Alertas.mostrarAlertaAdvertencia("Transacción Inválida", "La transacción seleccionada no es un tipo de ajuste o saldo que se pueda eliminar.");
        }
    }

    private void ejecutarTareaDeFusion(Long idAntiguo, Long idNuevo) {
        Task<Void> fusionTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Fusionando instrumentos...");
                instrumentoAdminService.fusionarYPrepararRecosteo(idAntiguo, idNuevo);
                return null;
            }
        };
        fusionTask.setOnSucceeded(e -> {
            Alertas.mostrarAlertaExito("Fusión Exitosa", "Fusión completada. Se recomienda ejecutar el costeo general.");
        });
        fusionTask.setOnFailed(e -> {
            Alertas.mostrarAlertaError("Fallo en Fusión", "La fusión falló: " + fusionTask.getException().getMessage());
            fusionTask.getException().printStackTrace();
        });
        progressIndicator.visibleProperty().bind(fusionTask.runningProperty());
        new Thread(fusionTask).start();
    }

    @FXML
    private void handleTableDoubleClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            OperacionesTrxsDto dtoSeleccionado = tablaTransacciones.getSelectionModel().getSelectedItem();
            if (dtoSeleccionado != null) {
                Task<TransaccionEntity> task = new Task<>() {
                    @Override
                    protected TransaccionEntity call() throws Exception {
                        return transaccionService.obtenerTransaccionPorId(dtoSeleccionado.getId());
                    }
                };
                task.setOnSucceeded(e -> {
                    TransaccionEntity transaccionCompleta = task.getValue();
                    if (transaccionCompleta != null) {
                        try {
                            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/TransaccionDetallesView.fxml"), this.resourceBundle);
                            Parent view = fxmlLoader.load();
                            TransaccionDetallesController controller = fxmlLoader.getController();
                            controller.initData(transaccionCompleta);
                            Scene scene = new Scene(view);
                            Stage stage = new Stage();
                            stage.setTitle("Detalles de Transacción #" + transaccionCompleta.getId());
                            stage.setScene(scene);
                            stage.initModality(Modality.APPLICATION_MODAL);
                            stage.initOwner(mainPane.getScene().getWindow());
                            stage.showAndWait();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            Alertas.mostrarAlertaError("Error de UI", "No se pudo abrir la ventana de detalles.");
                        }
                    }
                });
                task.setOnFailed(e -> {
                    task.getException().printStackTrace();
                    Alertas.mostrarAlertaError("Error de Base de Datos", "No se pudo cargar la transacción completa.");
                });
                new Thread(task).start();
            }
        }
    }

    @FXML
    private void handleRecostearGrupo() {
        TransaccionEntity txSeleccionada = obtenerTransaccionDesdeSeleccion();
        if (txSeleccionada == null) {
            Alertas.mostrarAlertaAdvertencia("Selección Requerida", "Por favor, seleccione una transacción para identificar el grupo a recostear.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar Proceso");
        confirmacion.setHeaderText("¿Está seguro de que desea resetear y recostear este grupo?");
        confirmacion.setContentText("Esta acción desmarcará todas las transacciones 'para revisión' de este grupo ("
                + txSeleccionada.getInstrumento().getInstrumentoNemo()
                + ") y volverá a ejecutar el costeo desde el inicio.");

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            ejecutarTareaDeRecosteo(txSeleccionada);
        }
    }

    @FXML
    private void handleIgnorarTrx() {
        OperacionesTrxsDto selectedItem = tablaTransacciones.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            Alertas.mostrarAlertaAdvertencia("Selección Requerida", "Por favor, seleccione la transacción que desea marcar.");
            return;
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Actualizando estado de la transacción...");
                // Llamamos al nuevo método del servicio
                transaccionService.toggleIgnorarEnCosteo(selectedItem.getId());
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            Alertas.mostrarAlertaExito("Éxito", "El estado de la transacción ha sido actualizado.");
            // Recargamos la tabla para ver los cambios visuales
            handleBuscar();
        });

        task.setOnFailed(e -> {
            Alertas.mostrarAlertaError("Error", "No se pudo actualizar la transacción: " + task.getException().getMessage());
            task.getException().printStackTrace();
        });

        progressIndicator.visibleProperty().bind(task.runningProperty());
        new Thread(task).start();
    }

    private void ejecutarTareaDeRecosteo(TransaccionEntity tx) {
        String claveAgrupacion = tx.getEmpresa().getId() + "|" + tx.getCuenta() + "|" + tx.getCustodio().getId() + "|" + tx.getInstrumento().getId();
        Task<Void> recosteoTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Reseteando y recosteando grupo...");
                costoService.recostearGrupo(claveAgrupacion);
                return null;
            }
        };
        recosteoTask.setOnSucceeded(e -> {
            Alertas.mostrarAlertaExito("Proceso Exitoso", "El grupo ha sido reseteado y recosteado con éxito.");
            handleBuscar();
        });
        recosteoTask.setOnFailed(e -> {
            Alertas.mostrarAlertaError("Fallo en el Proceso", "Ocurrió un error al recostear el grupo: " + recosteoTask.getException().getMessage());
            recosteoTask.getException().printStackTrace();
        });
        progressIndicator.visibleProperty().bind(recosteoTask.runningProperty());
        new Thread(recosteoTask).start();
    }

    private TransaccionEntity obtenerTransaccionDesdeSeleccion() {
        OperacionesTrxsDto dto = tablaTransacciones.getSelectionModel().getSelectedItem();
        if (dto == null) {
            return null;
        }
        return transaccionService.obtenerTransaccionPorId(dto.getId());
    }

    @FXML
    private void handleCerrar() {
        if (mainPane != null) {
            mainPane.setCenter(null);
        }
    }

    @Override
    public void setMainPane(BorderPane mainPane) {
        this.mainPane = mainPane;
    }
}
