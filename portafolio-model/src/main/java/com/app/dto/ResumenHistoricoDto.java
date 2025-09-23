package com.app.dto;

import com.app.repository.InstrumentoData;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ResumenHistoricoDto implements InstrumentoData {

    private Long instrumentoId;
    private String nemo;
    private String nombreInstrumento; 

    private BigDecimal totalCostoFifo;
    private BigDecimal totalGasto;
    private BigDecimal totalDividendo;
    private BigDecimal totalUtilidad;
    private BigDecimal totalTotal;

    // Constructor para la fila de "TOTALES"
    public ResumenHistoricoDto(String nemo) {
        this.nemo = nemo;
    }
}