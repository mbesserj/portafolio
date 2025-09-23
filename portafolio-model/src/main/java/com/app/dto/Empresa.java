package com.app.dto;

import com.app.entities.EmpresaEntity;
import java.time.LocalDate;
import static java.time.LocalDate.now;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Clase DTO para transferir datos de la entidad Empresa.
 * Representa los datos de una empresa de forma desacoplada de la entidad.
 */
@Data 
@NoArgsConstructor 
public class Empresa {

    private Long id;
    private String rut;
    private String razonSocial;
    private LocalDate fechaCreado;

    /**
     * Constructor para crear una nueva Empresa con RUT y Razón Social.
     * La fecha de creación se asigna automáticamente al día actual.
     * @param rut El RUT de la empresa.
     * @param razonSocial La razón social de la empresa.
     */
    public Empresa(String rut, String razonSocial) {
        this.rut = rut;
        this.razonSocial = razonSocial;
        this.fechaCreado = now();
    }

    /**
     * Constructor completo con todos los campos.
     * @param id El ID único de la empresa.
     * @param rut El RUT de la empresa.
     * @param razonSocial La razón social de la empresa.
     * @param fechaCreado La fecha de creación de la empresa.
     */
    public Empresa(Long id, String rut, String razonSocial, LocalDate fechaCreado) {
        this.id = id;
        this.rut = rut;
        this.razonSocial = razonSocial;
        this.fechaCreado = fechaCreado;
    }

    /**
     * Crea Empresa Entity a partir de DAO
     * @return EmpresaEntity
     */
    public EmpresaEntity toEntity() {
        EmpresaEntity entity = new EmpresaEntity();
        entity.setRut(this.rut != null ? this.rut : "");
        entity.setRazonSocial(this.razonSocial != null ? this.razonSocial : "");
        entity.setFechaCreado(this.fechaCreado != null ? this.fechaCreado : LocalDate.now());
        return entity;
    }
}
