package com.ui.controller;

import com.ui.factory.AppFacade;
import com.model.dto.AjustePropuestoDto;
import com.model.dto.KardexReporteDto;
import com.model.entities.InstrumentoEntity;
import com.model.entities.TransaccionEntity;
import com.model.enums.TipoAjuste;
import com.ui.factory.ControllerFactory;
import com.serv.service.ConfrontaService;
import com.serv.service.TipoMovimientosService;
import com.ui.factory.ServiceResult;
import com.ui.util.Alertas;
import com.ui.util.MainPaneAware;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.concurrent.Task;

public class NavigatorService {

    private static final Logger logger = LoggerFactory.getLogger(NavigatorService.class);
    private final ControllerFactory controllerFactory;
    private final ResourceBundle bundle;
    private final Stage primaryStage;
    private BorderPane mainPane;
    private final AppFacade facade;

    public NavigatorService(ControllerFactory controllerFactory, ResourceBundle bundle, Stage primaryStage, AppFacade facade) {
        this.controllerFactory = controllerFactory;
        this.bundle = bundle;
        this.primaryStage = primaryStage;
        this.facade = facade;
        controllerFactory.setNavigatorService(this);
    }

    public void setMainPane(BorderPane mainPane) {
        this.mainPane = mainPane;
    }

    // --- MÉTODOS DE ARRANQUE Y LOGIN (AÑADIDOS) ---
    /**
     * Muestra la ventana modal de login y espera a que el usuario inicie
     * sesión.
     *
     * @return true si el login fue exitoso, false si fue cancelado.
     */
    public boolean mostrarVentanaLogin() {
        try {
            // Usar el bundle que ya tienes en lugar de cargar uno nuevo
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"), bundle);

            LoginController loginController = new LoginController(facade, bundle);
            loader.setController(loginController);

            Parent root = loader.load();

            Stage loginStage = new Stage();
            loginController.setStage(loginStage);

            loginStage.setTitle(bundle.getString("login.titulo"));
            loginStage.setScene(new Scene(root));
            loginStage.initModality(Modality.APPLICATION_MODAL);

            loginStage.showAndWait();

            return loginController.isLoginExitoso();

        } catch (Exception e) {
            logger.error("Error cargando ventana de login", e);
            return false;
        }
    }

    private void mostrarVentanaLoginSinRecursos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Sistema de Portafolio");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            logger.error("Error crítico cargando login", e);
        }
    }

    /**
     * Muestra la ventana modal para crear el primer usuario administrador.
     *
     * @return true si el usuario fue creado, false si se canceló.
     */
    public boolean mostrarVentanaCrearAdmin() {
        try {
            FXMLLoader loader = createLoader("/fxml/CrearAdminView.fxml");
            Parent root = loader.load();
            CrearAdminController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle(bundle.getString("admin.crear.titulo"));
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(primaryStage);
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);

            dialogStage.showAndWait();

            return controller.isCreadoExitosamente();
        } catch (IOException e) {
            logger.error("Error al cargar la ventana de creación de admin", e);
            Alertas.mostrarAlertaError("Error Crítico", "No se pudo cargar la ventana para crear administrador.");
            return false;
        }
    }

    public void mostrarVentanaPrincipal() {
        try {
            FXMLLoader loader = createLoader("/fxml/MainView.fxml");
            Parent root = loader.load();

            Scene scene = new Scene(root);
            primaryStage.setTitle(bundle.getString("app.titulo"));
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            logger.error("Error fatal al cargar la ventana principal", e);
            Alertas.mostrarAlertaError("Error Crítico", "No se pudo iniciar la aplicación.");
        }
    }

    // --- MÉTODOS DE NAVEGACIÓN INTERNA (EXISTENTES) ---
    public void cargarVistaKardex() {
        loadViewIntoMainPane("/fxml/KardexView.fxml");
    }

    public void cargarVistaSaldos() {
        loadViewIntoMainPane("/fxml/SaldosView.fxml");
    }

    public void cargarVistaSaldosMensuales() {
        loadViewIntoMainPane("/fxml/SaldoMensualView.fxml");
    }

    public void cargarVistaResumenEmpSaldo() {
        loadViewIntoMainPane("/fxml/ResumenEmpresaView.fxml");
    }

    public void cargarVistaResultadosInstrumento() {
        loadViewIntoMainPane("/fxml/ResultadoInstrumentoView.fxml");
    }

    public void cargarVistaResumenPortafolio() {
        loadViewIntoMainPane("/fxml/ResumenPortafolioView.fxml");
    }

    public void cargarVistaResumenHistorico() {
        loadViewIntoMainPane("/fxml/ResumenHistorico.fxml");
    }

    public void cargarVistaOperacionesTrxs() {
        loadViewIntoMainPane("/fxml/OperacionesTrxsView.fxml");
    }

    public void cargarVistaProblemasTrxs() {
        loadViewIntoMainPane("/fxml/ProblemasTrxsView.fxml");
    }

    public void cargarVistaCierreContable() {
        loadViewIntoMainPane("/fxml/CuadraturaSaldosView.fxml");
    }

    public void cargarVistaConfrontaSaldo() {
        loadViewIntoMainPaneWithServiceCheck("/fxml/ConfrontaSaldosView.fxml", ConfrontaService.class);
    }

    public void mostrarVentanaTiposMovimiento() {
        showModalWindowWithServiceCheck("/fxml/TipoMovimientosView.fxml", "ventana.tiposMovimiento.titulo", TipoMovimientosService.class);
    }

    public void mostrarVistaTransaccionManual() {
        showModalWindow("/fxml/TransaccionManualView.fxml", "ventana.transaccion.manual");
    }

    // --- MÉTODOS PRIVADOS AUXILIARES (EXISTENTES) ---
    private void loadViewIntoMainPane(String fxmlPath) {
        if (mainPane == null) {
            logger.error("Intento de cargar vista '{}' pero mainPane es nulo.", fxmlPath);
            return;
        }
        try {
            FXMLLoader loader = createLoader(fxmlPath);
            Parent view = loader.load();
            Object controller = loader.getController();
            if (controller instanceof MainPaneAware) {
                ((MainPaneAware) controller).setMainPane(mainPane);
            }
            mainPane.setCenter(view);
        } catch (IOException e) {
            logger.error("Error al cargar FXML: {}", fxmlPath, e);
            Alertas.mostrarAlertaError("Error de Navegación", "No se pudo cargar la vista: " + fxmlPath);
        }
    }

    private void loadViewIntoMainPaneWithServiceCheck(String fxmlPath, Class<?> serviceClass) {
        if (controllerFactory.isServiceAvailable(serviceClass)) {
            loadViewIntoMainPane(fxmlPath);
        } else {
            String serviceName = serviceClass.getSimpleName();
            logger.warn("Navegación bloqueada. Servicio no disponible: {}", serviceName);
            Alertas.mostrarAlertaAdvertencia("Funcionalidad no Disponible", "El servicio '" + serviceName + "' es requerido pero no está disponible.");
        }
    }

    private <T> void showModalWindow(String fxmlPath, String titleKey) {
        try {
            FXMLLoader loader = createLoader(fxmlPath);
            Parent view = loader.load();
            Stage stage = new Stage();
            stage.setTitle(bundle.getString(titleKey));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(primaryStage);
            stage.setScene(new Scene(view));
            stage.showAndWait();
        } catch (IOException e) {
            logger.error("Error al mostrar ventana modal: {}", fxmlPath, e);
        }
    }

    private void showModalWindowWithServiceCheck(String fxmlPath, String titleKey, Class<?> serviceClass) {
        if (controllerFactory.isServiceAvailable(serviceClass)) {
            showModalWindow(fxmlPath, titleKey);
        } else {
            String serviceName = serviceClass.getSimpleName();
            logger.warn("Ventana modal bloqueada. Servicio no disponible: {}", serviceName);
            Alertas.mostrarAlertaAdvertencia("Funcionalidad no Disponible", "El servicio '" + serviceName + "' es requerido pero no está disponible.");
        }
    }

    private FXMLLoader createLoader(String fxmlPath) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath), bundle);
        loader.setControllerFactory(controllerFactory::createController);
        return loader;
    }

    public void mostrarVentanaDetallesKardex(KardexReporteDto kardexDto) {
        try {
            // 1. Crea el cargador de FXML para la vista de detalles.
            //    Asegúrate de que la ruta al FXML sea la correcta en tu proyecto.
            FXMLLoader loader = createLoader("/fxml/KardexDetallesView.fxml");
            Parent view = loader.load();

            // 2. Obtiene la instancia del controlador de la nueva ventana.
            KardexDetallesController controller = loader.getController();

            // 3. Le pasa el objeto DTO seleccionado al controlador para que pueda
            //    poblar los campos de la interfaz de detalles.
            controller.initData(kardexDto);

            // 4. Configura y muestra la nueva ventana (Stage).
            Stage stage = new Stage();
            stage.setTitle(bundle.getString("kardex.detalles.titulo"));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(primaryStage);
            stage.setScene(new Scene(view));
            stage.setResizable(false);

            // Muestra la ventana y espera a que el usuario la cierre.
            stage.showAndWait();

        } catch (IOException e) {
            logger.error("Error al mostrar la ventana modal de detalles de kárdex", e);
            Alertas.mostrarAlertaError("Error de UI", "No se pudo abrir la ventana de detalles.");
        }
    }

    /**
     * Obtiene los detalles completos de una transacción en segundo plano y
     * luego muestra una ventana modal con esa información.
     *
     * @param transaccionId El ID de la transacción a mostrar.
     */
    public void mostrarVentanaDetallesTransaccion(Long transaccionId) {
        // 1. Creamos una Tarea (Task) para hacer el trabajo pesado (la consulta a la BD)
        //    en un hilo separado, para no congelar la interfaz de usuario.
        Task<TransaccionEntity> task = new Task<>() {
            @Override
            protected TransaccionEntity call() throws Exception {
                updateMessage("Cargando detalles de la transacción...");
                // 2. Dentro de la tarea, llamamos a la fachada. Usamos .getData() para
                //    "abrir el paquete". Si hay un error, lanzará una excepción
                //    que será capturada por el manejador setOnFailed.
                return controllerFactory.getFacade().obtenerTransaccionPorId(transaccionId).getData();
            }
        };

        // 3. Definimos qué hacer cuando la tarea termina CON ÉXITO.
        //    Este código se ejecuta automáticamente en el hilo de la UI.
        task.setOnSucceeded(e -> {
            TransaccionEntity transaccionCompleta = task.getValue();
            if (transaccionCompleta != null) {
                try {
                    FXMLLoader loader = createLoader("/fxml/TransaccionDetallesView.fxml");
                    Parent view = loader.load();
                    TransaccionDetallesController controller = loader.getController();
                    controller.initData(transaccionCompleta);

                    Stage stage = new Stage();
                    stage.setTitle("Detalles de Transacción #" + transaccionCompleta.getId());
                    stage.initModality(Modality.APPLICATION_MODAL);
                    stage.initOwner(primaryStage);
                    stage.setScene(new Scene(view));
                    stage.showAndWait();
                } catch (IOException ex) {
                    logger.error("Error de UI", "No se pudo abrir la ventana de detalles.", ex);
                }
            }
        });

        // 4. Definimos qué hacer si la tarea FALLA.
        task.setOnFailed(e -> {
            logger.error("Error de Base de Datos", "No se pudo cargar la transacción completa.", task.getException());
        });

        // 5. Iniciamos la tarea en un nuevo hilo.
        new Thread(task).start();
    }

    /**
     * Abre la ventana modal para crear una transacción manual. Se bloquea hasta
     * que la ventana se cierra y devuelve el resultado.
     *
     * @param empresaId El ID de la empresa seleccionada.
     * @param custodioId El ID del custodio seleccionado.
     * @param cuenta La cuenta seleccionada.
     * @return Un Optional<Boolean> con 'true' si se guardó, 'false' si se
     * canceló, o un Optional vacío si hubo un error al abrir la ventana.
     */
    public Optional<Boolean> mostrarVentanaTransaccionManual(Long empresaId, Long custodioId, String cuenta) {
        try {
            // 1. Antes de abrir la ventana, necesitamos los instrumentos para poblar su ComboBox.
            ServiceResult<List<InstrumentoEntity>> resultado = controllerFactory.getFacade()
                    .obtenerInstrumentosConTransacciones(empresaId, custodioId, cuenta);

            // Si la fachada no pudo obtener los instrumentos, no podemos continuar.
            if (resultado.isError()) {
                Alertas.mostrarAlertaError("Error de Datos", resultado.getMessage());
                return Optional.empty();
            }

            // 2. Cargamos el FXML de la ventana.
            FXMLLoader loader = createLoader("/fxml/TransaccionManualView.fxml");
            Parent view = loader.load();

            // 3. Obtenemos su controlador.
            TransaccionManualController controller = loader.getController();

            // 4. Le pasamos los datos que necesita (incluida la lista de instrumentos).
            controller.initData(empresaId, custodioId, cuenta, resultado.getData());

            // 5. Configuramos y mostramos la ventana.
            Stage stage = new Stage();
            stage.setTitle(bundle.getString("transaccion.manual.titulo"));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(primaryStage);
            stage.setScene(new Scene(view));
            stage.setResizable(false);
            stage.showAndWait();

            // 6. Al cerrarse, le preguntamos al controlador cuál fue el resultado y lo devolvemos.
            return Optional.of(controller.isGuardadoExitoso());

        } catch (IOException e) {
            logger.error("Error al mostrar la ventana de transacción manual", e);
            Alertas.mostrarAlertaError("Error de UI", "No se pudo abrir la ventana.");
            return Optional.empty();
        }
    }

    public Optional<Boolean> mostrarVentanaAjusteManual(Long transaccionId, TipoAjuste tipo) {
        try {
            // 1. Obtenemos la transacción completa desde la fachada.
            ServiceResult<TransaccionEntity> txResultado = controllerFactory.getFacade()
                    .obtenerTransaccionPorId(transaccionId);

            if (txResultado.isError()) {
                Alertas.mostrarAlertaError("Error de Datos", txResultado.getMessage());
                return Optional.empty();
            }

            // 2. Obtenemos la propuesta de ajuste también desde la fachada.
            ServiceResult<AjustePropuestoDto> propuestaResultado = controllerFactory.getFacade()
                    .proponerAjuste(transaccionId, tipo);

            if (propuestaResultado.isError()) {
                Alertas.mostrarAlertaError("Error de Lógica", propuestaResultado.getMessage());
                return Optional.empty();
            }

            // 3. Cargamos el FXML de la ventana.
            FXMLLoader loader = createLoader("/fxml/AjusteManualView.fxml");
            Parent view = loader.load();

            // 4. Obtenemos su controlador.
            AjusteManualController controller = loader.getController();

            // 5. Le pasamos todos los datos que necesita.
            controller.initData(txResultado.getData(), tipo, propuestaResultado.getData());

            // 6. Configuramos y mostramos la ventana.
            Stage stage = new Stage();
            stage.setTitle(bundle.getString("ajuste.manual.titulo"));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(primaryStage);
            stage.setScene(new Scene(view));
            stage.setResizable(false);
            stage.showAndWait(); // La ejecución se detiene aquí.

            // 7. Al cerrarse, le preguntamos al controlador cuál fue el resultado y lo devolvemos.
            return Optional.of(controller.isAprobado());

        } catch (Exception e) {
            logger.error("Error al mostrar la ventana de ajuste manual", e);
            Alertas.mostrarAlertaError("Error de UI", "No se pudo abrir la ventana de ajuste.");
            return Optional.empty();
        }
    }
}
