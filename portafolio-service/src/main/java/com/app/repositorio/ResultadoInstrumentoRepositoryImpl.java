package com.app.repositorio;

import com.app.dto.ResultadoInstrumentoDto;
import com.app.repository.ResultadoInstrumentoRepository;
import com.app.utiles.LibraryInitializer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ResultadoInstrumentoRepositoryImpl implements ResultadoInstrumentoRepository {

    @Override
    public List<ResultadoInstrumentoDto> findOperacionesByFiltro(Long empresaId, Long custodioId, String cuenta, Long instrumentoId) {
        // Implementación de la consulta principal que ya tenías, con el mapeo corregido.
        EntityManager em = null;
        String sql = """
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
            ORDER BY fecha_tran, id;
        """;
        try {
            em = LibraryInitializer.getEntityManager();
            Query query = em.createNativeQuery(sql)
                .setParameter("empresaId", empresaId)
                .setParameter("custodioId", custodioId)
                .setParameter("cuenta", cuenta)
                .setParameter("instrumentoId", instrumentoId);

            List<Object[]> results = query.getResultList();
            return results.stream().map(row -> {
                // Mapeo corregido a los campos del DTO
                ResultadoInstrumentoDto dto = new ResultadoInstrumentoDto();
                dto.setIdTransaccion(((Number) row[0]).longValue());
                dto.setFecha((LocalDate) row[1]);
                dto.setTipoMovimiento((String) row[2]);
                dto.setCant_compra((BigDecimal) row[3]);
                dto.setCant_venta((BigDecimal) row[4]);
                dto.setSaldo((BigDecimal) row[5]);
                dto.setCompras((BigDecimal) row[6]);
                dto.setVentas((BigDecimal) row[7]);
                dto.setCostoDeVenta((BigDecimal) row[8]);
                dto.setUtilidadRealizada((BigDecimal) row[9]);
                return dto;
            }).collect(Collectors.toList());
        } finally {
            if (em != null) em.close();
        }
    }

    @Override
    public List<ResultadoInstrumentoDto> findDividendosByFiltro(Long empresaId, Long custodioId, String cuenta, Long instrumentoId) {
        // Lógica de dividendos movida aquí
        EntityManager em = null;
        String sql = """
            SELECT t.id, t.fecha, tm.tipo_movimiento, t.monto
            FROM transacciones t JOIN tipo_movimientos tm ON t.movimiento_id = tm.id
            WHERE t.empresa_id = :empresaId AND t.custodio_id = :custodioId AND t.cuenta = :cuenta AND t.instrumento_id = :instrumentoId
            AND tm.tipo_movimiento LIKE '%Dividendo%'
        """;
         try {
            em = LibraryInitializer.getEntityManager();
            Query query = em.createNativeQuery(sql)
                .setParameter("empresaId", empresaId)
                .setParameter("custodioId", custodioId)
                .setParameter("cuenta", cuenta)
                .setParameter("instrumentoId", instrumentoId);
            
            List<Object[]> results = query.getResultList();
            return results.stream().map(row -> {
                ResultadoInstrumentoDto dto = new ResultadoInstrumentoDto(
                    ((Number) row[0]).longValue(),
                    (LocalDate) row[1],
                    (String) row[2],
                    null // Cantidad no aplica para dividendos
                );
                dto.setDividendos((BigDecimal) row[3]);
                return dto;
            }).collect(Collectors.toList());
        } finally {
            if (em != null) em.close();
        }
    }
    
    @Override
    public Map<Long, BigDecimal> findGastosByFiltro(Long empresaId, Long custodioId, String cuenta, Long instrumentoId) {
        // Lógica de gastos movida aquí
        EntityManager em = null;
        String jpql = "SELECT t.id, COALESCE(t.gastos,0) + COALESCE(t.iva,0) + COALESCE(t.comisiones,0) "
                  + "FROM TransaccionEntity t "
                  + "WHERE t.empresa.id = :empresaId AND t.custodio.id = :custodioId AND t.cuenta = :cuenta AND t.instrumento.id = :instrumentoId";
        try {
            em = LibraryInitializer.getEntityManager();
            return em.createQuery(jpql, Object[].class)
                .setParameter("empresaId", empresaId)
                .setParameter("custodioId", custodioId)
                .setParameter("cuenta", cuenta)
                .setParameter("instrumentoId", instrumentoId)
                .getResultStream()
                .collect(Collectors.toMap(
                    row -> (Long) row[0],
                    row -> (BigDecimal) row[1]
                ));
        } finally {
            if (em != null) em.close();
        }
    }
}