package com.app.engine;

import com.app.entities.TransaccionEntity;
import com.app.enums.TipoEnumsCosteo;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.app.interfaces.KardexApiInterfaz;
import com.app.interfaces.SaldoApiInterfaz;
import com.app.interfaces.TipoMovimientoInterfaz;

public class FifoCostingEngine {

    private static final Logger logger = LoggerFactory.getLogger(FifoCostingEngine.class);
    private final EntityManager em;
    private final KardexApiInterfaz kardexRepository;
    private final SaldoApiInterfaz saldoRepository;
    private final TipoMovimientoInterfaz tipoMovimientoRepository;

    public FifoCostingEngine(EntityManager em, KardexApiInterfaz kardexRepository,
                           SaldoApiInterfaz saldoRepository,
                           TipoMovimientoInterfaz tipoMovimientoRepository) {
        this.em = em;
        this.kardexRepository = kardexRepository;
        this.saldoRepository = saldoRepository;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
    }

    public void procesarCosteo() {
        // 1. OBTENER TRANSACCIONES
        List<TransaccionEntity> transacciones = findUncostedTransactions();
        logger.info("Transacciones encontradas para procesar: {}", transacciones.size());

        // 2. AGRUPAR
        Map<String, List<TransaccionEntity>> grupos = transacciones.stream()
                .collect(Collectors.groupingBy(this::claveAgrupacion));

        // 3. DELEGAR PROCESAMIENTO POR GRUPO
        for (Map.Entry<String, List<TransaccionEntity>> entry : grupos.entrySet()) {
            logger.info("Procesando grupo de costeo: {}", entry.getKey());
            
            // Creamos un procesador específico para este grupo
            CostingGroupProcessor groupProcessor = new CostingGroupProcessor(
                entry.getKey(),
                entry.getValue(),
                em,
                kardexRepository,
                saldoRepository,
                tipoMovimientoRepository
            );
            
            groupProcessor.process(); // ¡Y a procesar!
        }
        em.flush();
    }

    private List<TransaccionEntity> findUncostedTransactions() {
        return em.createQuery("""
            SELECT t FROM TransaccionEntity t
            WHERE t.tipoMovimiento.movimientoContable.tipoContable <> :noCostear
              AND t.costeado = false
              AND t.paraRevision = false
              AND t.ignorarEnCosteo = false
            ORDER BY t.fecha ASC,
                     CASE WHEN t.tipoMovimiento.esSaldoInicial = true THEN 0 ELSE 1 END,
                     CASE WHEN t.tipoMovimiento.movimientoContable.tipoContable = 'INGRESO' THEN 2 ELSE 3 END,
                     t.id ASC
            """, TransaccionEntity.class)
            .setParameter("noCostear", TipoEnumsCosteo.NO_COSTEAR)
            .getResultList();
    }

    private String claveAgrupacion(TransaccionEntity t) {
        return t.getEmpresa().getId() + "|" + t.getCuenta() + "|" + t.getCustodio().getId() + "|" + t.getInstrumento().getId();
    }
}