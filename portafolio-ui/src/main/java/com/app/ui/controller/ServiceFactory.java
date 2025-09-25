package com.app.ui.controller;

import com.app.dto.*;
import com.app.entities.*;
import com.app.repositorio.*;
import com.app.repository.*;
import com.app.service.*;
import com.costing.api.CostingApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.app.enums.ListaEnumsCustodios;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Supplier;

/**
 * Clase ÚNICA y centralizada que gestiona la creación de todos los servicios y
 * actúa como el punto de entrada (Facade) para la interfaz de usuario.
 * Esta clase fusiona las responsabilidades de ServiceFactory y PortafolioFacade.
 */
public class ServiceFactory {

    private static final Logger logger = LoggerFactory.getLogger(ServiceFactory.class);

    // --- Recursos de la UI ---
    private NavigatorService navigatorService;
    private ResourceBundle resourceBundle;
    private final Map<Class<?>, Supplier<?>> controllerSuppliers = new HashMap<>();

    // --- Instanciación ÚNICA de Repositorios ---
    private final KardexApi kardexRepService = new KardexApi();
    private final PrecioRepository precioRepService = new PrecioRepositoryImpl();
    private final SaldoRepository saldoRepService = new SaldosRepositoryImpl();
    private final TipoMovimientoRepository tipoMovimientoRepo = new TipoMovimientoRepositoryImpl();
    private final ResumenHistoricoRepository resumenHistoricoRepository = new ResumenHistoricoRepositoryImpl();
    private final ResultadoInstrumentoRepository resultadoInstrumentoRepository = new ResultadoInstrumentoRepositoryImpl();
    private final CostingApi costeoService = new CostingApi();
    private final PortafolioFacade facadeService = new PortafolioFacade();

    // --- Instanciación ÚNICA de Servicios Core ---
    private final AuthenticationService authenticationService = new AuthenticationService();
    private final EmpresaService empresaService = new EmpresaService();
    private final CustodioService custodioService = new CustodioService();
    private final InstrumentoService instrumentoService = new InstrumentoService();
    private final TransaccionService transaccionService = new TransaccionService();
    private final FiltroService filtroService = new FiltroService();
    private final ResumenSaldoEmpresaService resumenSaldoEmpresaService = new ResumenSaldoEmpresaService();
    private final SaldoMensualService saldoMensualService = new SaldoMensualService();
    private final ConfrontaService confrontaService = new ConfrontaService();
    private final OperacionesTrxsService operacionesTrxsService = new OperacionesTrxsService();
    private final ProblemasTrxsService problemasTrxsService = new ProblemasTrxsService();
    private final ProcesoCargaDiariaService procesoCargaDiaria = new ProcesoCargaDiariaService();
    private final ProcesoCargaInicialService procesoCargaInicial = new ProcesoCargaInicialService();
    private final ProcesoCosteoInicialService procesoCosteoInicial = new ProcesoCosteoInicialService();
    private final NormalizarService normalizarService = new NormalizarService();
    private final FusionInstrumentoService fusionInstrumentoService = new FusionInstrumentoService();
    private final UsuarioService usuarioService = new UsuarioService();
    private final PerfilService perfilService = new PerfilService();
    private final SaldosRepositoryImpl saldoService = new SaldosRepositoryImpl();
    private final KardexRepositoryImpl kardexService = new KardexRepositoryImpl();
    private final ResumenHistoricoService resumenHistoricoService = new ResumenHistoricoService(resumenHistoricoRepository);
    private final SaldoActualService saldoActualService = new SaldoActualService(kardexRepService, precioRepService);
    private final TipoMovimientosService tipoMovimientosService = new TipoMovimientosService(tipoMovimientoRepo);
    
    public ServiceFactory() {
        initializeControllerSuppliers();
        logger.info("ServiceFactory (con Facade integrado) inicializado.");
    }

    // ========== MÉTODOS PÚBLICOS DEL FACADE (Punto de entrada para la UI) ==========

    public boolean autenticarUsuario(String usuario, String password) {
        try {
            return authenticationService.autenticar(usuario, password);
        } catch (Exception e) {
            logger.error("Error en autenticación", e);
            return false;
        }
    }

    public boolean hayUsuariosRegistrados() {
        return usuarioService.hayUsuariosRegistrados();
    }

    public List<EmpresaEntity> obtenerEmpresasConTransacciones() {
        return filtroService.obtenerEmpresasConTransacciones();
    }
    
    public List<CustodioEntity> obtenerCustodiosConTransacciones(Long empresaId) {
        return filtroService.obtenerCustodiosConTransacciones(empresaId);
    }

    public List<String> obtenerCuentasConTransacciones(Long empresaId, Long custodioId) {
        return filtroService.obtenerCuentasConTransacciones(empresaId, custodioId);
    }

    public List<InstrumentoEntity> obtenerInstrumentosConTransacciones(Long empresaId, Long custodioId, String cuenta) {
        return filtroService.obtenerInstrumentosConTransacciones(empresaId, custodioId, cuenta);
    }

    public ResultadoCargaDto ejecutarCargaInicial(ListaEnumsCustodios custodio, File archivo) {
        return procesoCargaInicial.ejecutar(custodio, archivo);
    }

    public ResultadoCargaDto ejecutarCargaDiaria(ListaEnumsCustodios custodio, File archivo) {
        return procesoCargaDiaria.ejecutar(custodio, archivo);
    }
    
    // ... Aquí puedes seguir añadiendo todos los otros métodos públicos que estaban en PortafolioFacade ...
    // Por ejemplo:
    // public List<ResumenHistoricoDto> obtenerResumenHistorico(...) { return resumenHistoricoService.generarReporte(...); }
    // public void fusionarInstrumentos(...) { fusionInstrumentoService.fusionarYPrepararRecosteo(...); }


    // ========== MÉTODOS DE CONFIGURACIÓN Y CREACIÓN DE CONTROLADORES ==========

    public void setNavigatorService(NavigatorService navigatorService) { this.navigatorService = navigatorService; }
    public void setResourceBundle(ResourceBundle resourceBundle) { this.resourceBundle = resourceBundle; }

    private void initializeControllerSuppliers() {
        // --- Aquí defines cómo se construye cada controlador ---
        // Ahora, en lugar de pasar 'this' (el ServiceFactory), puedes pasar servicios específicos
        // o mantener el 'this' si el controlador necesita acceso a múltiples métodos del facade.

        controllerSuppliers.put(AppController.class, () -> new AppController(this, navigatorService, resourceBundle));
        controllerSuppliers.put(LoginController.class, () -> new LoginController(authenticationService));
        controllerSuppliers.put(CrearAdminController.class, () -> new CrearAdminController(usuarioService));
        controllerSuppliers.put(SaldosController.class, () -> new SaldosController(saldoActualService, this, resourceBundle));

    }

    @SuppressWarnings("unchecked")
    public <T> T getController(Class<T> controllerClass) {
        try {
            Supplier<?> supplier = controllerSuppliers.get(controllerClass);
            if (supplier != null) {
                return (T) supplier.get();
            }
            // Fallback para controladores sin dependencias
            return controllerClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            logger.error("Error al crear controlador {}: {}", controllerClass.getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("No se pudo crear el controlador: " + controllerClass.getName(), e);
        }
    }
    
    // --- Getters para servicios (si algún controlador los necesita directamente) ---
    public EmpresaService getEmpresaService() { return empresaService; }
    public CustodioService getCustodioService() { return custodioService; }
    public FiltroService getFiltroService() { return filtroService; }
    public InstrumentoService getInstrumentoService() { return instrumentoService; }
    public CostingApi getCosteoService() { return costeoService; }
    public NormalizarService getNormalizarService() { return normalizarService; }
    public PortafolioFacade getPortafolioFacade() { return facadeService; }
    public AuthenticationService getAuthenticationService() { return authenticationService; }
    public UsuarioService getUsuarioService() { return usuarioService; }
    
}