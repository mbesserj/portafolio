package com.app.dto;

import com.app.entities.CustodioEntity;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Genera getters, setters, toString, equals y hashCode
@NoArgsConstructor // Genera constructor sin argumentos
public class Custodio {

    private Long id;
    private String nombreCustodio;
    private LocalDate fechaCreado;

    public Custodio(Long id, String nombreCustodio, LocalDate fechaCreado) {
        this.id = id;
        this.nombreCustodio = nombreCustodio;
        this.fechaCreado = fechaCreado;
    }

    /**
     * Crea CustodioEntity a partir del DAO
     * @return CustodioEntity
     */
    public CustodioEntity toEntity() {
        CustodioEntity entity = new CustodioEntity();
        entity.setNombreCustodio(this.nombreCustodio != null ? this.nombreCustodio : "");
        return entity;
    }
}