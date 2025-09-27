package com.app.ui.App;

import com.app.ui.factory.AppFacade;
import com.app.ui.factory.ControllerFactory;
import com.app.ui.controller.NavigatorService;
import com.app.factory.ServiceContainer;
import com.costing.api.KardexApiFactory; 
import com.costing.api.SaldoApiFactory;
import com.costing.api.CostingApiFactory;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.ResourceBundle;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 1. Iniciar el Contenedor de Servicios
        ServiceContainer container = ServiceContainer.getInstance();

        // 2. Registrar las APIs Externas (paso crucial)
        // Aquí conectas las implementaciones reales de tus librerías externas.
        container.configureExternalApis()
                .withKardexApi(KardexApiFactory.createService())
                .withSaldoApi(SaldoApiFactory.createService())
                .withCostingApi(CostingApiFactory.createService())
                .register();

        // 3. Obtener la Fachada Principal (AppFacade)
        // AppFacade debe estar registrado como un servicio en tu ServiceContainer
        AppFacade appFacade = container.getService(AppFacade.class);

        // 4. Configurar la Internacionalización
        ResourceBundle bundle = ResourceBundle.getBundle("com.app.bundles.messages", new Locale("es", "CL"));

        // 5. Crear los componentes de la UI, inyectando dependencias
        ControllerFactory controllerFactory = new ControllerFactory(appFacade, bundle);
        NavigatorService navigatorService = new NavigatorService(controllerFactory, bundle, primaryStage);

        // 6. Ejecutar la Lógica de Arranque usando la Fachada
        appFacade.hayUsuariosRegistrados().ifSuccess(hayUsuarios -> {
            if (!hayUsuarios) {
                // Si no hay usuarios, mostrar la ventana de creación de admin
                if (!navigatorService.mostrarVentanaCrearAdmin()) {
                    System.out.println("Creación de admin cancelada. Cerrando aplicación.");
                    Platform.exit();
                    return;
                }
            }

            // Siempre mostrar la ventana de login después
            if (!navigatorService.mostrarVentanaLogin()) {
                System.out.println("Login fallido o cancelado. Cerrando aplicación.");
                Platform.exit();
                return;
            }

            // Si el login es exitoso, mostrar la ventana principal
            navigatorService.mostrarVentanaPrincipal();

        }).ifError(errorMessage -> {
            System.err.println("Error crítico al iniciar la aplicación: " + errorMessage);
            Platform.exit();
        });
    }

    @Override
    public void stop() throws Exception {
        ServiceContainer.getInstance().shutdown();
        super.stop();
        System.out.println("Aplicación cerrada limpiamente.");
    }
}