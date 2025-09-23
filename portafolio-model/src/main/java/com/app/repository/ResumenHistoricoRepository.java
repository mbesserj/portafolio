package com.app.repository;

import com.app.dto.ResumenHistoricoDto;
import java.util.List;

public interface ResumenHistoricoRepository {
    List<ResumenHistoricoDto> obtenerResumenHistorico(Long empresaId, Long custodioId, String cuenta);
}