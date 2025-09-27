package com.app.factory;

import com.app.interfaces.*;
import com.app.repositorio.*;
import com.app.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Contenedor de servicios unificado que gestiona el ciclo de vida y las
 * dependencias de los componentes de la aplicación.
 *
 * Implementa un registro de servicios para evitar grandes bloques if-else,
 * adhiriendo al Principio Abierto/Cerrado.
 */
public final class ServiceContainer {

    private static final Logger logger = LoggerFactory.getLogger(ServiceContainer.class);
    private static volatile ServiceContainer instance;

    // Un único caché para todas las instancias de servicios (Singletons)
    private final Map<Class<?>, Object> serviceCache = new ConcurrentHashMap<>();

    // CAMBIO 1: El "Registro de Recetas". Un mapa que sabe cómo crear cada servicio.
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
        // Primero intenta obtener la instancia desde el caché (el Singleton).
        return (T) serviceCache.computeIfAbsent(serviceClass, key -> {
            // Si no está en el caché, la crea usando la receta del registro.
            Supplier<?> serviceSupplier = serviceRegistry.get(key);
            if (serviceSupplier == null) {
                throw new IllegalArgumentException("No hay una receta de creación para el servicio: " + key.getName());
            }
            logger.info("Creando nueva instancia para el servicio: {}", key.getSimpleName());
            return serviceSupplier.get();
        });
    }

    /**
     * Aquí es donde se define el "grafo de dependencias" de toda la aplicación.
     * Este es el único lugar que se modifica al añadir un nuevo servicio.
     */
    private void registerServices() {

        // --- Registro de Repositorios (Dependencias) ---

        serviceRegistry.put(ResumenHistoricoService.class, () -> new ResumenHistoricoService(getService(ResumenHistoricoInterfaz.class)));
        serviceRegistry.put(ConfrontaService.class, () -> new ConfrontaService(getService(ConfrontaRepository.class)));

        serviceRegistry.put(ResultadoInstrumentoService.class, () -> new ResultadoInstrumentoService(
                getService(SaldoApiInterfaz.class),
                getService(KardexApiInterfaz.class),
                getService(ResultadoRepository.class)));

        serviceRegistry.put(ResumenPortafolioService.class, () -> new ResumenPortafolioService(
                getService(SaldoApiInterfaz.class),
                getService(KardexApiInterfaz.class),
                getService(AggregatesForInstrument.class)));

        serviceRegistry.put(SaldoActualService.class, () -> new SaldoActualService(
                getService(KardexApiInterfaz.class),
                getService(PrecioRepository.class)));

        serviceRegistry.put(UsuarioService.class, () -> new UsuarioService(
                getService(PerfilService.class),
                getService(AuthenticationService.class) 
        ));

        serviceRegistry.put(TipoMovimientosService.class, () -> new TipoMovimientosService(
                getService(TipoMovimientoInterfaz.class
                )));

        // ---  Servicios sin dependencias  ---
        
        serviceRegistry.put(ConfrontaRepository.class, ConfrontaRepositoryImpl::new);
        serviceRegistry.put(ResultadoRepository.class, ResultadoInstrumentoRepositoryImpl::new);
        serviceRegistry.put(LimpiezaService.class, LimpiezaService::new);
        serviceRegistry.put(CustodioService.class, CustodioService::new);
        serviceRegistry.put(EmpresaService.class, EmpresaService::new);
        serviceRegistry.put(FiltroService.class, FiltroService::new);
        serviceRegistry.put(FusionInstrumentoService.class, FusionInstrumentoService::new);
        serviceRegistry.put(NormalizarService.class, NormalizarService::new);
        serviceRegistry.put(OperacionesTrxsService.class, OperacionesTrxsService::new);
        serviceRegistry.put(PerfilService.class, PerfilService::new);
        serviceRegistry.put(ProblemasTrxsService.class, ProblemasTrxsService::new);
        serviceRegistry.put(ProcesoCargaDiariaService.class, ProcesoCargaDiariaService::new);
        serviceRegistry.put(ResumenSaldoEmpresaService.class, ResumenSaldoEmpresaService::new);
        serviceRegistry.put(BCryptPasswordEncoder.class, BCryptPasswordEncoder::new);
        serviceRegistry.put(SaldoMensualService.class, SaldoMensualService::new);
        serviceRegistry.put(TransaccionService.class, TransaccionService::new);

    }
}