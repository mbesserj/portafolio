
package com.serv.service;

import com.model.entities.InstrumentoEntity;
import com.model.entities.TransaccionEntity;
import com.model.entities.EmpresaEntity;
import com.model.entities.TipoMovimientoEntity;
import com.model.entities.CustodioEntity;
import com.model.dto.TransaccionManualDto;
import com.model.interfaces.AbstractRepository;
import com.serv.sql.QueryRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransaccionService extends AbstractRepository {

    private static final Logger logger = LoggerFactory.getLogger(TransaccionService.class);

    public TransaccionService() {
        super();
    }
    
    /**
     * Obtiene una TransaccionEntity por su ID, cargando de forma anticipada todas las relaciones.
     * 
     * @param id El ID de la transacción
     * @return La transacción encontrada o null si no existe
     */
    public TransaccionEntity obtenerTransaccionPorId(Long id) {
        if (id == null) {
            logger.debug("ID de transacción es null");
            return null;
        }

        return executeReadOnly(em -> {
            try {
                String sql_transaccion_completa = QueryRepository.getTransaccionQuery(QueryRepository.TipoTransaccionQueries.TRANSACCION_COMPLETA_QUERY);

                TypedQuery<TransaccionEntity> query = em.createQuery(sql_transaccion_completa, TransaccionEntity.class);
                query.setParameter("id", id);
                
                TransaccionEntity transaccion = query.getSingleResult();
                logger.debug("Transacción encontrada: ID {}, Folio: {}", id, transaccion.getFolio());
                return transaccion;

            } catch (NoResultException e) {
                logger.warn("Transacción con ID {} no encontrada", id);
                return null;
            } catch (Exception e) {
                logger.error("Error al obtener transacción con ID: {}", id, e);
                return null;
            }
        });
    }

    /**
     * Crea y persiste una nueva transacción manual a partir de los datos del DTO.
     * 
     * @param dto DTO con los datos de la transacción manual
     * @throws IllegalArgumentException si el DTO es null o tiene datos inválidos
     * @throws EntityNotFoundException si alguna entidad relacionada no existe
     * @throws RuntimeException si falla la persistencia
     */
    public void crearTransaccionManual(TransaccionManualDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("El DTO de transacción manual no puede ser null");
        }

        // Validaciones básicas del DTO
        validarDtoTransaccionManual(dto);

        executeInTransaction(em -> {
            try {
                // 1. Obtener las entidades relacionadas usando los IDs del DTO
                EmpresaEntity empresa = obtenerEntidadOLanzarExcepcion(em, EmpresaEntity.class, 
                    dto.empresaId(), "Empresa");
                CustodioEntity custodio = obtenerEntidadOLanzarExcepcion(em, CustodioEntity.class, 
                    dto.custodioId(), "Custodio");
                InstrumentoEntity instrumento = obtenerEntidadOLanzarExcepcion(em, InstrumentoEntity.class, 
                    dto.instrumentoId(), "Instrumento");
                TipoMovimientoEntity tipoMovimiento = obtenerEntidadOLanzarExcepcion(em, TipoMovimientoEntity.class, 
                    dto.tipoMovimientoId(), "Tipo de Movimiento");

                // 2. Crear y poblar la entidad Transaccion
                TransaccionEntity nuevaTrx = construirTransaccionDesdeDto(dto, empresa, custodio, 
                    instrumento, tipoMovimiento);

                // 3. Persistir
                em.persist(nuevaTrx);

                logger.info("Transacción manual creada con éxito - ID: {}, Instrumento: {}, Cantidad: {}", 
                           nuevaTrx.getId(), instrumento.getInstrumentoNemo(), dto.cantidad());
                           
            } catch (EntityNotFoundException e) {
                logger.error("Entidad relacionada no encontrada: {}", e.getMessage());
                throw e;
            } catch (Exception e) {
                logger.error("Error al crear transacción manual", e);
                throw new RuntimeException("Error al crear transacción manual: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Cambia el estado booleano de 'ignorarEnCosteo' para una transacción específica.
     * 
     * @param transaccionId El ID de la transacción
     * @throws IllegalArgumentException si transaccionId es null
     * @throws EntityNotFoundException si la transacción no existe
     */
    public void toggleIgnorarEnCosteo(Long transaccionId) {
        if (transaccionId == null) {
            throw new IllegalArgumentException("El ID de la transacción no puede ser null");
        }

        executeInTransaction(em -> {
            try {
                TransaccionEntity trx = em.find(TransaccionEntity.class, transaccionId);
                if (trx == null) {
                    throw new EntityNotFoundException("No se encontró la transacción con ID: " + transaccionId);
                }

                boolean estadoAnterior = trx.isIgnorarEnCosteo();
                trx.setIgnorarEnCosteo(!estadoAnterior);
                em.merge(trx);

                logger.info("Estado 'ignorarEnCosteo' cambiado para transacción ID: {} - {} -> {}", 
                           transaccionId, estadoAnterior, trx.isIgnorarEnCosteo());
                           
            } catch (EntityNotFoundException e) {
                logger.error("Transacción no encontrada: {}", e.getMessage());
                throw e;
            } catch (Exception e) {
                logger.error("Error al cambiar estado 'ignorarEnCosteo' para transacción {}", transaccionId, e);
                throw new RuntimeException("Error al actualizar transacción: " + e.getMessage(), e);
            }
        });
    }

    // Métodos auxiliares privados
    private void validarDtoTransaccionManual(TransaccionManualDto dto) {
        if (dto.empresaId() == null) {
            throw new IllegalArgumentException("El ID de empresa no puede ser null");
        }
        if (dto.custodioId() == null) {
            throw new IllegalArgumentException("El ID de custodio no puede ser null");
        }
        if (dto.instrumentoId() == null) {
            throw new IllegalArgumentException("El ID de instrumento no puede ser null");
        }
        if (dto.tipoMovimientoId() == null) {
            throw new IllegalArgumentException("El ID de tipo de movimiento no puede ser null");
        }
        if (dto.cuenta() == null || dto.cuenta().trim().isEmpty()) {
            throw new IllegalArgumentException("La cuenta no puede ser nula o vacía");
        }
        if (dto.fecha() == null) {
            throw new IllegalArgumentException("La fecha no puede ser null");
        }
        if (dto.cantidad() == null || dto.cantidad().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
        if (dto.precio() == null || dto.precio().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero");
        }
    }

    private <T> T obtenerEntidadOLanzarExcepcion(jakarta.persistence.EntityManager em, Class<T> entityClass, 
                                               Long id, String entityName) {
        T entity = em.find(entityClass, id);
        if (entity == null) {
            throw new EntityNotFoundException(entityName + " no encontrada con ID: " + id);
        }
        return entity;
    }

    private TransaccionEntity construirTransaccionDesdeDto(TransaccionManualDto dto, EmpresaEntity empresa,
                                                          CustodioEntity custodio, InstrumentoEntity instrumento,
                                                          TipoMovimientoEntity tipoMovimiento) {
        TransaccionEntity nuevaTrx = new TransaccionEntity();

        // Relaciones
        nuevaTrx.setEmpresa(empresa);
        nuevaTrx.setCustodio(custodio);
        nuevaTrx.setInstrumento(instrumento);
        nuevaTrx.setTipoMovimiento(tipoMovimiento);

        // Datos básicos
        nuevaTrx.setCuenta(dto.cuenta().trim());
        nuevaTrx.setFecha(dto.fecha());
        nuevaTrx.setCantidad(dto.cantidad());
        nuevaTrx.setPrecio(dto.precio());
        nuevaTrx.setGlosa(dto.glosa() != null ? dto.glosa().trim() : null);
        nuevaTrx.setComisiones(dto.comisiones() != null ? dto.comisiones() : BigDecimal.ZERO);
        nuevaTrx.setGastos(dto.gastos() != null ? dto.gastos() : BigDecimal.ZERO);
        nuevaTrx.setIva(dto.iva() != null ? dto.iva() : BigDecimal.ZERO);
        nuevaTrx.setMoneda(dto.moneda() != null ? dto.moneda().trim() : "CLP");

        // Folio
        nuevaTrx.setFolio(dto.folio() != null && !dto.folio().trim().isEmpty() ? 
                         dto.folio().trim() : "MANUAL");

        // Cálculos
        BigDecimal totalCalculado = dto.cantidad().multiply(dto.precio());
        nuevaTrx.setMonto(totalCalculado);
        nuevaTrx.setMontoClp(totalCalculado);

        // Estados
        nuevaTrx.setCosteado(false);
        nuevaTrx.setParaRevision(false);
        nuevaTrx.setIgnorarEnCosteo(false);

        return nuevaTrx;
    }
}