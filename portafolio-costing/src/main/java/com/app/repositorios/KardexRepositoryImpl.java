package com.app.repositorios;

import com.app.dto.InventarioCostoDto;
import com.app.dto.KardexReporteDto;
import com.app.entities.KardexEntity;
import com.app.entities.SaldoKardexEntity;
import com.app.entities.TransaccionEntity;
import com.app.repository.AbstractRepository;
import com.app.repository.KardexRepository;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class KardexRepositoryImpl extends AbstractRepository implements KardexRepository {

    public KardexRepositoryImpl() {
        super();
    }

    @Override
    public int deleteKardexByClaveAgrupacion(String claveAgrupacion) {
        return executeInTransaction(em -> {
            return em.createQuery("DELETE FROM KardexEntity k WHERE k.claveAgrupacion = :clave")
                    .setParameter("clave", claveAgrupacion)
                    .executeUpdate();
        });
    }

    @Override
    public int deleteDetalleCosteoByClaveAgrupacion(String claveAgrupacion) {
        return executeInTransaction(em -> {
            return em.createQuery("DELETE FROM DetalleCosteoEntity d WHERE d.claveAgrupacion = :clave")
                    .setParameter("clave", claveAgrupacion)
                    .executeUpdate();
        });
    }
    
    @Override
    public int deleteSaldoKardexByGrupo(Long empresaId, Long custodioId, Long instrumentoId, String cuenta) {
        return executeInTransaction(em -> { 
            return em.createQuery("""
            DELETE FROM SaldoKardexEntity sk 
            WHERE sk.empresa.id = :empresaId 
              AND sk.custodio.id = :custodioId 
              AND sk.instrumento.id = :instrumentoId 
              AND sk.cuenta = :cuenta
            """)
                    .setParameter("empresaId", empresaId)
                    .setParameter("custodioId", custodioId)
                    .setParameter("instrumentoId", instrumentoId)
                    .setParameter("cuenta", cuenta)
                    .executeUpdate(); 
        });
    }

    @Override
    public List<KardexReporteDto> obtenerMovimientosPorGrupo(Long empresaId, Long custodioId, String cuenta, Long instrumentoId) {
        return execute(em -> {
            String sql = "SELECT * FROM kardex_view k WHERE k.empresa_id = ?1 AND k.custodio_id = ?2 AND k.nemo_id = ?3 AND k.cuenta = ?4 ORDER BY id, nemo_id ASC, fecha_tran ASC, CASE WHEN tipo_oper = 'INGRESO' THEN 1 ELSE 2 END ASC";
            Query query = em.createNativeQuery(sql, "KardexReporteMapping");
            query.setParameter(1, empresaId);
            query.setParameter(2, custodioId);
            query.setParameter(3, instrumentoId);
            query.setParameter(4, cuenta);
            return query.getResultList();
        });
    }

    @Override
    public List<InventarioCostoDto> obtenerSaldosFinalesPorGrupo(Long empresaId, Long custodioId) {
        return execute(em -> {
            String jpql = "SELECT NEW com.app.dto.InventarioCostoDto(k.instrumento.id, k.instrumento.instrumentoNemo, k.instrumento.instrumentoNombre, k.saldoCantidad, k.saldoValor) FROM KardexEntity k WHERE k.id IN (SELECT MAX(subk.id) FROM KardexEntity subk WHERE subk.empresa.id = :empresaId AND subk.custodio.id = :custodioId GROUP BY subk.instrumento.id)";
            return em.createQuery(jpql, InventarioCostoDto.class)
                    .setParameter("empresaId", empresaId)
                    .setParameter("custodioId", custodioId)
                    .getResultList();
        });
    }

    @Override
    public Optional<KardexEntity> obtenerUltimoSaldoAntesDe(TransaccionEntity transaccion) {
        return execute(em -> {
            try {
                String claveAgrupacion = transaccion.getEmpresa().getId() + "|" + transaccion.getCuenta() + "|" + transaccion.getCustodio().getId() + "|" + transaccion.getInstrumento().getId();
                TypedQuery<KardexEntity> query = em.createQuery("SELECT k FROM KardexEntity k WHERE k.claveAgrupacion = :clave AND k.fechaTransaccion <= :fecha AND k.transaccion.id <> :txId ORDER BY k.fechaTransaccion DESC, k.id DESC", KardexEntity.class);
                query.setParameter("clave", claveAgrupacion);
                query.setParameter("fecha", transaccion.getFecha());
                query.setParameter("txId", transaccion.getId());
                query.setMaxResults(1);
                return Optional.of(query.getSingleResult());
            } catch (NoResultException e) {
                return Optional.empty();
            }
        });
    }

    @Override
    public Optional<KardexEntity> findLastByGroup(Long empresaId, String cuenta, Long custodioId, Long instrumentoId) {
        return execute(em
                -> em.createQuery("SELECT k FROM KardexEntity k WHERE k.empresa.id = :empresaId AND k.cuenta = :cuenta AND k.custodio.id = :custodioId AND k.instrumento.id = :instrumentoId ORDER BY k.fechaTransaccion DESC, k.id DESC", KardexEntity.class)
                        .setParameter("empresaId", empresaId).setParameter("cuenta", cuenta)
                        .setParameter("custodioId", custodioId).setParameter("instrumentoId", instrumentoId)
                        .setMaxResults(1).getResultList().stream().findFirst()
        );
    }

    @Override
    public Optional<KardexEntity> findLastByGroupBeforeDate(Long empresaId, String cuenta, Long custodioId, Long instrumentoId, LocalDate fecha) {
        return execute(em
                -> em.createQuery("SELECT k FROM KardexEntity k WHERE k.empresa.id = :empresaId AND k.cuenta = :cuenta AND k.custodio.id = :custodioId AND k.instrumento.id = :instrumentoId AND k.fechaTransaccion < :fecha ORDER BY k.fechaTransaccion DESC, k.id DESC", KardexEntity.class)
                        .setParameter("empresaId", empresaId).setParameter("cuenta", cuenta)
                        .setParameter("custodioId", custodioId).setParameter("instrumentoId", instrumentoId)
                        .setParameter("fecha", fecha).setMaxResults(1).getResultList().stream().findFirst()
        );
    }

    @Override
    public List<KardexEntity> findByGroupAndDateRange(Long empresaId, Long custodioId, Long instrumentoId, String cuenta, LocalDate fechaInicio, LocalDate fechaFin) {
        return execute(em -> {
            TypedQuery<KardexEntity> query = em.createQuery("SELECT k FROM KardexEntity k WHERE k.empresa.id = :empresaId AND k.custodio.id = :custodioId AND k.instrumento.id = :instrumentoId AND k.cuenta = :cuenta AND k.fechaTransaccion BETWEEN :fechaInicio AND :fechaFin ORDER BY k.fechaTransaccion, k.id", KardexEntity.class);
            query.setParameter("empresaId", empresaId).setParameter("custodioId", custodioId)
                    .setParameter("instrumentoId", instrumentoId).setParameter("cuenta", cuenta)
                    .setParameter("fechaInicio", fechaInicio).setParameter("fechaFin", fechaFin);
            return query.getResultList();
        });
    }
}