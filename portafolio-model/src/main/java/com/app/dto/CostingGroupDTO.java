package com.app.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CostingGroupDTO {
    private String claveAgrupacion;
    private LocalDate fechaCreado;
    private String instrumentoNemonico;
    private String razonSocial;
    private String cuenta;
}
