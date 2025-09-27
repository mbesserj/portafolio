package com.service.factory;

import com.app.interfaces.*;
import com.service.repositorio.*;
import com.service.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class ServiceContainer {

    private static final Logger logger = LoggerFactory.getLogger(ServiceContainer.class);
    private static volatile ServiceContainer instance;

    private final Map<Class<?>, Object> serviceCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, Supplier<?>> serviceRegistry = new ConcurrentHashMap<>();

    private ServiceContainer() {
        registerServices();
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

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass) {
        return (T) serviceCache.computeIfAbsent(serviceClass, key -> {
            Supplier<?> serviceSupplier = serviceRegistry.get(key);
            if (serviceSupplier == null) {
                String availableServices = serviceRegistry.keySet().stream()
                        .map(Class::getSimpleName)
                        .sorted()
                        .collect(Collectors.joining(", "));
                throw new IllegalArgumentException(
                        String.format("Servicio no registrado: %s. Disponibles: [%s]",
                                key.getSimpleName(), availableServices));
            }
            logger.info("Creando nueva instancia para: {}", key.getSimpleName());
            return serviceSupplier.get();
        });
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

    private void registerServices() {
        logger.info("Registrando servicios en ServiceContainer...");

        // === FASE 1: COMPONENTES BÁSICOS ===
        serviceRegistry.put(BCryptPasswordEncoder.class, () -> new BCryptPasswordEncoder(12));

        // === FASE 2: REPOSITORIOS ===
        // Solo registrar si las implementaciones existen
        try {
            serviceRegistry.put(ConfrontaRepository.class, ConfrontaRepositoryImpl::new);
            logger.debug("Registrado: ConfrontaRepositoryImpl");
        } catch (Exception e) {
            logger.warn("ConfrontaRepositoryImpl no disponible: {}", e.getMessage());
        }

        try {
            serviceRegistry.put(ResultadoRepository.class, ResultadoInstrumentoRepositoryImpl::new);
            logger.debug("Registrado: ResultadoInstrumentoRepositoryImpl");
        } catch (Exception e) {
            logger.warn("ResultadoInstrumentoRepositoryImpl no disponible: {}", e.getMessage());
        }

        try {
            serviceRegistry.put(ResumenHistoricoInterfaz.class, ResumenHistoricoRepositoryImpl::new);
            logger.debug("Registrado: ResumenHistoricoRepositoryImpl");
        } catch (Exception e) {
            logger.warn("ResumenHistoricoRepositoryImpl no disponible: {}", e.getMessage());
        }

        try {
            serviceRegistry.put(PrecioRepository.class, PrecioRepositoryImpl::new);
            logger.debug("Registrado: PrecioRepositoryImpl");
        } catch (Exception e) {
            logger.warn("PrecioRepositoryImpl no disponible: {}", e.getMessage());
        }

        // === FASE 3: SERVICIOS SIN DEPENDENCIAS ===
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

        // === FASE 4: SERVICIOS CON DEPENDENCIAS ===
        // AuthenticationService - Primera dependencia
        serviceRegistry.put(AuthenticationService.class, () -> new AuthenticationService(
                getService(BCryptPasswordEncoder.class)));

        // UsuarioService - Depende de PerfilService y AuthenticationService  
        serviceRegistry.put(UsuarioService.class, () -> new UsuarioService(
                getService(PerfilService.class),
                getService(AuthenticationService.class)));

        // ConfrontaService - Solo si el repositorio está disponible
        if (serviceRegistry.containsKey(ConfrontaRepository.class)) {
            serviceRegistry.put(ConfrontaService.class, () -> new ConfrontaService(
                    (ConfrontaRepository) getService(ConfrontaRepository.class)));
        }

        // ResumenHistoricoService - Solo si la interfaz está disponible
        if (serviceRegistry.containsKey(ResumenHistoricoInterfaz.class)) {
            serviceRegistry.put(ResumenHistoricoService.class, () -> new ResumenHistoricoService(
                    getService(ResumenHistoricoInterfaz.class)));
        }

        // === FASE 5: SERVICIOS CON APIS EXTERNAS (OPCIONALES) ===
        // Estos servicios dependen de APIs externas que pueden no estar disponibles
        // NOTA: Estos servicios se registrarán solo si las APIs están configuradas
        // externamente o si las implementaciones existen
        logger.info("Servicios básicos registrados. Total: {}", serviceRegistry.size());
    }

    /**
     * Registra las APIs externas después de que estén configuradas
     */
    public void registerExternalApis(KardexApiInterfaz kardexApi,
            SaldoApiInterfaz saldoApi,
            CostingApiInterfaz costingApi) {
        if (kardexApi != null) {
            serviceCache.put(KardexApiInterfaz.class, kardexApi);
            logger.info("KardexApiInterfaz registrada externamente");
        }

        if (saldoApi != null) {
            serviceCache.put(SaldoApiInterfaz.class, saldoApi);
            logger.info("SaldoApiInterfaz registrada externamente");
        }

        if (costingApi != null) {
            serviceCache.put(CostingApiInterfaz.class, costingApi);
            logger.info("CostingApiInterfaz registrada externamente");
        }

        // Ahora podemos registrar servicios que dependen de estas APIs
        registerApiDependentServices();
    }

    private void registerApiDependentServices() {
        // AggregatesForInstrument - Si existe la implementación
        try {
            Class.forName("com.service.repositorio.AggregatesForInstrument");
            serviceRegistry.put(AggregatesForInstrument.class, AggregatesForInstrument::new);
            logger.debug("Registrado: AggregatesForInstrument");
        } catch (ClassNotFoundException e) {
            logger.warn("AggregatesForInstrument no disponible");
        }

        // ResultadoInstrumentoService - Requiere SaldoApi, KardexApi y ResultadoInsInterfaz
        if (serviceCache.containsKey(SaldoApiInterfaz.class)
                && serviceCache.containsKey(KardexApiInterfaz.class)
                && serviceRegistry.containsKey(ResultadoRepository.class)) {

            serviceRegistry.put(ResultadoInstrumentoService.class, () -> new ResultadoInstrumentoService(
                    getService(SaldoApiInterfaz.class),
                    getService(KardexApiInterfaz.class),
                    getService(ResultadoRepository.class)));
            logger.debug("Registrado: ResultadoInstrumentoService");
        }

        // ResumenPortafolioService - Requiere SaldoApi, KardexApi y AggregatesForInstrument
        if (serviceCache.containsKey(SaldoApiInterfaz.class)
                && serviceCache.containsKey(KardexApiInterfaz.class)
                && serviceRegistry.containsKey(AggregatesForInstrument.class)) {

            serviceRegistry.put(ResumenPortafolioService.class, () -> new ResumenPortafolioService(
                    getService(SaldoApiInterfaz.class),
                    getService(KardexApiInterfaz.class),
                    getService(AggregatesForInstrument.class)));
            logger.debug("Registrado: ResumenPortafolioService");
        }

        // SaldoActualService - Requiere KardexApi y PrecioRepInterfaz
        if (serviceCache.containsKey(KardexApiInterfaz.class)
                && serviceRegistry.containsKey(PrecioRepository.class)) {

            serviceRegistry.put(SaldoActualService.class, () -> new SaldoActualService(
                    getService(KardexApiInterfaz.class),
                    getService(PrecioRepository.class)));
            logger.debug("Registrado: SaldoActualService");
        }

        // TipoMovimientosService - Solo si TipoMovimientoInterfaz está disponible
        try {
            // Intentamos verificar si existe la interfaz en el modelo de datos
            Class<?> tipoMovimientoInterfaz = Class.forName("com.app.interfaces.TipoMovimientoInterfaz");
            serviceRegistry.put(TipoMovimientosService.class, () -> new TipoMovimientosService(
                    (com.app.interfaces.TipoMovimientoInterfaz) getOptionalService(
                            (Class<com.app.interfaces.TipoMovimientoInterfaz>) tipoMovimientoInterfaz)));
            logger.debug("Registrado: TipoMovimientosService");
        } catch (ClassNotFoundException e) {
            logger.warn("TipoMovimientoInterfaz no disponible en el modelo de datos");
        }
    }

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
}
