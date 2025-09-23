package com.app.service;

import com.app.dto.ResultadoInstrumentoDto;
import com.app.entities.KardexEntity;
import com.app.entities.SaldoEntity;
import com.app.repository.ResultadoInstrumentoRepository; 
import com.costing.api.KardexApi;
import java.math.BigDecimal;
import java.util.*;

/**
 * Servicio refactorizado para la lógica de negocio del reporte de resultados.
 * Delega todo el acceso a datos al ResultadoInstrumentoRepository.
 */
public class ResultadoInstrumentoService {

    private final KardexApi kardexService;
    private final ResultadoInstrumentoRepository resultadoInstrumentoRepository;

    /**
     * Constructor actualizado para recibir el repositorio como dependencia.
     */
    public ResultadoInstrumentoService(KardexApi kardexService, ResultadoInstrumentoRepository resultadoInstrumentoRepository) {
        this.kardexService = kardexService;
        this.resultadoInstrumentoRepository = resultadoInstrumentoRepository;
    }

    public List<ResultadoInstrumentoDto> obtenerHistorialResultados(Long empresaId, Long custodioId, String cuenta, Long instrumentoId) {
        try {
            // 1. Obtener datos desde el repositorio, no directamente con EntityManager
            List<ResultadoInstrumentoDto> historialOperaciones = resultadoInstrumentoRepository.findOperacionesByFiltro(empresaId, custodioId, cuenta, instrumentoId);
            List<ResultadoInstrumentoDto> historialDividendos = resultadoInstrumentoRepository.findDividendosByFiltro(empresaId, custodioId, cuenta, instrumentoId);
            Map<Long, BigDecimal> mapaDeGastos = resultadoInstrumentoRepository.findGastosByFiltro(empresaId, custodioId, cuenta, instrumentoId);

            // 2. Combinar y ordenar
            List<ResultadoInstrumentoDto> historialCompleto = new ArrayList<>();
            historialCompleto.addAll(historialOperaciones);
            historialCompleto.addAll(historialDividendos);
            
            // Se añade un comparador a prueba de nulos para las filas especiales (TOTALES) que no tienen fecha
            historialCompleto.sort(Comparator.comparing(ResultadoInstrumentoDto::getFecha, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(dto -> dto.getIdTransaccion() != null ? dto.getIdTransaccion() : Long.MAX_VALUE));

            // Asignar gastos
            historialCompleto.forEach(dto -> {
                if (dto.getIdTransaccion() != null) {
                    dto.setGastos(mapaDeGastos.getOrDefault(dto.getIdTransaccion(), BigDecimal.ZERO));
                }
            });

            // 3. Calcular utilidad neta por operación
            calcularUtilidadPorOperacion(historialCompleto);

            // 4. Añadir filas de resumen
            if (!historialCompleto.isEmpty()) {
                añadirFilaDeTotales(historialCompleto);
                añadirFilaUtilidadNoRealizada(historialCompleto, empresaId, custodioId, cuenta, instrumentoId);
            }

            return historialCompleto;

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
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
        BigDecimal totalCompras = historial.stream().map(ResultadoInstrumentoDto::getCompras).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalVentas = historial.stream().map(ResultadoInstrumentoDto::getVentas).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCostoVenta = historial.stream().map(ResultadoInstrumentoDto::getCostoDeVenta).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDividendos = historial.stream().map(ResultadoInstrumentoDto::getDividendos).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGastos = historial.stream().map(ResultadoInstrumentoDto::getGastos).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalUtilidadRealizada = historial.stream()
                .filter(dto -> !"TOTALES".equals(dto.getTipoMovimiento()) && !"Utilidad/Pérdida no Realizada".equals(dto.getTipoMovimiento()))
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

    private void añadirFilaUtilidadNoRealizada(List<ResultadoInstrumentoDto> historial, Long empresaId, Long custodioId, String cuenta, Long instrumentoId) {
        Optional<KardexEntity> ultimoKardexOpt = kardexService.findLastByGroup(empresaId, cuenta, custodioId, instrumentoId);

        if (ultimoKardexOpt.isPresent() && ultimoKardexOpt.get().getSaldoCantidad().compareTo(BigDecimal.ZERO) > 0) {
            KardexEntity ultimoKardex = ultimoKardexOpt.get();
            BigDecimal costoActual = ultimoKardex.getSaldoValor();
            BigDecimal cantidadActual = ultimoKardex.getSaldoCantidad();

            Optional<SaldoEntity> ultimoSaldoOpt = kardexService.obtenerUltimoSaldo(empresaId, custodioId, cuenta, instrumentoId);

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