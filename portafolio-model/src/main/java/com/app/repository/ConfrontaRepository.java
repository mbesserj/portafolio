package com.app.repository;

import com.app.dto.ConfrontaSaldoDto;
import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio para consultas relacionadas con la confrontación de saldos.
 */
public interface ConfrontaRepository {
    
    /**
     * Ejecuta la consulta nativa para encontrar diferencias entre los saldos
     * del kardex y los saldos de mercado a una fecha de corte determinada.
     * @param fechaCorte La fecha máxima para considerar los saldos de mercado.
     * @return Una lista de DTOs con las diferencias encontradas.
     */
    List<ConfrontaSaldoDto> obtenerDiferenciasDeSaldos(LocalDate fechaCorte);
}