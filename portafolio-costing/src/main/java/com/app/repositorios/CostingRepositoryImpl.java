package com.app.repositorios;

import com.app.repository.CostingService;
import com.app.dto.AjustePropuestoDto;
import com.app.dto.CostingGroupDTO;
import com.app.engine.FifoCostingEngine;
import com.app.entities.KardexEntity;
import com.app.entities.TransaccionEntity;
import com.app.enums.TipoAjuste;
import com.app.exception.CostingException;
import com.app.repositorios.AjustesProcess;
import com.app.repositorios.*;
import com.app.repository.AbstractRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.util.List;

public class CostingRepositoryImpl extends AbstractRepository implements CostingService {

    public CostingRepositoryImpl() {
        super();
    }

    // --- MÉTODOS DE COSTEO ---
    @Override
    public void runFullCosting() throws CostingException {
        try {
            executeInTransaction(em -> {
                createEngine().procesarCosteo();
            });
        } catch (Exception e) {
            throw new CostingException("Falló el proceso de costeo completo.", e);
        }
    }

    @Override
    public List<CostingGroupDTO> getCostingGroups() throws CostingException {
        try {
            return executeReadOnly(em -> obtenerGruposDeCosteo());
        } catch (Exception e) {
            throw new CostingException("No se pudo obtener la lista de grupos de costeo.", e);
        }
    }

    @Override
    public void reCostGroup(String groupKey) throws CostingException {
        try {
            executeInTransaction(em -> {
                String[] parts = parseGroupKey(groupKey);
                Long empresaId = Long.parseLong(parts[0]);
                String cuenta = parts[1];
                Long custodioId = Long.parseLong(parts[2]);
                Long instrumentoId = Long.parseLong(parts[3]);

                new KardexRepositoryImpl().deleteDetalleCosteoByClaveAgrupacion(groupKey);
                new KardexRepositoryImpl().deleteKardexByClaveAgrupacion(groupKey);
                new KardexRepositoryImpl().deleteSaldoKardexByGrupo(empresaId, custodioId, instrumentoId, cuenta);
                new ResetCosteoRepositoryImpl().resetCosteoFlagsByGrupo(empresaId, cuenta, custodioId, instrumentoId);

                createEngine().procesarCosteo();
            });
        } catch (Exception e) {
            throw new CostingException("Falló el recosteo del grupo: " + groupKey, e);
        }
    }

    // --- MÉTODOS PARA AJUSTES ---
    @Override
    public AjustePropuestoDto proponerAjusteManual(Long txReferenciaId, TipoAjuste tipo) {
        try {
            return executeReadOnly(em -> {
                TransaccionEntity tx = findTransaction(em, txReferenciaId);
                return createAjustesProcess().proponerAjusteManual(tx, tipo);
            });
        } catch (Exception e) {
            try {
                throw new CostingException("Falló la propuesta de ajuste: " + e.getMessage(), e);
                
            } catch (CostingException ex) {
                System.getLogger(CostingRepositoryImpl.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
            return null;
        }
    }

    @Override
    public void crearAjusteManual(Long txReferenciaId, TipoAjuste tipo, BigDecimal cantidad, BigDecimal precio) {
        try {
            executeInTransaction(em -> {
                TransaccionEntity tx = findTransaction(em, txReferenciaId);
                createAjustesProcess().crearAjusteManual(tx, tipo, cantidad, precio);
            });
        } catch (Exception e) {
            try {
                throw new CostingException("Falló la creación de ajuste: " + e.getMessage(), e);
            } catch (CostingException ex) {
                System.getLogger(CostingRepositoryImpl.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        }
    }

    @Override
    public void eliminarAjusteManual(Long idAjuste) throws CostingException {
        try {
            // Se corrige la ambigüedad usando un bloque explícito
            executeInTransaction(em -> {
                createAjustesProcess().eliminarAjusteManual(idAjuste);
            });
        } catch (Exception e) {
            throw new CostingException("Falló la eliminación de ajuste: " + e.getMessage(), e);
        }
    }

    // --- MÉTODOS PRIVADOS DE AYUDA ---
    private FifoCostingEngine createEngine() {
        return new FifoCostingEngine(this.em, new KardexRepositoryImpl(), new SaldosRepositoryImpl(), new TipoMovimientoRepositoryImpl());
    }

    private AjustesProcess createAjustesProcess() {
        return new AjustesProcess(this.em, new SaldosRepositoryImpl(), new TipoMovimientoRepositoryImpl(), new KardexRepositoryImpl());
    }

    private TransaccionEntity findTransaction(EntityManager em, Long txId) {
        TransaccionEntity tx = em.find(TransaccionEntity.class, txId);
        if (tx == null) {
            throw new IllegalArgumentException("No se encontró la transacción con ID: " + txId);
        }
        return tx;
    }

    private String[] parseGroupKey(String groupKey) {
        if (groupKey == null || groupKey.isBlank()) {
            throw new IllegalArgumentException("La clave de grupo no puede ser nula o vacía.");
        }
        String[] parts = groupKey.split("\\|");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Formato de clave de grupo inválido: " + groupKey);
        }
        return parts;
    }
    
    @Override
    public List<CostingGroupDTO> obtenerGruposDeCosteo() {
        return execute(em -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<CostingGroupDTO> cq = cb.createQuery(CostingGroupDTO.class);
            Root<KardexEntity> k = cq.from(KardexEntity.class);

            cq.select(cb.construct(
                CostingGroupDTO.class, 
                k.get("claveAgrupacion"),
                cb.min(k.get("fechaCreacion")), 
                k.get("instrumento").get("instrumentoNemo"), 
                k.get("empresa").get("razonSocial"),
                k.get("cuenta")
            ));

            cq.groupBy(
                k.get("claveAgrupacion"), 
                k.get("instrumento").get("instrumentoNemo"), 
                k.get("empresa").get("razonSocial"), 
                k.get("cuenta")
            );
            
            cq.orderBy(
                cb.asc(k.get("instrumento").get("instrumentoNemo")), 
                cb.asc(k.get("empresa").get("razonSocial"))
            );
            return em.createQuery(cq).getResultList();
        });
    }
}