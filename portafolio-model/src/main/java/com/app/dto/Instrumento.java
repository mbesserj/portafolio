package com.app.dto;

import com.app.entities.InstrumentoEntity;
import java.time.LocalDate;
import static java.time.LocalDate.now;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Clase DTO (Data Transfer Object) para transferir datos de la entidad Instrumento.
 * Representa los datos de un instrumento de forma desacoplada de la entidad.
 */
@Data // Genera getters, setters, toString, equals y hashCode
@NoArgsConstructor // Genera constructor sin argumentos
public class Instrumento {

    private Long id;
    private String instrumentoNemo;
    private String instrumentoNombre;
    private LocalDate fechaCreado;

    /**
     * Constructor para crear un nuevo Instrumento con su nemotécnico y nombre.
     * La fecha de creación se asigna automáticamente al día actual.
     * @param instrumentoNemo El nemotécnico del instrumento.
     * @param instrumentoNombre El nombre del instrumento.
     */
    public Instrumento(String instrumentoNemo, String instrumentoNombre) {
        this.instrumentoNemo = instrumentoNemo;
        this.instrumentoNombre = instrumentoNombre;
        this.fechaCreado = now();
    }

    /**
     * Constructor completo con todos los campos.
     * @param id El ID único del instrumento.
     * @param instrumentoNemo El nemotécnico del instrumento.
     * @param instrumentoNombre El nombre del instrumento.
     * @param fechaCreado La fecha de creación del registro.
     */
    public Instrumento(Long id, String instrumentoNemo, String instrumentoNombre, LocalDate fechaCreado) {
        this.id = id;
        this.instrumentoNemo = instrumentoNemo;
        this.instrumentoNombre = instrumentoNombre;
        this.fechaCreado = fechaCreado;
    }

    /**
     * Crea InstrumentoEntity a partir del DTO.
     * @return InstrumentoEntity
     */
    public InstrumentoEntity toEntity() {
        InstrumentoEntity entity = new InstrumentoEntity();

        entity.setInstrumentoNemo(this.instrumentoNemo != null ? this.instrumentoNemo : "");
        entity.setInstrumentoNombre(this.instrumentoNombre != null ? this.instrumentoNombre : "");        
        return entity;
    }
}