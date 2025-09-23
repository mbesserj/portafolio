
package com.app.entities;

import com.app.utiles.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "auditorias")
@Data
@NoArgsConstructor 
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true) 
public class AuditoriaEntity extends BaseEntity implements Serializable {

    @Column(name = "fecha_auditoria", nullable = false)
    private LocalDate fechaAuditoria = LocalDate.now();

    @Column(name = "fecha_archivo")
    private LocalDate fechaArchivo;

    @Column(name = "fecha_datos")
    private LocalDate fechaDatos;

    @Column(name = "tipo_entidad")
    private String tipoEntidad;

    @Column(name = "valor_clave")
    private String valorClave;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @Column(name = "archivo_origen", nullable = false)
    private String archivoOrigen;

    @Column(name = "fila_numero", nullable = false)
    private int filaNumero;

    @Column(name = "motivo", length = 1000, nullable = false)
    private String motivo;

    @Column(name = "registros_insertados")
    private int registrosInsertados;

    @Column(name = "registros_rechazados")
    private int registrosRechazados;

    @Column(name = "registros_duplicados")
    private int registrosDuplicados;

}