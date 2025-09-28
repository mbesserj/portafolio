package com.costing.process;

import com.model.interfaces.AbstractRepository;
import com.model.interfaces.CostingApi;
import com.model.dto.AjustePropuestoDto;
import com.model.dto.CostingGroupDTO;
import com.costing.engine.FifoCostingEngine;
import com.model.entities.KardexEntity;
import com.model.entities.TransaccionEntity;
import com.model.enums.TipoAjuste;
import com.model.exception.CostingException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.model.interfaces.KardexApi;
import com.model.interfaces.SaldoApi;
import com.model.interfaces.TipoMovimiento;
import jakarta.persistence.EntityManager;

/**
 * Implementación central corregida de la lógica de negocio para el módulo de costeo.
 */
public class CostingServiceImpl extends AbstractRepository implements CostingApi {

    private static final Logger logger = LoggerFactory.getLogger(CostingServiceImpl.class);

    // --- DEPENDENCIAS INYECTADAS ---
    private final KardexApi kardexRepository;
    private final SaldoApi saldoRepository;
    private final ResetCosteoServiceImpl resetCosteoRepository;
    private final AjustesProcess ajustesProcess;
    private final TipoMovimiento tipoMovimientoRepository; 

    // --- ÚNICO CONSTRUCTOR ---
    public CostingServiceImpl(KardexApi kardexRepo,
                              SaldoApi saldoRepo,
                              ResetCosteoServiceImpl resetRepo,
                              AjustesProcess ajustesProcess,
                              TipoMovimiento tipoMovimientoRepo) {
        super();
        this.kardexRepository = kardexRepo;
        this.saldoRepository = saldoRepo;
        this.resetCosteoRepository = resetRepo;
        this.ajustesProcess = ajustesProcess;
        this.tipoMovimientoRepository = tipoMovimientoRepo;
    }

    @Override
    public void ejecutarCosteoCompleto() throws CostingException {
        try {
            executeInTransaction(entityManager -> {
                // Crear engine con el EntityManager actual
                FifoCostingEngine engine = new FifoCostingEngine(entityManager, 
                    new KardexServiceImpl(), 
                    new SaldosServiceImpl(), 
                    new TipoMovimientoServiceImpl());
                engine.procesarCosteo();
                return null;
            });
        } catch (Exception e) {
            throw new CostingException("Falló el proceso de costeo completo.", e);
        }
    }

    @Override
    public List<CostingGroupDTO> obtenerGruposCosteo() throws CostingException {
        try {
            return obtenerGruposDeCosteo();
        } catch (Exception e) {
            throw new CostingException("No se pudo obtener la lista de grupos de costeo.", e);
        }
    }

    @Override
    public void recostearGrupo(String groupKey) throws CostingException {
        try {
            executeInTransaction(entityManager -> {
                String[] parts = parseGroupKey(groupKey);
                Long empresaId = Long.parseLong(parts[0]);
                String cuenta = parts[1];
                Long custodioId = Long.parseLong(parts[2]);
                Long instrumentoId = Long.parseLong(parts[3]);

                // Usar las implementaciones de servicio para las operaciones
                new KardexServiceImpl().deleteDetalleCosteoByClaveAgrupacion(groupKey);
                new KardexServiceImpl().deleteKardexByClaveAgrupacion(groupKey);
                new KardexServiceImpl().deleteSaldoKardexByGrupo(empresaId, custodioId, instrumentoId, cuenta);
                new ResetCosteoServiceImpl().resetCosteoFlagsByGrupo(empresaId, cuenta, custodioId, instrumentoId);

                // Crear nuevo engine con el EntityManager actual
                FifoCostingEngine engine = new FifoCostingEngine(em, 
                    new KardexServiceImpl(), 
                    new SaldosServiceImpl(), 
                    new TipoMovimientoServiceImpl());
                engine.procesarCosteo();
                return null;
            });
        } catch (Exception e) {
            throw new CostingException("Falló el recosteo del grupo: " + groupKey, e);
        }
    }

    @Override
    public AjustePropuestoDto proponerAjuste(Long txReferenciaId, TipoAjuste tipo) throws CostingException {
        try {
            return executeReadOnly(entityManager -> {
                TransaccionEntity tx = findTransaction(entityManager, txReferenciaId);
                return createAjustesProcess().proponerAjusteManual(tx, tipo);
            });
        } catch (Exception e) {
            logger.error("Error al proponer ajuste para Tx ID: {}", txReferenciaId, e);
            throw new CostingException("Falló la propuesta de ajuste: " + e.getMessage(), e);
        }
    }

    @Override
    public void crearAjuste(Long txReferenciaId, TipoAjuste tipo, BigDecimal cantidad, BigDecimal precio) throws CostingException {
        try {
            executeInTransaction(entityManager -> {
                TransaccionEntity tx = findTransaction(entityManager, txReferenciaId);
                createAjustesProcess().crearAjusteManual(tx, tipo, cantidad, precio);
                return null;
            });
        } catch (Exception e) {
            logger.error("Error al crear ajuste para Tx ID: {}", txReferenciaId, e);
            throw new CostingException("Falló la creación de ajuste: " + e.getMessage(), e);
        }
    }

    @Override
    public void eliminarAjuste(Long idAjuste) throws CostingException {
        try {
            executeInTransaction(entityManager -> {
                createAjustesProcess().eliminarAjusteManual(idAjuste);
                return null;
            });
        } catch (Exception e) {
            throw new CostingException("Falló la eliminación de ajuste: " + e.getMessage(), e);
        }
    }

    public List<CostingGroupDTO> obtenerGruposDeCosteo() {
        return executeReadOnly(entityManager -> {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
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

    // Métodos auxiliares privados
    private AjustesProcess createAjustesProcess() {
        return new AjustesProcess(new SaldosServiceImpl(), new TipoMovimientoServiceImpl(), new KardexServiceImpl());
    }

    private TransaccionEntity findTransaction(EntityManager entityManager, Long txId) {
        TransaccionEntity tx = entityManager.find(TransaccionEntity.class, txId);
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
}