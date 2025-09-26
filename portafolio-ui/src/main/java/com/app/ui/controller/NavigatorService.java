package com.app.ui.controller;

import com.app.facade.ServiceFactory;
import com.app.ui.util.Alertas;
import com.app.ui.util.MainPaneAware;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.ResourceBundle;

/**
 * Servicio centralizado para manejar toda la navegación de la aplicación,
 * incluyendo el flujo de arranque y la carga de vistas en el panel principal.
 */
public class NavigatorService {

    private final ResourceBundle resourceBundle;
    private final ServiceFactory serviceFactory;
    private Window owner;
    private BorderPane mainPane;

    public NavigatorService(ResourceBundle resourceBundle, ServiceFactory serviceFactory) {
        this.resourceBundle = resourceBundle;
        this.serviceFactory = serviceFactory;
    }

    public void setOwner(Window owner) { this.owner = owner; }
    public void setMainPane(BorderPane mainPane) { this.mainPane = mainPane; }

    // --- MÉTODOS ESPECIALES PARA EL ARRANQUE (DEVUELVEN VALOR) ---

    public boolean mostrarVentanaCrearAdmin() {
        try {
            FXMLLoader loader = createLoader("/fxml/CrearAdminView.fxml");
            Parent root = loader.load();
            CrearAdminController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle(resourceBundle.getString("ventana.crear_admin.titulo"));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(owner);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            
            stage.showAndWait(); 
            return controller.isAdminCreado();
        } catch (IOException e) {
            e.printStackTrace();
            Alertas.mostrarAlertaError("Error Crítico", "No se pudo cargar la ventana de creación de administrador.");
            return false;
        }
    }

    public boolean mostrarVentanaLogin() {
        try {
            FXMLLoader loader = createLoader("/fxml/LoginView.fxml");
            Parent root = loader.load();
            LoginController controller = loader.getController();
            
            Stage stage = new Stage();
            stage.setTitle(resourceBundle.getString("ventana.login.titulo"));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(owner);
            stage.setScene(new Scene(root));
            stage.setResizable(false);

            stage.showAndWait(); 
            return controller.isLoginExitoso(); 
        } catch (IOException e) {
            e.printStackTrace();
            Alertas.mostrarAlertaError("Error Crítico", "No se pudo cargar la ventana de login.");
            return false;
        }
    }

    // --- MÉTODOS PARA CARGAR VISTAS EN EL PANEL PRINCIPAL ---

    public void cargarVistaKardex() { cargarVistaEnCentro("/fxml/KardexView.fxml"); }
    public void cargarVistaSaldos() { cargarVistaEnCentro("/fxml/SaldosView.fxml"); }
    public void cargarVistaSaldosMensuales() { cargarVistaEnCentro("/fxml/SaldoMensualView.fxml"); }
    public void cargarVistaOperacionesTrxs() { cargarVistaEnCentro("/fxml/OperacionesTrxsView.fxml"); }
    public void cargarVistaProblemasTrxs() { cargarVistaEnCentro("/fxml/ProblemasTrxsView.fxml"); }
    public void cargarVistaCierreContable() { cargarVistaEnCentro("/fxml/CuadraturaSaldosView.fxml"); }
    public void cargarVistaResumenEmpSaldo() { cargarVistaEnCentro("/fxml/ResumenEmpresaView.fxml"); }
    public void cargarVistaConfrontaSaldo() { cargarVistaEnCentro("/fxml/ConfrontaSaldosView.fxml"); }
    public void cargarVistaResultadosInstrumento() { cargarVistaEnCentro("/fxml/ResultadoInstrumentoView.fxml"); }
    public void cargarVistaResumenHistorico() { cargarVistaEnCentro("/fxml/ResumenHistorico.fxml");}
    

    // --- MÉTODOS PARA MOSTRAR VENTANAS MODALES ---

    public void mostrarVentanaTiposMovimiento() { lanzarVentanaModal("/fxml/TipoMovimientosView.fxml", "ventana.tiposMovimiento.titulo"); }
    public void mostrarVentanaAdminUsuarios() { lanzarVentanaModal("/fxml/AdminUsuariosView.fxml", "ventana.adminUsuarios.titulo"); }
    public void mostrarCrearUsuarios() { lanzarVentanaModal("/fxml/CrearUsuarioView.fxml", "ventana.crearUsuario.titulo"); }
    public void mostrarVistaTransaccionManual() { lanzarVentanaModal("/fxml/TransaccionManualView.fxml", "ventana.transaccion.manual"); }
    public void cargarVistaResumenPortafolio() { cargarVistaEnCentro("/fxml/ResumenPortafolioView.fxml"); }
  
// --- LÓGICA DE NAVEGACIÓN GENÉRICA (PRIVADA) ---

    private void cargarVistaEnCentro(String fxmlPath) {
        try {
            FXMLLoader loader = createLoader(fxmlPath);
            Parent view = loader.load();
            Object controller = loader.getController();
            if (controller instanceof MainPaneAware) {
                ((MainPaneAware) controller).setMainPane(mainPane);
            }
            mainPane.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
            Alertas.mostrarAlertaError("Error de Navegación", "No se pudo cargar la vista: " + fxmlPath);
        }
    }

    private void lanzarVentanaModal(String fxmlPath, String titleKey) { 
        try {
            FXMLLoader loader = createLoader(fxmlPath);
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(resourceBundle.getString(titleKey));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(owner);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            Alertas.mostrarAlertaError("Error de Navegación", "No se pudo mostrar la ventana: " + titleKey);
        }
    }

    private FXMLLoader createLoader(String fxmlPath) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath), resourceBundle);
        loader.setControllerFactory(serviceFactory::getController);
        return loader;
    }
}