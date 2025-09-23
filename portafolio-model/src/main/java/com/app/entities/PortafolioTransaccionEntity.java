package com.app.entities;

import com.app.utiles.BaseEntity;
import jakarta.persistence.*;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "portafolios_transacciones")
@Data
@NoArgsConstructor
@AllArgsConstructor 
@EqualsAndHashCode(callSuper = true)
public class PortafolioTransaccionEntity extends BaseEntity implements Serializable {

    // La relación ManyToOne: muchas uniones pertenecen a un solo portafolio
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portafolio_id", nullable = false)
    private PortafolioEntity portafolio;

    // La relación ManyToOne: muchas uniones pertenecen a una sola transacción
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaccion_id", nullable = false)
    private TransaccionEntity transaccion;
    
}