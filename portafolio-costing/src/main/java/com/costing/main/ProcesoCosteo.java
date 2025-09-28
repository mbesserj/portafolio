package com.costing.main;

import com.model.dto.CostingGroupDTO;
import com.model.exception.CostingException;
import com.model.interfaces.CostingApi;
import com.model.utiles.LibraryInitializer;
import com.costing.api.CostingApiFactory; 
import java.util.List;

public class ProcesoCosteo {

    public static void main(String[] args) {
        System.out.println("--- INICIANDO CLIENTE DE PRUEBA PARA EL MOTOR DE COSTEO ---");
        
        LibraryInitializer.init();
        
        try {
            // CAMBIO 2: Crear la instancia del servicio UNA SOLA VEZ al principio.
            CostingApi costingService = CostingApiFactory.createService();
            
            System.out.println("\n[TAREA 1] Obteniendo grupos de costeo...");
            // Se reutiliza la misma instancia del servicio.
            List<CostingGroupDTO> grupos = costingService.obtenerGruposCosteo();
            System.out.println("=> Se encontraron " + grupos.size() + " grupos.");
            if (!grupos.isEmpty()) {
                System.out.println("   - Primer grupo encontrado: " + 
                    grupos.get(0).getInstrumentoNemonico() + " | " + 
                    grupos.get(0).getClaveAgrupacion());
            }

            System.out.println("\n[TAREA 2] Ejecutando el costeo completo...");
            // Se reutiliza la misma instancia del servicio.
            costingService.ejecutarCosteoCompleto();
            System.out.println("=> Proceso de costeo completo finalizado con éxito.");

            if (!grupos.isEmpty()) {
                String claveEjemplo = grupos.get(0).getClaveAgrupacion();
                System.out.println("\n[TAREA 3] Recosteando el grupo: " + claveEjemplo);
                // Se reutiliza la misma instancia del servicio.
                costingService.recostearGrupo(claveEjemplo);
                System.out.println("=> Recosteo del grupo finalizado con éxito.");
            } else {
                System.out.println("\n[TAREA 3] Omitida: No hay grupos para recostear.");
            }

        } catch (CostingException e) {
            System.err.println("\n!!! ERROR EN LA EJECUCIÓN DEL PROCESO !!!");
            System.err.println("Mensaje: " + e.getMessage());
            if (e.getCause() != null) {
                e.getCause().printStackTrace();
            }
        } finally {
            LibraryInitializer.shutdown();
            System.out.println("\n--- CLIENTE DE PRUEBA FINALIZADO (Recursos liberados) ---");
        }
    }
}