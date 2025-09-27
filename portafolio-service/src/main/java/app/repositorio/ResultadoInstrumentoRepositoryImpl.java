package com.app.repositorio;

import com.app.dto.ResultadoInstrumentoDto;
import com.app.interfaces.AbstractRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.app.interfaces.ResultadoRepository;
import com.app.sql.QueryRepository;

public class ResultadoInstrumentoRepositoryImpl extends AbstractRepository implements ResultadoRepository {

    @Override
    public List<ResultadoInstrumentoDto> findOperacionesByFiltro(Long empresaId, Long custodioId, String cuenta, Long instrumentoId) {
        return executeReadOnly(em -> {
            String sql_operaciones_query = QueryRepository.getResultadoInstrumentoQuery(QueryRepository.ResultadoInstrumentoQueries.OPERACIONES_QUERY);

            List<Object[]> results = em.createNativeQuery(sql_operaciones_query)
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
            String sql_dividendos = QueryRepository.getResultadoInstrumentoQuery(QueryRepository.ResultadoInstrumentoQueries.DIVIDENDOS_QUERY);
            List<Object[]> results = em.createQuery(sql_dividendos, Object[].class)
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
                -> em.createQuery(QueryRepository.getResultadoInstrumentoQuery(QueryRepository.ResultadoInstrumentoQueries.GASTOS_QUERY), Object[].class)
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