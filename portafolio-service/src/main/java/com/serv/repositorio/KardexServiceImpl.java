package com.serv.repositorio;

import com.model.dto.InventarioCostoDto;
import com.model.dto.KardexReporteDto;
import com.model.entities.KardexEntity;
import com.model.entities.TransaccionEntity;
import com.model.interfaces.AbstractRepository;
import com.model.interfaces.KardexApi;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del repositorio de Kardex con manejo robusto de transacciones.
 * Versión final con todas las correcciones aplicadas.
 */
public class KardexServiceImpl extends AbstractRepository implements KardexApi {

    private static final Logger logger = LoggerFactory.getLogger(KardexServiceImpl.class);

    // Query optimizada para sincronización de saldos kardex
    private static final String SINCRONIZACION_QUERY = """
        INSERT INTO saldos_kardex 
            (empresa_id, custodio_id, instrumento_id, cuenta, saldo_cantidad, costo_total, costo_promedio, fecha_ultima_actualizacion, fecha_creacion, creado_por)
        WITH UltimosSaldos AS (
            SELECT 
                s.empresa_id, s.custodio_id, s.instrumento_id, s.cuenta,
                s.fecha, s.cantidad, s.monto_clp, s.precio,
                ROW_NUMBER() OVER(PARTITION BY s.empresa_id, s.custodio_id, s.instrumento_id, s.cuenta ORDER BY s.fecha DESC, s.id DESC) as rn
            FROM saldos s
        )
        SELECT 
            us.empresa_id,
            us.custodio_id,
            us.instrumento_id,
            us.cuenta,
            us.cantidad AS saldo_cantidad,
            us.monto_clp AS costo_total,
            CASE WHEN us.cantidad <> 0 THEN us.monto_clp / us.cantidad ELSE 0 END AS costo_promedio,
            us.fecha AS fecha_ultima_actualizacion,
            CURDATE() AS fecha_creacion,
            'CARGA_INICIAL' AS creado_por
        FROM UltimosSaldos us
        WHERE us.rn = 1 AND us.cantidad <> 0
        """;

    /**
     * Constructor por defecto.
     */
    public KardexServiceImpl() {
        super();
    }

    /**
     * Elimina registros de kardex por clave de agrupación.
     *
     * @param claveAgrupacion La clave de agrupación
     * @return Número de registros eliminados
     * @throws IllegalArgumentException si claveAgrupacion es null o vacía
     */
    @Override
    public int deleteKardexByClaveAgrupacion(String claveAgrupacion) {
        if (claveAgrupacion == null || claveAgrupacion.trim().isEmpty()) {
            throw new IllegalArgumentException("La clave de agrupación no puede ser nula o vacía");
        }

        return executeInTransaction(em -> {
            try {
                int eliminados = em.createQuery("DELETE FROM KardexEntity k WHERE k.claveAgrupacion = :clave")
                        .setParameter("clave", claveAgrupacion.trim())
                        .executeUpdate();

                logger.info("Eliminados {} registros de kardex para clave: {}", eliminados, claveAgrupacion);
                return eliminados;

            } catch (Exception e) {
                logger.error("Error al eliminar kardex por clave: {}", claveAgrupacion, e);
                throw new RuntimeException("Error al eliminar registros de kardex", e);
            }
        });
    }

    /**
     * Elimina registros de detalle costeo por clave de agrupación.
     *
     * @param claveAgrupacion La clave de agrupación
     * @return Número de registros eliminados
     * @throws IllegalArgumentException si claveAgrupacion es null o vacía
     */
    @Override
    public int deleteDetalleCosteoByClaveAgrupacion(String claveAgrupacion) {
        if (claveAgrupacion == null || claveAgrupacion.trim().isEmpty()) {
            throw new IllegalArgumentException("La clave de agrupación no puede ser nula o vacía");
        }

        return executeInTransaction(em -> {
            try {
                int eliminados = em.createQuery("DELETE FROM DetalleCosteoEntity d WHERE d.claveAgrupacion = :clave")
                        .setParameter("clave", claveAgrupacion.trim())
                        .executeUpdate();

                logger.info("Eliminados {} registros de detalle costeo para clave: {}", eliminados, claveAgrupacion);
                return eliminados;

            } catch (Exception e) {
                logger.error("Error al eliminar detalle costeo por clave: {}", claveAgrupacion, e);
                throw new RuntimeException("Error al eliminar registros de detalle costeo", e);
            }
        });
    }

    /**
     * Elimina saldo kardex por grupo específico.
     *
     * @param empresaId ID de la empresa
     * @param custodioId ID del custodio
     * @param instrumentoId ID del instrumento
     * @param cuenta Nombre de la cuenta
     * @return Número de registros eliminados
     * @throws IllegalArgumentException si algún parámetro es inválido
     */
    @Override
    public int deleteSaldoKardexByGrupo(Long empresaId, Long custodioId, Long instrumentoId, String cuenta) {
        // Validaciones
        if (empresaId == null || empresaId <= 0) {
            throw new IllegalArgumentException("ID de empresa debe ser válido");
        }
        if (custodioId == null || custodioId <= 0) {
            throw new IllegalArgumentException("ID de custodio debe ser válido");
        }
        if (instrumentoId == null || instrumentoId <= 0) {
            throw new IllegalArgumentException("ID de instrumento debe ser válido");
        }
        if (cuenta == null || cuenta.trim().isEmpty()) {
            throw new IllegalArgumentException("La cuenta no puede ser nula o vacía");
        }

        return executeInTransaction(em -> {
            try {
                int eliminados = em.createQuery("""
                    DELETE FROM SaldoKardexEntity sk 
                    WHERE sk.empresa.id = :empresaId 
                      AND sk.custodio.id = :custodioId 
                      AND sk.instrumento.id = :instrumentoId 
                      AND sk.cuenta = :cuenta
                    """)
                        .setParameter("empresaId", empresaId)
                        .setParameter("custodioId", custodioId)
                        .setParameter("instrumentoId", instrumentoId)
                        .setParameter("cuenta", cuenta.trim())
                        .executeUpdate();

                logger.info("Eliminados {} saldos kardex para grupo [empresa:{}, custodio:{}, instrumento:{}, cuenta:{}]",
                        eliminados, empresaId, custodioId, instrumentoId, cuenta);
                return eliminados;

            } catch (Exception e) {
                logger.error("Error al eliminar saldo kardex para grupo", e);
                throw new RuntimeException("Error al eliminar saldo kardex", e);
            }
        });
    }

    /**
     * Obtiene movimientos de kardex para un grupo específico.
     *
     * @param empresaId ID de la empresa
     * @param custodioId ID del custodio
     * @param cuenta Nombre de la cuenta
     * @param instrumentoId ID del instrumento
     * @return Lista de movimientos de kardex
     */
    @Override
    public List<KardexReporteDto> obtenerMovimientosPorGrupo(Long empresaId, Long custodioId, String cuenta, Long instrumentoId) {
        // Validaciones
        if (empresaId == null || custodioId == null || instrumentoId == null) {
            logger.warn("Parámetros nulos en obtenerMovimientosPorGrupo");
            return List.of();
        }
        if (cuenta == null || cuenta.trim().isEmpty()) {
            logger.warn("Cuenta nula o vacía en obtenerMovimientosPorGrupo");
            return List.of();
        }

        return executeReadOnly(em -> {
            try {
                String sql = """
                    SELECT * FROM kardex_view k 
                    WHERE k.empresa_id = ?1 
                      AND k.custodio_id = ?2 
                      AND k.nemo_id = ?3 
                      AND k.cuenta = ?4 
                    ORDER BY k.id, k.nemo_id ASC, k.fecha_tran ASC, 
                             CASE WHEN k.tipo_oper = 'INGRESO' THEN 1 ELSE 2 END ASC
                    """;

                Query query = em.createNativeQuery(sql, "KardexReporteMapping");
                query.setParameter(1, empresaId)
                        .setParameter(2, custodioId)
                        .setParameter(3, instrumentoId)
                        .setParameter(4, cuenta.trim());

                @SuppressWarnings("unchecked")
                List<KardexReporteDto> resultList = query.getResultList();

                logger.debug("Obtenidos {} movimientos de kardex para grupo", resultList.size());
                return resultList;

            } catch (Exception e) {
                logger.error("Error al obtener movimientos por grupo", e);
                return List.of();
            }
        });
    }

    /**
     * Obtiene saldos finales por grupo (empresa y custodio).
     *
     * @param empresaId ID de la empresa
     * @param custodioId ID del custodio
     * @return Lista de inventarios con costos
     */
    @Override
    public List<InventarioCostoDto> obtenerSaldosFinalesPorGrupo(Long empresaId, Long custodioId) {
        if (empresaId == null || custodioId == null) {
            logger.warn("Parámetros nulos en obtenerSaldosFinalesPorGrupo");
            return List.of();
        }

        return executeReadOnly(em -> {
            try {
                String jpql = """
                    SELECT NEW com.app.dto.InventarioCostoDto(
                        k.instrumento.id, 
                        k.instrumento.instrumentoNemo, 
                        k.instrumento.instrumentoNombre, 
                        k.saldoCantidad, 
                        k.saldoValor
                    ) 
                    FROM KardexEntity k 
                    WHERE k.id IN (
                        SELECT MAX(subk.id) 
                        FROM KardexEntity subk 
                        WHERE subk.empresa.id = :empresaId 
                          AND subk.custodio.id = :custodioId 
                        GROUP BY subk.instrumento.id
                    )
                    ORDER BY k.instrumento.instrumentoNemo
                    """;

                TypedQuery<InventarioCostoDto> query = em.createQuery(jpql, InventarioCostoDto.class);
                query.setParameter("empresaId", empresaId)
                        .setParameter("custodioId", custodioId);

                List<InventarioCostoDto> resultado = query.getResultList();
                logger.debug("Obtenidos {} saldos finales por grupo", resultado.size());
                return resultado;

            } catch (Exception e) {
                logger.error("Error al obtener saldos finales por grupo", e);
                return List.of();
            }
        });
    }

    /**
     * Obtiene saldos finales por grupo y cuenta específica.
     *
     * @param empresaId ID de la empresa
     * @param custodioId ID del custodio
     * @param cuenta Nombre de la cuenta
     * @return Lista de inventarios con costos
     */
    @Override
    public List<InventarioCostoDto> obtenerSaldosFinalesPorGrupoYCuenta(Long empresaId, Long custodioId, String cuenta) {
        if (empresaId == null || custodioId == null) {
            logger.warn("Parámetros nulos en obtenerSaldosFinalesPorGrupoYCuenta");
            return List.of();
        }
        if (cuenta == null || cuenta.trim().isEmpty()) {
            logger.warn("Cuenta nula o vacía en obtenerSaldosFinalesPorGrupoYCuenta");
            return List.of();
        }

        return executeReadOnly(em -> {
            try {
                String jpql = """
                    SELECT NEW com.app.dto.InventarioCostoDto(
                        k.instrumento.id, 
                        k.instrumento.instrumentoNemo, 
                        k.instrumento.instrumentoNombre, 
                        k.saldoCantidad, 
                        k.saldoValor
                    ) 
                    FROM KardexEntity k 
                    WHERE k.id IN (
                        SELECT MAX(subk.id) 
                        FROM KardexEntity subk 
                        WHERE subk.empresa.id = :empresaId 
                          AND subk.custodio.id = :custodioId 
                          AND subk.cuenta = :cuenta 
                        GROUP BY subk.instrumento.id
                    )
                    ORDER BY k.instrumento.instrumentoNemo
                    """;

                TypedQuery<InventarioCostoDto> query = em.createQuery(jpql, InventarioCostoDto.class);
                query.setParameter("empresaId", empresaId)
                        .setParameter("custodioId", custodioId)
                        .setParameter("cuenta", cuenta.trim());

                List<InventarioCostoDto> resultado = query.getResultList();
                logger.debug("Obtenidos {} saldos finales por grupo y cuenta", resultado.size());
                return resultado;

            } catch (Exception e) {
                logger.error("Error al obtener saldos finales por grupo y cuenta", e);
                return List.of();
            }
        });
    }

    /**
     * Obtiene el último saldo antes de una transacción específica.
     *
     * @param transaccion La transacción de referencia
     * @return Optional con el último kardex encontrado
     * @throws IllegalArgumentException si transaccion es null
     */
    @Override
    public Optional<KardexEntity> obtenerUltimoSaldoAntesDe(TransaccionEntity transaccion) {
        if (transaccion == null) {
            throw new IllegalArgumentException("La transacción no puede ser nula");
        }

        return executeReadOnly(em -> {
            try {
                String claveAgrupacion = construirClaveAgrupacion(
                        transaccion.getEmpresa().getId(),
                        transaccion.getCuenta(),
                        transaccion.getCustodio().getId(),
                        transaccion.getInstrumento().getId()
                );

                TypedQuery<KardexEntity> query = em.createQuery("""
                    SELECT k FROM KardexEntity k 
                    WHERE k.claveAgrupacion = :clave 
                      AND k.fechaTransaccion <= :fecha 
                      AND k.transaccion.id <> :txId 
                    ORDER BY k.fechaTransaccion DESC, k.id DESC
                    """, KardexEntity.class);

                query.setParameter("clave", claveAgrupacion)
                        .setParameter("fecha", transaccion.getFecha())
                        .setParameter("txId", transaccion.getId())
                        .setMaxResults(1);

                List<KardexEntity> resultados = query.getResultList();
                Optional<KardexEntity> resultado = resultados.stream().findFirst();

                logger.debug("Último saldo antes de Tx {}: {}",
                        transaccion.getId(),
                        resultado.isPresent() ? "encontrado" : "no encontrado");

                return resultado;

            } catch (Exception e) {
                logger.error("Error al obtener último saldo antes de transacción {}", transaccion.getId(), e);
                return Optional.empty();
            }
        });
    }

    /**
     * Encuentra el último kardex por grupo.
     *
     * @param empresaId ID de la empresa
     * @param cuenta Nombre de la cuenta
     * @param custodioId ID del custodio
     * @param instrumentoId ID del instrumento
     * @return Optional con el último kardex encontrado
     */
    @Override
    public Optional<KardexEntity> findLastByGroup(Long empresaId, String cuenta, Long custodioId, Long instrumentoId) {
        if (empresaId == null || custodioId == null || instrumentoId == null) {
            return Optional.empty();
        }
        if (cuenta == null || cuenta.trim().isEmpty()) {
            return Optional.empty();
        }

        return executeReadOnly(em -> {
            try {
                TypedQuery<KardexEntity> query = em.createQuery("""
                    SELECT k FROM KardexEntity k 
                    WHERE k.empresa.id = :empresaId 
                      AND k.cuenta = :cuenta 
                      AND k.custodio.id = :custodioId 
                      AND k.instrumento.id = :instrumentoId 
                    ORDER BY k.fechaTransaccion DESC, k.id DESC
                    """, KardexEntity.class);

                query.setParameter("empresaId", empresaId)
                        .setParameter("cuenta", cuenta.trim())
                        .setParameter("custodioId", custodioId)
                        .setParameter("instrumentoId", instrumentoId)
                        .setMaxResults(1);

                return query.getResultList().stream().findFirst();

            } catch (Exception e) {
                logger.error("Error al buscar último kardex por grupo", e);
                return Optional.empty();
            }
        });
    }

    /**
     * Encuentra el último kardex por grupo antes de una fecha específica.
     *
     * @param empresaId ID de la empresa
     * @param cuenta Nombre de la cuenta
     * @param custodioId ID del custodio
     * @param instrumentoId ID del instrumento
     * @param fecha Fecha límite
     * @return Optional con el último kardex encontrado antes de la fecha
     */
    @Override
    public Optional<KardexEntity> findLastByGroupBeforeDate(Long empresaId, String cuenta, Long custodioId, Long instrumentoId, LocalDate fecha) {
        if (empresaId == null || custodioId == null || instrumentoId == null || fecha == null) {
            return Optional.empty();
        }
        if (cuenta == null || cuenta.trim().isEmpty()) {
            return Optional.empty();
        }

        return executeReadOnly(em -> {
            try {
                TypedQuery<KardexEntity> query = em.createQuery("""
                    SELECT k FROM KardexEntity k 
                    WHERE k.empresa.id = :empresaId 
                      AND k.cuenta = :cuenta 
                      AND k.custodio.id = :custodioId 
                      AND k.instrumento.id = :instrumentoId 
                      AND k.fechaTransaccion < :fecha 
                    ORDER BY k.fechaTransaccion DESC, k.id DESC
                    """, KardexEntity.class);

                query.setParameter("empresaId", empresaId)
                        .setParameter("cuenta", cuenta.trim())
                        .setParameter("custodioId", custodioId)
                        .setParameter("instrumentoId", instrumentoId)
                        .setParameter("fecha", fecha)
                        .setMaxResults(1);

                return query.getResultList().stream().findFirst();

            } catch (Exception e) {
                logger.error("Error al buscar último kardex por grupo antes de fecha", e);
                return Optional.empty();
            }
        });
    }

    /**
     * Encuentra kardex por grupo en un rango de fechas.
     *
     * @param empresaId ID de la empresa
     * @param custodioId ID del custodio
     * @param instrumentoId ID del instrumento
     * @param cuenta Nombre de la cuenta
     * @param fechaInicio Fecha de inicio del rango
     * @param fechaFin Fecha de fin del rango
     * @return Lista de kardex en el rango especificado
     */
    @Override
    public List<KardexEntity> findByGroupAndDateRange(Long empresaId, Long custodioId, Long instrumentoId, String cuenta, LocalDate fechaInicio, LocalDate fechaFin) {
        if (empresaId == null || custodioId == null || instrumentoId == null || fechaInicio == null || fechaFin == null) {
            return List.of();
        }
        if (cuenta == null || cuenta.trim().isEmpty()) {
            return List.of();
        }
        if (fechaInicio.isAfter(fechaFin)) {
            logger.warn("Fecha inicio {} es posterior a fecha fin {}", fechaInicio, fechaFin);
            return List.of();
        }

        return executeReadOnly(em -> {
            try {
                TypedQuery<KardexEntity> query = em.createQuery("""
                    SELECT k FROM KardexEntity k 
                    WHERE k.empresa.id = :empresaId 
                      AND k.custodio.id = :custodioId 
                      AND k.instrumento.id = :instrumentoId 
                      AND k.cuenta = :cuenta 
                      AND k.fechaTransaccion BETWEEN :fechaInicio AND :fechaFin 
                    ORDER BY k.fechaTransaccion, k.id
                    """, KardexEntity.class);

                query.setParameter("empresaId", empresaId)
                        .setParameter("custodioId", custodioId)
                        .setParameter("instrumentoId", instrumentoId)
                        .setParameter("cuenta", cuenta.trim())
                        .setParameter("fechaInicio", fechaInicio)
                        .setParameter("fechaFin", fechaFin);

                List<KardexEntity> resultado = query.getResultList();
                logger.debug("Encontrados {} kardex en rango {} - {}", resultado.size(), fechaInicio, fechaFin);
                return resultado;

            } catch (Exception e) {
                logger.error("Error al buscar kardex por grupo y rango de fechas", e);
                return List.of();
            }
        });
    }

    /**
     * Sincroniza los saldos kardex desde la tabla de saldos. Este método
     * ejecuta la query de sincronización definida como constante.
     */
    @Override
    public void sincronizarSaldosKardexDesdeSaldos() {
        executeInTransaction(em -> {
            try {
                logger.info("Iniciando sincronización de saldos kardex desde tabla saldos");

                int registrosInsertados = em.createNativeQuery(SINCRONIZACION_QUERY).executeUpdate();

                logger.info("Sincronización completada. Registros insertados: {}", registrosInsertados);
                return registrosInsertados;

            } catch (Exception e) {
                logger.error("Error durante la sincronización de saldos kardex", e);
                throw new RuntimeException("Error en sincronización de saldos kardex", e);
            }
        });
    }

    /**
     * Construye la clave de agrupación estándar para un grupo de transacciones.
     *
     * @param empresaId ID de la empresa
     * @param cuenta Nombre de la cuenta
     * @param custodioId ID del custodio
     * @param instrumentoId ID del instrumento
     * @return Clave de agrupación en formato
     * "empresaId|cuenta|custodioId|instrumentoId"
     */
    private String construirClaveAgrupacion(Long empresaId, String cuenta, Long custodioId, Long instrumentoId) {
        return empresaId + "|" + cuenta + "|" + custodioId + "|" + instrumentoId;
    }
}
