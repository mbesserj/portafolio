
package com.app.utiles;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Clase que define la clave primaria compuesta para CargaTransaccionEntity.
 * Usa la anotación @Embeddable para ser incrustada en la entidad.
 * La clave se compone de la fecha de la transacción, el número de fila del archivo,
 * y el tipo de movimiento.
 */
@Embeddable
public class Pk implements Serializable {

    private LocalDate transactionDate;
    private Integer rowNum;
    private String tipoClase;

    // Constructor sin argumentos requerido por JPA
    public Pk() {
    }

    public Pk(LocalDate transactionDate, Integer rowNum, String tipoClase) {
        this.transactionDate = transactionDate;
        this.rowNum = rowNum;
        this.tipoClase = tipoClase;
    }

    // Getters y Setters
    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public Integer getRowNum() {
        return rowNum;
    }

    public void setRowNum(Integer rowNum) {
        this.rowNum = rowNum;
    }

    public String getTipoClase() {
        return tipoClase;
    }

    public void setTipoClase(String tipoClase) {
        this.tipoClase = tipoClase;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pk that = (Pk) o;
        return Objects.equals(transactionDate, that.transactionDate) &&
               Objects.equals(rowNum, that.rowNum) &&
               Objects.equals(tipoClase, that.tipoClase);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionDate, rowNum, tipoClase);
    }
}