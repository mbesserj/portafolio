
package com.app.repository;

public interface TransaccionRepository {
    
    int resetCosteoFlagsByGrupo(Long empresaId, String cuenta, Long custodioId, Long instrumentoId);
}
