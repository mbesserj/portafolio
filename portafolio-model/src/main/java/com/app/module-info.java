module com.app.portafolio.model {
    // Exporta los paquetes que contienen las entidades, DAOs, DTOs y utilidades
    exports com.app.entities;
    exports com.app.dao;
    exports com.app.dto;
    exports com.app.utiles;
    exports com.app.enums;
    exports com.app.exception;
    exports com.app.repository;

    // Dependencias para la persistencia (Hibernate, JPA) y validación
    requires org.hibernate.orm.core;
    requires jakarta.persistence;
    requires jakarta.validation;
    requires org.hibernate.validator;
    requires jakarta.el;
    requires jakarta.inject;
    requires org.slf4j;
    requires org.mapstruct;
    requires static lombok;

    // Consolidación de las declaraciones 'opens' para el mismo paquete
    // Permite que Hibernate, Validator, Lombok y MapStruct accedan a las entidades en tiempo de ejecución
    opens com.app.entities to org.hibernate.orm.core, org.mapstruct, org.hibernate.validator;
    opens com.app.dto to org.mapstruct;
    opens com.app.dao to org.hibernate.orm.core;
    opens com.app.utiles to org.hibernate.orm.core;
    
}