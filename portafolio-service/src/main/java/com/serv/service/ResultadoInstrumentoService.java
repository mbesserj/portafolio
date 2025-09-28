package com.serv.service;

import com.model.dto.ResultadoInstrumentoDto;
import com.model.entities.KardexEntity;
import com.model.entities.SaldoEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.*;
import com.model.interfaces.ResultadoRepository;
import com.model.interfaces.KardexApi;
import com.model.interfaces.SaldoApi;

/**
 * Servicio refactorizado para la lógica de negocio del reporte de resultados.
 * Delega todo el acceso a datos al ResultadoInstrumentoRepository.
 */
public class ResultadoInstrumentoService {

    private static final Logger logger = LoggerFactory.getLogger(ResultadoInstrumentoService.class);
    
    private final KardexApi kardexService;
    private final SaldoApi saldoService;
    private final ResultadoRepository resultadoInstrumentoRepository;

    /**
     * Constructor con inyección de dependencias.
     * 
     * @param kardexService Servicio para operaciones de kardex
     * @param resultadoInstrumentoRepository Repositorio para consultas de resultados
     * @throws IllegalArgumentException si algún parámetro es null
     */
    public ResultadoInstrumentoService(SaldoApi saldoService, KardexApi kardexService, ResultadoRepository resultadoInstrumentoRepository) {
        if (kardexService == null) {
            throw new IllegalArgumentException("KardexApi no puede ser null");
        }
        if (saldoService == null) {
            throw new IllegalArgumentException("SaldoApi no puede ser null");
        }
        if (resultadoInstrumentoRepository == null) {
            throw new IllegalArgumentException("ResultadoInstrumentoRepository no puede ser null");
        }
        this.kardexService = kardexService;
        this.saldoService = saldoService;
        this.resultadoInstrumentoRepository = resultadoInstrumentoRepository;
    }

    /**
     * Obtiene el historial completo de resultados para un instrumento específico.
     * 
     * @param empresaId ID de la empresa
     * @param custodioId ID del custodio
     * @param cuenta Nombre de la cuenta
     * @param instrumentoId ID del instrumento
     * @return Lista completa con historial, totales y utilidad no realizada
     */
    public List<ResultadoInstrumentoDto> obtenerHistorialResultados(Long empresaId, Long custodioId, 
                                                                   String cuenta, Long instrumentoId) {
        // Validaciones
        if (empresaId == null || custodioId == null || instrumentoId == null) {
            throw new IllegalArgumentException("Los IDs de empresa, custodio e instrumento no pueden ser null");
        }
        if (cuenta == null || cuenta.trim().isEmpty()) {
            throw new IllegalArgumentException("La cuenta no puede ser nula o vacía");
        }

        try {
            logger.debug("Obteniendo historial de resultados para instrumento {} en cuenta {}", instrumentoId, cuenta);
            
            // 1. Obtener datos desde el repositorio
            List<ResultadoInstrumentoDto> historialOperaciones = resultadoInstrumentoRepository
                .findOperacionesByFiltro(empresaId, custodioId, cuenta, instrumentoId);
            List<ResultadoInstrumentoDto> historialDividendos = resultadoInstrumentoRepository
                .findDividendosByFiltro(empresaId, custodioId, cuenta, instrumentoId);
            Map<Long, BigDecimal> mapaDeGastos = resultadoInstrumentoRepository
                .findGastosByFiltro(empresaId, custodioId, cuenta, instrumentoId);

            // 2. Combinar y ordenar
            List<ResultadoInstrumentoDto> historialCompleto = new ArrayList<>();
            historialCompleto.addAll(historialOperaciones);
            historialCompleto.addAll(historialDividendos);
            
            // Ordenar con comparador a prueba de nulos
            historialCompleto.sort(
                Comparator.comparing(ResultadoInstrumentoDto::getFecha, 
                    Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(dto -> dto.getIdTransaccion() != null ? dto.getIdTransaccion() : Long.MAX_VALUE)
            );

            // 3. Asignar gastos
            asignarGastos(historialCompleto, mapaDeGastos);

            // 4. Calcular utilidad neta por operación
            calcularUtilidadPorOperacion(historialCompleto);

            // 5. Añadir filas de resumen
            if (!historialCompleto.isEmpty()) {
                añadirFilaDeTotales(historialCompleto);
                añadirFilaUtilidadNoRealizada(historialCompleto, empresaId, custodioId, cuenta, instrumentoId);
            }

            logger.debug("Historial generado con {} registros", historialCompleto.size());
            return historialCompleto;

        } catch (Exception e) {
            logger.error("Error al obtener historial de resultados para instrumento {}", instrumentoId, e);
            return Collections.emptyList();
        }
    }

    private void asignarGastos(List<ResultadoInstrumentoDto> historial, Map<Long, BigDecimal> mapaDeGastos) {
        historial.forEach(dto -> {
            if (dto.getIdTransaccion() != null) {
                dto.setGastos(mapaDeGastos.getOrDefault(dto.getIdTransaccion(), BigDecimal.ZERO));
            }
        });
    }

    private void calcularUtilidadPorOperacion(List<ResultadoInstrumentoDto> historial) {
        for (ResultadoInstrumentoDto dto : historial) {
            BigDecimal utilidadDeLaLinea = BigDecimal.ZERO;

            if (dto.getUtilidadRealizada() != null) {
                utilidadDeLaLinea = utilidadDeLaLinea.add(dto.getUtilidadRealizada());
            }
            if (dto.getDividendos() != null) {
                utilidadDeLaLinea = utilidadDeLaLinea.add(dto.getDividendos());
            }
            if (dto.getGastos() != null) {
                utilidadDeLaLinea = utilidadDeLaLinea.subtract(dto.getGastos());
            }
            dto.setUtilidadRealizada(utilidadDeLaLinea);
        }
    }

    private void añadirFilaDeTotales(List<ResultadoInstrumentoDto> historial) {
        BigDecimal totalCompras = sumarCampo(historial, ResultadoInstrumentoDto::getCompras);
        BigDecimal totalVentas = sumarCampo(historial, ResultadoInstrumentoDto::getVentas);
        BigDecimal totalCostoVenta = sumarCampo(historial, ResultadoInstrumentoDto::getCostoDeVenta);
        BigDecimal totalDividendos = sumarCampo(historial, ResultadoInstrumentoDto::getDividendos);
        BigDecimal totalGastos = sumarCampo(historial, ResultadoInstrumentoDto::getGastos);
        BigDecimal totalUtilidadRealizada = historial.stream()
                .filter(dto -> !"TOTALES".equals(dto.getTipoMovimiento()) && 
                             !"Utilidad/Pérdida no Realizada".equals(dto.getTipoMovimiento()))
                .map(ResultadoInstrumentoDto::getUtilidadRealizada)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ResultadoInstrumentoDto totalesDto = new ResultadoInstrumentoDto("TOTALES");
        totalesDto.setCompras(totalCompras);
        totalesDto.setVentas(totalVentas);
        totalesDto.setCostoDeVenta(totalCostoVenta);
        totalesDto.setDividendos(totalDividendos);
        totalesDto.setGastos(totalGastos);
        totalesDto.setUtilidadRealizada(totalUtilidadRealizada);

        historial.add(totalesDto);
    }

    private BigDecimal sumarCampo(List<ResultadoInstrumentoDto> historial, 
                                 java.util.function.Function<ResultadoInstrumentoDto, BigDecimal> getter) {
        return historial.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void añadirFilaUtilidadNoRealizada(List<ResultadoInstrumentoDto> historial, Long empresaId, 
                                             Long custodioId, String cuenta, Long instrumentoId) {
        Optional<KardexEntity> ultimoKardexOpt = kardexService.findLastByGroup(empresaId, cuenta, custodioId, instrumentoId);

        if (ultimoKardexOpt.isPresent() && ultimoKardexOpt.get().getSaldoCantidad().compareTo(BigDecimal.ZERO) > 0) {
            KardexEntity ultimoKardex = ultimoKardexOpt.get();
            BigDecimal costoActual = ultimoKardex.getSaldoValor();
            BigDecimal cantidadActual = ultimoKardex.getSaldoCantidad();

            Optional<SaldoEntity> ultimoSaldoOpt = saldoService.obtenerUltimoSaldo(empresaId, custodioId, cuenta, instrumentoId);

            if (ultimoSaldoOpt.isPresent()) {
                BigDecimal precioDeMercado = ultimoSaldoOpt.get().getPrecio();
                BigDecimal valorDeMercadoTotal = precioDeMercado.multiply(cantidadActual);
                BigDecimal utilidadNoRealizada = valorDeMercadoTotal.subtract(costoActual);

                ResultadoInstrumentoDto unplDto = new ResultadoInstrumentoDto("Utilidad/Pérdida no Realizada");
                unplDto.setUtilidadRealizada(utilidadNoRealizada);

                historial.add(unplDto);
            }
        }
    }
}