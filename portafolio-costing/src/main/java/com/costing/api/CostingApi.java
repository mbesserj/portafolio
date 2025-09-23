package com.costing.api;

import com.app.repository.CostingService;
import com.app.dto.CostingGroupDTO;
import com.app.exception.CostingException;
import com.app.repositorios.CostingRepositoryImpl;
import java.util.List;

/**
 * Esta es la clase de entrada principal para el módulo de costeo.
 * Actúa como una API pública y simplificada.
 * Cualquier módulo externo solo necesita interactuar con esta clase.
 */
public class CostingApi {

    private final CostingService costingService;

    /**
     * Constructor que inicializa el servicio interno.
     * Toda la complejidad de la creación de objetos está oculta aquí.
     * Ya no se necesita el EntityManager.
     */
    public CostingApi() {
        // La implementación real (CostingRepositoryImpl) está encapsulada.
        // El mundo exterior no necesita saber que existe.
        this.costingService = new CostingRepositoryImpl();
    }

    /**
     * Inicia el proceso de costeo completo para todas las transacciones pendientes.
     *
     * @throws CostingException si ocurre un error durante el proceso.
     */
    public void iniciarCosteoCompleto() throws CostingException {
        costingService.runFullCosting();
    }

    /**
     * Inicia el proceso de RECOSTEO para un grupo específico.
     *
     * @param groupKey La clave única del grupo a recostear (ej: "1|12345|1|25").
     * @throws CostingException si ocurre un error durante el proceso.
     */
    public void recostearGrupo(String groupKey) throws CostingException {
        costingService.reCostGroup(groupKey);
    }

    /**
     * Obtiene una lista de todos los grupos de costeo existentes.
     *
     * @return una Lista de DTOs con la información de los grupos.
     */
    public List<CostingGroupDTO> obtenerGruposDeCosteo() {
        try {
            return costingService.getCostingGroups();
        } catch (CostingException ex) {
            System.getLogger(CostingApi.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            return null;
        }
    }
}