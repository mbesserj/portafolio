package com.app.facade;

import com.app.dto.*;
import com.app.interfaces.CostingApiInterfaz;
import com.app.interfaces.KardexApiInterfaz;
import com.app.interfaces.SaldoApiInterfaz;
import com.app.repositorio.AggregatesForInstrument;
import com.app.service.*;
import com.app.ui.controller.AdminUsuariosController;
import com.app.ui.controller.AjusteManualController;
import com.app.ui.controller.AppController;
import com.app.ui.controller.ConfrontaSaldosController;
import com.app.ui.controller.CrearAdminController;
import com.app.ui.controller.CrearUsuarioController;
import com.app.ui.controller.KardexController;
import com.app.ui.controller.KardexDetallesController;
import com.app.ui.controller.LoginController;
import com.app.ui.controller.NavigatorService;
import com.app.ui.controller.OperacionesTrxsController;
import com.app.ui.controller.ProblemasTrxsController;
import com.app.ui.controller.ResultadosProcesoController;
import com.app.ui.controller.ResumenSaldosController;
import com.app.ui.controller.SaldoMensualController;
import com.app.ui.controller.TipoMovimientosController;
import com.app.ui.controller.TransaccionDetallesController;
import com.costing.api.CostingApi;
import com.costing.api.KardexApi;
import com.costing.api.SaldoApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Supplier;

/**
 * ServiceFactory corregido que solo utiliza clases y constructores que existen
 * en el proyecto actual. Esta versión evita referencias a clases no
 * disponibles.
 */
public class ServiceFactory {

    private static final Logger logger = LoggerFactory.getLogger(ServiceFactory.class);

    // ========== CONFIGURACIÓN DE UI ==========
    private NavigatorService navigatorService;
    private ResourceBundle resourceBundle;
    private final Map<Class<?>, Supplier<?>> controllerSuppliers = new HashMap<>();

    // ========== SERVICIOS CORE (Solo los que tienen constructores sin parámetros) ==========
    private final EmpresaService empresaService = new EmpresaService();
    private final CustodioService custodioService = new CustodioService();
    private final InstrumentoService instrumentoService = new InstrumentoService();
    private final TransaccionService transaccionService = new TransaccionService();
    private final FiltroService filtroService = new FiltroService();
    private final ResumenSaldoEmpresaService resumenSaldoEmpresaService = new ResumenSaldoEmpresaService();
    private final SaldoMensualService saldoMensualService = new SaldoMensualService();
    private final OperacionesTrxsService operacionesTrxsService = new OperacionesTrxsService();
    private final ProblemasTrxsService problemasTrxsService = new ProblemasTrxsService();
    private final NormalizarService normalizarService = new NormalizarService();
    private final FusionInstrumentoService fusionInstrumentoService = new FusionInstrumentoService();
    private final AggregatesForInstrument aggregatesOpt = new AggregatesForInstrument();
    private final ResumenPortafolioService portafolioService = new ResumenPortafolioService(SaldoApi.createService(), KardexApi.createService(), aggregatesOpt);

    // ========== SERVICIOS CON DEPENDENCIAS (Inicialización diferida) ==========
    private AuthenticationService authenticationService;
    private UsuarioService usuarioService;
    private PerfilService perfilService;

    // ========== SERVICIOS QUE REQUIEREN CONFIGURACIÓN ESPECIAL ==========
    private ConfrontaService confrontaService;
    private ResumenHistoricoService resumenHistoricoService;
    private ResultadoInstrumentoService resultadoInstrumentoService;
    private SaldoActualService saldoActualService;
    private TipoMovimientosService tipoMovimientosService;

    // ========== SERVICIOS DE API DE COSTING ==========
    private CostingApiInterfaz costingApiService;
    private KardexApiInterfaz kardexApiService;
    private SaldoApiInterfaz saldoApiService;

    public ServiceFactory() {
        initializeBasicServices();
        initializeControllerSuppliers();
        logger.info("ServiceFactory inicializado con servicios básicos disponibles");
    }

    /**
     * Inicializa servicios básicos que pueden ser creados sin dependencias
     */
    private void initializeBasicServices() {
        try {
            // Inicializar servicios de autenticación y usuarios
            authenticationService = new AuthenticationService();
            perfilService = new PerfilService();

            // UsuarioService requiere dependencias específicas
            // Asumiendo que el constructor correcto es new UsuarioService(perfilService)
            // Si también necesita BCrypt, se debe instanciar aquí
            usuarioService = new UsuarioService(perfilService, authenticationService);

            // Inicializar las APIs de costing
            logger.debug("Inicializando APIs de portafolio-costing...");
            costingApiService = CostingApi.createService();
            kardexApiService = KardexApi.createService();
            saldoApiService = SaldoApi.createService();
            logger.info("APIs de portafolio-costing inicializadas.");

        } catch (Exception e) {
            logger.error("Error inicializando servicios básicos: {}", e.getMessage());
        }
    }


    // ========== CONFIGURACIÓN DE CONTROLADORES ==========
    public void setNavigatorService(NavigatorService navigatorService) {
        this.navigatorService = navigatorService;
    }

    public void setResourceBundle(ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    private void initializeControllerSuppliers() {
        // Solo registrar controladores que pueden ser creados exitosamente

        // Controlador principal
        controllerSuppliers.put(AppController.class, ()
                -> new AppController(this, navigatorService, resourceBundle));

        // Controladores de autenticación (solo si los servicios están disponibles)
        if (authenticationService != null) {
            controllerSuppliers.put(LoginController.class, ()
                    -> new LoginController(authenticationService));
        }

        if (usuarioService != null) {
            controllerSuppliers.put(CrearAdminController.class, ()
                    -> new CrearAdminController(usuarioService));
            controllerSuppliers.put(CrearUsuarioController.class, ()
                    -> new CrearUsuarioController(usuarioService));
        }

        if (usuarioService != null && perfilService != null) {
            controllerSuppliers.put(AdminUsuariosController.class, ()
                    -> new AdminUsuariosController(usuarioService, perfilService));
        }
        if (kardexApiService != null) {
            controllerSuppliers.put(KardexController.class, ()
                    -> new KardexController(kardexApiService, this, resourceBundle)
            );
        }

        // Reportes básicos que funcionan
        controllerSuppliers.put(SaldoMensualController.class, ()
                -> new SaldoMensualController(saldoMensualService, empresaService, custodioService));
        controllerSuppliers.put(ResumenSaldosController.class, ()
                -> new ResumenSaldosController(resumenSaldoEmpresaService));

        // Operaciones básicas
        controllerSuppliers.put(OperacionesTrxsController.class, ()
                -> new OperacionesTrxsController(operacionesTrxsService, this, filtroService, this::getController, resourceBundle));
        controllerSuppliers.put(ProblemasTrxsController.class, ()
                -> new ProblemasTrxsController(problemasTrxsService, empresaService, custodioService));

        // Controladores simples sin dependencias complejas
        controllerSuppliers.put(AjusteManualController.class, AjusteManualController::new);
        controllerSuppliers.put(TransaccionDetallesController.class, TransaccionDetallesController::new);
        controllerSuppliers.put(KardexDetallesController.class, KardexDetallesController::new);
        controllerSuppliers.put(ResultadosProcesoController.class, ResultadosProcesoController::new);
    }

    @SuppressWarnings("unchecked")
    public <T> T getController(Class<T> controllerClass) {
        try {
            Supplier<?> supplier = controllerSuppliers.get(controllerClass);
            if (supplier != null) {
                T controller = (T) supplier.get();
                logger.debug("Controlador {} creado exitosamente", controllerClass.getSimpleName());
                return controller;
            }

            if (confrontaService != null && saldoActualService != null) {
                controllerSuppliers.put(ConfrontaSaldosController.class, ()
                        -> 
                        new ConfrontaSaldosController(confrontaService)
                );
            }
            
            if (controllerClass == TipoMovimientosController.class) {
                // Solo crear si tenemos el servicio necesario
                if (tipoMovimientosService != null) {
                    return (T) new TipoMovimientosController(tipoMovimientosService);
                } else {
                    throw new RuntimeException("TipoMovimientosController requiere TipoMovimientosService");
                }
            }

            // Fallback: constructor vacío
            logger.warn("No hay supplier registrado para {}, intentando constructor vacío", controllerClass.getSimpleName());
            return controllerClass.getDeclaredConstructor().newInstance();

        } catch (Exception e) {
            logger.error("Error creando controlador {}: {}", controllerClass.getSimpleName(), e.getMessage());
            throw new RuntimeException("No se pudo crear el controlador: " + controllerClass.getName(), e);
        }
    }

    // ========== GETTERS PARA SERVICIOS (Solo servicios disponibles) ==========

    public CostingApiInterfaz getCostingApiService() {
        if (costingApiService == null) {
            throw new IllegalStateException("CostingApi no está disponible. Verifique la inicialización.");
        }
        return costingApiService;
    }

    public KardexApiInterfaz getKardexApiService() {
        if (kardexApiService == null) {
            throw new IllegalStateException("KardexApi no está disponible. Verifique la inicialización.");
        }
        return kardexApiService;
    }

    public SaldoApiInterfaz getSaldoApiService() {
        if (saldoApiService == null) {
            throw new IllegalStateException("SaldoApi no está disponible. Verifique la inicialización.");
        }
        return saldoApiService;
    }

    public EmpresaService getEmpresaService() {
        return empresaService;
    }

    public CustodioService getCustodioService() {
        return custodioService;
    }

    public InstrumentoService getInstrumentoService() {
        return instrumentoService;
    }

    public TransaccionService getTransaccionService() {
        return transaccionService;
    }

    public FiltroService getFiltroService() {
        return filtroService;
    }

    public NormalizarService getNormalizarService() {
        return normalizarService;
    }

    public FusionInstrumentoService getFusionService() {
        return fusionInstrumentoService;
    }

    // Servicios de reportes
    public ResumenSaldoEmpresaService getResumenSaldoEmpresaService() {
        return resumenSaldoEmpresaService;
    }

    public SaldoMensualService getSaldoMensualService() {
        return saldoMensualService;
    }

    // Servicios de operaciones
    public OperacionesTrxsService getOperacionesTrxsService() {
        return operacionesTrxsService;
    }

    public ProblemasTrxsService getProblemasTrxsService() {
        return problemasTrxsService;
    }

    public AggregatesForInstrument getAggregatesOpt() {
        return aggregatesOpt;
    }

    // Servicios con verificación de disponibilidad
    public AuthenticationService getAuthenticationService() {
        if (authenticationService == null) {
            throw new IllegalStateException("AuthenticationService no está disponible");
        }
        return authenticationService;
    }

    public UsuarioService getUsuarioService() {
        if (usuarioService == null) {
            throw new IllegalStateException("UsuarioService no está disponible");
        }
        return usuarioService;
    }

    public PerfilService getPerfilService() {
        if (perfilService == null) {
            throw new IllegalStateException("PerfilService no está disponible");
        }
        return perfilService;
    }

    // Métodos para servicios que requieren inicialización especial
    public TipoMovimientosService getTipoMovimientosService() {
        if (tipoMovimientosService == null) {
            throw new IllegalStateException("TipoMovimientosService requiere inicialización específica");
        }
        return tipoMovimientosService;
    }

    public ConfrontaService getConfrontaService() {
        if (confrontaService == null) {
            throw new IllegalStateException("ConfrontaService requiere configuración de repositorio");
        }
        return confrontaService;
    }

    // ========== MÉTODOS DE DIAGNÓSTICO ==========

    public List<String> obtenerControladoresTodosRegistrados() {
        return controllerSuppliers.keySet().stream()
                .map(Class::getSimpleName)
                .sorted()
                .toList();
    }
}