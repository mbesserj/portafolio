package com.serv.service;

import com.model.dto.ResumenSaldoEmpresaDto;
import com.model.interfaces.AbstractRepository;
import com.serv.sql.QueryRepository;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResumenSaldoEmpresaService extends AbstractRepository {

    private static final String RESUMEN_SALDO_MAPPING = "ResumenSaldoEmpresaMapping";

    private static final Logger logger = LoggerFactory.getLogger(ResumenSaldoEmpresaService.class);
    
    public ResumenSaldoEmpresaService() {
        super();
    }

    /**
     * Orquesta la obtención y procesamiento del resumen de saldos. Este es el
     * método público principal que expone el servicio.
     *
     * @return Una lista de DTOs con los datos de detalle, subtotales y total
     * general.
     */
    public List<ResumenSaldoEmpresaDto> obtenerResumenSaldos() {
        return executeReadOnly(em -> {
            List<ResumenSaldoEmpresaDto> resultadosRaw = ejecutarConsultaSaldos(em);

            if (resultadosRaw.isEmpty()) {
                logger.info("La consulta de saldos no arrojó resultados.");
                return Collections.emptyList();
            }

            return procesarResultadosConTotales(resultadosRaw);
        });
    }

    /**
     * RESPONSABILIDAD 1: INTERACCIÓN CON LA BASE DE DATOS. Se conecta a la BD y
     * ejecuta la consulta nativa para obtener los saldos. No contiene lógica de
     * negocio, solo la obtención de datos.
     *
     * @param em EntityManager gestionado por AbstractRepository
     * @return Una lista "cruda" de saldos agrupados.
     */
    @SuppressWarnings("unchecked")
    private List<ResumenSaldoEmpresaDto> ejecutarConsultaSaldos(jakarta.persistence.EntityManager em) {
        try {
            String sql_resumen_saldo = QueryRepository.getResumenSaldoQuery(QueryRepository.ResumenSaldoQueries.RESUMEN_SALDO_QUERY);
            Query query = em.createNativeQuery(sql_resumen_saldo, RESUMEN_SALDO_MAPPING);
            return query.getResultList();
        } catch (Exception e) {
            logger.error("Error al ejecutar la consulta de resumen de saldos.", e);
            return Collections.emptyList();
        }
    }

    /**
     * RESPONSABILIDAD 2: LÓGICA DE NEGOCIO Y PROCESAMIENTO. Toma la lista cruda
     * y la procesa para agregar subtotales y totales.
     *
     * @param resultados La lista de DTOs obtenida de la base de datos.
     * @return La lista final procesada y lista para la vista.
     */
    private List<ResumenSaldoEmpresaDto> procesarResultadosConTotales(List<ResumenSaldoEmpresaDto> resultados) {
        List<ResumenSaldoEmpresaDto> resultadosFinales = new ArrayList<>();

        // Fase 1: Calcular los Grandes Totales una sola vez para eficiencia.
        final BigDecimal granTotalClp = resultados.stream()
                .map(ResumenSaldoEmpresaDto::getSaldoClp)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        final BigDecimal granTotalUsd = resultados.stream()
                .map(ResumenSaldoEmpresaDto::getSaldoUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Fase 2: Iterar para construir la lista final con subtotales.
        String empresaActual = "";
        BigDecimal subtotalClp = BigDecimal.ZERO;
        BigDecimal subtotalUsd = BigDecimal.ZERO;

        for (ResumenSaldoEmpresaDto dto : resultados) {
            if (empresaActual.isEmpty()) {
                empresaActual = dto.getEmpresa();
            }

            // Al cambiar de empresa, se inserta la fila de subtotal.
            if (!dto.getEmpresa().equals(empresaActual)) {
                resultadosFinales.add(crearFilaSubtotal(empresaActual, subtotalClp, subtotalUsd, granTotalClp));
                empresaActual = dto.getEmpresa();
                subtotalClp = BigDecimal.ZERO;
                subtotalUsd = BigDecimal.ZERO;
            }

            subtotalClp = subtotalClp.add(dto.getSaldoClp());
            subtotalUsd = subtotalUsd.add(dto.getSaldoUsd());
            calcularYSetearPorcentaje(dto, granTotalClp);
            resultadosFinales.add(dto);
        }

        // Fase 3: Agregar el último subtotal y el total general.
        if (!empresaActual.isEmpty()) {
            resultadosFinales.add(crearFilaSubtotal(empresaActual, subtotalClp, subtotalUsd, granTotalClp));
        }
        resultadosFinales.add(crearFilaTotal(granTotalClp, granTotalUsd));

        return resultadosFinales;
    }

    // --- Métodos de Ayuda (Helpers) para la creación de filas de resumen ---
    private ResumenSaldoEmpresaDto crearFilaSubtotal(String empresa, BigDecimal subtotalClp, BigDecimal subtotalUsd, BigDecimal granTotalClp) {
        ResumenSaldoEmpresaDto subtotalDto = crearFilaResumen("Subtotal " + empresa, subtotalClp, subtotalUsd, "subtotal-row");
        calcularYSetearPorcentaje(subtotalDto, granTotalClp);
        return subtotalDto;
    }

    private ResumenSaldoEmpresaDto crearFilaTotal(BigDecimal granTotalClp, BigDecimal granTotalUsd) {
        ResumenSaldoEmpresaDto totalDto = crearFilaResumen("Total General", granTotalClp, granTotalUsd, "total-row");
        totalDto.setPorcentaje(new BigDecimal("100.00"));
        return totalDto;
    }

    /**
     * Función genérica y reutilizable para crear una fila de resumen.
     */
    private ResumenSaldoEmpresaDto crearFilaResumen(String etiqueta, BigDecimal saldoClp, BigDecimal saldoUsd, String styleClass) {
        ResumenSaldoEmpresaDto dto = new ResumenSaldoEmpresaDto();
        dto.setEmpresa(etiqueta);
        dto.setSaldoClp(saldoClp);
        dto.setSaldoUsd(saldoUsd);
        dto.setStyleClass(styleClass);
        return dto;
    }

    /**
     * Calcula y asigna el porcentaje que representa el saldo CLP de un DTO
     * sobre el total general.
     */
    private void calcularYSetearPorcentaje(ResumenSaldoEmpresaDto dto, BigDecimal granTotalClp) {
        if (granTotalClp.compareTo(BigDecimal.ZERO) != 0 && dto.getSaldoClp() != null) {
            BigDecimal porcentaje = dto.getSaldoClp()
                    .multiply(new BigDecimal("100"))
                    .divide(granTotalClp, 2, RoundingMode.HALF_UP);
            dto.setPorcentaje(porcentaje);
        } else {
            dto.setPorcentaje(BigDecimal.ZERO);
        }
    }
}
