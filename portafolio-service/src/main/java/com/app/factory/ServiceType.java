
package com.app.factory;

import com.app.service.AuthenticationService;
import com.app.service.ConfrontaService;
import com.app.service.CustodioService;
import com.app.service.EmpresaService;
import com.app.service.FiltroService;
import com.app.service.FusionInstrumentoService;
import com.app.service.InstrumentoService;
import com.app.service.LimpiezaService;
import com.app.service.NormalizarService;
import com.app.service.OperacionesTrxsService;
import com.app.service.PerfilService;
import com.app.service.ProblemasTrxsService;
import com.app.service.ProcesoCargaDiariaService;
import com.app.service.ResumenSaldoEmpresaService;
import com.app.service.SaldoMensualService;
import com.app.service.TransaccionService;
import com.app.service.UsuarioService;

public enum ServiceType {
    AUTHENTICATION(AuthenticationService.class),
    CONFRONTA(ConfrontaService.class),
    CUSTODIO(CustodioService.class),
    EMPRESA(EmpresaService.class),
    FILTRO(FiltroService.class),
    FUSION_INSTRUMENTO(FusionInstrumentoService.class),
    INSTRUMENTO(InstrumentoService.class),
    LIMPIEZA(LimpiezaService.class),
    NORMALIZAR(NormalizarService.class),
    OPERACIONES_TRXS(OperacionesTrxsService.class),
    PERFIL(PerfilService.class),
    PROBLEMAS_TRXS(ProblemasTrxsService.class),
    PROCESO_CARGA_DIARIA(ProcesoCargaDiariaService.class),
    RESUMEN_SALDO_EMPRESA(ResumenSaldoEmpresaService.class),
    SALDO_MENSUAL(SaldoMensualService.class),
    TRANSACCION(TransaccionService.class),
    USUARIO(UsuarioService.class);

    private final Class<?> serviceClass;

    ServiceType(Class<?> serviceClass) {
        this.serviceClass = serviceClass;
    }

    public Class<?> getServiceClass() {
        return serviceClass;
    }

    /**
     * Método de conveniencia para usar el enum con el factory
     */
    @SuppressWarnings("unchecked")
    public <T> T getService() {
        return (T) ServiceContainer.getInstance().getService(serviceClass);
    }
}