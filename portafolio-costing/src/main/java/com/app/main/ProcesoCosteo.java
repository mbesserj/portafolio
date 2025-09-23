package com.app.main;

import com.app.dto.CostingGroupDTO;
import com.app.exception.CostingException;
import com.costing.api.CostingApi;
import com.app.utiles.LibraryInitializer;
import java.util.List;

public class ProcesoCosteo {

    public static void main(String[] args) {
        System.out.println("--- INICIANDO CLIENTE DE PRUEBA PARA EL MOTOR DE COSTEO ---");
        
        // 1. INICIA LA CONEXIÓN UNA SOLA VEZ
        LibraryInitializer.init();
        
        try {
            CostingApi costingApi = new CostingApi();

            // TAREA 1: Obtener la lista de grupos de costeo
            System.out.println("\n[TAREA 1] Obteniendo grupos de costeo...");
            List<CostingGroupDTO> grupos = costingApi.obtenerGruposDeCosteo();
            System.out.println("=> Se encontraron " + grupos.size() + " grupos.");
            if (!grupos.isEmpty()) {
                System.out.println("   - Primer grupo encontrado: " + grupos.get(0).getInstrumentoNemonico() + " | " + grupos.get(0).getClaveAgrupacion());
            }

            // TAREA 2: Ejecutar el costeo completo
            System.out.println("\n[TAREA 2] Ejecutando el costeo completo...");
            costingApi.iniciarCosteoCompleto();
            System.out.println("=> Proceso de costeo completo finalizado con éxito.");

            // TAREA 3: Recostear un grupo específico
            if (!grupos.isEmpty()) {
                String claveEjemplo = grupos.get(0).getClaveAgrupacion();
                System.out.println("\n[TAREA 3] Recosteando el grupo: " + claveEjemplo);
                costingApi.recostearGrupo(claveEjemplo);
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
            // 2. CIERRA LA CONEXIÓN UNA SOLA VEZ, AL FINAL DE TODO
            LibraryInitializer.shutdown();
            System.out.println("\n--- CLIENTE DE PRUEBA FINALIZADO (Recursos liberados) ---");
        }
    }
}