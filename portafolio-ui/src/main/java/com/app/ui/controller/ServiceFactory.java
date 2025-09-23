package com.app.ui.controller;

import com.app.repository.PrecioRepository;
import com.app.repository.ResultadoInstrumentoRepository;
import com.app.repository.ResumenHistoricoRepository;
import com.app.repository.TipoMovimientoRepository;
import com.app.repositorio.KardexRepositoryImpl;
import com.app.repositorio.PrecioRepositoryImpl;
import com.app.repositorio.ResultadoInstrumentoRepositoryImpl;
import com.app.repositorio.ResumenHistoricoRepositoryImpl;
import com.app.repositorio.TipoMovimientoRepositoryImpl;
import com.app.repositorio.TransaccionRepository;
import com.app.ui.service.NavigatorService;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Supplier;
import com.app.repository.KardexRepository;
import com.app.service.AuthenticationService;
import com.app.service.ConfrontaService;
import com.app.service.CostService;
import com.app.service.CustodioService;
import com.app.service.EmpresaService;
import com.app.service.FiltroService;
import com.app.service.FusionInstrumentoService;
import com.app.service.InstrumentoService;
import com.app.service.KardexService;
import com.app.service.NormalizarService;
import com.app.service.OperacionesTrxsService;
import com.app.service.PerfilService;
import com.app.service.ProblemasTrxsService;
import com.app.service.ProcesoCargaDiaria;
import com.app.service.ProcesoCargaInicial;
import com.app.service.ProcesoCosteoInicial;
import com.app.service.ResultadoInstrumentoService;
import com.app.service.ResumenHistoricoService;
import com.app.service.ResumenPortafolioService;
import com.app.service.ResumenSaldoEmpresaService;
import com.app.service.SaldoActualService;
import com.app.service.SaldoMensualService;
import com.app.service.SaldoService;
import com.app.service.TipoMovimientosService;
import com.app.service.TransaccionService;
import com.app.service.UsuarioService;

public class ServiceFactory {

    private NavigatorService navigatorService;
    private ResourceBundle resourceBundle;
    private final Map<Class<?>, Object> serviceCache = new HashMap<>();

    // --- Instanciación de Repositorios y Servicios ---
    // Repositorios
    private final KardexRepository kardexRepService = new KardexRepositoryImpl();
    private final PrecioRepository precioRepService = new PrecioRepositoryImpl();
    private final TipoMovimientoRepository tipoMovimientoService = new TipoMovimientoRepositoryImpl();
    private final TransaccionRepository transaccionRepository = new TransaccionRepository();
    private final ResumenHistoricoRepository resumenHistoricoRepository = new ResumenHistoricoRepositoryImpl();
    private final ResultadoInstrumentoRepository resultadoInstrumentoRepository = new ResultadoInstrumentoRepositoryImpl(); 

    // Servicios
    private final SaldoActualService saldoActualService = new SaldoActualService(kardexRepService, precioRepService);
    private final TipoMovimientosService tipoMovimientosService = new TipoMovimientosService(tipoMovimientoService);
    private final ProcesoCosteoInicial procesoCosteoInicial = new ProcesoCosteoInicial();
    private final ProcesoCargaDiaria cargaDiariaService = new ProcesoCargaDiaria();
    private final ProcesoCargaInicial cargaInicialSaldos = new ProcesoCargaInicial();
    private final CustodioService custodioService = new CustodioService();
    private final EmpresaService empresaService = new EmpresaService();
    private final InstrumentoService instrumentoService = new InstrumentoService();
    private final OperacionesTrxsService operacionesTrxsService = new OperacionesTrxsService();
    private final PerfilService perfilService = new PerfilService();
    private final ProblemasTrxsService problemasTrxsService = new ProblemasTrxsService();
    private final SaldoMensualService saldoMensualService = new SaldoMensualService();
    private final TransaccionService transaccionService = new TransaccionService();
    private final UsuarioService usuarioService = new UsuarioService();
    private final CostService costeoService = new CostService();
    private final FusionInstrumentoService fusionService = new FusionInstrumentoService();
    private final NormalizarService normalizarService = new NormalizarService();
    private final AuthenticationService authService = new AuthenticationService();
    private final ResumenSaldoEmpresaService resumenSaldosService = new ResumenSaldoEmpresaService();
    private final ConfrontaService confrontaService = new ConfrontaService();

    private final SaldoService saldoService = new SaldoService();
    private final KardexService kardexService = new KardexService();
    private final ResumenHistoricoService resumenHistoricoService = new ResumenHistoricoService(resumenHistoricoRepository);
    private final ResumenPortafolioService resumenPortafolioService = new ResumenPortafolioService(kardexService, saldoService, transaccionRepository);    
    private final ResultadoInstrumentoService resultadoInstrumentoService = new ResultadoInstrumentoService(saldoService, kardexService, resultadoInstrumentoRepository); 

    // --- Getters para todos los servicios ---
    public ResumenHistoricoService getResumenHistoricoService() {
        return resumenHistoricoService;
    }

    public ResumenPortafolioService getResumenPortafolioService() {
        return resumenPortafolioService;
    }

    public SaldoService getSaldoService() {
        return saldoService;
    }

    public KardexService getKardexService() {
        return kardexService;
    }

    public ResultadoInstrumentoService getResultadoInstrumentoService() {
        return resultadoInstrumentoService;
    }

    public SaldoActualService getSaldoActualService() {
        return saldoActualService;
    }

    public TipoMovimientosService getTipoMovimientosService() {
        return tipoMovimientosService;
    }

    public ProcesoCosteoInicial getProcesoCosteoInicial() {
        return procesoCosteoInicial;
    }

    public ProcesoCargaDiaria getCargaDiaria() {
        return cargaDiariaService;
    }

    public ProcesoCargaInicial getCargaInicialSaldos() {
        return cargaInicialSaldos;
    }

    public CustodioService getCustodioService() {
        return custodioService;
    }

    public EmpresaService getEmpresaService() {
        return empresaService;
    }

    public InstrumentoService getInstrumentoService() {
        return instrumentoService;
    }

    public OperacionesTrxsService getOperacionesTrxsService() {
        return operacionesTrxsService;
    }

    public PerfilService getPerfilService() {
        return perfilService;
    }

    public ProblemasTrxsService getProblemasTrxsService() {
        return problemasTrxsService;
    }

    public SaldoMensualService getSaldoMensualService() {
        return saldoMensualService;
    }

    public TransaccionService getTransaccionService() {
        return transaccionService;
    }

    public UsuarioService getUsuarioService() {
        return usuarioService;
    }

    public CostService getCostService() {
        return costeoService;
    }

    public FusionInstrumentoService getFusionService() {
        return fusionService;
    }

    public NormalizarService getNormalizarService() {
        return normalizarService;
    }

    public AuthenticationService getAuthentication() {
        return authService;
    }

    public KardexRepository getKardexRepService() {
        return kardexRepService;
    }

    public ResumenSaldoEmpresaService getResumenSaldosService() {
        return resumenSaldosService;
    }

    public ConfrontaService getConfrontaService() {
        return confrontaService;
    }

    public void setNavigatorService(NavigatorService navigatorService) {
        this.navigatorService = navigatorService;
    }

    public void setResourceBundle(ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    public FiltroService getFiltroService() {
        return getService(FiltroService.class, FiltroService::new);
    }

    // --- MÉTODO QUE ACTÚA COMO CONTROLLER FACTORY ---
    public Object getController(Class<?> type) {
        if (type == AppController.class) {
            return new AppController(this, navigatorService, resourceBundle);
        }
        if (type == LoginController.class) {
            return new LoginController(getAuthentication());
        }
        if (type == CrearAdminController.class) {
            return new CrearAdminController(getUsuarioService());
        }
        if (type == KardexController.class) {
            return new KardexController((KardexRepositoryImpl) getKardexRepService(), this, resourceBundle);
        }
        if (type == SaldosController.class) {
            return new SaldosController(getSaldoActualService(), this, resourceBundle);
        }
        if (type == ResumenSaldosController.class) {
            return new ResumenSaldosController(getResumenSaldosService());
        }
        if (type == ConfrontaSaldosController.class) {
            return new ConfrontaSaldosController();
        }
        if (type == ResultadoInstrumentoController.class) {
            return new ResultadoInstrumentoController(
                    getResultadoInstrumentoService(),
                    this,
                    resourceBundle
            );
        }
        if (type == ResumenPortafolioController.class) {
            return new ResumenPortafolioController(
                    getResumenPortafolioService(),
                    this,
                    resourceBundle
            );
        }
        if (type == OperacionesTrxsController.class) {
            return new OperacionesTrxsController(
                    getOperacionesTrxsService(),
                    this,
                    getFiltroService(),
                    this::getController,
                    resourceBundle
            );
        }
        if (type == SaldoMensualController.class) {
            return new SaldoMensualController(getSaldoMensualService(), getEmpresaService(), getCustodioService());
        }
        if (type == ProblemasTrxsController.class) {
            return new ProblemasTrxsController(getProblemasTrxsService(), getEmpresaService(), getCustodioService());
        }
        if (type == TipoMovimientosController.class) {
            return new TipoMovimientosController(getTipoMovimientosService());
        }
        if (type == ResumenHistoricoController.class) {
            return new ResumenHistoricoController(getResumenHistoricoService(), this, resourceBundle);
        }
        if (type == TransaccionManualController.class) {
            return new TransaccionManualController(
                    getTransaccionService(),
                    getTipoMovimientosService(),
                    getInstrumentoService(),
                    getEmpresaService(),
                    getCustodioService()
            );
        }
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo crear el controlador: " + type.getName(), e);
        }
    }

    private <T> T getService(Class<T> serviceClass, Supplier<T> factory) {
        return (T) serviceCache.computeIfAbsent(serviceClass, k -> factory.get());
    }
}