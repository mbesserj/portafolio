/**
 * Módulo de negocio para el costeo de activos del portafolio.
 * Define las dependencias y la API pública que expone para ser consumida
 * por otros módulos de la aplicación.
 */
module com.app.portafolio.costing {

    // --- DEPENDENCIAS DEL MÓDULO ---

    // 1. Depende del módulo 'model' para poder usar sus entidades y DTOs.
    //    Al no ser una dependencia 'transitive', cualquier módulo cliente que
    //    use nuestra API y necesite los DTOs, deberá también requerir 'com.app.portafolio.model'.
    requires com.app.portafolio.model;

    // 2. Dependencias de librerías de terceros.
    requires org.slf4j;
    requires jakarta.persistence;
    requires org.hibernate.orm.core; // Necesario si usas clases de Hibernate directamente.


    // --- API PÚBLICA ---

    /**
     * Exporta únicamente el paquete de la API pública.
     * Todas las clases en otros paquetes (como 'com.app.costing.internal')
     * permanecerán encapsuladas y no serán visibles para otros módulos.
     */
    exports com.costing.api;
}

