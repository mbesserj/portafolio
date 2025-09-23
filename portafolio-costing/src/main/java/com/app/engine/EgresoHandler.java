package com.app.engine;

import com.app.entities.*;
import com.app.repository.TipoMovimientoRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.Queue;

public class EgresoHandler {

    public record EgresoResult(BigDecimal nuevoSaldoCantidad, BigDecimal nuevoSaldoValor) {
    }

    private static final Logger logger = LoggerFactory.getLogger(EgresoHandler.class);
    private final EntityManager em;
    private final TipoMovimientoRepository tipoMovimientoRepository;
    private final KardexFactory kardexFactory = new KardexFactory();
    private static final BigDecimal TOLERANCIA_AJUSTE = new BigDecimal("0.5");

    public EgresoHandler(EntityManager em, TipoMovimientoRepository repo) {
        this.em = em;
        this.tipoMovimientoRepository = repo;
    }

    public EgresoResult handle(TransaccionEntity egreso, Queue<IngresoDisponible> ingresosQueue, BigDecimal currentQty, BigDecimal currentVal, String clave) throws InsufficientBalanceException {
        BigDecimal cantidadEgreso = egreso.getCantidad();

        // 1. Verificar si hay saldo suficiente
        if (currentQty.compareTo(cantidadEgreso) < 0) {
            BigDecimal diferencia = cantidadEgreso.subtract(currentQty);
            // Aplicar ajuste por tolerancia si es necesario
            if (diferencia.abs().compareTo(TOLERANCIA_AJUSTE) <= 0) {
                logger.warn("Saldo casi suficiente para Tx ID: {}. Creando ajuste automático por tolerancia de {}", egreso.getId(), diferencia);
                KardexEntity kardexAjuste = crearAjusteAutomatico(egreso, diferencia, currentVal, currentQty);
                currentQty = currentQty.add(kardexAjuste.getCantidad());
                currentVal = currentVal.add(kardexAjuste.getCostoTotal());
                ingresosQueue.add(new IngresoDisponible(kardexAjuste));
            } else {
                throw new InsufficientBalanceException("Saldo insuficiente. Cantidad requerida: " + cantidadEgreso + ", disponible: " + currentQty);
            }
        }

        // 2. Lógica FIFO para consumir de la cola
        BigDecimal cantidadPendiente = cantidadEgreso;
        BigDecimal costoTotalCalculado = BigDecimal.ZERO;

        while (cantidadPendiente.compareTo(BigDecimal.ZERO) > 0) {
            IngresoDisponible ingresoFIFO = ingresosQueue.peek();
            if (ingresoFIFO == null) {
                throw new InsufficientBalanceException("Cola de ingresos vacía pero se intenta procesar egreso. Cantidad pendiente: " + cantidadPendiente);
            }

            BigDecimal cantidadUsada = cantidadPendiente.min(ingresoFIFO.cantidadDisponible);
            BigDecimal costoUnitario = ingresoFIFO.kardexIngreso.getCostoUnitario();
            BigDecimal costoParcial = cantidadUsada.multiply(costoUnitario);

            costoTotalCalculado = costoTotalCalculado.add(costoParcial);

            // Actualizar la cantidad disponible del ingreso consumido
            ingresoFIFO.cantidadDisponible = ingresoFIFO.cantidadDisponible.subtract(cantidadUsada);
            ingresoFIFO.kardexIngreso.setCantidadDisponible(ingresoFIFO.cantidadDisponible);
            em.merge(ingresoFIFO.kardexIngreso);

            cantidadPendiente = cantidadPendiente.subtract(cantidadUsada);

            // Crear el registro de Kardex para este consumo parcial del egreso
            BigDecimal saldoQtyParcial = currentQty.subtract(cantidadEgreso).add(cantidadPendiente);
            BigDecimal saldoValParcial = currentVal.subtract(costoTotalCalculado);
            KardexEntity kardexParcial = kardexFactory.createFromEgreso(egreso, cantidadUsada, costoParcial, saldoQtyParcial, saldoValParcial, clave);
            em.persist(kardexParcial);

            // Crear el detalle para trazabilidad
            crearDetalleCosteo(ingresoFIFO.kardexIngreso.getTransaccion(), egreso, cantidadUsada, costoParcial, clave);

            // Si el ingreso se consumió por completo, se retira de la cola
            if (ingresoFIFO.cantidadDisponible.compareTo(BigDecimal.ZERO) <= 0) {
                ingresosQueue.poll();
            }
        }

        BigDecimal nuevoSaldoCantidad = currentQty.subtract(cantidadEgreso);
        BigDecimal nuevoSaldoValor = currentVal.subtract(costoTotalCalculado);

        return new EgresoResult(nuevoSaldoCantidad, nuevoSaldoValor);
    }

    private void crearDetalleCosteo(TransaccionEntity ingreso, TransaccionEntity egreso, BigDecimal cantidad, BigDecimal costo, String clave) {
        DetalleCosteoEntity detalle = new DetalleCosteoEntity();
        detalle.setIngreso(ingreso);
        detalle.setEgreso(egreso);
        detalle.setCantidadUsada(cantidad);
        detalle.setCostoParcial(costo);
        detalle.setClaveAgrupacion(clave);
        em.persist(detalle);
    }

    private KardexEntity crearAjusteAutomatico(TransaccionEntity txOriginal, BigDecimal cantidadAjuste, BigDecimal saldoValor, BigDecimal saldoCantidad) {
        BigDecimal costoUnitarioAjuste = txOriginal.getPrecio() != null ? txOriginal.getPrecio() : BigDecimal.ZERO;
        BigDecimal montoAjuste = cantidadAjuste.multiply(costoUnitarioAjuste);

        // Busca o lanza error si no encuentra el tipo de movimiento para el ajuste
        TipoMovimientoEntity tipoMovimientoAjuste = tipoMovimientoRepository.buscarPorNombre("AJUSTE_AUTO_TOLERANCIA")
                .orElseThrow(() -> new IllegalStateException("El tipo de movimiento 'AJUSTE_AUTO_TOLERANCIA' no está configurado."));

        // Crea una nueva entidad de transacción para el ajuste
        TransaccionEntity ajusteTx = new TransaccionEntity();
        ajusteTx.setEmpresa(txOriginal.getEmpresa());
        ajusteTx.setCuenta(txOriginal.getCuenta());
        ajusteTx.setCustodio(txOriginal.getCustodio());
        ajusteTx.setInstrumento(txOriginal.getInstrumento());
        ajusteTx.setFecha(txOriginal.getFecha());
        ajusteTx.setTipoMovimiento(tipoMovimientoAjuste);
        ajusteTx.setCantidad(cantidadAjuste);
        ajusteTx.setPrecio(costoUnitarioAjuste);
        ajusteTx.setTotal(montoAjuste);
        ajusteTx.setGlosa("Ajuste automático por tolerancia para la transacción original ID: " + txOriginal.getId());
        ajusteTx.setCosteado(true); // El ajuste se considera costeado inmediatamente
        ajusteTx.setParaRevision(false);
        em.persist(ajusteTx);

        // Crea el registro de Kardex para este nuevo ingreso por ajuste
        BigDecimal nuevoSaldoCantidad = saldoCantidad.add(cantidadAjuste);
        BigDecimal nuevoSaldoValor = saldoValor.add(montoAjuste);
        String claveAgrupacion = txOriginal.getEmpresa().getId() + "|" + txOriginal.getCuenta() + "|" + txOriginal.getCustodio().getId() + "|" + txOriginal.getInstrumento().getId();

        return kardexFactory.createFromIngreso(ajusteTx, nuevoSaldoCantidad, nuevoSaldoValor, claveAgrupacion);
    }
}
