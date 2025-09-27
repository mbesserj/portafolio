package com.service.service;

import com.app.dto.InventarioCostoDto;
import com.app.dto.ResumenSaldoDto;
import com.app.interfaces.KardexApiInterfaz;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.app.interfaces.PrecioRepository;

/**
 * Servicio de negocio para operaciones relacionadas con Saldos.
 * Orquesta la obtención de datos desde diferentes repositorios y aplica la lógica de valorización.
 */
public class SaldoActualService {

    private static final Logger logger = LoggerFactory.getLogger(SaldoActualService.class);

    // Dependencias inyectadas: El servicio depende de los contratos (interfaces), no de las implementaciones.
    private final KardexApiInterfaz kardexRepository;
    private final PrecioRepository precioRepository;

    /**
     * Constructor para inyectar las dependencias de los repositorios.
     * @param kardexRepository Repositorio para consultas de kárdex.
     * @param precioRepository Repositorio para consultas de precios.
     */
    public SaldoActualService(KardexApiInterfaz kardexRepository, PrecioRepository precioRepository) {
        this.kardexRepository = kardexRepository;
        this.precioRepository = precioRepository;
    }

    /**
     * Lógica de negocio principal: Calcula los saldos valorizados a precios de mercado.
     * Este método es el corazón del servicio: combina datos de múltiples fuentes.
     *
     * @param empresaId ID de la empresa.
     * @param custodioId ID del custodio.
     * @return Una lista de DTOs con los saldos valorizados.
     */
    public List<ResumenSaldoDto> obtenerSaldosValorizados(Long empresaId, Long custodioId) {
        logger.info("Iniciando proceso de valorización de saldos para el grupo [Empresa: {}, Custodio: {}]", empresaId, custodioId);

        // PASO 1: Obtener el inventario costeado usando el repositorio de Kárdex.
        List<InventarioCostoDto> inventarioCosteado = kardexRepository.obtenerSaldosFinalesPorGrupo(empresaId, custodioId);
        if (inventarioCosteado.isEmpty()) {
            logger.warn("No se encontraron saldos en el kárdex para el grupo. El proceso finaliza.");
            return new ArrayList<>();
        }
        logger.info("Se obtuvieron {} saldos del kárdex.", inventarioCosteado.size());

        // PASO 2: Obtener los precios de mercado usando el repositorio de Precios.
        Map<Long, BigDecimal> mapaDePrecios = precioRepository.obtenerUltimosPreciosParaGrupo(empresaId, custodioId);
        logger.info("Se obtuvieron {} precios de mercado.", mapaDePrecios.size());

        // PASO 3: Combinar, calcular y filtrar los resultados.
        List<ResumenSaldoDto> resultados = new ArrayList<>();
        for (InventarioCostoDto item : inventarioCosteado) {
            BigDecimal saldoCantidad = item.getSaldoCantidadFinal();

            // Lógica de negocio: solo procesar items con saldo positivo.
            if (saldoCantidad != null && saldoCantidad.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal precioMercado = mapaDePrecios.getOrDefault(item.getInstrumentoId(), BigDecimal.ZERO);
                BigDecimal costoTotal = item.getCostoTotalFifo();
                BigDecimal valorMercado = saldoCantidad.multiply(precioMercado);
                BigDecimal costoUnitario = costoTotal.divide(saldoCantidad, 4, RoundingMode.HALF_UP);
                BigDecimal utilidad = (precioMercado.compareTo(BigDecimal.ZERO) == 0) ? BigDecimal.ZERO : valorMercado.subtract(costoTotal);

                // Se crea un DTO vacío y se puebla con setters.
                ResumenSaldoDto dto = new ResumenSaldoDto();
                dto.setInstrumentoId(item.getInstrumentoId());
                dto.setInstrumentoNemo(item.getInstrumentoNemo());
                dto.setInstrumentoNombre(item.getInstrumentoNombre());
                dto.setSaldoCantidad(saldoCantidad);
                dto.setCostoTotal(costoTotal);
                dto.setCostoUnitario(costoUnitario);
                dto.setPrecioMercado(precioMercado);
                dto.setValorMercado(valorMercado);
                dto.setUtilidadNoRealizada(utilidad);
                
                resultados.add(dto);
            }
        }

        logger.info("Proceso de valorización completado. Se devuelven {} resultados.", resultados.size());
        return resultados;
    }
}