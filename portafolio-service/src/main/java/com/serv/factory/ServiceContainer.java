package com.serv.factory;

import com.serv.repositorio.*;
import com.serv.service.*;
import com.model.interfaces.*;

import com.costing.api.CostingApiFactory;
import com.serv.repositorio.KardexApiFactory;
import com.serv.repositorio.SaldoApiFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class ServiceContainer {

    private static final Logger logger = LoggerFactory.getLogger(ServiceContainer.class);
    private static volatile ServiceContainer instance;

    private final Map<Class<?>, Object> serviceCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, Supplier<?>> serviceRegistry = new ConcurrentHashMap<>();
    private final Map<Class<?>, Supplier<?>> apiDependentServices = new ConcurrentHashMap<>();

    private ServiceContainer() {
        registerCoreServices();
        registerApiImplementations();
        registerApiDependentServices();
    }

    public static ServiceContainer getInstance() {
        if (instance == null) {
            synchronized (ServiceContainer.class) {
                if (instance == null) {
                    instance = new ServiceContainer();
                }
            }
        }
        return instance;
    }

    public <T> T getService(Class<T> serviceClass) {
        return (T) serviceCache.computeIfAbsent(serviceClass, key -> {
            Supplier<?> serviceSupplier = serviceRegistry.get(key);
            if (serviceSupplier == null) {
                throw new IllegalArgumentException("Servicio no registrado: " + key.getSimpleName());
            }
            logger.info("Creando nueva instancia para: {}", key.getSimpleName());
            return serviceSupplier.get();
        });
    }

    private void registerApiImplementations() {
        logger.info("Registrando implementaciones de APIs externas usando sus fábricas...");

        try {
            serviceCache.put(CostingApi.class, CostingApiFactory.createService());
            logger.info("✓ API de Costing registrada.");
        } catch (Throwable t) {
            logger.warn("✗ No se pudo registrar la API de Costing. Módulo podría faltar. Causa: {}", t.getMessage());
        }

        try {
            serviceCache.put(KardexApi.class, KardexApiFactory.createService());
            logger.info("✓ API de Kardex registrada.");
        } catch (Throwable t) {
            logger.warn("✗ No se pudo registrar la API de Kardex. Módulo podría faltar. Causa: {}", t.getMessage());
        }

        try {
            serviceCache.put(SaldoApi.class, SaldoApiFactory.createService());
            logger.info("✓ API de Saldos registrada.");
        } catch (Throwable t) {
            logger.warn("✗ No se pudo registrar la API de Saldos. Módulo podría faltar. Causa: {}", t.getMessage());
        }
    }

    private void registerCoreServices() {
        logger.info("Registrando servicios del core...");

        // === COMPONENTES BÁSICOS ===
        serviceRegistry.put(BCryptPasswordEncoder.class, () -> new BCryptPasswordEncoder(12));

        // === REPOSITORIOS ===
        registerRepository(ConfrontaRepository.class, ConfrontaRepositoryImpl::new);
        registerRepository(PrecioRepository.class, PrecioRepositoryImpl::new);
        registerRepository(ResultadoRepository.class, ResultadoInstrumentoRepositoryImpl::new);
        registerRepository(ResumenHistorico.class, ResumenHistoricoRepositoryImpl::new);

        // === SERVICIOS BÁSICOS (sin duplicaciones) ===
        serviceRegistry.put(EmpresaService.class, EmpresaService::new);
        serviceRegistry.put(CustodioService.class, CustodioService::new);
        serviceRegistry.put(InstrumentoService.class, InstrumentoService::new);
        serviceRegistry.put(TransaccionService.class, TransaccionService::new);
        serviceRegistry.put(FusionInstrumentoService.class, FusionInstrumentoService::new);
        serviceRegistry.put(LimpiezaService.class, LimpiezaService::new);
        serviceRegistry.put(NormalizarService.class, NormalizarService::new);
        serviceRegistry.put(OperacionesTrxsService.class, OperacionesTrxsService::new);
        serviceRegistry.put(ProblemasTrxsService.class, ProblemasTrxsService::new);
        serviceRegistry.put(ProcesoCargaDiariaService.class, ProcesoCargaDiariaService::new);
        serviceRegistry.put(FiltroService.class, FiltroService::new);
        serviceRegistry.put(AggregatesForInstrument.class, AggregatesForInstrument::new);
        
        // SERVICIOS FALTANTES AGREGADOS:
        serviceRegistry.put(SaldoMensualService.class, SaldoMensualService::new);
        serviceRegistry.put(ResumenSaldoEmpresaService.class, ResumenSaldoEmpresaService::new);

        // === SERVICIOS CON DEPENDENCIAS SIMPLES ===
        serviceRegistry.put(PerfilService.class, PerfilService::new);
        
        serviceRegistry.put(AuthenticationService.class, () -> new AuthenticationService(
                getService(BCryptPasswordEncoder.class)));

        serviceRegistry.put(UsuarioService.class, () -> new UsuarioService(
                getService(PerfilService.class),
                getService(AuthenticationService.class)));

        // === SERVICIOS CONDICIONALES ===
        registerConditionalService(ConfrontaRepository.class, ConfrontaService.class,
                () -> new ConfrontaService(getService(ConfrontaRepository.class)));
        registerConditionalService(ResumenHistorico.class, ResumenHistoricoService.class,
                () -> new ResumenHistoricoService(getService(ResumenHistorico.class)));

        // === SERVICIOS QUE DEPENDEN DE INTERFACES DEL MODELO (CONDICIONALES) ===
        registerModelDependentService(TipoMovimientosService.class, "com.model.interfaces.TipoMovimiento",
                () -> {
                    try {
                        Class<?> tipoMovimientoInterfaz = Class.forName("com.model.interfaces.TipoMovimiento");
                        Object tipoMovimientoImpl = getOptionalService((Class<Object>) tipoMovimientoInterfaz);
                        if (tipoMovimientoImpl == null) {
                            throw new RuntimeException("TipoMovimiento implementation not available");
                        }
                        return new TipoMovimientosService((TipoMovimiento) tipoMovimientoImpl);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("TipoMovimiento interface no disponible", e);
                    }
                });

        logger.info("Servicios del core registrados exitosamente");
    }

    private void registerApiDependentServices() {
        logger.info("Registrando servicios que dependen de APIs externas...");

        // Configurar servicios dependientes de APIs
        apiDependentServices.put(ResultadoInstrumentoService.class, () -> new ResultadoInstrumentoService(
                getService(SaldoApi.class), 
                getService(KardexApi.class), 
                getService(ResultadoRepository.class)));

        apiDependentServices.put(ResumenPortafolioService.class, () -> new ResumenPortafolioService(
                getService(SaldoApi.class), 
                getService(KardexApi.class), 
                getService(AggregatesForInstrument.class)));

        apiDependentServices.put(SaldoActualService.class, () -> new SaldoActualService(
                getService(KardexApi.class), 
                getService(PrecioRepository.class)));

        // AGREGAR ProcesoCargaInicialService (corregido)
        apiDependentServices.put(ProcesoCargaInicialService.class, () -> new ProcesoCargaInicialService(
                getService(CostingApi.class), 
                getService(SaldoApi.class), 
                getService(KardexApi.class)));

        // AGREGAR ProcesoCosteoInicialService (faltaba)
        apiDependentServices.put(ProcesoCosteoInicialService.class, () -> new ProcesoCosteoInicialService(
                getService(CostingApi.class), 
                getService(SaldoApi.class)));

        // Registrar servicios si sus dependencias están disponibles
        registerServiceConditionally(ResultadoInstrumentoService.class, 
            apiDependentServices.get(ResultadoInstrumentoService.class));
            
        registerServiceConditionally(ResumenPortafolioService.class, 
            apiDependentServices.get(ResumenPortafolioService.class));
            
        registerServiceConditionally(SaldoActualService.class, 
            apiDependentServices.get(SaldoActualService.class));
            
        registerServiceConditionally(ProcesoCargaInicialService.class, 
            apiDependentServices.get(ProcesoCargaInicialService.class));

        registerServiceConditionally(ProcesoCosteoInicialService.class, 
            apiDependentServices.get(ProcesoCosteoInicialService.class));
    }

    private void registerServiceConditionally(Class<?> serviceClass, Supplier<?> factory) {
        try {
            // Verificamos que las dependencias existan ANTES de registrar
            factory.get();
            // Si la línea anterior no lanzó excepción, es seguro registrar la fábrica
            serviceRegistry.put(serviceClass, factory);
            logger.info("✓ Servicio dependiente registrado: {}", serviceClass.getSimpleName());
        } catch (Exception e) {
            logger.warn("✗ No se pudo registrar el servicio dependiente {}. Causa: {}",
                    serviceClass.getSimpleName(), e.getMessage());
        }
    }

    private void registerModelDependentService(Class<?> serviceClass, String requiredInterface, Supplier<?> factory) {
        try {
            // Verificar que la interfaz del modelo existe
            Class.forName(requiredInterface);
            // Intentar crear el servicio
            factory.get();
            serviceRegistry.put(serviceClass, factory);
            logger.info("✓ Servicio dependiente del modelo registrado: {}", serviceClass.getSimpleName());
        } catch (ClassNotFoundException e) {
            logger.warn("✗ Interfaz del modelo no disponible para {}: {}", 
                       serviceClass.getSimpleName(), requiredInterface);
        } catch (Exception e) {
            logger.warn("✗ No se pudo registrar servicio dependiente del modelo {}: {}", 
                       serviceClass.getSimpleName(), e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getOptionalService(Class<T> serviceClass) {
        try {
            return getService(serviceClass);
        } catch (IllegalArgumentException e) {
            logger.warn("Servicio opcional no disponible: {}", serviceClass.getSimpleName());
            return null;
        }
    }

    private void registerRepository(Class<?> interfaceClass, Supplier<?> implementationSupplier) {
        try {
            serviceRegistry.put(interfaceClass, implementationSupplier);
            logger.debug("✓ Repositorio registrado: {}", interfaceClass.getSimpleName());
        } catch (Exception e) {
            logger.warn("✗ Repositorio no disponible: {}. Causa: {}",
                    interfaceClass.getSimpleName(), e.getMessage());
        }
    }

    private void registerConditionalService(Class<?> dependency, Class<?> serviceClass, Supplier<?> factory) {
        if (serviceRegistry.containsKey(dependency)) {
            serviceRegistry.put(serviceClass, factory);
            logger.debug("✓ Servicio condicional registrado: {}", serviceClass.getSimpleName());
        } else {
            logger.debug("✗ Servicio condicional no registrado (dependencia faltante): {}", serviceClass.getSimpleName());
        }
    }

    // ============================================================================
    // API BUILDER PATTERN (mantener existente)
    // ============================================================================
    public ApiBuilder configureExternalApis() {
        return new ApiBuilder(this);
    }

    public static class ApiBuilder {
        private final ServiceContainer container;
        private final Map<Class<?>, Object> apisToRegister = new HashMap<>();

        ApiBuilder(ServiceContainer container) {
            this.container = container;
        }

        public ApiBuilder withKardexApi(KardexApi api) {
            apisToRegister.put(KardexApi.class, api);
            return this;
        }

        public ApiBuilder withSaldoApi(SaldoApi api) {
            apisToRegister.put(SaldoApi.class, api);
            return this;
        }

        public ApiBuilder withCostingApi(CostingApi api) {
            apisToRegister.put(CostingApi.class, api);
            return this;
        }

        public ApiRegistrationResult register() {
            int apisRegistered = 0;
            List<String> errors = new ArrayList<>();

            for (Map.Entry<Class<?>, Object> entry : apisToRegister.entrySet()) {
                try {
                    container.serviceCache.put(entry.getKey(), entry.getValue());
                    logger.info("API registrada: {}", entry.getKey().getSimpleName());
                    apisRegistered++;
                } catch (Exception e) {
                    String error = String.format("Error registrando %s: %s",
                            entry.getKey().getSimpleName(), e.getMessage());
                    errors.add(error);
                    logger.error(error);
                }
            }

            // Intentar registrar servicios dependientes después de registrar las APIs
            container.registerApiDependentServices();

            return new ApiRegistrationResult(apisRegistered, errors);
        }
    }

    public static class ApiRegistrationResult {
        private final int apisRegistered;
        private final List<String> errors;

        public ApiRegistrationResult(int apisRegistered, List<String> errors) {
            this.apisRegistered = apisRegistered;
            this.errors = Collections.unmodifiableList(errors);
        }

        public boolean isSuccess() {
            return errors.isEmpty();
        }

        public int getApisRegistered() {
            return apisRegistered;
        }

        public List<String> getErrors() {
            return errors;
        }

        @Override
        public String toString() {
            return String.format("APIs registered: %d, Errors: %d", apisRegistered, errors.size());
        }
    }

    public void registerExternalApis(KardexApi kardexApi, SaldoApi saldoApi, CostingApi costingApi) {
        configureExternalApis()
                .withKardexApi(kardexApi)
                .withSaldoApi(saldoApi)
                .withCostingApi(costingApi)
                .register();
    }

    // ============================================================================
    // MÉTODOS DE UTILIDAD Y DIAGNÓSTICO
    // ============================================================================
    public void validateDependencies() {
        logger.info("=== VALIDANDO DEPENDENCIAS ===");
        serviceRegistry.forEach((serviceClass, supplier) -> {
            try {
                Object instance = supplier.get();
                logger.info("✓ {} - OK", serviceClass.getSimpleName());
            } catch (Exception e) {
                logger.error("✗ {} - ERROR: {}", serviceClass.getSimpleName(), e.getMessage());
            }
        });
        logger.info("=== VALIDACIÓN COMPLETADA ===");
    }

    public void listRegisteredServices() {
        logger.info("=== SERVICIOS REGISTRADOS ({}) ===", serviceRegistry.size());
        serviceRegistry.keySet().stream()
                .map(Class::getSimpleName)
                .sorted()
                .forEach(name -> logger.info("- {}", name));

        logger.info("=== SERVICIOS INSTANCIADOS ({}) ===", serviceCache.size());
        serviceCache.keySet().stream()
                .map(Class::getSimpleName)
                .sorted()
                .forEach(name -> logger.info("- {}", name));
    }

    public void clearCache() {
        serviceCache.clear();
        logger.info("Cache de servicios limpiado");
    }

    public ServiceContainerStats getStats() {
        return new ServiceContainerStats(
                serviceRegistry.size(),
                serviceCache.size(),
                serviceRegistry.keySet().stream()
                        .map(Class::getSimpleName)
                        .sorted()
                        .collect(Collectors.toList())
        );
    }

    public static class ServiceContainerStats {
        private final int totalRegistered;
        private final int totalInstantiated;
        private final List<String> availableServices;

        public ServiceContainerStats(int totalRegistered, int totalInstantiated, List<String> availableServices) {
            this.totalRegistered = totalRegistered;
            this.totalInstantiated = totalInstantiated;
            this.availableServices = availableServices;
        }

        public int getTotalRegistered() { return totalRegistered; }
        public int getTotalInstantiated() { return totalInstantiated; }
        public List<String> getAvailableServices() { return availableServices; }

        @Override
        public String toString() {
            return String.format("ServiceContainer Stats: %d registered, %d instantiated", 
                totalRegistered, totalInstantiated);
        }
    }

    public void shutdown() {
        logger.info("Iniciando secuencia de apagado de servicios...");

        for (Object serviceInstance : serviceCache.values()) {
            if (serviceInstance instanceof AutoCloseable) {
                try {
                    logger.debug("Cerrando servicio: {}", serviceInstance.getClass().getSimpleName());
                    ((AutoCloseable) serviceInstance).close();
                } catch (Exception e) {
                    logger.error("Error al cerrar el servicio {}: {}",
                            serviceInstance.getClass().getSimpleName(), e.getMessage(), e);
                }
            }
        }

        serviceCache.clear();
        serviceRegistry.clear();

        logger.info("Secuencia de apagado de servicios completada.");
    }
}