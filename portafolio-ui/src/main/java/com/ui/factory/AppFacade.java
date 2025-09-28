package com.ui.factory;

import com.serv.service.TransaccionService;
import com.serv.service.ResumenPortafolioService;
import com.serv.service.NormalizarService;
import com.serv.service.SaldoActualService;
import com.serv.service.UsuarioService;
import com.serv.service.InstrumentoService;
import com.serv.service.AuthenticationService;
import com.serv.service.FiltroService;
import com.serv.service.EmpresaService;
import com.serv.service.OperacionesTrxsService;
import com.serv.service.FusionInstrumentoService;
import com.serv.service.CustodioService;
import com.serv.service.ProcesoCargaDiariaService;
import com.serv.service.ResultadoInstrumentoService;
import com.model.dto.AjustePropuestoDto;
import com.model.dto.KardexReporteDto;
import com.model.dto.OperacionesTrxsDto;
import com.model.dto.ResultadoCargaDto;
import com.model.dto.ResultadoInstrumentoDto;
import com.model.dto.ResumenInstrumentoDto;
import com.model.dto.ResumenSaldoDto;
import com.model.entities.CustodioEntity;
import com.model.entities.EmpresaEntity;
import com.model.entities.InstrumentoEntity;
import com.model.entities.TransaccionEntity;
import com.model.enums.ListaEnumsCustodios;
import com.model.enums.TipoAjuste;
import com.serv.factory.ServiceContainer;
import com.model.interfaces.CostingApi;
import com.model.interfaces.KardexApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AppFacade {

    private static final Logger logger = LoggerFactory.getLogger(AppFacade.class);
    private final ServiceContainer container;

    public AppFacade(ServiceContainer container) {
        this.container = container;
    }

    public ServiceResult<Boolean> autenticarUsuario(String usuario, String contrasena) {
        return executeServiceCall(
                () -> {
                    // 1. Obtenemos el servicio de autenticación del contenedor.
                    AuthenticationService authService = container.getService(AuthenticationService.class);

                    // 2. Llamamos al método de autenticación real, que devuelve un objeto AuthenticationResult.
                    AuthenticationService.AuthenticationResult authResult = authService.autenticar(usuario, contrasena);

                    // 3. Traducimos el resultado complejo a un simple booleano que la UI necesita.
                    return authResult.isSuccess();
                },
                "Usuario o contraseña incorrectos." // Mensaje de error genérico por seguridad
        );
    }

    public List<CustodioEntity> obtenerTodos() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    // --- INTERFACES FUNCIONALES PARA MANEJO DE EXCEPCIONES ---
    @FunctionalInterface
    private interface ServiceCallable<T> {

        T call() throws Exception;
    }

    @FunctionalInterface
    private interface ServiceRunnable {

        void run() throws Exception;
    }

    // --- METODOS DEL NEGOCIO ---
    public ServiceResult<Boolean> hayUsuariosRegistrados() {
        return executeServiceCall(
                () -> container.getService(UsuarioService.class).hayUsuariosRegistrados(),
                "No se pudo verificar la existencia de usuarios."
        );
    }

    public ServiceResult<ResultadoCargaDto> ejecutarCargaDiaria(ListaEnumsCustodios custodio, File archivo) {
        return executeServiceCall(
                () -> container.getService(ProcesoCargaDiariaService.class).ejecutar(custodio, archivo),
                "Error en carga diaria."
        );
    }

    public ServiceResult<ResultadoCargaDto> ejecutarCargaInicial(ListaEnumsCustodios custodio, File archivo) {
        return ServiceResult.error("Carga inicial no implementada en la fachada.", null);
    }

    public ServiceResult<Void> iniciarCosteoCompleto() {
        return executeServiceCall(
                () -> container.getService(CostingApi.class).ejecutarCosteoCompleto(),
                "Error al iniciar el costeo completo."
        );
    }

    public ServiceResult<ResultadoCargaDto> reprocesarNormalizacion() {
        return executeServiceCall(
                () -> container.getService(NormalizarService.class).ejecutar(),
                "Error durante el reprocesamiento de la normalización."
        );
    }

    // --- MÉTODOS DE CONSULTA PARA FILTROS ---
    public ServiceResult<List<EmpresaEntity>> obtenerEmpresasConTransacciones() {
        return executeServiceCall(
                () -> container.getService(FiltroService.class).obtenerEmpresasConTransacciones(),
                "No se pudieron cargar las empresas."
        );
    }

    public ServiceResult<List<CustodioEntity>> obtenerCustodiosConTransacciones(Long empresaId) {
        return executeServiceCall(
                () -> container.getService(FiltroService.class).obtenerCustodiosConTransacciones(empresaId),
                "No se pudieron cargar los custodios."
        );
    }

    public ServiceResult<List<String>> obtenerCuentasConTransacciones(Long empresaId, Long custodioId) {
        return executeServiceCall(
                () -> container.getService(FiltroService.class).obtenerCuentasConTransacciones(empresaId, custodioId),
                "No se pudieron cargar las cuentas."
        );
    }

    public ServiceResult<List<InstrumentoEntity>> obtenerInstrumentosConTransacciones(Long empresaId, Long custodioId, String cuenta) {
        return executeServiceCall(
                () -> container.getService(FiltroService.class).obtenerInstrumentosConTransacciones(empresaId, custodioId, cuenta),
                "No se pudieron cargar los instrumentos."
        );
    }

    // --- MÉTODOS PARA CONTROLADORES ---
    public ServiceResult<List<OperacionesTrxsDto>> obtenerOperacionesPorGrupo(
            Long empresaId, Long custodioId, String cuenta, Long instrumentoId, Long instrumentoNuevoId) {
        return executeServiceCall(() -> {
            EmpresaService empresaService = container.getService(EmpresaService.class);
            CustodioService custodioService = container.getService(CustodioService.class);
            InstrumentoService instrumentoService = container.getService(InstrumentoService.class);
            String razonSocial = empresaService.obtenerPorId(empresaId).getRazonSocial();
            String nombreCustodio = custodioService.obtenerPorId(custodioId).getNombreCustodio();
            List<String> nemos = new ArrayList<>();
            nemos.add(instrumentoService.obtenerPorId(instrumentoId).getInstrumentoNemo());
            if (instrumentoNuevoId != null && !instrumentoNuevoId.equals(instrumentoId)) {
                nemos.add(instrumentoService.obtenerPorId(instrumentoNuevoId).getInstrumentoNemo());
            }
            return container.getService(OperacionesTrxsService.class).obtenerTransaccionesPorGrupo(razonSocial, nombreCustodio, cuenta, nemos);
        }, "No se pudieron cargar las operaciones.");
    }

    public ServiceResult<List<KardexReporteDto>> obtenerMovimientosKardex(
            Long empresaId, Long custodioId, String cuenta, Long instrumentoId) {
        return executeServiceCall(
                () -> container.getService(KardexApi.class).obtenerMovimientosPorGrupo(empresaId, custodioId, cuenta, instrumentoId),
                "No se pudieron obtener los movimientos de kárdex."
        );
    }

    public ServiceResult<Void> fusionarInstrumentos(Long idInstrumentoAntiguo, Long idInstrumentoNuevo) {
        return executeServiceCall(
                () -> container.getService(FusionInstrumentoService.class).fusionarYPrepararRecosteo(idInstrumentoAntiguo, idInstrumentoNuevo),
                "La fusión de instrumentos falló."
        );
    }

    public ServiceResult<Void> eliminarAjuste(Long transaccionId) {
        return executeServiceCall(
                () -> container.getService(CostingApi.class).eliminarAjuste(transaccionId),
                "Error al eliminar el ajuste."
        );
    }

    public ServiceResult<Void> recostearGrupoPorTransaccion(Long transaccionId) {
        return executeServiceCall(() -> {
            TransaccionEntity tx = container.getService(TransaccionService.class).obtenerTransaccionPorId(transaccionId);
            if (tx == null) {
                throw new IllegalArgumentException("No se encontró la transacción con ID: " + transaccionId);
            }
            String claveAgrupacion = tx.getClaveAgrupacion();
            container.getService(CostingApi.class).recostearGrupo(claveAgrupacion);
        }, "Error al recostear el grupo.");
    }

    public ServiceResult<Void> toggleIgnorarEnCosteo(Long transaccionId) {
        return executeServiceCall(
                () -> container.getService(TransaccionService.class).toggleIgnorarEnCosteo(transaccionId),
                "Error al actualizar el estado de la transacción."
        );
    }

    public ServiceResult<TransaccionEntity> obtenerTransaccionPorId(Long transaccionId) {
        return executeServiceCall(
                () -> container.getService(TransaccionService.class).obtenerTransaccionPorId(transaccionId),
                "No se pudo obtener la transacción."
        );
    }

    public ServiceResult<AjustePropuestoDto> proponerAjuste(Long transaccionId, TipoAjuste tipo) {
        return executeServiceCall(
                () -> container.getService(CostingApi.class).proponerAjuste(transaccionId, tipo),
                "Error al proponer el ajuste."
        );
    }

    public ServiceResult<Void> crearAjuste(Long transaccionId, TipoAjuste tipo, BigDecimal cantidad, BigDecimal precio) {
        return executeServiceCall(
                () -> container.getService(CostingApi.class).crearAjuste(transaccionId, tipo, cantidad, precio),
                "Error al crear el ajuste."
        );
    }

    // --- MÉTODOS PARA LOS CONTROLADORES RESTANTES ---
    /**
     * Obtiene los saldos actuales valorizados para una empresa y custodio.
     * Utilizado por SaldosController.
     */
    public ServiceResult<List<ResumenSaldoDto>> obtenerSaldosValorizados(Long empresaId, Long custodioId) {
        return executeServiceCall(
                () -> container.getService(SaldoActualService.class).obtenerSaldosValorizados(empresaId, custodioId),
                "Error al obtener los saldos valorizados."
        );
    }

    /**
     * Obtiene todos los custodios. Utilizado por filtros y vistas
     * administrativas.
     */
    public ServiceResult<List<CustodioEntity>> obtenerTodosCustodios() {
        return executeServiceCall(
                () -> container.getService(CustodioService.class).obtenerTodos(),
                "Error al obtener la lista de custodios."
        );
    }

    /**
     * Obtiene todas las empresas. Utilizado por filtros y vistas
     * administrativas.
     */
    public ServiceResult<List<EmpresaEntity>> obtenerTodasEmpresas() {
        return executeServiceCall(
                () -> container.getService(EmpresaService.class).obtenerTodas(),
                "Error al obtener la lista de empresas."
        );
    }

    /**
     * Obtiene el resumen de portafolio para una cuenta específica. Utilizado
     * por ResumenPortafolioController.
     */
    public ServiceResult<List<ResumenInstrumentoDto>> obtenerResumenPortafolio(Long empresaId, Long custodioId, String cuenta) {
        return executeServiceCall(
                () -> container.getService(ResumenPortafolioService.class).obtenerResumenPortafolio(empresaId, custodioId, cuenta),
                "Error al obtener el resumen de portafolio."
        );
    }

    /**
     * Obtiene el historial de resultados para un instrumento específico.
     * Utilizado por ResultadoInstrumentoController.
     */
    public ServiceResult<List<ResultadoInstrumentoDto>> obtenerHistorialResultados(Long empresaId, Long custodioId, String cuenta, Long instrumentoId) {
        return executeServiceCall(
                () -> container.getService(ResultadoInstrumentoService.class).obtenerHistorialResultados(empresaId, custodioId, cuenta, instrumentoId),
                "Error al obtener el historial de resultados del instrumento."
        );
    }

    public ServiceResult<UsuarioService.UserRegistrationResult> crearUsuarioAdmin(String usuario, String contrasena) {
        return executeServiceCall(
                () -> container.getService(UsuarioService.class).crearUsuarioAdmin(usuario, contrasena),
                "Error al crear el usuario administrador."
        );
    }

    public List<EmpresaEntity> obtenerEmpresaTodas() {
        try {
            return container.getService(EmpresaService.class).obtenerTodas();
        } catch (Exception e) {
            logger.error("Error al obtener todas las empresas", e);
            return new ArrayList<>();
        }
    }

    public ServiceResult<List<CustodioEntity>> obtenerCustodiosPorEmpresa(Long empresaId) {
        return executeServiceCall(
                () -> container.getService(CustodioService.class).obtenerCustodiosPorEmpresa(empresaId),
                "Error al obtener custodios por empresa."
        );
    }

    public ServiceResult<List<String>> obtenerCuentasPorCustodioYEmpresa(Long custodioId, Long empresaId) {
        return executeServiceCall(
                () -> container.getService(CustodioService.class).obtenerCuentasPorCustodioYEmpresa(custodioId, empresaId),
                "Error al obtener cuentas por custodio y empresa."
        );
    }

    // --------------------------------------------------
    // --- HELPERS PARA EJECUTAR LLAMADAS A SERVICIOS ---
    // --------------------------------------------------
    private <T> ServiceResult<T> executeServiceCall(ServiceCallable<T> serviceCall, String errorMessage) {
        try {
            return ServiceResult.success(serviceCall.call());
        } catch (Exception e) {
            logger.error("{} - Causa: {}", errorMessage, e.getMessage(), e);
            return ServiceResult.error(errorMessage, e);
        }
    }

    private ServiceResult<Void> executeServiceCall(ServiceRunnable serviceCall, String errorMessage) {
        try {
            serviceCall.run();
            return ServiceResult.success(null);
        } catch (Exception e) {
            logger.error("{} - Causa: {}", errorMessage, e.getMessage(), e);
            return ServiceResult.error(errorMessage, e);
        }
    }

}
