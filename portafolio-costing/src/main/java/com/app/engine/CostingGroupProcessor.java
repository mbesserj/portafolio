package com.app.engine;

import com.app.entities.*;
import com.app.enums.TipoEnumsCosteo;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.app.interfaz.KardexApiInterfaz;
import com.app.interfaz.SaldoApiInterfaz;
import com.app.interfaz.TipoMovimientoInterfaz;

// Esta clase maneja el estado y el flujo de un solo grupo
public class CostingGroupProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CostingGroupProcessor.class);

    // Dependencias
    private final EntityManager em;
    private final KardexApiInterfaz kardexRepository;
    private final SaldoApiInterfaz saldoRepository;

    // Handlers especialistas
    private final IngresoHandler ingresoHandler;
    private final EgresoHandler egresoHandler;

    // Estado del grupo
    private final String claveAgrupacion;
    private final List<TransaccionEntity> transactions;
    private BigDecimal saldoCantidad;
    private BigDecimal saldoValor;
    private final Queue<IngresoDisponible> ingresosQueue = new LinkedList<>();
    private final Map<String, SaldoKardexEntity> cacheSaldos = new HashMap<>();

    public CostingGroupProcessor(String claveAgrupacion, List<TransaccionEntity> transactions, EntityManager em,
            KardexApiInterfaz kardexRepository, SaldoApiInterfaz saldoRepository,
            TipoMovimientoInterfaz tipoMovimientoRepository) {
        this.claveAgrupacion = claveAgrupacion;
        this.transactions = transactions;
        this.em = em;
        this.kardexRepository = kardexRepository;
        this.saldoRepository = saldoRepository;

        // Inicializamos los especialistas
        this.ingresoHandler = new IngresoHandler(em);
        this.egresoHandler = new EgresoHandler(em, tipoMovimientoRepository);
    }

    public void process() {
        if (transactions == null || transactions.isEmpty()) {
            logger.warn("No hay transacciones para procesar en el grupo: {}", claveAgrupacion);
            return;
        }

        logger.info("Iniciando procesamiento del grupo: {} con {} transacciones",
                claveAgrupacion, transactions.size());

        initializeBalances();
        initializeFifoQueue();

        boolean hasFailed = false;
        int processedCount = 0;

        for (TransaccionEntity tx : transactions) {
            if (hasFailed) {
                markTransactionForRevision(tx);
                continue;
            }

            try {
                if (tx.getTipoMovimiento().getMovimientoContable().getTipoContable() == TipoEnumsCosteo.INGRESO) {

                    // Usar el resultado completo del IngresoHandler
                    IngresoHandler.IngresoResult result = ingresoHandler.handle(tx, ingresosQueue, saldoCantidad, saldoValor, claveAgrupacion);
                    saldoCantidad = result.nuevoSaldoCantidad();
                    saldoValor = result.nuevoSaldoValor();

                } else if (tx.getTipoMovimiento().getMovimientoContable().getTipoContable() == TipoEnumsCosteo.EGRESO) {

                    EgresoHandler.EgresoResult result = egresoHandler.handle(tx, ingresosQueue, saldoCantidad, saldoValor, claveAgrupacion);
                    saldoCantidad = result.nuevoSaldoCantidad();
                    saldoValor = result.nuevoSaldoValor();
                }

                markTransactionAsCosted(tx);
                updateSaldoKardex(tx, saldoCantidad, saldoValor);
                processedCount++;

                logger.debug("Transacción procesada exitosamente - ID: {}, Tipo: {}",
                        tx.getId(), tx.getTipoMovimiento().getMovimientoContable().getTipoContable());

            } catch (InsufficientBalanceException e) {
                logger.error("Error de saldo insuficiente para Tx ID: {}. Marcando para revisión.", tx.getId(), e);
                markTransactionForRevision(tx);
                hasFailed = true;
            } catch (Exception e) {
                logger.error("Error inesperado procesando Tx ID: {}. Marcando para revisión.", tx.getId(), e);
                markTransactionForRevision(tx);
                hasFailed = true;
            }
        }

        if (!hasFailed) {
            updateSaldosDiarios();
            logger.info("Grupo procesado exitosamente: {} - {} transacciones procesadas",
                    claveAgrupacion, processedCount);
        } else {
            logger.warn("Grupo procesado con errores: {} - {} transacciones procesadas, resto marcado para revisión",
                    claveAgrupacion, processedCount);
        }
    }

    // --- Métodos de inicialización y actualización (extraídos de la clase original) ---
    private void initializeBalances() {
        TransaccionEntity firstTx = transactions.get(0);
        if (firstTx.getTipoMovimiento().isEsSaldoInicial()) {
            logger.info("Detectado inicio de historial para el grupo {}. Se parte de saldos CERO.", claveAgrupacion);
            this.saldoCantidad = BigDecimal.ZERO;
            this.saldoValor = BigDecimal.ZERO;
        } else {
            Optional<KardexEntity> ultimoKardex = kardexRepository.findLastByGroupBeforeDate(
                    firstTx.getEmpresa().getId(), firstTx.getCuenta(), firstTx.getCustodio().getId(), firstTx.getInstrumento().getId(), firstTx.getFecha());
            this.saldoCantidad = ultimoKardex.map(KardexEntity::getSaldoCantidad).orElse(BigDecimal.ZERO);
            this.saldoValor = ultimoKardex.map(KardexEntity::getSaldoValor).orElse(BigDecimal.ZERO);
            logger.info("Continuando historial para el grupo {}. Saldo inicial: Cantidad={}, Valor={}", claveAgrupacion, saldoCantidad, saldoValor);
        }
    }

    private void initializeFifoQueue() {
        TransaccionEntity firstTx = transactions.get(0);

        List<KardexEntity> ingresosHistoricos = em.createQuery("""
            SELECT k FROM KardexEntity k
            WHERE k.claveAgrupacion = :clave
              AND k.tipoContable = :tipoIngreso
              AND k.cantidadDisponible > 0
              AND k.fechaTransaccion < :hastaFecha
            """, KardexEntity.class)
                .setParameter("clave", this.claveAgrupacion)
                .setParameter("tipoIngreso", TipoEnumsCosteo.INGRESO)
                .setParameter("hastaFecha", firstTx.getFecha())
                .getResultList();

        ingresosHistoricos.stream()
                .sorted(Comparator.comparing(KardexEntity::getFechaTransaccion).thenComparing(KardexEntity::getId))
                .forEach(k -> this.ingresosQueue.add(new IngresoDisponible(k)));

        logger.debug("Cola FIFO inicializada con {} ingresos históricos disponibles.", this.ingresosQueue.size());
    }

    private void updateSaldoKardex(TransaccionEntity tx, BigDecimal nuevoSaldoCantidad, BigDecimal nuevoCostoTotal) {
        SaldoKardexEntity saldo = cacheSaldos.get(claveAgrupacion);
        if (saldo == null) {
            saldo = this.saldoRepository.findByGrupo(
                    tx.getEmpresa().getId(), tx.getCustodio().getId(), tx.getInstrumento().getId(), tx.getCuenta()
            ).orElseGet(() -> {
                SaldoKardexEntity nuevoSaldo = new SaldoKardexEntity();
                nuevoSaldo.setEmpresa(this.em.find(EmpresaEntity.class, tx.getEmpresa().getId()));
                nuevoSaldo.setCustodio(this.em.find(CustodioEntity.class, tx.getCustodio().getId()));
                nuevoSaldo.setInstrumento(this.em.find(InstrumentoEntity.class, tx.getInstrumento().getId()));
                nuevoSaldo.setCuenta(tx.getCuenta());
                return nuevoSaldo;
            });
            cacheSaldos.put(claveAgrupacion, saldo);
        }

        saldo.setSaldoCantidad(nuevoSaldoCantidad);
        saldo.setCostoTotal(nuevoCostoTotal);
        saldo.recalcularCostoPromedio();
        saldo.setFechaUltimaActualizacion(tx.getFecha());

        if (saldo.getId() == null) {
            this.em.persist(saldo);
        } else {
            this.em.merge(saldo);
        }
    }

    private void updateSaldosDiarios() {
        if (transactions == null || transactions.isEmpty()) {
            return;
        }

        TransaccionEntity primeraTx = transactions.get(0);

        // Determinar el rango de fechas afectado por las transacciones procesadas
        LocalDate fechaInicioRango = transactions.stream()
                .map(TransaccionEntity::getFecha)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());

        LocalDate fechaFinRango = transactions.stream()
                .map(TransaccionEntity::getFecha)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());

        logger.debug("Actualizando saldos diarios para el grupo {} desde {} hasta {}", claveAgrupacion, fechaInicioRango, fechaFinRango);

        // 1. Obtener el último saldo DIARIO justo ANTES de que comience nuestro rango
        Optional<SaldosDiariosEntity> ultimoSaldoPrevio = saldoRepository.findLastBeforeDate(
                primeraTx.getEmpresa(), primeraTx.getCustodio(), primeraTx.getInstrumento(), primeraTx.getCuenta(), fechaInicioRango);

        BigDecimal saldoCantidadAnterior = ultimoSaldoPrevio.map(SaldosDiariosEntity::getSaldoCantidad).orElse(BigDecimal.ZERO);
        BigDecimal saldoValorAnterior = ultimoSaldoPrevio.map(SaldosDiariosEntity::getSaldoValor).orElse(BigDecimal.ZERO);

        // 2. Obtener todos los movimientos del KARDEX dentro del rango, agrupados por día
        Map<LocalDate, List<KardexEntity>> kardexPorDia = kardexRepository.findByGroupAndDateRange(
                primeraTx.getEmpresa().getId(), primeraTx.getCustodio().getId(), primeraTx.getInstrumento().getId(), primeraTx.getCuenta(),
                fechaInicioRango, fechaFinRango
        ).stream().collect(Collectors.groupingBy(KardexEntity::getFechaTransaccion));

        // 3. Obtener los registros de SaldosDiarios que YA EXISTEN en la BD para nuestro rango
        Map<LocalDate, SaldosDiariosEntity> saldosDiariosExistentes = saldoRepository.findAllByGroupAndDateRange(
                primeraTx.getEmpresa(), primeraTx.getCustodio(), primeraTx.getInstrumento(), primeraTx.getCuenta(),
                fechaInicioRango, fechaFinRango
        ).stream().collect(Collectors.toMap(SaldosDiariosEntity::getFecha, Function.identity()));

        // 4. Iterar día por día y calcular/actualizar el saldo de cierre
        for (LocalDate dia = fechaInicioRango; !dia.isAfter(fechaFinRango); dia = dia.plusDays(1)) {
            List<KardexEntity> movimientosDelDia = kardexPorDia.get(dia);

            // Si hubo movimientos en este día, el saldo de cierre es el del último movimiento
            if (movimientosDelDia != null && !movimientosDelDia.isEmpty()) {
                KardexEntity ultimoMovimientoDelDia = movimientosDelDia.stream()
                        .max(Comparator.comparing(KardexEntity::getId))
                        .get(); // .get() es seguro aquí porque la lista no está vacía

                saldoCantidadAnterior = ultimoMovimientoDelDia.getSaldoCantidad();
                saldoValorAnterior = ultimoMovimientoDelDia.getSaldoValor();
            }
            // Si no hubo movimientos, simplemente arrastramos el saldo del día anterior

            SaldosDiariosEntity saldoDelDia = saldosDiariosExistentes.get(dia);

            if (saldoDelDia != null) { // Si ya existía un registro para este día, lo actualizamos
                saldoDelDia.setSaldoCantidad(saldoCantidadAnterior);
                saldoDelDia.setSaldoValor(saldoValorAnterior);
                em.merge(saldoDelDia);
            } else { // Si no existía, creamos uno nuevo
                saldoDelDia = new SaldosDiariosEntity();
                saldoDelDia.setFecha(dia);
                saldoDelDia.setEmpresa(primeraTx.getEmpresa());
                saldoDelDia.setCustodio(primeraTx.getCustodio());
                saldoDelDia.setInstrumento(primeraTx.getInstrumento());
                saldoDelDia.setCuenta(primeraTx.getCuenta());
                saldoDelDia.setSaldoCantidad(saldoCantidadAnterior);
                saldoDelDia.setSaldoValor(saldoValorAnterior);
                em.persist(saldoDelDia);
            }
        }
    }

    private void markTransactionForRevision(TransaccionEntity tx) {
        tx.setCosteado(false);
        tx.setParaRevision(true);
        em.merge(tx);
    }

    private void markTransactionAsCosted(TransaccionEntity tx) {
        tx.setCosteado(true);
        tx.setParaRevision(false);
        em.merge(tx);
    }
}
