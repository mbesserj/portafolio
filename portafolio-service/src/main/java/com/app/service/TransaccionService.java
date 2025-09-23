package com.app.service;

import com.app.dto.TransaccionManualDto;
import com.app.entities.*;
import com.app.utiles.LibraryInitializer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class TransaccionService {

    private static final Logger logger = LoggerFactory.getLogger(TransaccionService.class);

    /**
     * Obtiene una TransaccionEntity por su ID, cargando de forma anticipada.
     *
     * @param id
     * @return retorna el listado de transacción.
     */
    public TransaccionEntity obtenerTransaccionPorId(Long id) {
        try (EntityManager em = LibraryInitializer.getEntityManager()) {

            TypedQuery<TransaccionEntity> query = em.createQuery(
                    "SELECT t FROM TransaccionEntity t "
                    + "LEFT JOIN FETCH t.instrumento "
                    + "LEFT JOIN FETCH t.tipoMovimiento "
                    + "LEFT JOIN FETCH t.empresa "
                    + "LEFT JOIN FETCH t.custodio "
                    + "WHERE t.id = :id", TransaccionEntity.class);

            query.setParameter("id", id);
            return query.getSingleResult();

        } catch (NoResultException e) {
            logger.warn("Transaccion con ID {} no encontrada.", id);
            return null;
        } catch (Exception e) {
            logger.error("Error al buscar Transaccion por ID {}.", id, e);
            throw e; // Relanza la excepción para que la capa superior la maneje
        }
    }

    /**
     * Crea y persiste una nueva transacción manual a partir de los datos
     * esenciales. Esta versión es más robusta ya que busca las entidades por
     * ID, centralizando la lógica de negocio y simplificando la llamada desde
     * el controlador.
     *
     * @param empresaId ID de la empresa.
     * @param custodioId ID del custodio.
     * @param instrumentoId ID del instrumento.
     * @param tipoMovimientoId ID del tipo de movimiento.
     * @param cuenta El número de cuenta.
     * @param folio
     * @param fecha La fecha de la transacción.
     * @param cantidad La cantidad de títulos/nominales.
     * @param precio El precio unitario.
     * @param glosa Un comentario o descripción para la transacción.
     * @param moneda
     */
    public void crearTransaccionManual(TransaccionManualDto dto) {
        EntityManager em = null;
        try {
            em = LibraryInitializer.getEntityManager();
            em.getTransaction().begin();

            // 1. Obtener las entidades relacionadas usando los IDs del DTO
            EmpresaEntity empresa = em.find(EmpresaEntity.class, dto.empresaId());
            if (empresa == null) {
                throw new EntityNotFoundException("Empresa no encontrada con ID: " + dto.empresaId());
            }

            CustodioEntity custodio = em.find(CustodioEntity.class, dto.custodioId());
            if (custodio == null) {
                throw new EntityNotFoundException("Custodio no encontrado con ID: " + dto.custodioId());
            }

            InstrumentoEntity instrumento = em.find(InstrumentoEntity.class, dto.instrumentoId());
            if (instrumento == null) {
                throw new EntityNotFoundException("Instrumento no encontrado con ID: " + dto.instrumentoId());
            }

            TipoMovimientoEntity tipoMovimiento = em.find(TipoMovimientoEntity.class, dto.tipoMovimientoId());
            if (tipoMovimiento == null) {
                throw new EntityNotFoundException("Tipo de Movimiento no encontrado con ID: " + dto.tipoMovimientoId());
            }

            // 2. Crear y poblar la entidad Transaccion a partir del DTO
            TransaccionEntity nuevaTrx = new TransaccionEntity();

            nuevaTrx.setEmpresa(empresa);
            nuevaTrx.setCustodio(custodio);
            nuevaTrx.setInstrumento(instrumento);
            nuevaTrx.setTipoMovimiento(tipoMovimiento);

            nuevaTrx.setCuenta(dto.cuenta());
            nuevaTrx.setFecha(dto.fecha());
            nuevaTrx.setCantidad(dto.cantidad());
            nuevaTrx.setPrecio(dto.precio());
            nuevaTrx.setGlosa(dto.glosa());
            nuevaTrx.setComisiones(dto.comisiones());
            nuevaTrx.setGastos(dto.gastos());
            nuevaTrx.setIva(dto.iva());
            nuevaTrx.setMoneda(dto.moneda());

            if (dto.folio() == null || dto.folio().isBlank()) {
                nuevaTrx.setFolio("MANUAL");
            } else {
                nuevaTrx.setFolio(dto.folio());
            }

            BigDecimal totalCalculado = dto.cantidad().multiply(dto.precio());
            nuevaTrx.setMonto(totalCalculado);
            nuevaTrx.setMontoClp(totalCalculado);

            nuevaTrx.setCosteado(false);
            nuevaTrx.setParaRevision(false);

            em.persist(nuevaTrx);
            em.getTransaction().commit();

            logger.info("Transacción manual creada con éxito con ID: {}", nuevaTrx.getId());

        } catch (Exception e) {
            logger.error("Error al crear la transacción manual", e);
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            // Lanza una excepción más específica para que la UI pueda mostrar un mensaje útil
            throw new RuntimeException("No se pudo guardar la transacción manual: " + e.getMessage(), e);
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    /**
     * Cambia el estado booleano de 'ignorarEnCosteo' para una transacción
     * específica. Si está en true lo pasa a false, y viceversa.
     *
     * @param transaccionId El ID de la transacción a modificar.
     */
    public void toggleIgnorarEnCosteo(Long transaccionId) {
        EntityManager em = null;
        try {
            em = LibraryInitializer.getEntityManager();
            em.getTransaction().begin();

            // 1. Buscar la entidad por su ID
            TransaccionEntity trx = em.find(TransaccionEntity.class, transaccionId);
            if (trx == null) {
                throw new EntityNotFoundException("No se encontró la transacción con ID: " + transaccionId);
            }

            // 2. Invertir el valor actual del campo
            trx.setIgnorarEnCosteo(!trx.isIgnorarEnCosteo());

            // 3. Persistir el cambio en la base de datos
            em.merge(trx);
            em.getTransaction().commit();

            logger.info("Estado 'ignorarEnCosteo' cambiado para la transacción ID: {}. Nuevo estado: {}", transaccionId, trx.isIgnorarEnCosteo());

        } catch (Exception e) {
            logger.error("Error al cambiar el estado 'ignorarEnCosteo' para la transacción ID: {}", transaccionId, e);
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            // Relanzamos la excepción para que el controlador la maneje
            throw new RuntimeException("No se pudo actualizar la transacción: " + e.getMessage(), e);
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

}
