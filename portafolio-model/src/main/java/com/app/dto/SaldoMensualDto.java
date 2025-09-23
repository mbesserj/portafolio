package com.app.dto;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@SqlResultSetMapping(
        name = "SaldoMensualMapping",
        classes = @ConstructorResult(
                targetClass = SaldoMensualDto.class,
                columns = {
                    @ColumnResult(name = "nemo", type = String.class),
                    @ColumnResult(name = "instrumento_nemo", type = String.class),
                    @ColumnResult(name = "Enero", type = BigDecimal.class),
                    @ColumnResult(name = "Febrero", type = BigDecimal.class),
                    @ColumnResult(name = "Marzo", type = BigDecimal.class),
                    @ColumnResult(name = "Abril", type = BigDecimal.class),
                    @ColumnResult(name = "Mayo", type = BigDecimal.class),
                    @ColumnResult(name = "Junio", type = BigDecimal.class),
                    @ColumnResult(name = "Julio", type = BigDecimal.class),
                    @ColumnResult(name = "Agosto", type = BigDecimal.class),
                    @ColumnResult(name = "Septiembre", type = BigDecimal.class),
                    @ColumnResult(name = "Octubre", type = BigDecimal.class),
                    @ColumnResult(name = "Noviembre", type = BigDecimal.class),
                    @ColumnResult(name = "Diciembre", type = BigDecimal.class)
                }
        )
)
@Data
@NoArgsConstructor
public class SaldoMensualDto {

    private String nemo;
    private String instrumentoNemo;

    // Inicialización por defecto para proteger la lógica de Java
    private BigDecimal enero = BigDecimal.ZERO;
    private BigDecimal febrero = BigDecimal.ZERO;
    private BigDecimal marzo = BigDecimal.ZERO;
    private BigDecimal abril = BigDecimal.ZERO;
    private BigDecimal mayo = BigDecimal.ZERO;
    private BigDecimal junio = BigDecimal.ZERO;
    private BigDecimal julio = BigDecimal.ZERO;
    private BigDecimal agosto = BigDecimal.ZERO;
    private BigDecimal septiembre = BigDecimal.ZERO;
    private BigDecimal octubre = BigDecimal.ZERO;
    private BigDecimal noviembre = BigDecimal.ZERO;
    private BigDecimal diciembre = BigDecimal.ZERO;

    public SaldoMensualDto(String nemo, String instrumentoNemo, BigDecimal enero, BigDecimal febrero, BigDecimal marzo,
            BigDecimal abril, BigDecimal mayo, BigDecimal junio, BigDecimal julio,
            BigDecimal agosto, BigDecimal septiembre, BigDecimal octubre,
            BigDecimal noviembre, BigDecimal diciembre) {
        this.nemo = nemo;
        this.instrumentoNemo = instrumentoNemo;
        // Se añade una capa de seguridad para convertir cualquier null de la BD a CERO.
        this.enero = (enero != null) ? enero : BigDecimal.ZERO;
        this.febrero = (febrero != null) ? febrero : BigDecimal.ZERO;
        this.marzo = (marzo != null) ? marzo : BigDecimal.ZERO;
        this.abril = (abril != null) ? abril : BigDecimal.ZERO;
        this.mayo = (mayo != null) ? mayo : BigDecimal.ZERO;
        this.junio = (junio != null) ? junio : BigDecimal.ZERO;
        this.julio = (julio != null) ? julio : BigDecimal.ZERO;
        this.agosto = (agosto != null) ? agosto : BigDecimal.ZERO;
        this.septiembre = (septiembre != null) ? septiembre : BigDecimal.ZERO;
        this.octubre = (octubre != null) ? octubre : BigDecimal.ZERO;
        this.noviembre = (noviembre != null) ? noviembre : BigDecimal.ZERO;
        this.diciembre = (diciembre != null) ? diciembre : BigDecimal.ZERO;
    }

    /**
     * Calcula y devuelve la suma total de los saldos de los 12 meses.
     *
     * @return La suma total del año como un BigDecimal.
     */
    public BigDecimal getSumaTotalAnual() {
        return getEnero().add(getFebrero()).add(getMarzo())
                .add(getAbril()).add(getMayo()).add(getJunio())
                .add(getJulio()).add(getAgosto()).add(getSeptiembre())
                .add(getOctubre()).add(getNoviembre()).add(getDiciembre());
    }

}
