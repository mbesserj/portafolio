package com.ui.controller;

import com.serv.service.EmpresaService;
import com.serv.service.InstrumentoService;
import com.serv.service.CustodioService;
import com.serv.service.TransaccionService;
import com.serv.service.TipoMovimientosService;
import com.model.dto.TransaccionManualDto;
import com.model.entities.CustodioEntity;
import com.model.entities.EmpresaEntity;
import com.model.entities.InstrumentoEntity;
import com.model.entities.TipoMovimientoEntity;
import com.ui.util.Alertas;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Function;

public class TransaccionManualController implements Initializable {

    // --- Servicios ---
    private final TransaccionService transaccionService;
    private final TipoMovimientosService tipoMovimientosService;
    private final InstrumentoService instrumentoService;
    private final EmpresaService empresaService;
    private final CustodioService custodioService;

    // --- Contexto ---
    private Long empresaId;
    private Long custodioId;
    private String cuenta;

    // --- FXML Fields ---
    @FXML
    private CheckBox chkCrearNuevo;
    @FXML
    private GridPane paneExistente;
    @FXML
    private ComboBox<InstrumentoEntity> cmbInstrumento;
    @FXML
    private ComboBox<EmpresaEntity> cmbEmpresa;
    @FXML
    private ComboBox<CustodioEntity> cmbCustodio;
    @FXML
    private TextField txtCuenta;
    @FXML
    private GridPane paneNuevo;
    @FXML
    private TextField txtNemoNuevo;
    @FXML
    private TextField txtNombreNuevo;
    @FXML
    private DatePicker dpFecha;
    @FXML
    private ComboBox<TipoMovimientoEntity> cmbTipoMovimiento;
    @FXML
    private TextField txtCantidad;
    @FXML
    private TextField txtPrecio;
    @FXML
    private TextField txtComisiones;
    @FXML
    private TextField txtGastos;
    @FXML
    private TextField txtIva;
    @FXML
    private TextField txtFolio;
    @FXML
    private TextField txtGlosa;
    @FXML
    private TextField txtMoneda;
    @FXML
    private Button btnGuardar;

    private boolean guardadoExitoso = false;

    // Constructor actualizado
    public TransaccionManualController(
            TransaccionService transaccionService,
            TipoMovimientosService tipoMovimientosService,
            InstrumentoService instrumentoService,
            EmpresaService empresaService,
            CustodioService custodioService
    ) {
        this.transaccionService = transaccionService;
        this.tipoMovimientosService = tipoMovimientosService;
        this.instrumentoService = instrumentoService;
        this.empresaService = empresaService;
        this.custodioService = custodioService;
    }

    @FXML
    private void handleCancelar(ActionEvent event) {
        cerrarVentana();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Alternar entre instrumento existente y nuevo
        chkCrearNuevo.selectedProperty().addListener((obs, oldVal, newVal) -> {
            boolean crear = newVal;
            paneExistente.setVisible(!crear);
            paneExistente.setManaged(!crear);
            paneNuevo.setVisible(crear);
            paneNuevo.setManaged(crear);
        });

        // Cargar tipos de movimiento
        List<TipoMovimientoEntity> tipos = tipoMovimientosService.obtenerTodosLosTipos();
        cmbTipoMovimiento.setItems(FXCollections.observableArrayList(tipos));

        // Validaciones numéricas
        validarInputNumerico(txtCantidad);
        validarInputNumerico(txtPrecio);
        validarInputNumerico(txtComisiones);
        validarInputNumerico(txtGastos);
        validarInputNumerico(txtIva);

        // Mostrar Instrumento por nombre
        Function<InstrumentoEntity, String> instrumentoFormatter = InstrumentoEntity::getInstrumentoNombre;
        cmbInstrumento.setCellFactory(param -> createDisplayCell(instrumentoFormatter));
        cmbInstrumento.setButtonCell(createDisplayCell(instrumentoFormatter));

        cmbInstrumento.setItems(FXCollections.observableArrayList(instrumentoService.obtenerTodos()));

        // Mostrar TipoMovimiento por nombre
        Function<TipoMovimientoEntity, String> tipoMovimientoFormatter = TipoMovimientoEntity::getTipoMovimiento;
        cmbTipoMovimiento.setCellFactory(param -> createDisplayCell(tipoMovimientoFormatter));
        cmbTipoMovimiento.setButtonCell(createDisplayCell(tipoMovimientoFormatter));

        // Mostrar Empresa por razón social
        List<EmpresaEntity> empresas = empresaService.obtenerTodas();
        cmbEmpresa.setItems(FXCollections.observableArrayList(empresas));
        Function<EmpresaEntity, String> empresaFormatter = EmpresaEntity::getRazonSocial;
        cmbEmpresa.setCellFactory(param -> createDisplayCell(empresaFormatter));
        cmbEmpresa.setButtonCell(createDisplayCell(empresaFormatter));

        // Mostrar Custodio por nombre
        List<CustodioEntity> custodios = custodioService.obtenerTodos();
        cmbCustodio.setItems(FXCollections.observableArrayList(custodios));
        
        Function<CustodioEntity, String> custodioFormatter = CustodioEntity::getNombreCustodio;
        cmbCustodio.setCellFactory(param -> createDisplayCell(custodioFormatter));
        cmbCustodio.setButtonCell(createDisplayCell(custodioFormatter));
    }

    // initData actualizado para recibir la lista de instrumentos
    public void initData(Long empresaId, Long custodioId, String cuenta, List<InstrumentoEntity> instrumentos) {
        this.empresaId = empresaId;
        this.custodioId = custodioId;
        this.cuenta = cuenta;

        cmbInstrumento.setItems(FXCollections.observableArrayList(instrumentos));

        dpFecha.setValue(LocalDate.now());
        txtMoneda.setText("CLP");
    }

    @FXML
    private void handleGuardar(ActionEvent event) {
        try {
            InstrumentoEntity instrumentoSeleccionado;
            if (chkCrearNuevo.isSelected()) {
                String nemo = txtNemoNuevo.getText();
                String nombre = txtNombreNuevo.getText();
                if (nemo.isBlank() || nombre.isBlank()) {
                    Alertas.mostrarAlertaAdvertencia("Datos Incompletos", "Debe especificar el Nemo y el Nombre del nuevo instrumento.");
                    return;
                }
                instrumentoSeleccionado = instrumentoService.buscarOCrear(nemo, nombre);
            } else {
                instrumentoSeleccionado = cmbInstrumento.getValue();
                if (instrumentoSeleccionado == null) {
                    Alertas.mostrarAlertaAdvertencia("Selección Requerida", "Debe seleccionar un instrumento existente.");
                    return;
                }
            }

            EmpresaEntity empresa = cmbEmpresa.getValue();
            CustodioEntity custodio = cmbCustodio.getValue();
            String cta = txtCuenta.getText();

            if (empresa == null || custodio == null || cta.isBlank()) {
                Alertas.mostrarAlertaAdvertencia("Datos faltantes", "Debe seleccionar empresa, custodio y cuenta.");
                return;
            }

            // Función de ayuda para convertir texto a BigDecimal
            Function<TextField, BigDecimal> toBigDecimal = (tf)
                    -> tf.getText() == null || tf.getText().isBlank() ? BigDecimal.ZERO : new BigDecimal(tf.getText());

            // 1. Se crea el DTO y se llena con los datos del formulario
            TransaccionManualDto dto = new TransaccionManualDto(
                    cmbEmpresa.getValue().getId(),
                    cmbCustodio.getValue().getId(),
                    instrumentoSeleccionado.getId(),
                    cmbTipoMovimiento.getValue().getId(),
                    txtCuenta.getText(),
                    txtFolio.getText(),
                    dpFecha.getValue(),
                    toBigDecimal.apply(txtCantidad),
                    toBigDecimal.apply(txtPrecio),
                    toBigDecimal.apply(txtComisiones),
                    toBigDecimal.apply(txtGastos),
                    toBigDecimal.apply(txtIva),
                    txtGlosa.getText(),
                    txtMoneda.getText()
            );

            // 2. Se llama al servicio con el DTO ya poblado
            transaccionService.crearTransaccionManual(dto);
            guardadoExitoso = true;

            Alertas.mostrarAlertaExito("Éxito", "Transacción guardada correctamente.");
            cerrarVentana();

        } catch (NumberFormatException e) {
            Alertas.mostrarAlertaError("Error de Formato", "Los campos numéricos deben contener números válidos.");
        } catch (Exception e) {
            Alertas.mostrarAlertaError("Error al Guardar", "Ocurrió un error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Métodos de ayuda ---
    private void validarInputNumerico(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                textField.setText(oldValue);
            }
        });
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnGuardar.getScene().getWindow();
        stage.close();
    }

    public boolean isGuardadoExitoso() {
        return guardadoExitoso;
    }

    /**
     * Crea una celda de lista formateada para usar en ComboBox.
     *
     * @param textExtractor Una función que sabe cómo obtener el texto de un
     * objeto.
     * @return Un objeto ListCell configurado.
     */
    private <T> ListCell<T> createDisplayCell(Function<T, String> textExtractor) {
        return new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(textExtractor.apply(item));
                }
            }
        };
    }
}
