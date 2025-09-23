/**
 * Módulo para los procesos de Extracción, Transformación y Carga (ETL).
 */
module com.app.portafolio.etl {
    // 1. Depende del modelo para crear las entidades.
    requires com.app.portafolio.model;
    requires org.slf4j;
    requires jakarta.persistence;

    // Declara que este módulo necesita leer las librerías de POI.
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;


    // 2. EXPORTA su paquete de servicios.
    exports com.etl.service;
}

