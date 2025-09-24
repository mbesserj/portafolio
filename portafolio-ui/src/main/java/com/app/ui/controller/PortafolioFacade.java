package com.app.ui.controller;

import com.app.dto.*;
import com.app.entities.*;
import com.app.enums.ListaEnumsCustodios;
import com.app.service.*;
import com.app.repositorio.*;
import com.costing.api.KardexApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

/**
 * Facade principal que coordina todos los servicios del portafolio.
 * Este es el punto de entrada único para la UI de JavaFX.
 */
public class PortafolioFacade {
    
    private static final Logger logger = LoggerFactory.getLogger(PortafolioFacade.class);
    
    // === SERVICIOS BÁSICOS ===
    private final AuthenticationService authenticationService;
    private final EmpresaService empresaService;
    private final CustodioService custodioService;
    private final InstrumentoService instrumentoService;
    private final TransaccionService transaccionService;
    private final FiltroService filtroService;
    
    // === SERVICIOS DE REPORTES ===
    private final ResumenPortafolioService resumenPortafolioService;
    private final ResumenHistoricoService resumenHistoricoService;
    private final ResumenSaldoEmpresaService resumenSaldoEmpresaService;
    private final SaldoMensualService saldoMensualService;
    private final ConfrontaService confrontaService;
    private final ResultadoInstrumentoService resultadoInstrumentoService;
    
    // === SERVICIOS DE OPERACIONES ===
    private final OperacionesTrxsService operacionesTrxsService;
    private final ProblemasTrxsService problemasTrxsService;
    
    // === SERVICIOS DE CARGA ===
    private final ProcesoCargaDiaria procesoCargaDiaria;
    private final ProcesoCargaInicial procesoCargaInicial;
    private final ProcesoCosteoInicial procesoCosteoInicial;
    private final NormalizarService normalizarService;
    
    // === SERVICIOS DE GESTIÓN ===
    private final FusionInstrumentoService fusionInstrumentoService;
    private final UsuarioService usuarioService;
    private final PerfilService perfilService;

    public PortafolioFacade() {
        // Inicializar servicios básicos
        this.authenticationService = new AuthenticationService();
        this.empresaService = new EmpresaService();
        this.custodioService = new CustodioService();
        this.instrumentoService = new InstrumentoService();
        this.transaccionService = new TransaccionService();
        this.filtroService = new FiltroService();
        
        // Inicializar servicios de reportes con dependencias
        KardexApi kardexApi = new KardexApi();
        TransaccionRepository transaccionRepo = new TransaccionRepository();
        ResultadoInstrumentoRepositoryImpl resultadoRepo = new ResultadoInstrumentoRepositoryImpl();
        ResumenHistoricoRepositoryImpl resumenRepo = new ResumenHistoricoRepositoryImpl();
        
        this.resumenPortafolioService = new ResumenPortafolioService(kardexApi, transaccionRepo);
        this.resumenHistoricoService = new ResumenHistoricoService(resumenRepo);
        this.resumenSaldoEmpresaService = new ResumenSaldoEmpresaService();
        this.saldoMensualService = new SaldoMensualService();
        this.confrontaService = new ConfrontaService();
        this.resultadoInstrumentoService = new ResultadoInstrumentoService(kardexApi, resultadoRepo);
        
        // Inicializar servicios de operaciones
        this.operacionesTrxsService = new OperacionesTrxsService();
        this.problemasTrxsService = new ProblemasTrxsService();
        
        // Inicializar servicios de carga
        this.procesoCargaDiaria = new ProcesoCargaDiaria();
        this.procesoCargaInicial = new ProcesoCargaInicial();
        this.procesoCosteoInicial = new ProcesoCosteoInicial();
        this.normalizarService = new NormalizarService();
        
        // Inicializar servicios de gestión
        this.fusionInstrumentoService = new FusionInstrumentoService();
        this.usuarioService = new UsuarioService();
        this.perfilService = new PerfilService();
    }
    
    // ========== MÉTODOS DE AUTENTICACIÓN ==========
    
    public boolean autenticarUsuario(String usuario, String password) {
        try {
            return authenticationService.autenticar(usuario, password);
        } catch (Exception e) {
            logger.error("Error en autenticación", e);
            return false;
        }
    }
    
    public boolean hayUsuariosRegistrados() {
        try {
            return usuarioService.hayUsuariosRegistrados();
        } catch (Exception e) {
            logger.error("Error al verificar usuarios", e);
            return false;
        }
    }
    
    // ========== MÉTODOS DE CONSULTA BÁSICA ==========
    
    public List<EmpresaEntity> obtenerTodasLasEmpresas() {
        try {
            return empresaService.obtenerTodas();
        } catch (Exception e) {
            logger.error("Error al obtener empresas", e);
            throw new RuntimeException("Error al cargar empresas", e);
        }
    }
    
    public List<EmpresaEntity> obtenerEmpresasConTransacciones() {
        try {
            return empresaService.obtenerEmpresasConTransacciones();
        } catch (Exception e) {
            logger.error("Error al obtener empresas con transacciones", e);
            throw new RuntimeException("Error al cargar empresas con transacciones", e);
        }
    }
    
    public List<CustodioEntity> obtenerCustodiosPorEmpresa(Long empresaId) {
        try {
            return custodioService.obtenerCustodiosPorEmpresa(empresaId);
        } catch (Exception e) {
            logger.error("Error al obtener custodios por empresa", e);
            throw new RuntimeException("Error al cargar custodios", e);
        }
    }
    
    public List<String> obtenerCuentasPorCustodioYEmpresa(Long custodioId, Long empresaId) {
        try {
            return custodioService.obtenerCuentasPorCustodioYEmpresa(custodioId, empresaId);
        } catch (Exception e) {
            logger.error("Error al obtener cuentas", e);
            throw new RuntimeException("Error al cargar cuentas", e);
        }
    }
    
    public List<InstrumentoEntity> obtenerInstrumentosConTransacciones(Long empresaId, Long custodioId, String cuenta) {
        try {
            return instrumentoService.obtenerInstrumentosConTransacciones(empresaId, custodioId, cuenta);
        } catch (Exception e) {
            logger.error("Error al obtener instrumentos", e);
            throw new RuntimeException("Error al cargar instrumentos", e);
        }
    }
    
    // ========== MÉTODOS DE REPORTES PRINCIPALES ==========
    
    /**
     * Obtiene el resumen completo del portafolio para una cuenta específica
     */
    public List<ResumenInstrumentoDto> obtenerResumenPortafolio(Long empresaId, Long custodioId, String cuenta) {
        try {
            logger.info("Generando resumen de portafolio para empresa: {}, custodio: {}, cuenta: {}", 
                       empresaId, custodioId, cuenta);
            return resumenPortafolioService.generarResumen(empresaId, custodioId, cuenta);
        } catch (Exception e) {
            logger.error("Error al obtener resumen de portafolio", e);
            throw new RuntimeException("Error al generar el resumen del portafolio", e);
        }
    }
    
    /**
     * Obtiene el resumen histórico de operaciones cerradas
     */
    public List<ResumenHistoricoDto> obtenerResumenHistorico(Long empresaId, Long custodioId, String cuenta) {
        try {
            return resumenHistoricoService.generarReporte(empresaId, custodioId, cuenta);
        } catch (Exception e) {
            logger.error("Error al obtener resumen histórico", e);
            throw new RuntimeException("Error al generar el resumen histórico", e);
        }
    }
    
    /**
     * Obtiene el resumen de saldos por empresa
     */
    public List<ResumenSaldoEmpresaDto> obtenerResumenSaldosEmpresas() {
        try {
            return resumenSaldoEmpresaService.obtenerResumenSaldos();
        } catch (Exception e) {
            logger.error("Error al obtener resumen de saldos por empresa", e);
            throw new RuntimeException("Error al generar el resumen de saldos", e);
        }
    }
    
    /**
     * Obtiene los saldos mensuales para análisis temporal
     */
    public List<SaldoMensualDto> obtenerSaldosMensuales(String razonSocial, String custodio, int anio, String moneda) {
        try {
            return saldoMensualService.obtenerSaldosMensuales(razonSocial, custodio, anio, moneda);
        } catch (Exception e) {
            logger.error("Error al obtener saldos mensuales", e);
            throw new RuntimeException("Error al generar los saldos mensuales", e);
        }
    }
    
    /**
     * Obtiene las diferencias en la confrontación de saldos
     */
    public List<ConfrontaSaldoDto> obtenerConfrontacionSaldos() {
        try {
            return confrontaService.obtenerDiferenciasDeSaldos();
        } catch (Exception e) {
            logger.error("Error al obtener confrontación de saldos", e);
            throw new RuntimeException("Error al realizar la confrontación de saldos", e);
        }
    }
    
    /**
     * Obtiene el historial detallado de resultados para un instrumento
     */
    public List<ResultadoInstrumentoDto> obtenerHistorialResultados(Long empresaId, Long custodioId, String cuenta, Long instrumentoId) {
        try {
            return resultadoInstrumentoService.obtenerHistorialResultados(empresaId, custodioId, cuenta, instrumentoId);
        } catch (Exception e) {
            logger.error("Error al obtener historial de resultados", e);
            throw new RuntimeException("Error al cargar el historial de resultados", e);
        }
    }
    
    // ========== MÉTODOS DE OPERACIONES Y TRANSACCIONES ==========
    
    /**
     * Obtiene el detalle de operaciones para instrumentos específicos
     */
    public List<OperacionesTrxsDto> obtenerOperacionesPorInstrumento(String empresa, String custodio, String cuenta, List<String> nemos) {
        try {
            return operacionesTrxsService.obtenerTransaccionesPorGrupo(empresa, custodio, cuenta, nemos);
        } catch (Exception e) {
            logger.error("Error al obtener operaciones por instrumento", e);
            throw new RuntimeException("Error al cargar las operaciones", e);
        }
    }
    
    /**
     * Obtiene las transacciones marcadas para revisión
     */
    public List<ProblemasTrxsDto> obtenerTransaccionesConProblemas(String razonSocialEmpresa, String nombreCustodio) {
        try {
            return problemasTrxsService.obtenerTransaccionesConProblemas(razonSocialEmpresa, nombreCustodio);
        } catch (Exception e) {
            logger.error("Error al obtener transacciones con problemas", e);
            throw new RuntimeException("Error al cargar transacciones problemáticas", e);
        }
    }
    
    /**
     * Crea una nueva transacción manual
     */
    public void crearTransaccionManual(TransaccionManualDto transaccionDto) {
        try {
            transaccionService.crearTransaccionManual(transaccionDto);
            logger.info("Transacción manual creada exitosamente");
        } catch (Exception e) {
            logger.error("Error al crear transacción manual", e);
            throw new RuntimeException("Error al guardar la transacción: " + e.getMessage(), e);
        }
    }
    
    /**
     * Cambia el estado de ignorar en costeo para una transacción
     */
    public void toggleIgnorarTransaccionEnCosteo(Long transaccionId) {
        try {
            transaccionService.toggleIgnorarEnCosteo(transaccionId);
            logger.info("Estado de transacción {} modificado exitosamente", transaccionId);
        } catch (Exception e) {
            logger.error("Error al cambiar estado de transacción", e);
            throw new RuntimeException("Error al modificar la transacción", e);
        }
    }
    
    /**
     * Obtiene una transacción por su ID
     */
    public TransaccionEntity obtenerTransaccionPorId(Long id) {
        try {
            return transaccionService.obtenerTransaccionPorId(id);
        } catch (Exception e) {
            logger.error("Error al obtener transacción por ID", e);
            throw new RuntimeException("Error al cargar la transacción", e);
        }
    }
    
    // ========== MÉTODOS DE CARGA Y PROCESAMIENTO ==========
    
    /**
     * Ejecuta el proceso de carga inicial completa
     */
    public ResultadoCargaDto ejecutarCargaInicial(ListaEnumsCustodios custodio, File archivo) {
        try {
            logger.info("Iniciando carga inicial desde facade para custodio: {}", custodio);
            return procesoCargaInicial.ejecutar(custodio, archivo);
        } catch (Exception e) {
            logger.error("Error en carga inicial", e);
            throw new RuntimeException("Error durante la carga inicial: " + e.getMessage(), e);
        }
    }
    
    /**
     * Ejecuta el proceso de carga diaria incremental
     */
    public ResultadoCargaDto ejecutarCargaDiaria(ListaEnumsCustodios custodio, File archivo) {
        try {
            logger.info("Iniciando carga diaria desde facade para custodio: {}", custodio);
            return procesoCargaDiaria.ejecutar(custodio, archivo);
        } catch (Exception e) {
            logger.error("Error en carga diaria", e);
            throw new RuntimeException("Error durante la carga diaria: " + e.getMessage(), e);
        }
    }
    
    /**
     * Ejecuta el proceso de costeo inicial
     */
    public void ejecutarCosteoInicial() {
        try {
            logger.info("Iniciando costeo inicial desde facade");
            procesoCosteoInicial.ejecutar();
            logger.info("Costeo inicial completado exitosamente");
        } catch (Exception e) {
            logger.error("Error en costeo inicial", e);
            throw new RuntimeException("Error durante el costeo inicial: " + e.getMessage(), e);
        }
    }
    
    /**
     * Ejecuta solo el proceso de normalización
     */
    public ResultadoCargaDto ejecutarNormalizacion() {
        try {
            logger.info("Iniciando normalización desde facade");
            return normalizarService.ejecutar();
        } catch (Exception e) {
            logger.error("Error en normalización", e);
            throw new RuntimeException("Error durante la normalización: " + e.getMessage(), e);
        }
    }
    
    // ========== MÉTODOS DE FILTROS DINÁMICOS ==========
    
    public List<EmpresaEntity> obtenerEmpresasConTransaccionesFiltro() {
        try {
            return filtroService.obtenerEmpresasConTransacciones();
        } catch (Exception e) {
            logger.error("Error al obtener empresas con transacciones (filtro)", e);
            throw new RuntimeException("Error al cargar filtro de empresas", e);
        }
    }
    
    public List<CustodioEntity> obtenerCustodiosConTransacciones(Long empresaId) {
        try {
            return filtroService.obtenerCustodiosConTransacciones(empresaId);
        } catch (Exception e) {
            logger.error("Error al obtener custodios con transacciones", e);
            throw new RuntimeException("Error al cargar filtro de custodios", e);
        }
    }
    
    public List<String> obtenerCuentasConTransacciones(Long empresaId, Long custodioId) {
        try {
            return filtroService.obtenerCuentasConTransacciones(empresaId, custodioId);
        } catch (Exception e) {
            logger.error("Error al obtener cuentas con transacciones", e);
            throw new RuntimeException("Error al cargar filtro de cuentas", e);
        }
    }
    
    public List<InstrumentoEntity> obtenerInstrumentosConTransaccionesFiltro(Long empresaId, Long custodioId, String cuenta) {
        try {
            return filtroService.obtenerInstrumentosConTransacciones(empresaId, custodioId, cuenta);
        } catch (Exception e) {
            logger.error("Error al obtener instrumentos con transacciones (filtro)", e);
            throw new RuntimeException("Error al cargar filtro de instrumentos", e);
        }
    }
    
    // ========== MÉTODOS DE GESTIÓN ==========
    
    /**
     * Fusiona dos instrumentos
     */
    public void fusionarInstrumentos(Long idInstrumentoAntiguo, Long idInstrumentoNuevo) {
        try {
            fusionInstrumentoService.fusionarYPrepararRecosteo(idInstrumentoAntiguo, idInstrumentoNuevo);
            logger.info("Fusión de instrumentos completada: {} -> {}", idInstrumentoAntiguo, idInstrumentoNuevo);
        } catch (Exception e) {
            logger.error("Error al fusionar instrumentos", e);
            throw new RuntimeException("Error en la fusión de instrumentos: " + e.getMessage(), e);
        }
    }
    
    /**
     * Registra un nuevo usuario
     */
    public UsuarioEntity registrarUsuario(String username, String password, String email) {
        try {
            return usuarioService.registrarNuevoUsuario(username, password, email);
        } catch (Exception e) {
            logger.error("Error al registrar usuario", e);
            throw new RuntimeException("Error al registrar usuario: " + e.getMessage(), e);
        }
    }
    
    // ========== MÉTODOS DE UTILIDAD ==========
    
    /**
     * Verifica la conectividad con la base de datos
     */
    public boolean verificarConectividad() {
        try {
            empresaService.obtenerTodas();
            return true;
        } catch (Exception e) {
            logger.error("Error de conectividad con base de datos", e);
            return false;
        }
    }
    
    /**
     * Obtiene información del sistema para diagnósticos
     */
    public String obtenerInformacionSistema() {
        StringBuilder info = new StringBuilder();
        try {
            int totalEmpresas = empresaService.obtenerTodas().size();
            int empresasConTrx = empresaService.obtenerEmpresasConTransacciones().size();
            
            info.append("Sistema Portafolio - Estado:\n");
            info.append("- Total de empresas: ").append(totalEmpresas).append("\n");
            info.append("- Empresas con transacciones: ").append(empresasConTrx).append("\n");
            info.append("- Conectividad BD: ").append(verificarConectividad() ? "OK" : "ERROR").append("\n");
            info.append("- Fecha: ").append(LocalDate.now()).append("\n");
            
        } catch (Exception e) {
            info.append("Error al obtener información del sistema: ").append(e.getMessage());
        }
        return info.toString();
    }
}