package com.app.service;

// --- Imports de Repositorios (Interfaces del Módulo Model) ---
import com.app.repository.PrecioRepository;

// --- Imports de Servicios y Repositorios (Implementaciones de otros Módulos) ---
import com.app.repositorio.*; // Importa todas tus implementaciones

/**
 * Fábrica central y definitiva. Responsable de instanciar e inyectar todas las
 * dependencias (Repositorios y Servicios). Actúa como la fachada principal para
 * la capa de UI.
 */
public class ServiceFactory {

    // --- FASE 1: Creación de instancias de Repositorios ---
    // Se crean las implementaciones concretas. El resto de la aplicación solo conocerá las interfaces.
    private final PrecioRepository precioService = new PrecioRepositoryImpl();
    
    // --- FASE 2: Creación de instancias de Servicios (con inyección de dependencias) ---
    // Servicios del módulo 'cost' que dependen de repositorios

    // Servicios del módulo 'etl'
    private final ProcesoCargaDiaria procesoCargaDiaria = new ProcesoCargaDiaria();
    private final ProcesoCargaInicial procesoCargaInicial = new ProcesoCargaInicial();
    private final ProcesoCosteoInicial procesoCosteoInicial = new ProcesoCosteoInicial();

    // Servicios que viven en este mismo módulo ('service')
    private final CustodioService custodioService = new CustodioService();
    private final EmpresaService empresaService = new EmpresaService();
    private final InstrumentoService instrumentoService = new InstrumentoService();
    private final NormalizarService normalizarService = new NormalizarService();
    private final ResumenSaldoEmpresaService resumenSaldosService = new ResumenSaldoEmpresaService();
    private final ConfrontaService confrontaService = new ConfrontaService();
    
    // --- FASE 3: Getters públicos para que la UI pueda acceder a los servicios ---
    public ProcesoCargaDiaria getProcesoCargaDiaria() {
        return procesoCargaDiaria;
    }

    public ProcesoCargaInicial getProcesoCargaInicial() {
        return procesoCargaInicial;
    }

    public ProcesoCosteoInicial getProcesoCosteoInicial() {
        return procesoCosteoInicial;
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

    public NormalizarService getNormalizarService() {
        return normalizarService;
    }

    public ResumenSaldoEmpresaService getResumenSaldosService() {
        return resumenSaldosService;
    }

    public ConfrontaService getConfrontaService() {
        return confrontaService;
    }
    
}