package com.app.ui.App;

import com.app.service.UsuarioService;
import com.app.ui.controller.NavigatorService;
import com.app.facade.ServiceFactory;
import com.app.utiles.LibraryInitializer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.ResourceBundle;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        ServiceFactory serviceFactory = new ServiceFactory();

        String bundlePath = "com.app.bundles.messages";
        Locale locale = new Locale("es", "CL");
        ResourceBundle bundle = ResourceBundle.getBundle(bundlePath, locale);
        
        serviceFactory.setResourceBundle(bundle);

        // 1. CREAMOS EL NAVEGADOR Y LO HACEMOS DISPONIBLE
        NavigatorService navigatorService = new NavigatorService(bundle, serviceFactory);
        serviceFactory.setNavigatorService(navigatorService); 

        // 2. LA LÓGICA DE ARRANQUE AHORA USA EL NAVEGADOR
        UsuarioService usuarioService = serviceFactory.getUsuarioService();
        if (!serviceFactory.hayUsuariosRegistrados()) {
            if (!navigatorService.mostrarVentanaCrearAdmin()) {
                System.out.println("Creación de admin cancelada. Cerrando aplicación.");
                Platform.exit();
                return;
            }
        }

        if (!navigatorService.mostrarVentanaLogin()) {
            System.out.println("Login fallido o cancelado. Cerrando aplicación.");
            Platform.exit();
            return;
        }

        // 3. LA CARGA DE LA VENTANA PRINCIPAL NO CAMBIA
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AppMain.fxml"), bundle);
     
        // Es crucial que tu ServiceFactory también pueda ser usado por el ControllerFactory de JavaFX
        // para inyectar todos los servicios necesarios en AppController.
        loader.setControllerFactory(serviceFactory::getController); 
       
        Parent root = loader.load();

        // Le pasamos la ventana principal para que la use como "dueña" de futuras ventanas modales.
        navigatorService.setOwner(primaryStage);

        primaryStage.setTitle(bundle.getString("ventana.principal.titulo"));
        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.setOnCloseRequest(event -> LibraryInitializer.shutdown());
        primaryStage.show();
        
    }
}