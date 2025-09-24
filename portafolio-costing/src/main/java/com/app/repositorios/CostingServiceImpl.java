package com.app.repositorios;

import com.app.interfaz.AbstractRepository;
import com.app.interfaz.CostingApiInterfaz;
import com.app.dto.AjustePropuestoDto;
import com.app.dto.CostingGroupDTO;
import com.app.engine.FifoCostingEngine;
import com.app.entities.KardexEntity;
import com.app.entities.TransaccionEntity;
import com.app.enums.TipoAjuste;
import com.app.exception.CostingException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.app.interfaz.KardexApiInterfaz;
import com.app.interfaz.SaldoApiInterfaz;

/**
 * Implementación central de la lógica de negocio para el módulo de costeo.
 * Implementa la API pública y orquesta las operaciones con los repositorios.
 */
public class CostingServiceImpl extends AbstractRepository implements CostingApiInterfaz {

    private static final Logger logger = LoggerFactory.getLogger(CostingServiceImpl.class);
    private KardexApiInterfaz kardexRepository;
    private SaldoApiInterfaz saldoRepository;
    private ResetCosteoServiceImpl resetCosteoRepository;
    private AjustesProcess ajustesProcess;

    public CostingServiceImpl() {
        super();
    }

    public CostingServiceImpl(KardexApiInterfaz kardexRepo,
            SaldoApiInterfaz saldoRepo,
            ResetCosteoServiceImpl resetRepo,
            AjustesProcess ajustesProcess) {
        super();
        this.kardexRepository = kardexRepo;
        this.saldoRepository = saldoRepo;
        this.resetCosteoRepository = resetRepo;
        this.ajustesProcess = ajustesProcess;
    }

    /**
     * 
     * @throws CostingException 
     */
    @Override
    public void ejecutarCosteoCompleto() throws CostingException {
        try {
            executeInTransaction(em -> {
                createEngine().procesarCosteo();
            });
        } catch (Exception e) {
            throw new CostingException("Falló el proceso de costeo completo.", e);
        }
    }

    /**
     * 
     * @return
     * @throws CostingException 
     */
    @Override
    public List<CostingGroupDTO> obtenerGruposCosteo() throws CostingException {
        try {
            return obtenerGruposDeCosteo();
        } catch (Exception e) {
            throw new CostingException("No se pudo obtener la lista de grupos de costeo.", e);
        }
    }

    /**
     * 
     * @param groupKey
     * @throws CostingException 
     */
    @Override
    public void recostearGrupo(String groupKey) throws CostingException {
        try {
            executeInTransaction(em -> {
                String[] parts = parseGroupKey(groupKey);
                Long empresaId = Long.parseLong(parts[0]);
                String cuenta = parts[1];
                Long custodioId = Long.parseLong(parts[2]);
                Long instrumentoId = Long.parseLong(parts[3]);

                new KardexServiceImpl().deleteDetalleCosteoByClaveAgrupacion(groupKey);
                new KardexServiceImpl().deleteKardexByClaveAgrupacion(groupKey);
                new KardexServiceImpl().deleteSaldoKardexByGrupo(empresaId, custodioId, instrumentoId, cuenta);
                new ResetCosteoServiceImpl().resetCosteoFlagsByGrupo(empresaId, cuenta, custodioId, instrumentoId);

                createEngine().procesarCosteo();
            });
        } catch (Exception e) {
            throw new CostingException("Falló el recosteo del grupo: " + groupKey, e);
        }
    }

    /**
     * 
     * @param txReferenciaId
     * @param tipo
     * @return
     * @throws CostingException 
     */
    @Override
    public AjustePropuestoDto proponerAjuste(Long txReferenciaId, TipoAjuste tipo) throws CostingException {
        try {
            return executeReadOnly(em -> {
                TransaccionEntity tx = findTransaction(txReferenciaId);
                return createAjustesProcess().proponerAjusteManual(tx, tipo);
            });
        } catch (Exception e) {
            logger.error("Error al proponer ajuste para Tx ID: {}", txReferenciaId, e);
            throw new CostingException("Falló la propuesta de ajuste: " + e.getMessage(), e);
        }
    }

    /**
     * 
     * @param txReferenciaId
     * @param tipo
     * @param cantidad
     * @param precio
     * @throws CostingException 
     */
    @Override
    public void crearAjuste(Long txReferenciaId, TipoAjuste tipo, BigDecimal cantidad, BigDecimal precio) throws CostingException {
        try {
            executeInTransaction(em -> {
                TransaccionEntity tx = findTransaction(txReferenciaId);
                createAjustesProcess().crearAjusteManual(tx, tipo, cantidad, precio);
            });
        } catch (Exception e) {
            logger.error("Error al crear ajuste para Tx ID: {}", txReferenciaId, e);
            throw new CostingException("Falló la creación de ajuste: " + e.getMessage(), e);
        }
    }

    /**
     * 
     * @param idAjuste
     * @throws CostingException 
     */
    @Override
    public void eliminarAjuste(Long idAjuste) throws CostingException {
        try {
            executeInTransaction(em -> {
                try {
                    createAjustesProcess().eliminarAjusteManual(idAjuste);
                } catch (Exception ex) {
                    throw new RuntimeException(ex); // Envolver excepción verificada
                }
            });
        } catch (Exception e) {
            throw new CostingException("Falló la eliminación de ajuste: " + e.getMessage(), e);
        }
    }

    
    /**
     * 
     * @return 
     */
    private FifoCostingEngine createEngine() {
        return new FifoCostingEngine(this.em, new KardexServiceImpl(), new SaldosServiceImpl(), new TipoMovimientoServiceImpl());
    }

    /**
     * 
     * @return 
     */
    private AjustesProcess createAjustesProcess() {
        return new AjustesProcess(new SaldosServiceImpl(), new TipoMovimientoServiceImpl(), new KardexServiceImpl());
    }

    /**
     * 
     * @param txId
     * @return 
     */
    private TransaccionEntity findTransaction(Long txId) {
        TransaccionEntity tx = em.find(TransaccionEntity.class, txId);
        if (tx == null) {
            throw new IllegalArgumentException("No se encontró la transacción con ID: " + txId);
        }
        return tx;
    }

    /**
     * 
     * @param groupKey
     * @return 
     */
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

    /**
     * 
     * @return 
     */
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