package com.serv.service;

import com.model.dto.SaldoMensualDto;
import com.model.interfaces.AbstractRepository;
import com.app.sql.QueryRepository;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaldoMensualService extends AbstractRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(SaldoMensualService.class);
    
    // CONSTANTES PARA PREVENIR INYECCIÓN SQL
    private static final String COLUMN_MONTO_CLP = "s.monto_clp";
    private static final String COLUMN_MONTO_USD = "s.monto_usd";
    private static final String MONEDA_USD = "USD";
    private static final String MONEDA_CLP = "CLP";
    
    public SaldoMensualService() {
        super();
    }

    /**
     * Obtiene los saldos mensuales de forma segura, evitando SQL injection.
     * 
     * @param razonSocial Razón social de la empresa (validado como parámetro)
     * @param custodio Nombre del custodio (opcional, validado como parámetro)
     * @param anio Año de consulta (validado como parámetro)
     * @param moneda Moneda a consultar: "USD" o "CLP" (validado)
     * @return Lista de DTOs con saldos mensuales o lista vacía si hay error
     */
    public List<SaldoMensualDto> obtenerSaldosMensuales(String razonSocial, String custodio, int anio, String moneda) {
        
        // VALIDACIÓN DE PARÁMETROS ANTES DE CUALQUIER PROCESAMIENTO
        if (!validarParametrosEntrada(razonSocial, custodio, anio, moneda)) {
            return Collections.emptyList();
        }
        
        return executeReadOnly(em -> {
            try {
                // CONSTRUCCIÓN SEGURA DE LA QUERY
                String queryFinal = construirQuerySegura(moneda, custodio != null && !custodio.trim().isEmpty());
                
                logger.debug("Ejecutando consulta de saldos mensuales para empresa: {}, año: {}, moneda: {}", 
                           razonSocial, anio, moneda);
                
                Query query = em.createNativeQuery(queryFinal, "SaldoMensualMapping");
                
                // CONFIGURACIÓN DE PARÁMETROS DE FORMA SEGURA
                query.setParameter(1, razonSocial.trim());
                query.setParameter(2, anio);
                if (custodio != null && !custodio.trim().isEmpty()) {
                    query.setParameter(3, custodio.trim());
                }
                
                @SuppressWarnings("unchecked")
                List<SaldoMensualDto> resultados = query.getResultList();
                
                logger.debug("Obtenidos {} registros base de saldos mensuales", resultados.size());
                
                // PROCESAMIENTO DE RESULTADOS
                return procesarResultadosCompletos(resultados);

            } catch (Exception e) {
                logger.error("Error al obtener saldos mensuales para empresa: {}, año: {}, moneda: {}", 
                           razonSocial, anio, moneda, e);
                return Collections.emptyList();
            }
        });
    }
    
    /**
     * Valida que todos los parámetros de entrada sean correctos y seguros.
     */
    private boolean validarParametrosEntrada(String razonSocial, String custodio, int anio, String moneda) {
        if (razonSocial == null || razonSocial.trim().isEmpty()) {
            logger.warn("Razón social no puede ser nula o vacía");
            return false;
        }
        
        if (anio < 2000 || anio > 2100) {
            logger.warn("Año inválido: {}. Debe estar entre 2000 y 2100", anio);
            return false;
        }
        
        if (moneda == null || (!MONEDA_USD.equalsIgnoreCase(moneda) && !MONEDA_CLP.equalsIgnoreCase(moneda))) {
            logger.warn("Moneda inválida: {}. Debe ser 'USD' o 'CLP'", moneda);
            return false;
        }
        
        // Validación adicional para custodio si se proporciona
        if (custodio != null && custodio.trim().length() > 100) {
            logger.warn("Nombre de custodio demasiado largo: {}", custodio.length());
            return false;
        }
        
        return true;
    }
    
    /**
     * Construye la query de forma segura usando solo constantes predefinidas.
     * NO usa concatenación dinámica de strings del usuario.
     */
    private String construirQuerySegura(String moneda, boolean incluirCustodio) {

        String columnaMoneda = MONEDA_USD.equalsIgnoreCase(moneda) ? COLUMN_MONTO_USD : COLUMN_MONTO_CLP;        
        String custodioClause = incluirCustodio ? "AND c.custodio = ?3" : "";
        
        // --- CONSTRUCCIÓN DE QUERY USANDO SOLO CONSTANTES Y PARÁMETROS SEGUROS ---
        
        String sql_base_query_template = QueryRepository.getSaldoMensualQuery(QueryRepository.SaldoMensualQueries.BASE_QUERY_TEMPLATE_QUERY);

        return String.format(sql_base_query_template, 
            // Los 12 meses - todos con la misma columna segura
            columnaMoneda, columnaMoneda, columnaMoneda, columnaMoneda,
            columnaMoneda, columnaMoneda, columnaMoneda, columnaMoneda,
            columnaMoneda, columnaMoneda, columnaMoneda, columnaMoneda,
            // Cláusula adicional de custodio
            custodioClause
        );
    }
    
    /**
     * Procesa los resultados completos incluyendo filtrado y cálculo de filas de resumen.
     */
    private List<SaldoMensualDto> procesarResultadosCompletos(List<SaldoMensualDto> resultados) {
        // FILTRADO - Solo registros con actividad
        List<SaldoMensualDto> resultadosFiltrados = resultados.stream()
            .filter(dto -> dto.getSumaTotalAnual().compareTo(BigDecimal.ZERO) != 0)
            .collect(Collectors.toList());

        if (resultadosFiltrados.isEmpty()) {
            logger.debug("No hay registros con saldos diferentes de cero");
            return Collections.emptyList();
        }

        // CÁLCULO DE FILAS DE RESUMEN
        SaldoMensualDto totalRow = calcularFilaTotal(resultadosFiltrados);
        int ultimoMesConDatos = encontrarUltimoMesConDatos(totalRow);
        
        logger.debug("Último mes con datos: {}", ultimoMesConDatos);
        
        SaldoMensualDto utilidadRow = calcularFilaUtilidad(totalRow, ultimoMesConDatos);
        SaldoMensualDto utilidadAcumuladaRow = calcularFilaUtilidadAcumulada(utilidadRow, ultimoMesConDatos);
        SaldoMensualDto variationAnualRow = calcularFilaDeVariaciones(totalRow, ultimoMesConDatos);
        SaldoMensualDto variationMensualRow = calcularFilaVariacionMensual(totalRow, ultimoMesConDatos);

        // CONSTRUCCIÓN DEL RESULTADO FINAL
        resultadosFiltrados.add(totalRow);
        resultadosFiltrados.add(utilidadRow);
        resultadosFiltrados.add(utilidadAcumuladaRow);
        resultadosFiltrados.add(variationAnualRow);
        resultadosFiltrados.add(variationMensualRow);
        
        logger.debug("Procesamiento completado. Total registros: {}", resultadosFiltrados.size());
        return resultadosFiltrados;
    }
    
    /**
     * Encuentra el último mes del año que tiene datos diferentes de cero.
     */
    private int encontrarUltimoMesConDatos(SaldoMensualDto totalRow) {
        if (totalRow == null) {
            return 0;
        }
        
        for (int mes = 12; mes >= 1; mes--) {
            BigDecimal valorMes = getValorPorMes(totalRow, mes);
            if (valorMes != null && valorMes.compareTo(BigDecimal.ZERO) != 0) {
                return mes;
            }
        }
        return 0;
    }

    /**
     * Calcula la fila de totales sumando todos los instrumentos por mes.
     */
    private SaldoMensualDto calcularFilaTotal(List<SaldoMensualDto> resultados) {
        SaldoMensualDto totalRow = new SaldoMensualDto();
        totalRow.setNemo("Total general");
        totalRow.setInstrumentoNemo("");
        
        for (SaldoMensualDto dto : resultados) {
            for (int mes = 1; mes <= 12; mes++) {
                BigDecimal valorActual = getValorPorMes(totalRow, mes);
                BigDecimal valorDto = getValorPorMes(dto, mes);
                
                // Manejo seguro de nulos
                if (valorActual == null) valorActual = BigDecimal.ZERO;
                if (valorDto == null) valorDto = BigDecimal.ZERO;
                
                setValorPorMes(totalRow, mes, valorActual.add(valorDto));
            }
        }
        return totalRow;
    }

    /**
     * Calcula la utilidad mensual comparando con el mes anterior.
     */
    private SaldoMensualDto calcularFilaUtilidad(SaldoMensualDto totalRow, int ultimoMesConDatos) {
        SaldoMensualDto utilidadRow = new SaldoMensualDto();
        utilidadRow.setNemo("Utilidad/(Pérdida)");
        utilidadRow.setInstrumentoNemo("Mensual");
        
        // Enero siempre es cero (no hay mes anterior)
        setValorPorMes(utilidadRow, 1, BigDecimal.ZERO);
        
        for (int mes = 2; mes <= 12; mes++) {
            if (mes > ultimoMesConDatos) {
                setValorPorMes(utilidadRow, mes, BigDecimal.ZERO);
            } else {
                BigDecimal valorMesActual = getValorPorMes(totalRow, mes);
                BigDecimal valorMesAnterior = getValorPorMes(totalRow, mes - 1);
                
                // Manejo seguro de nulos
                if (valorMesActual == null) valorMesActual = BigDecimal.ZERO;
                if (valorMesAnterior == null) valorMesAnterior = BigDecimal.ZERO;
                
                setValorPorMes(utilidadRow, mes, valorMesActual.subtract(valorMesAnterior));
            }
        }
        return utilidadRow;
    }
    
    /**
     * Calcula la utilidad acumulada desde enero hasta cada mes.
     */
    private SaldoMensualDto calcularFilaUtilidadAcumulada(SaldoMensualDto utilidadMensualRow, int ultimoMesConDatos) {
        SaldoMensualDto acumuladaRow = new SaldoMensualDto();
        acumuladaRow.setNemo("Utilidad/(Pérdida) Acum.");
        acumuladaRow.setInstrumentoNemo("Acumulada");
        
        BigDecimal utilidadAcumulada = BigDecimal.ZERO;
        
        for (int mes = 1; mes <= 12; mes++) {
            if (mes > ultimoMesConDatos) {
                setValorPorMes(acumuladaRow, mes, BigDecimal.ZERO);
            } else {
                BigDecimal utilidadMes = getValorPorMes(utilidadMensualRow, mes);
                if (utilidadMes != null) {
                    utilidadAcumulada = utilidadAcumulada.add(utilidadMes);
                }
                setValorPorMes(acumuladaRow, mes, utilidadAcumulada);
            }
        }
        return acumuladaRow;
    }

    /**
     * Calcula el porcentaje de variación anual (vs enero).
     */
    private SaldoMensualDto calcularFilaDeVariaciones(SaldoMensualDto totalRow, int ultimoMesConDatos) {
        SaldoMensualDto variationRow = new SaldoMensualDto();
        variationRow.setNemo("Variación Anual %");
        variationRow.setInstrumentoNemo("Var. % vs Enero");
        
        BigDecimal baseEnero = getValorPorMes(totalRow, 1);
        if (baseEnero == null) baseEnero = BigDecimal.ZERO;
        
        // Enero siempre es 0% (comparado consigo mismo)
        setValorPorMes(variationRow, 1, BigDecimal.ZERO);
        
        for (int mes = 2; mes <= 12; mes++) {
            if (mes > ultimoMesConDatos) {
                setValorPorMes(variationRow, mes, BigDecimal.ZERO);
            } else {
                BigDecimal valorMes = getValorPorMes(totalRow, mes);
                if (valorMes == null) valorMes = BigDecimal.ZERO;
                
                BigDecimal variacion = calcularVariacion(valorMes, baseEnero);
                setValorPorMes(variationRow, mes, variacion);
            }
        }
        return variationRow;
    }

    /**
     * Calcula el porcentaje de variación mensual (vs mes anterior).
     */
    private SaldoMensualDto calcularFilaVariacionMensual(SaldoMensualDto totalRow, int ultimoMesConDatos) {
        SaldoMensualDto variationRow = new SaldoMensualDto();
        variationRow.setNemo("Variación Mensual %");
        variationRow.setInstrumentoNemo("Var. % vs Mes Ant.");
        
        // Enero siempre es 0% (no hay mes anterior)
        setValorPorMes(variationRow, 1, BigDecimal.ZERO);

        for (int mes = 2; mes <= 12; mes++) {
            if (mes > ultimoMesConDatos) {
                setValorPorMes(variationRow, mes, BigDecimal.ZERO);
            } else {
                BigDecimal valorMesActual = getValorPorMes(totalRow, mes);
                BigDecimal valorMesAnterior = getValorPorMes(totalRow, mes - 1);
                
                if (valorMesActual == null) valorMesActual = BigDecimal.ZERO;
                if (valorMesAnterior == null) valorMesAnterior = BigDecimal.ZERO;
                
                BigDecimal variacion = calcularVariacion(valorMesActual, valorMesAnterior);
                setValorPorMes(variationRow, mes, variacion);
            }
        }
        return variationRow;
    }

    /**
     * Calcula el porcentaje de variación de forma segura, evitando división por cero.
     */
    private BigDecimal calcularVariacion(BigDecimal valorActual, BigDecimal valorBase) {
        if (valorBase == null || valorBase.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (valorActual == null) {
            valorActual = BigDecimal.ZERO;
        }
        
        try {
            return valorActual.divide(valorBase, 6, RoundingMode.HALF_UP)
                .subtract(BigDecimal.ONE)
                .multiply(new BigDecimal("100"))
                .setScale(1, RoundingMode.HALF_UP);
        } catch (ArithmeticException e) {
            logger.warn("Error en cálculo de variación: actual={}, base={}", valorActual, valorBase, e);
            return BigDecimal.ZERO;
        }
    }

    // --- MÉTODOS DE AYUDA PARA ACCESO SEGURO A CAMPOS DEL DTO ---
    
    /**
     * Obtiene el valor de un mes específico de forma segura.
     */
    private BigDecimal getValorPorMes(SaldoMensualDto dto, int mes) {
        if (dto == null) {
            return BigDecimal.ZERO;
        }
        
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
            default -> {
                logger.warn("Mes inválido solicitado: {}", mes);
                yield BigDecimal.ZERO;
            }
        };
    }

    /**
     * Establece el valor de un mes específico de forma segura.
     */
    private void setValorPorMes(SaldoMensualDto dto, int mes, BigDecimal valor) {
        if (dto == null) {
            logger.warn("Intento de establecer valor en DTO null");
            return;
        }
        
        // Asegurar que el valor nunca sea null
        if (valor == null) {
            valor = BigDecimal.ZERO;
        }
        
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
            default -> logger.warn("Intento de establecer valor en mes inválido: {}", mes);
        }
    }
}