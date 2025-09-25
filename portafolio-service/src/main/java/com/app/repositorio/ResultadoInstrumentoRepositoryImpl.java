package com.app.repositorio;

import com.app.dto.ResultadoInstrumentoDto;
import com.app.interfaces.AbstractRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.app.interfaces.ResultadoInsInterfaz;

public class ResultadoInstrumentoRepositoryImpl extends AbstractRepository implements ResultadoInsInterfaz {

    private static final String OPERACIONES_QUERY = """
        WITH resultados AS (
            SELECT 
                v.id, 
                v.fecha_tran, 
                tm.tipo_movimiento, 
                t.cantidad,
                CASE WHEN v.tipo_oper = 'INGRESO' THEN COALESCE(t.cantidad, 0) ELSE 0 END AS cant_compras,
                CASE WHEN v.tipo_oper = 'EGRESO' THEN COALESCE(t.cantidad, 0) ELSE 0 END AS cant_ventas,
                SUM(v.monto_compra) AS compra, 
                SUM(v.cant_usada * v.precio_venta) AS venta,
                SUM(v.costo_oper) AS costo, 
                SUM(v.utilidad) AS utilidad
            FROM kardex_view v
            JOIN transacciones t ON v.id = t.id
            JOIN tipo_movimientos tm ON t.movimiento_id = tm.id
            WHERE v.empresa_id = :empresaId AND v.custodio_id = :custodioId AND v.cuenta = :cuenta
              AND v.nemo_id = :instrumentoId AND v.tipo_oper IN ('INGRESO', 'EGRESO')
            GROUP BY v.id, v.fecha_tran, tm.tipo_movimiento, t.cantidad
        )
        SELECT 
            id, 
            fecha_tran, 
            tipo_movimiento,
            cant_compras,
            cant_ventas,
            SUM(cant_compras - cant_ventas) OVER (ORDER BY fecha_tran, id) AS saldo,
            compra, 
            venta, 
            costo, 
            utilidad
        FROM resultados
        ORDER BY fecha_tran, id
        """;

    private static final String DIVIDENDOS_QUERY = """
        SELECT t.id, t.fecha, tm.tipo_movimiento, t.monto
        FROM transacciones t 
        JOIN tipo_movimientos tm ON t.movimiento_id = tm.id
        WHERE t.empresa_id = :empresaId AND t.custodio_id = :custodioId 
          AND t.cuenta = :cuenta AND t.instrumento_id = :instrumentoId
          AND tm.tipo_movimiento LIKE '%Dividendo%'
        """;

    private static final String GASTOS_QUERY = """
        SELECT t.id, COALESCE(t.gastos,0) + COALESCE(t.iva,0) + COALESCE(t.comisiones,0)
        FROM TransaccionEntity t 
        WHERE t.empresa.id = :empresaId AND t.custodio.id = :custodioId 
          AND t.cuenta = :cuenta AND t.instrumento.id = :instrumentoId
        """;

    @Override
    public List<ResultadoInstrumentoDto> findOperacionesByFiltro(Long empresaId, Long custodioId, String cuenta, Long instrumentoId) {
        return executeReadOnly(em -> {
            List<Object[]> results = em.createNativeQuery(OPERACIONES_QUERY)
                    .setParameter("empresaId", empresaId)
                    .setParameter("custodioId", custodioId)
                    .setParameter("cuenta", cuenta)
                    .setParameter("instrumentoId", instrumentoId)
                    .getResultList();
            return mapToOperacionesDto(results);
        });
    }

    @Override
    public List<ResultadoInstrumentoDto> findDividendosByFiltro(Long empresaId, Long custodioId, String cuenta, Long instrumentoId) {
        return executeReadOnly(em -> {
            List<Object[]> results = em.createQuery(DIVIDENDOS_QUERY, Object[].class)
                    .setParameter("empresaId", empresaId)
                    .setParameter("custodioId", custodioId)
                    .setParameter("cuenta", cuenta)
                    .setParameter("instrumentoId", instrumentoId)
                    .getResultList();
            return mapToDividendosDto(results);
        });
    }

    @Override
    public Map<Long, BigDecimal> findGastosByFiltro(Long empresaId, Long custodioId, String cuenta, Long instrumentoId) {
        return executeReadOnly(em
                -> em.createQuery(GASTOS_QUERY, Object[].class)
                        .setParameter("empresaId", empresaId)
                        .setParameter("custodioId", custodioId)
                        .setParameter("cuenta", cuenta)
                        .setParameter("instrumentoId", instrumentoId)
                        .getResultStream()
                        .collect(Collectors.toMap(
                                row -> get(row, 0, Long.class),
                                row -> get(row, 1, BigDecimal.class)
                        ))
        );
    }

    // --- Métodos privados para mapeo ---
    private List<ResultadoInstrumentoDto> mapToOperacionesDto(List<Object[]> results) {
        return results.stream().map(row -> {
            ResultadoInstrumentoDto dto = new ResultadoInstrumentoDto();
            dto.setIdTransaccion(get(row, 0, Long.class));
            dto.setFecha(get(row, 1, LocalDate.class));
            dto.setTipoMovimiento(get(row, 2, String.class));
            dto.setCant_compra(get(row, 3, BigDecimal.class));
            dto.setCant_venta(get(row, 4, BigDecimal.class));
            dto.setSaldo(get(row, 5, BigDecimal.class));
            dto.setCompras(get(row, 6, BigDecimal.class));
            dto.setVentas(get(row, 7, BigDecimal.class));
            dto.setCostoDeVenta(get(row, 8, BigDecimal.class));
            dto.setUtilidadRealizada(get(row, 9, BigDecimal.class));
            return dto;
        }).collect(Collectors.toList());
    }

    private List<ResultadoInstrumentoDto> mapToDividendosDto(List<Object[]> results) {
        return results.stream().map(row -> {
            ResultadoInstrumentoDto dto = new ResultadoInstrumentoDto(
                    get(row, 0, Long.class),
                    get(row, 1, LocalDate.class),
                    get(row, 2, String.class),
                    null
            );
            dto.setDividendos(get(row, 3, BigDecimal.class));
            return dto;
        }).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private <T> T get(Object[] row, int index, Class<T> type) {
        if (row == null || index >= row.length || row[index] == null) {
            return null;
        }
        // Manejo especial para conversiones numéricas comunes desde SQL nativo.
        if (type == Long.class && row[index] instanceof Number) {
            return (T) Long.valueOf(((Number) row[index]).longValue());
        }
        return (T) row[index];
    }
}