package com.app.ui.factory;

import com.app.ui.controller.*;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Supplier;

public class ControllerFactory {

    private final AppFacade appFacade;
    private final ResourceBundle bundle;
    private NavigatorService navigatorService;

    // Usamos un mapa para registrar cómo se crea cada controlador.
    private final Map<Class<?>, Supplier<?>> controllerRegistry = new HashMap<>();

    public ControllerFactory(AppFacade appFacade, ResourceBundle bundle) {
        this.appFacade = appFacade;
        this.bundle = bundle;
        // El contenedor ya no es necesario aquí.
    }

    // El método getFacade() que solicitaste.
    public AppFacade getFacade() {
        return this.appFacade;
    }

    public void setNavigatorService(NavigatorService navigatorService) {
        this.navigatorService = navigatorService;
        // Una vez que tenemos el navigator, registramos los controladores que lo necesitan.
        registerControllers();
    }

    private void registerControllers() {
        // --- Registra aquí CÓMO construir cada controlador ---

        // Controlador principal que necesita todas las dependencias
        controllerRegistry.put(AppController.class,
                () -> new AppController(appFacade, navigatorService, bundle));

        // Controladores que necesitan la fachada y los textos (la mayoría)
        controllerRegistry.put(KardexController.class,
                () -> new KardexController(appFacade, bundle));
        controllerRegistry.put(OperacionesTrxsController.class,
                () -> new OperacionesTrxsController(appFacade, bundle));
        
        controllerRegistry.put(LoginController.class,
                () -> new LoginController(appFacade, bundle));
        controllerRegistry.put(CrearAdminController.class,
                () -> new CrearAdminController(appFacade, bundle));

        // ...Añade aquí el resto de tus controladores que siguen este patrón...
        // controllerRegistry.put(SaldosController.class, () -> new SaldosController(appFacade, bundle));
        // controllerRegistry.put(ResumenPortafolioController.class, () -> new ResumenPortafolioController(appFacade, bundle));
        // EJEMPLO: Cómo registrar un controlador que NO necesita NADA
        // Si tuvieras una ventana "Acerca de..." simple, se registraría así:
        // controllerRegistry.put(AcercaDeController.class, () -> new AcercaDeController());
    }

    @SuppressWarnings("unchecked")
    public <T> T createController(Class<T> controllerClass) {
        // CAMBIO: La lógica de creación ahora es simple y extensible.
        Supplier<?> supplier = controllerRegistry.get(controllerClass);
        if (supplier != null) {
            return (T) supplier.get();
        }

        // Fallback para controladores simples sin dependencias
        try {
            return controllerClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("No se encontró un registro para crear el controlador: " + controllerClass.getSimpleName(), e);
        }
    }

    public boolean isServiceAvailable(Class<?> serviceClass) {
        return true;
    }
}
