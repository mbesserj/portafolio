package com.app.entities;

import com.app.enums.TipoEnumsCosteo;
import com.app.utiles.BaseEntity;
import jakarta.persistence.*;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tipos_contables")
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@EqualsAndHashCode(callSuper = true) 
public class MovimientoContableEntity extends BaseEntity implements Serializable {

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contable")
    private TipoEnumsCosteo tipoContable;

    @Column(name = "descripcion")
    private String descripcionContable;
    
}