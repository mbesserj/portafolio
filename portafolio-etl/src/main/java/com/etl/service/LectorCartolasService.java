package com.etl.service;

import com.app.dto.ResultadoCargaDto; 
import com.etl.loader.CargaBanChileService;
import com.etl.loader.CargaFynsaService;
import jakarta.persistence.EntityManager;
import java.io.File;
import com.app.enums.ListaEnumsCustodios;


public class LectorCartolasService {

    private final CargaBanChileService cargaBanChileService;
    private final CargaFynsaService cargaFynsaService;

    public LectorCartolasService(EntityManager entityManager) {
        this.cargaBanChileService = new CargaBanChileService(entityManager);
        this.cargaFynsaService = new CargaFynsaService(entityManager);
    }

    /**
     * Recibe un custodio y un archivo, delega el procesamiento al servicio de carga específico
     * y devuelve el resultado de la operación.
     * @param custodio El custodio seleccionado.
     * @param file El archivo a procesar.
     * @return Un objeto ResultadoCargaDto con el estado de la operación.
     */
    public ResultadoCargaDto cargar(ListaEnumsCustodios custodio, File file) {
        // Usamos un 'switch' para una lógica limpia, segura y fácil de mantener.
        switch (custodio) {
            case BanChile:
                return cargaBanChileService.processFile(file);

            case Fynsa:
                return cargaFynsaService.processFile(file);

            default:
                // Si el custodio no es reconocido, devolvemos un error controlado.
                return ResultadoCargaDto.fallido("El custodio seleccionado no tiene un servicio de carga asociado.");
        }
    }
}