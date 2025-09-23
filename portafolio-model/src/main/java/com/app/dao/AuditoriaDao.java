package com.app.dao;

import com.app.dto.CargaTransaccion;
import com.app.entities.AuditoriaEntity;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;

public class AuditoriaDao extends AbstractJpaDao<AuditoriaEntity, Long> {

    public AuditoriaDao(final EntityManager entityManager) {
        super(entityManager, AuditoriaEntity.class);
    }

    /**
     * Registra un evento de auditoría/error en la base de datos.
     *
     * @param tipoEntidad Tipo de entidad afectada (ej. "Transaccion", "Saldo",
     * "Empresa").
     * @param valorClave Valor clave del registro afectado (ej. ID de carga,
     * RUT, Nemo).
     * @param descripcion Descripción detallada del evento o error.
     * @param archivoOrigen Nombre del archivo de origen.
     * @param fechaArchivo Fecha del archivo de datos.
     * @param fechaDatos Fecha de los datos dentro del archivo.
     * @param filaNumero Número de fila en el archivo Excel.
     * @param motivo Motivo del registro (ej. "VALIDACION_FALLIDA", "DUPLICADO",
     * "ERROR_INESPERADO").
     * @param registrosInsertados Cantidad de registros insertados (para
     * resumen).
     * @param registrosRechazados Cantidad de registros rechazados (para
     * resumen).
     * @param registrosDuplicados Cantidad de registros duplicados (para
     * resumen).
     */
    public void registrarErrorEntity(String tipoEntidad, String valorClave, String descripcion,
            String archivoOrigen, LocalDate fechaArchivo, LocalDate fechaDatos,
            int filaNumero, String motivo, int registrosInsertados,
            int registrosRechazados, int registrosDuplicados) {
        AuditoriaEntity entity = new AuditoriaEntity();
        entity.setTipoEntidad(tipoEntidad);
        entity.setValorClave(valorClave);
        entity.setDescripcion(descripcion);
        entity.setArchivoOrigen(archivoOrigen);
        entity.setFechaArchivo(fechaArchivo);
        entity.setFechaDatos(fechaDatos);
        entity.setFilaNumero(filaNumero);
        entity.setMotivo(motivo);
        entity.setRegistrosInsertados(registrosInsertados);
        entity.setRegistrosRechazados(registrosRechazados);
        entity.setRegistrosDuplicados(registrosDuplicados);
        // La fechaAuditoria se inicializa automáticamente en el constructor de AuditoriaEntity
        create(entity);
    }

    /**
     * Registra un resumen de la carga de registros en la tabla de auditoría.
     * Este método es para registrar el resultado general de una operación de
     * carga de archivos.
     *
     * @param archivoOrigen Nombre del archivo de origen.
     * @param fechaCarga Fecha en que se realizó la carga.
     * @param fechaDatos Fecha de los datos que contiene el archivo.
     * @param insertedCount Número de registros insertados exitosamente.
     * @param rejectedCount Número de registros rechazados por errores.
     * @param duplicatedCount Número de registros identificados como duplicados.
     */
    public void createCreationRecordsAudit(String archivoOrigen, LocalDate fechaCarga, LocalDate fechaDatos,
            int insertedCount, int rejectedCount, int duplicatedCount) {
        AuditoriaEntity audit = new AuditoriaEntity();
        audit.setTipoEntidad("Resumen Carga");
        audit.setValorClave("Archivo: " + archivoOrigen);
        audit.setDescripcion(String.format("Carga finalizada. Insertados: %d, Rechazados: %d, Duplicados: %d",
                insertedCount, rejectedCount, duplicatedCount));
        audit.setArchivoOrigen(archivoOrigen);
        audit.setFechaArchivo(fechaCarga);
        audit.setFechaDatos(fechaDatos);
        audit.setFilaNumero(0);
        audit.setMotivo("CARGA_COMPLETADA");
        audit.setRegistrosInsertados(insertedCount);
        audit.setRegistrosRechazados(rejectedCount);
        audit.setRegistrosDuplicados(duplicatedCount);
        create(audit);
    }

    public void registrar(CargaTransaccion dto, String estado) {
        AuditoriaEntity auditoria = new AuditoriaEntity();
        auditoria.setTipoEntidad("CargaTransaccion");
        auditoria.setValorClave(dto.getFolio() != null ? dto.getFolio() : "Sin folio");
        auditoria.setDescripcion("Estado de normalización: " + estado);
        auditoria.setArchivoOrigen("Normalización"); // puedes ajustar si tienes el nombre del archivo
        auditoria.setFechaArchivo(LocalDate.now());
        auditoria.setFechaDatos(dto.getTransactionDate());
        auditoria.setFilaNumero(dto.getRowNum());
        auditoria.setMotivo(estado);
        auditoria.setRegistrosInsertados("NORMALIZADO".equals(estado) ? 1 : 0);
        auditoria.setRegistrosRechazados("NORMALIZADO".equals(estado) ? 0 : 1);
        auditoria.setRegistrosDuplicados(0);

        create(auditoria);
    }

}
