module com.app.portafolio.service {
    // --- DEPENDENCIAS DE MÓDulos DEL PROYECTO ---
    requires transitive com.app.portafolio.model; 
    
    requires com.app.portafolio.costing;
    requires com.app.portafolio.etl;
    requires com.app.portafolio.normalizar;

    // --- DEPENDENCIAS DE LIBRERÍAS ---
    requires jakarta.persistence;
    requires org.slf4j;
    requires spring.security.core;
    requires spring.security.crypto;

    // para que la UI pueda usar la ServiceFactory.
    exports com.service.service;
    exports com.service.repositorio;
    exports com.service.factory;
    exports com.service.interfaces;
    exports com.service.config;
    exports com.service.sql;
}

