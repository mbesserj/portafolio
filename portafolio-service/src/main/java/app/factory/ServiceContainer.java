package com.app.factory;

import com.app.service.SaldoMensualService;
import com.app.service.CustodioService;
import com.app.service.ResumenHistoricoService;
import com.app.service.FusionInstrumentoService;
import com.app.service.FiltroService;
import com.app.service.ResumenSaldoEmpresaService;
import com.app.service.SaldoActualService;
import com.app.service.ConfrontaService;
import com.app.service.NormalizarService;
import com.app.service.ProcesoCargaDiariaService;
import com.app.service.TipoMovimientosService;
import com.app.service.ResultadoInstrumentoService;
import com.app.service.LimpiezaService;
import com.app.service.EmpresaService;
import com.app.service.UsuarioService;
import com.app.service.ProblemasTrxsService;
import com.app.service.OperacionesTrxsService;
import com.app.service.InstrumentoService;
import com.app.service.PerfilService;
import com.app.service.TransaccionService;
import com.app.service.ResumenPortafolioService;
import com.app.service.AuthenticationService;
import com.app.repositorio.AggregatesForInstrument;
import com.app.repositorio.PrecioRepositoryImpl;
import com.app.repositorio.ResumenHistoricoRepositoryImpl;
import com.app.repositorio.ConfrontaRepositoryImpl;
import com.app.repositorio.ResultadoInstrumentoRepositoryImpl;
import com.app.interfaces.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.costing.api.CostingApiFactory;
import com.costing.api.KardexApiFactory;
import com.costing.api.SaldoApiFactory;

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

    /**
     * Usa las Fábricas de los módulos externos para obtener e instanciar las
     * APIs.
     */
    private void registerApiImplementations() {
        logger.info("Registrando implementaciones de APIs externas usando sus fábricas...");

        try {
            // Llama a la fábrica para construir y obtener la API de Costeo
            serviceCache.put(CostingApi.class, CostingApiFactory.createService());
            logger.info("✓ API de Costing registrada.");
        } catch (Throwable t) {
            logger.warn("✗ No se pudo registrar la API de Costing. Módulo podría faltar. Causa: {}", t.getMessage());
        }

        try {
            // Llama a la fábrica para construir y obtener la API de Kardex
            serviceCache.put(KardexApi.class, KardexApiFactory.createService());
            logger.info("✓ API de Kardex registrada.");
        } catch (Throwable t) {
            logger.warn("✗ No se pudo registrar la API de Kardex. Módulo podría faltar. Causa: {}", t.getMessage());
        }

        try {
            // Llama a la fábrica para construir y obtener la API de Saldos
            serviceCache.put(SaldoApi.class, SaldoApiFactory.createService());
            logger.info("✓ API de Saldos registrada.");
        } catch (Throwable t) {
            logger.warn("✗ No se pudo registrar la API de Saldos. Módulo podría faltar. Causa: {}", t.getMessage());
        }
    }

    private void registerCoreServices() {
        logger.info("Registrando servicios del core...");
        serviceRegistry.put(BCryptPasswordEncoder.class, () -> new BCryptPasswordEncoder(12));

        // ... (registrar repositorios y otros servicios básicos de la UI aquí)
        registerRepository(ConfrontaRepository.class, ConfrontaRepositoryImpl::new);

        // --- Servicios que dependen de otros servicios del core ---
        serviceRegistry.put(AuthenticationService.class, () -> new AuthenticationService(getService(BCryptPasswordEncoder.class)));

        // --- Servicios que dependen de las APIs externas ---
        // Ahora solo declaramos la dependencia y la fábrica, es más simple
        apiDependentServices.put(ResultadoInstrumentoService.class, () -> new ResultadoInstrumentoService(
                getService(SaldoApi.class), getService(KardexApi.class), getService(ResultadoRepository.class)));

        apiDependentServices.put(ResumenPortafolioService.class, () -> new ResumenPortafolioService(
                getService(SaldoApi.class), getService(KardexApi.class), getService(AggregatesForInstrument.class)));

        apiDependentServices.put(SaldoActualService.class, () -> new SaldoActualService(
                getService(KardexApi.class), getService(PrecioRepository.class)));
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

    /**
     * Registra los servicios que dependen de las APIs externas.
     */
    private void registerApiDependentServices() {
        logger.info("Registrando servicios que dependen de APIs externas...");

        registerServiceConditionally(ResultadoInstrumentoService.class, () -> new ResultadoInstrumentoService(
                getService(SaldoApi.class), getService(KardexApi.class), getService(ResultadoRepository.class)));

        registerServiceConditionally(ResumenPortafolioService.class, () -> new ResumenPortafolioService(
                getService(SaldoApi.class), getService(KardexApi.class), getService(AggregatesForInstrument.class)));

        registerServiceConditionally(SaldoActualService.class, () -> new SaldoActualService(
                getService(KardexApi.class), getService(PrecioRepository.class)));
    }

    private void registerServiceConditionally(Class<?> serviceClass, Supplier<?> factory) {
        try {
            // Verificamos que las dependencias existan ANTES de registrar
            // Para ello, intentamos crear una instancia de prueba.
            // Si esto falla, la excepción será capturada y el servicio no se registrará.
            factory.get();

            // Si la línea anterior no lanzó excepción, es seguro registrar la fábrica.
            serviceRegistry.put(serviceClass, factory);
            logger.info("✓ Servicio dependiente registrado: {}", serviceClass.getSimpleName());
        } catch (Exception e) {
            logger.warn("✗ No se pudo registrar el servicio dependiente {}. Causa (probable dependencia de API faltante): {}",
                    serviceClass.getSimpleName(), e.getMessage());
        }
    }

    /**
     * Verifica si un servicio puede ser registrado (todas sus dependencias
     * están disponibles).
     */
    private boolean canRegisterService(ApiDependentServiceConfig config) {
        // Verificar APIs requeridas
        for (Class<?> requiredApi : config.getRequiredApis()) {
            if (!serviceCache.containsKey(requiredApi)) {
                return false;
            }
        }

        // Verificar servicios requeridos
        for (Class<?> requiredService : config.getRequiredServices()) {
            if (!serviceRegistry.containsKey(requiredService)) {
                return false;
            }
        }

        // Verificar interfaces del modelo requeridas
        for (String modelInterface : config.getRequiredModelInterfaces()) {
            if (!isModelInterfaceAvailable(modelInterface)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Verifica si una interfaz del modelo está disponible.
     */
    private boolean isModelInterfaceAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // ============================================================================
    // MÉTODOS FACTORY ESPECÍFICOS
    // ============================================================================
    private AggregatesForInstrument createAggregatesForInstrument() {
        try {
            Class.forName("com.service.repositorio.AggregatesForInstrument");
            return new AggregatesForInstrument();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("AggregatesForInstrument no disponible", e);
        }
    }

    @SuppressWarnings("unchecked")
    private TipoMovimientosService createTipoMovimientosService() {
        try {
            Class<?> tipoMovimientoInterfaz = Class.forName("com.app.interfaces.TipoMovimientoInterfaz");
            Object tipoMovimientoImpl = getOptionalService((Class<Object>) tipoMovimientoInterfaz);

            if (tipoMovimientoImpl == null) {
                throw new RuntimeException("TipoMovimientoInterfaz implementation not available");
            }

            return new TipoMovimientosService((com.app.interfaces.TipoMovimiento) tipoMovimientoImpl);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("TipoMovimientoInterfaz no disponible en el modelo de datos", e);
        }
    }

    // ============================================================================
    // API BUILDER PATTERN
    // ============================================================================
    /**
     * Crea un builder para configurar APIs externas de forma fluida.
     */
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

        /**
         * Registra todas las APIs configuradas y activa los servicios
         * dependientes.
         */
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

    // ============================================================================
    // CLASES DE CONFIGURACIÓN Y RESULTADO
    // ============================================================================
    /**
     * Configuración para servicios que dependen de APIs externas.
     */
    public static class ApiDependentServiceConfig {

        private final Set<Class<?>> requiredApis;
        private final Set<Class<?>> requiredServices;
        private final Set<String> requiredModelInterfaces;
        private final Supplier<?> factory;
        private final String description;

        private ApiDependentServiceConfig(Builder builder) {
            this.requiredApis = Collections.unmodifiableSet(builder.requiredApis);
            this.requiredServices = Collections.unmodifiableSet(builder.requiredServices);
            this.requiredModelInterfaces = Collections.unmodifiableSet(builder.requiredModelInterfaces);
            this.factory = builder.factory;
            this.description = builder.description;
        }

        public static Builder builder() {
            return new Builder();
        }

        public Set<Class<?>> getRequiredApis() {
            return requiredApis;
        }

        public Set<Class<?>> getRequiredServices() {
            return requiredServices;
        }

        public Set<String> getRequiredModelInterfaces() {
            return requiredModelInterfaces;
        }

        public Supplier<?> getFactory() {
            return factory;
        }

        public String getDescription() {
            return description;
        }

        public String getMissingDependenciesMessage() {
            List<String> missing = new ArrayList<>();

            if (!requiredApis.isEmpty()) {
                missing.add("APIs: " + requiredApis.stream()
                        .map(Class::getSimpleName)
                        .collect(Collectors.joining(", ")));
            }
            if (!requiredServices.isEmpty()) {
                missing.add("Services: " + requiredServices.stream()
                        .map(Class::getSimpleName)
                        .collect(Collectors.joining(", ")));
            }
            if (!requiredModelInterfaces.isEmpty()) {
                missing.add("Model Interfaces: " + String.join(", ", requiredModelInterfaces));
            }

            return missing.isEmpty() ? "No dependencies" : "Missing: " + String.join("; ", missing);
        }

        public static class Builder {

            private final Set<Class<?>> requiredApis = new HashSet<>();
            private final Set<Class<?>> requiredServices = new HashSet<>();
            private final Set<String> requiredModelInterfaces = new HashSet<>();
            private Supplier<?> factory;
            private String description = "";

            public Builder requiresApi(Class<?> apiClass) {
                requiredApis.add(apiClass);
                return this;
            }

            public Builder requiresService(Class<?> serviceClass) {
                requiredServices.add(serviceClass);
                return this;
            }

            public Builder requiresModelInterface(String className) {
                requiredModelInterfaces.add(className);
                return this;
            }

            public Builder withFactory(Supplier<?> factory) {
                this.factory = factory;
                return this;
            }

            public Builder withDescription(String description) {
                this.description = description;
                return this;
            }

            public ApiDependentServiceConfig build() {
                if (factory == null) {
                    throw new IllegalStateException("Factory is required");
                }
                return new ApiDependentServiceConfig(this);
            }
        }
    }

    /**
     * Resultado del registro de APIs.
     */
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

    // ============================================================================
    // MÉTODOS ORIGINALES (registerServices, validateDependencies, etc.)
    // ============================================================================
    private void registerServices() {
        logger.info("Registrando servicios en ServiceContainer...");

        // === FASE 1: COMPONENTES BÁSICOS ===
        serviceRegistry.put(BCryptPasswordEncoder.class, () -> new BCryptPasswordEncoder(12));

        // === FASE 2: REPOSITORIOS ===
        registerRepositoriesWithTrycatch();

        // === FASE 3: SERVICIOS SIN DEPENDENCIAS ===
        registerBasicServices();

        // === FASE 4: SERVICIOS CON DEPENDENCIAS ===
        registerDependentServices();

        logger.info("Servicios básicos registrados. Total: {}", serviceRegistry.size());
    }

    private void registerRepositoriesWithTryOptional(Class<?> interfaceClass, Supplier<?> implementation, String name) {
        try {
            serviceRegistry.put(interfaceClass, implementation);
            logger.debug("Registrado: {}", name);
        } catch (Exception e) {
            logger.warn("{} no disponible: {}", name, e.getMessage());
        }
    }

    private void registerRepositoriesWithTrycatch() {
        logger.debug("Registrando repositorios...");

        registerRepository(ConfrontaRepository.class, ConfrontaRepositoryImpl::new);
        registerRepository(ResultadoRepository.class, ResultadoInstrumentoRepositoryImpl::new);
        registerRepository(ResumenHistorico.class, ResumenHistoricoRepositoryImpl::new);
        registerRepository(PrecioRepository.class, PrecioRepositoryImpl::new);

    }

    /**
     * Método auxiliar que registra un único repositorio dentro de un bloque
     * try-catch. Si el repositorio no puede ser instanciado, registra una
     * advertencia en lugar de fallar.
     *
     * @param interfaceClass La clase de la interfaz del repositorio.
     * @param implementationSupplier El proveedor para la implementación del
     * repositorio.
     */
    private void registerRepository(Class<?> interfaceClass, Supplier<?> implementationSupplier) {
        try {
            serviceRegistry.put(interfaceClass, implementationSupplier);
            logger.debug("✓ Repositorio registrado: {}", interfaceClass.getSimpleName());
        } catch (Exception e) {
            // Usamos warn porque la ausencia de un repo puede ser no-crítica
            logger.warn("✗ Repositorio no disponible: {}. Causa: {}",
                    interfaceClass.getSimpleName(), e.getMessage());
        }
    }

    private void registerRepositoriesWithTryOptional() {
        registerRepositoriesWithTryOptional(ConfrontaRepository.class, ConfrontaRepositoryImpl::new, "ConfrontaRepositoryImpl");
        registerRepositoriesWithTryOptional(ResultadoRepository.class, ResultadoInstrumentoRepositoryImpl::new, "ResultadoInstrumentoRepositoryImpl");
        registerRepositoriesWithTryOptional(ResumenHistorico.class, ResumenHistoricoRepositoryImpl::new, "ResumenHistoricoRepositoryImpl");
        registerRepositoriesWithTryOptional(PrecioRepository.class, PrecioRepositoryImpl::new, "PrecioRepositoryImpl");
    }

    private void registerBasicServices() {
        serviceRegistry.put(LimpiezaService.class, LimpiezaService::new);
        serviceRegistry.put(CustodioService.class, CustodioService::new);
        serviceRegistry.put(EmpresaService.class, EmpresaService::new);
        serviceRegistry.put(FiltroService.class, FiltroService::new);
        serviceRegistry.put(FusionInstrumentoService.class, FusionInstrumentoService::new);
        serviceRegistry.put(InstrumentoService.class, InstrumentoService::new);
        serviceRegistry.put(NormalizarService.class, NormalizarService::new);
        serviceRegistry.put(OperacionesTrxsService.class, OperacionesTrxsService::new);
        serviceRegistry.put(PerfilService.class, PerfilService::new);
        serviceRegistry.put(ProblemasTrxsService.class, ProblemasTrxsService::new);
        serviceRegistry.put(ProcesoCargaDiariaService.class, ProcesoCargaDiariaService::new);
        serviceRegistry.put(ResumenSaldoEmpresaService.class, ResumenSaldoEmpresaService::new);
        serviceRegistry.put(SaldoMensualService.class, SaldoMensualService::new);
        serviceRegistry.put(TransaccionService.class, TransaccionService::new);
    }

    private void registerDependentServices() {
        // AuthenticationService - Primera dependencia
        serviceRegistry.put(AuthenticationService.class, () -> new AuthenticationService(
                getService(BCryptPasswordEncoder.class)));

        // UsuarioService - Depende de PerfilService y AuthenticationService  
        serviceRegistry.put(UsuarioService.class, () -> new UsuarioService(
                getService(PerfilService.class),
                getService(AuthenticationService.class)));

        // Servicios condicionales
        registerConditionalService(ConfrontaRepository.class, ConfrontaService.class,
                () -> new ConfrontaService(getService(ConfrontaRepository.class)));

        registerConditionalService(ResumenHistorico.class, ResumenHistoricoService.class,
                () -> new ResumenHistoricoService(getService(ResumenHistorico.class)));
    }

    private void registerConditionalService(Class<?> dependency, Class<?> serviceClass, Supplier<?> factory) {
        if (serviceRegistry.containsKey(dependency)) {
            serviceRegistry.put(serviceClass, factory);
        }
    }

    /**
     * MÉTODO PÚBLICO MEJORADO: Registra APIs externas de forma más limpia
     */
    public void registerExternalApis(KardexApi kardexApi,
            SaldoApi saldoApi,
            CostingApi costingApi) {
        configureExternalApis()
                .withKardexApi(kardexApi)
                .withSaldoApi(saldoApi)
                .withCostingApi(costingApi)
                .register();
    }

    // Resto de métodos originales...
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
        private final java.util.List<String> availableServices;

        public ServiceContainerStats(int totalRegistered, int totalInstantiated,
                java.util.List<String> availableServices) {
            this.totalRegistered = totalRegistered;
            this.totalInstantiated = totalInstantiated;
            this.availableServices = availableServices;
        }

        public int getTotalRegistered() {
            return totalRegistered;
        }

        public int getTotalInstantiated() {
            return totalInstantiated;
        }

        public java.util.List<String> getAvailableServices() {
            return availableServices;
        }

        @Override
        public String toString() {
            return String.format("ServiceContainer Stats: %d registered, %d instantiated",
                    totalRegistered, totalInstantiated);
        }
    }

    /**
     * Apaga todos los servicios instanciados que implementen AutoCloseable.
     * Este método debe ser llamado al cerrar la aplicación para liberar
     * recursos.
     */
    public void shutdown() {
        logger.info("Iniciando secuencia de apagado de servicios...");

        // Iteramos sobre todos los servicios que ya fueron creados y están en el caché
        for (Object serviceInstance : serviceCache.values()) {

            // Verificamos si el servicio es "cerrable"
            if (serviceInstance instanceof AutoCloseable) {
                try {
                    logger.debug("Cerrando servicio: {}", serviceInstance.getClass().getSimpleName());
                    // Llamamos a su método close()
                    ((AutoCloseable) serviceInstance).close();
                } catch (Exception e) {
                    // Capturamos errores para que el fallo de un servicio no impida que otros se cierren
                    logger.error("Error al cerrar el servicio {}: {}",
                            serviceInstance.getClass().getSimpleName(), e.getMessage(), e);
                }
            }
        }

        // Opcional: Limpiar los mapas para liberar memoria
        serviceCache.clear();
        serviceRegistry.clear();

        logger.info("Secuencia de apagado de servicios completada.");
    }
}
