package com.app.interfaces;

import com.app.dto.ResumenHistoricoDto;
import java.util.List;

public interface ResumenHistoricoInterfaz {
    List<ResumenHistoricoDto> obtenerResumenHistorico(Long empresaId, Long custodioId, String cuenta);
}