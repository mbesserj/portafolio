package com.etl.interfaz;

import com.app.dto.ResultadoCargaDto; 
import com.app.exception.MappingException;
import jakarta.persistence.EntityManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration; 
import java.time.Instant; 
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCarga<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCarga.class);
    protected final EntityManager entityManager;

    public AbstractCarga(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // --- Métodos Abstractos (no cambian) ---
    protected abstract String getSheetType(File file, int sheetIndex);
    protected abstract CargaMapperInterfaz<T> getMapper(String sheetType);
    protected abstract AbstractCargaProcessor<T> getProcessor(String sheetType);
    protected int getHeaderRowIndex() { return 0; }

    /**
     * MÉTODO PRINCIPAL
     * Ahora procesa un único archivo y DEVUELVE un resumen de la operación.
     * @param file El archivo Excel a procesar.
     * @return Un objeto ResultadoCargaDto con el resumen de la carga.
     */
    // --- La firma del método ahora devuelve ResultadoCargaDto ---
    public ResultadoCargaDto processFile(File file) {
        if (file == null || !file.exists() || file.getName().startsWith("~$")) {
            logger.warn("Archivo inválido, se omite el procesamiento.");
            return ResultadoCargaDto.fallido("El archivo proporcionado es inválido o no existe.");
        }

        // --- Inicializar contadores y cronómetro ---
        Instant inicio = Instant.now();
        int filasProcesadas = 0;
        int erroresDeMapeo = 0;

        logger.info("Procesando archivo: {}", file.getName());
        try (FileInputStream fis = new FileInputStream(file); Workbook workbook = WorkbookFactory.create(fis)) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetType = getSheetType(file, i);
                if (sheetType == null || sheetType.isBlank()) continue;

                logger.info("Procesando hoja '{}' (tipo: {})", sheet.getSheetName(), sheetType);
                CargaMapperInterfaz<T> mapper = getMapper(sheetType);
                AbstractCargaProcessor<T> processor = getProcessor(sheetType);

                for (int j = getHeaderRowIndex() + 1; j <= sheet.getLastRowNum(); j++) {
                    Row row = sheet.getRow(j);
                    if (row == null) continue;

                    try {
                        T dto = mapper.map(row, j + 1, file.getName());
                        if (dto == null) {
                            logger.info("Final de datos detectado en la fila {}. Se detiene la lectura de la hoja.", j + 1);
                            break;
                        }
                        processor.procesar(dto);
                        filasProcesadas++; 

                    } catch (MappingException e) {
                        erroresDeMapeo++; 
                        logger.error("Error de mapeo en la fila {} del archivo {}: {}", j + 1, file.getName(), e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error crítico al leer el archivo: {}", file.getName(), e);
            return ResultadoCargaDto.fallido("Error de I/O al leer el archivo: " + e.getMessage());
        }

        // --- Devolver el resultado final exitoso ---
        Duration duracion = Duration.between(inicio, Instant.now());
        String mensaje = String.format("Proceso completado. Filas procesadas: %d, Errores: %d.", filasProcesadas, erroresDeMapeo);
        return ResultadoCargaDto.exitoso(filasProcesadas, duracion, mensaje);
    }
}