package com.app.main;

import com.app.dto.CostingGroupDTO;
import com.app.exception.CostingException;
import com.app.utiles.LibraryInitializer;
import com.costing.api.CostingApi;
import java.util.List;

public class ProcesoCosteo {

    public static void main(String[] args) {
        System.out.println("--- INICIANDO CLIENTE DE PRUEBA PARA EL MOTOR DE COSTEO ---");
        
        LibraryInitializer.init();
        
        try {
           
            System.out.println("\n[TAREA 1] Obteniendo grupos de costeo...");
            List<CostingGroupDTO> grupos = CostingApi.createService().obtenerGruposCosteo();
            System.out.println("=> Se encontraron " + grupos.size() + " grupos.");
            if (!grupos.isEmpty()) {
                System.out.println("   - Primer grupo encontrado: " + 
                    grupos.get(0).getInstrumentoNemonico() + " | " + 
                    grupos.get(0).getClaveAgrupacion());
            }

            System.out.println("\n[TAREA 2] Ejecutando el costeo completo...");
            CostingApi.createService().ejecutarCosteoCompleto();
            System.out.println("=> Proceso de costeo completo finalizado con éxito.");

            if (!grupos.isEmpty()) {
                String claveEjemplo = grupos.get(0).getClaveAgrupacion();
                System.out.println("\n[TAREA 3] Recosteando el grupo: " + claveEjemplo);
                CostingApi.createService().recostearGrupo(claveEjemplo);
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