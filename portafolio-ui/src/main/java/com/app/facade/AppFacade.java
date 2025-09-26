package com.app.facade;

import com.app.dto.*;
import com.app.entities.*;
import com.app.enums.ListaEnumsCustodios;
import com.app.factory.ServiceContainer;
import java.io.File;
import java.time.LocalDate;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fachada principal de la aplicación.
 * Proporciona un API simple para la capa de UI, orquestando las llamadas
 * a los servicios necesarios, los cuales obtiene del ServiceContainer.
 */
public class AppFacade {

    private static final Logger logger = LoggerFactory.getLogger(AppFacade.class);

    private final ServiceContainer container;

    public AppFacade(ServiceContainer container) {
        this.container = container;
    }

    // --- Métodos de Lógica de Negocio ---
    
        // ========== MÉTODOS FACADE PRINCIPALES ==========
    /**
     * Autenticación y seguridad
     */
    public boolean autenticarUsuario(String usuario, String password) {
        try {
            if (authenticationService == null) {
                logger.error("AuthenticationService no disponible");
                return false;
            }

            // Manejar el tipo de retorno correcto
            Object result = authenticationService.autenticar(usuario, password);
            if (result instanceof Boolean) {
                return (Boolean) result;
            } else if (result != null && result.getClass().getSimpleName().equals("AuthenticationResult")) {
                // Usar reflection para obtener el resultado si es un objeto complejo
                try {
                    return (Boolean) result.getClass().getMethod("isSuccess").invoke(result);
                } catch (Exception e) {
                    logger.warn("No se pudo obtener resultado de autenticación: {}", e.getMessage());
                    return false;
                }
            }
            return false;
        } catch (Exception e) {
            logger.error("Error en autenticación para usuario: {}", usuario, e);
            return false;
        }
    }

    public boolean hayUsuariosRegistrados() {
        try {
            if (usuarioService == null) {
                logger.error("UsuarioService no disponible");
                return false;
            }

            // Intentar diferentes métodos que podrían existir
            try {
                return usuarioService.hayUsuariosRegistrados();
            } catch (NoSuchMethodError e1) {
                try {
                    return usuarioService.hayUsuariosRegistrados();
                } catch (NoSuchMethodError e2) {
                    logger.warn("Método hayUsuariosRegistrados no disponible");
                    return true; // Asumir que hay usuarios para evitar bloqueos
                }
            }
        } catch (Exception e) {
            logger.error("Error verificando usuarios registrados", e);
            return false;
        }
    }

    public UsuarioEntity registrarUsuario(String username, String password, String email) {
        try {
            if (usuarioService == null) {
                throw new RuntimeException("UsuarioService no disponible");
            }

            Object result = usuarioService.registrarNuevoUsuario(username, password, email);

            // Manejar diferentes tipos de retorno
            if (result instanceof UsuarioEntity) {
                return (UsuarioEntity) result;
            } else if (result != null) {
                // Si es un objeto resultado complejo, extraer el usuario
                try {
                    return (UsuarioEntity) result.getClass().getMethod("getUsuario").invoke(result);
                } catch (Exception e) {
                    logger.warn("No se pudo extraer UsuarioEntity del resultado");
                    throw new RuntimeException("Error al procesar resultado de registro");
                }
            }
            throw new RuntimeException("Resultado de registro nulo");

        } catch (Exception e) {
            logger.error("Error registrando usuario: {}", username, e);
            throw new RuntimeException("Error al registrar usuario: " + e.getMessage(), e);
        }
    }

    /**
     * Consultas básicas de entidades
     */
    public List<EmpresaEntity> obtenerTodasLasEmpresas() {
        try {
            return empresaService.obtenerTodas();
        } catch (Exception e) {
            logger.error("Error obteniendo empresas", e);
            throw new RuntimeException("Error al cargar empresas", e);
        }
    }

    public List<EmpresaEntity> obtenerEmpresasConTransacciones() {
        try {
            return filtroService.obtenerEmpresasConTransacciones();
        } catch (Exception e) {
            logger.error("Error obteniendo empresas con transacciones", e);
            throw new RuntimeException("Error al cargar empresas con transacciones", e);
        }
    }

    public List<CustodioEntity> obtenerCustodiosPorEmpresa(Long empresaId) {
        try {
            return custodioService.obtenerCustodiosPorEmpresa(empresaId);
        } catch (Exception e) {
            logger.error("Error obteniendo custodios para empresa: {}", empresaId, e);
            throw new RuntimeException("Error al cargar custodios", e);
        }
    }

    public List<CustodioEntity> obtenerCustodiosConTransacciones(Long empresaId) {
        try {
            return filtroService.obtenerCustodiosConTransacciones(empresaId);
        } catch (Exception e) {
            logger.error("Error obteniendo custodios con transacciones para empresa: {}", empresaId, e);
            throw new RuntimeException("Error al cargar custodios con transacciones", e);
        }
    }

    public List<String> obtenerCuentasConTransacciones(Long empresaId, Long custodioId) {
        try {
            return filtroService.obtenerCuentasConTransacciones(empresaId, custodioId);
        } catch (Exception e) {
            logger.error("Error obteniendo cuentas para empresa: {} y custodio: {}", empresaId, custodioId, e);
            throw new RuntimeException("Error al cargar cuentas", e);
        }
    }

    public List<InstrumentoEntity> obtenerInstrumentosConTransacciones(Long empresaId, Long custodioId, String cuenta) {
        try {
            return filtroService.obtenerInstrumentosConTransacciones(empresaId, custodioId, cuenta);
        } catch (Exception e) {
            logger.error("Error obteniendo instrumentos para empresa: {}, custodio: {}, cuenta: {}",
                    empresaId, custodioId, cuenta, e);
            throw new RuntimeException("Error al cargar instrumentos", e);
        }
    }

    public List<ResumenInstrumentoDto> ResumenPortafolioService(Long empresaId, Long custodioId, String cuenta) {
        try {
            return ResumenPortafolioService(empresaId, custodioId, cuenta);
        } catch (Exception e) {
            logger.error("Error en la consulta de saldos del portafolio", e);
            throw new RuntimeException("Error al cargar instrumentos", e);
        }
    }

    /**
     * Reportes básicos (solo los que no requieren APIs complejas)
     */
    public List<ResumenSaldoEmpresaDto> obtenerResumenSaldosEmpresas() {
        try {
            return resumenSaldoEmpresaService.obtenerResumenSaldos();
        } catch (Exception e) {
            logger.error("Error obteniendo resumen saldos empresas", e);
            throw new RuntimeException("Error al generar resumen saldos", e);
        }
    }

    public List<SaldoMensualDto> obtenerSaldosMensuales(String razonSocial, String custodio, int anio, String moneda) {
        try {
            return saldoMensualService.obtenerSaldosMensuales(razonSocial, custodio, anio, moneda);
        } catch (Exception e) {
            logger.error("Error obteniendo saldos mensuales", e);
            throw new RuntimeException("Error al generar saldos mensuales", e);
        }
    }

    /**
     * Operaciones y transacciones
     */
    public List<OperacionesTrxsDto> obtenerOperacionesPorGrupo(String empresa, String custodio, String cuenta, List<String> nemos) {
        try {
            return operacionesTrxsService.obtenerTransaccionesPorGrupo(empresa, custodio, cuenta, nemos);
        } catch (Exception e) {
            logger.error("Error obteniendo operaciones por grupo", e);
            throw new RuntimeException("Error al cargar operaciones", e);
        }
    }

    public List<ProblemasTrxsDto> obtenerTransaccionesConProblemas(String razonSocial, String nombreCustodio) {
        try {
            return problemasTrxsService.obtenerTransaccionesConProblemas(razonSocial, nombreCustodio);
        } catch (Exception e) {
            logger.error("Error obteniendo transacciones con problemas", e);
            throw new RuntimeException("Error al cargar transacciones problemáticas", e);
        }
    }

    public void crearTransaccionManual(TransaccionManualDto transaccionDto) {
        try {
            transaccionService.crearTransaccionManual(transaccionDto);
            logger.info("Transacción manual creada exitosamente");
        } catch (Exception e) {
            logger.error("Error creando transacción manual", e);
            throw new RuntimeException("Error al guardar transacción: " + e.getMessage(), e);
        }
    }

    public TransaccionEntity obtenerTransaccionPorId(Long id) {
        try {
            return transaccionService.obtenerTransaccionPorId(id);
        } catch (Exception e) {
            logger.error("Error obteniendo transacción ID: {}", id, e);
            throw new RuntimeException("Error al cargar transacción", e);
        }
    }

    public void toggleIgnorarTransaccionEnCosteo(Long transaccionId) {
        try {
            transaccionService.toggleIgnorarEnCosteo(transaccionId);
            logger.info("Estado transacción {} modificado", transaccionId);
        } catch (Exception e) {
            logger.error("Error modificando estado transacción: {}", transaccionId, e);
            throw new RuntimeException("Error al modificar transacción", e);
        }
    }

    /**
     * Procesamiento (solo normalización)
     */
    public ResultadoCargaDto ejecutarNormalizacion() {
        try {
            logger.info("Iniciando normalización");
            return normalizarService.ejecutar();
        } catch (Exception e) {
            logger.error("Error en normalización", e);
            throw new RuntimeException("Error durante normalización: " + e.getMessage(), e);
        }
    }

    /**
     * Gestión de instrumentos
     */
    public void fusionarInstrumentos(Long idInstrumentoAntiguo, Long idInstrumentoNuevo) {
        try {
            fusionInstrumentoService.fusionarYPrepararRecosteo(idInstrumentoAntiguo, idInstrumentoNuevo);
            logger.info("Fusión completada: {} -> {}", idInstrumentoAntiguo, idInstrumentoNuevo);
        } catch (Exception e) {
            logger.error("Error fusionando instrumentos", e);
            throw new RuntimeException("Error en fusión: " + e.getMessage(), e);
        }
    }

    /**
     * Métodos que requieren servicios no disponibles - retornan funcionalidad
     * limitada
     */
    public ResultadoCargaDto ejecutarCargaInicial(ListaEnumsCustodios custodio, File archivo) {
        logger.warn("Funcionalidad de carga inicial no disponible - requiere configuración adicional");
        throw new UnsupportedOperationException("Carga inicial requiere configuración de servicios de procesamiento");
    }

    public ResultadoCargaDto ejecutarCargaDiaria(ListaEnumsCustodios custodio, File archivo) {
        logger.warn("Funcionalidad de carga diaria no disponible - requiere configuración adicional");
        throw new UnsupportedOperationException("Carga diaria requiere configuración de servicios de procesamiento");
    }

    public void iniciarCosteoCompleto() {
        logger.warn("Funcionalidad de costeo completo no disponible - requiere CostingApi");
        throw new UnsupportedOperationException("Costeo completo requiere CostingApi disponible");
    }

    /**
     * Utilidades del sistema
     */
    public boolean verificarConectividad() {
        try {
            empresaService.obtenerTodas();
            return true;
        } catch (Exception e) {
            logger.error("Error verificando conectividad", e);
            return false;
        }
    }

    public String obtenerInformacionSistema() {
        StringBuilder info = new StringBuilder();
        try {
            int totalEmpresas = empresaService.obtenerTodas().size();
            int empresasConTrx = filtroService.obtenerEmpresasConTransacciones().size();
            boolean conectividad = verificarConectividad();

            info.append("=== SISTEMA PORTAFOLIO - ESTADO ===\n");
            info.append("Fecha: ").append(LocalDate.now()).append("\n");
            info.append("Conectividad BD: ").append(conectividad ? "✓ OK" : "✗ ERROR").append("\n");
            info.append("Total empresas: ").append(totalEmpresas).append("\n");
            info.append("Empresas con transacciones: ").append(empresasConTrx).append("\n");
            info.append("Usuarios disponibles: ").append(usuarioService != null ? "✓ SÍ" : "✗ NO").append("\n");
            info.append("Controladores registrados: ").append(controllerSuppliers.size()).append("\n");

        } catch (Exception e) {
            info.append("ERROR obteniendo información: ").append(e.getMessage());
            logger.error("Error obteniendo información sistema", e);
        }
        return info.toString();
    }
}