package com.app.factory;

import com.app.interfaces.*;
import com.app.repositorio.*;
import com.app.service.*;
import com.costing.api.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class ServiceContainer {

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
            Supplier<?> supplier = serviceRegistry.get(key);
            if (supplier == null) {
                throw new IllegalArgumentException("Servicio no registrado: " + key.getName());
            }
            return supplier.get();
        });
    }

    private void registerServices() {
        // --- Dependencias Fundamentales ---
        serviceRegistry.put(BCryptPasswordEncoder.class, BCryptPasswordEncoder::new);

        // --- APIs de Costing ---
        serviceRegistry.put(CostingApiInterfaz.class, CostingApi::createService);
        serviceRegistry.put(KardexApiInterfaz.class, KardexApi::createService);
        serviceRegistry.put(SaldoApiInterfaz.class, SaldoApi::createService);

        // --- Repositorios y Clases de Datos ---
        serviceRegistry.put(AggregatesForInstrument.class, AggregatesForInstrument::new);
        serviceRegistry.put(ConfrontaRepository.class, ConfrontaRepositoryImpl::new); // Ejemplo

        // --- Servicios Simples (sin dependencias inyectadas) ---
        serviceRegistry.put(EmpresaService.class, EmpresaService::new);
        serviceRegistry.put(CustodioService.class, CustodioService::new);
        serviceRegistry.put(InstrumentoService.class, InstrumentoService::new);
        serviceRegistry.put(PerfilService.class, PerfilService::new);
        // ... registra aquí todos tus servicios simples ...
    }
}
