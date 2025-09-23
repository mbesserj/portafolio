/**
 * Módulo para los procesos de Extracción, Transformación y Carga (ETL).
 */
module com.app.portafolio.normalizar {
    // 1. Depende del modelo para crear las entidades.
    requires com.app.portafolio.model;
    requires org.slf4j;
    requires jakarta.persistence;

    // 2. EXPORTA su paquete de servicios.
    //    Esta es la otra línea clave que faltaba.
    exports com.app.normalizar;
}

