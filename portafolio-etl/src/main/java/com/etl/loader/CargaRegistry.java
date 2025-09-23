package com.etl.loader;

import com.etl.interfaz.AbstractCargaProcessor;
import com.etl.interfaz.CargaMapperInterfaz;
import java.util.HashMap;
import java.util.Map;

public class CargaRegistry {

    private final Map<String, CargaMapperInterfaz<?>> mappers = new HashMap<>();
    private final Map<String, AbstractCargaProcessor<?>> processors = new HashMap<>();

    public void register(String key, CargaMapperInterfaz<?> mapper, AbstractCargaProcessor<?> processor) {
        mappers.put(key, mapper);
        processors.put(key, processor);
    }

    public CargaMapperInterfaz<?> getMapper(String key) {
        return mappers.get(key);
    }

    public AbstractCargaProcessor<?> getProcessor(String key) {
        return processors.get(key);
    }

    public Map<String, CargaMapperInterfaz<?>> getMappers() {
        return mappers;
    }

    public Map<String, AbstractCargaProcessor<?>> getProcessors() {
        return processors;
    }
}