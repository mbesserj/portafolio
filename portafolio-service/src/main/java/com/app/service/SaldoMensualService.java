package com.app.service;

import com.app.dto.SaldoMensualDto;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SaldoMensualService extends AbstractRepository {

    public List<SaldoMensualDto> obtenerSaldosMensuales(String razonSocial, String custodio, int anio, String moneda) {
        return executeReadOnly(em -> {
            try {
                // --- PASO 1: EJECUCIÓN DE CONSULTA ---
                String montoColumn = "USD".equalsIgnoreCase(moneda) ? "s.monto_usd" : "s.monto_clp";
                StringBuilder sqlBuilder = new StringBuilder();
                sqlBuilder.append(String.format("""
                    SELECT
                        i.nemo,
                        CASE WHEN i.instrumento = '--' THEN 'Caja' ELSE i.instrumento END AS instrumento_nemo,
                        SUM(CASE WHEN MONTH(s.fecha) = 1 THEN %s ELSE 0 END) AS Enero,
                        SUM(CASE WHEN MONTH(s.fecha) = 2 THEN %s ELSE 0 END) AS Febrero,
                        SUM(CASE WHEN MONTH(s.fecha) = 3 THEN %s ELSE 0 END) AS Marzo,
                        SUM(CASE WHEN MONTH(s.fecha) = 4 THEN %s ELSE 0 END) AS Abril,
                        SUM(CASE WHEN MONTH(s.fecha) = 5 THEN %s ELSE 0 END) AS Mayo,
                        SUM(CASE WHEN MONTH(s.fecha) = 6 THEN %s ELSE 0 END) AS Junio,
                        SUM(CASE WHEN MONTH(s.fecha) = 7 THEN %s ELSE 0 END) AS Julio,
                        SUM(CASE WHEN MONTH(s.fecha) = 8 THEN %s ELSE 0 END) AS Agosto,
                        SUM(CASE WHEN MONTH(s.fecha) = 9 THEN %s ELSE 0 END) AS Septiembre,
                        SUM(CASE WHEN MONTH(s.fecha) = 10 THEN %s ELSE 0 END) AS Octubre,
                        SUM(CASE WHEN MONTH(s.fecha) = 11 THEN %s ELSE 0 END) AS Noviembre,
                        SUM(CASE WHEN MONTH(s.fecha) = 12 THEN %s ELSE 0 END) AS Diciembre
                    FROM saldos s
                    JOIN instrumentos i ON s.instrumento_id = i.id
                    JOIN empresas e ON s.empresa_id = e.id
                    JOIN custodios c ON s.custodio_id = c.id
                    WHERE e.razonsocial = ?1
                      AND YEAR(s.fecha) = ?2
                      AND s.fecha IN (SELECT DISTINCT MAX(fecha) FROM saldos WHERE year(fecha) = ?2 GROUP BY month(fecha))
                """, montoColumn, montoColumn, montoColumn, montoColumn, montoColumn, montoColumn, 
                montoColumn, montoColumn, montoColumn, montoColumn, montoColumn, montoColumn));
                
                if (custodio != null && !custodio.isEmpty()) {
                    sqlBuilder.append(" AND c.custodio = ?3");
                }
                sqlBuilder.append(" GROUP BY i.nemo, i.instrumento ORDER BY i.nemo");

                Query query = em.createNativeQuery(sqlBuilder.toString(), "SaldoMensualMapping");
                query.setParameter(1, razonSocial);
                query.setParameter(2, anio);
                if (custodio != null && !custodio.isEmpty()) {
                    query.setParameter(3, custodio);
                }
                
                @SuppressWarnings("unchecked")
                List<SaldoMensualDto> resultados = query.getResultList();

                // --- PASO 2: FILTRADO ---
                List<SaldoMensualDto> resultadosFiltrados = resultados.stream()
                    .filter(dto -> dto.getSumaTotalAnual().compareTo(BigDecimal.ZERO) != 0)
                    .collect(Collectors.toList());

                if (resultadosFiltrados.isEmpty()) {
                    return Collections.emptyList();
                }

                // --- PASO 3: CÁLCULO DE FILAS DE RESUMEN ---
                SaldoMensualDto totalRow = calcularFilaTotal(resultadosFiltrados);
                int ultimoMesConDatos = encontrarUltimoMesConDatos(totalRow);
                
                SaldoMensualDto utilidadRow = calcularFilaUtilidad(totalRow, ultimoMesConDatos);
                SaldoMensualDto utilidadAcumuladaRow = calcularFilaUtilidadAcumulada(utilidadRow, ultimoMesConDatos);
                SaldoMensualDto variationAnualRow = calcularFilaDeVariaciones(totalRow, ultimoMesConDatos);
                SaldoMensualDto variationMensualRow = calcularFilaVariacionMensual(totalRow, ultimoMesConDatos);

                // --- PASO 4: AÑADIR FILAS DE RESUMEN A LA LISTA FINAL ---
                resultadosFiltrados.add(totalRow);
                resultadosFiltrados.add(utilidadRow);
                resultadosFiltrados.add(utilidadAcumuladaRow);
                resultadosFiltrados.add(variationAnualRow);
                resultadosFiltrados.add(variationMensualRow);
                
                return resultadosFiltrados;

            } catch (Exception e) {
                logger.error("Error al obtener los saldos mensuales.", e);
                return Collections.emptyList();
            }
        });
    }
    
    private int encontrarUltimoMesConDatos(SaldoMensualDto totalRow) {
        for (int mes = 12; mes >= 1; mes--) {
            if (getValorPorMes(totalRow, mes).compareTo(BigDecimal.ZERO) != 0) {
                return mes;
            }
        }
        return 0;
    }

    private SaldoMensualDto calcularFilaTotal(List<SaldoMensualDto> resultados) {
        SaldoMensualDto totalRow = new SaldoMensualDto();
        totalRow.setNemo("Total general");
        totalRow.setInstrumentoNemo("");
        for (SaldoMensualDto dto : resultados) {
            for (int i = 1; i <= 12; i++) {
                setValorPorMes(totalRow, i, getValorPorMes(totalRow, i).add(getValorPorMes(dto, i)));
            }
        }
        return totalRow;
    }

    private SaldoMensualDto calcularFilaUtilidad(SaldoMensualDto totalRow, int ultimoMesConDatos) {
        SaldoMensualDto utilidadRow = new SaldoMensualDto();
        utilidadRow.setNemo("Utilidad/(Pérdida)");
        utilidadRow.setInstrumentoNemo("Mensual");
        setValorPorMes(utilidadRow, 1, BigDecimal.ZERO);
        for (int mes = 2; mes <= 12; mes++) {
            if (mes > ultimoMesConDatos) {
                setValorPorMes(utilidadRow, mes, BigDecimal.ZERO);
            } else {
                BigDecimal valorMesActual = getValorPorMes(totalRow, mes);
                BigDecimal valorMesAnterior = getValorPorMes(totalRow, mes - 1);
                setValorPorMes(utilidadRow, mes, valorMesActual.subtract(valorMesAnterior));
            }
        }
        return utilidadRow;
    }
    
    private SaldoMensualDto calcularFilaUtilidadAcumulada(SaldoMensualDto utilidadMensualRow, int ultimoMesConDatos) {
        SaldoMensualDto acumuladaRow = new SaldoMensualDto();
        acumuladaRow.setNemo("Utilidad/(Pérdida) Acum.");
        acumuladaRow.setInstrumentoNemo("Acumulada");
        BigDecimal utilidadAcumulada = BigDecimal.ZERO;
        for (int mes = 1; mes <= 12; mes++) {
            if (mes > ultimoMesConDatos) {
                setValorPorMes(acumuladaRow, mes, BigDecimal.ZERO);
            } else {
                utilidadAcumulada = utilidadAcumulada.add(getValorPorMes(utilidadMensualRow, mes));
                setValorPorMes(acumuladaRow, mes, utilidadAcumulada);
            }
        }
        return acumuladaRow;
    }

    private SaldoMensualDto calcularFilaDeVariaciones(SaldoMensualDto totalRow, int ultimoMesConDatos) {
        SaldoMensualDto variationRow = new SaldoMensualDto();
        variationRow.setNemo("Variación Anual %");
        variationRow.setInstrumentoNemo("Var. % vs Enero");
        BigDecimal baseEnero = getValorPorMes(totalRow, 1);
        setValorPorMes(variationRow, 1, BigDecimal.ZERO);
        for (int mes = 2; mes <= 12; mes++) {
            if (mes > ultimoMesConDatos) {
                setValorPorMes(variationRow, mes, BigDecimal.ZERO);
            } else {
                BigDecimal valorMes = getValorPorMes(totalRow, mes);
                setValorPorMes(variationRow, mes, calcularVariacion(valorMes, baseEnero));
            }
        }
        return variationRow;
    }

    private SaldoMensualDto calcularFilaVariacionMensual(SaldoMensualDto totalRow, int ultimoMesConDatos) {
        SaldoMensualDto variationRow = new SaldoMensualDto();
        variationRow.setNemo("Variación Mensual %");
        variationRow.setInstrumentoNemo("Var. % vs Mes Ant.");
        setValorPorMes(variationRow, 1, BigDecimal.ZERO);

        for (int mes = 2; mes <= 12; mes++) {
            if (mes > ultimoMesConDatos) {
                setValorPorMes(variationRow, mes, BigDecimal.ZERO);
            } else {
                BigDecimal valorMesActual = getValorPorMes(totalRow, mes);
                BigDecimal valorMesAnterior = getValorPorMes(totalRow, mes - 1);
                setValorPorMes(variationRow, mes, calcularVariacion(valorMesActual, valorMesAnterior));
            }
        }
        return variationRow;
    }

    private BigDecimal calcularVariacion(BigDecimal valorActual, BigDecimal valorBase) {
        if (valorBase == null || valorBase.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return valorActual.divide(valorBase, 4, RoundingMode.HALF_UP)
            .subtract(BigDecimal.ONE)
            .multiply(new BigDecimal("100"))
            .setScale(1, RoundingMode.HALF_UP);
    }

    // --- MÉTODOS DE AYUDA (HELPERS) ---
    private BigDecimal getValorPorMes(SaldoMensualDto dto, int mes) {
        return switch (mes) {
            case 1 -> dto.getEnero();
            case 2 -> dto.getFebrero();
            case 3 -> dto.getMarzo();
            case 4 -> dto.getAbril();
            case 5 -> dto.getMayo();
            case 6 -> dto.getJunio();
            case 7 -> dto.getJulio();
            case 8 -> dto.getAgosto();
            case 9 -> dto.getSeptiembre();
            case 10 -> dto.getOctubre();
            case 11 -> dto.getNoviembre();
            case 12 -> dto.getDiciembre();
            default -> BigDecimal.ZERO;
        };
    }

    private void setValorPorMes(SaldoMensualDto dto, int mes, BigDecimal valor) {
        switch (mes) {
            case 1 -> dto.setEnero(valor);
            case 2 -> dto.setFebrero(valor);
            case 3 -> dto.setMarzo(valor);
            case 4 -> dto.setAbril(valor);
            case 5 -> dto.setMayo(valor);
            case 6 -> dto.setJunio(valor);
            case 7 -> dto.setJulio(valor);
            case 8 -> dto.setAgosto(valor);
            case 9 -> dto.setSeptiembre(valor);
            case 10 -> dto.setOctubre(valor);
            case 11 -> dto.setNoviembre(valor);
            case 12 -> dto.setDiciembre(valor);
        }
    }
}