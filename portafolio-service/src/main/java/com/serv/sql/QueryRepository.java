package com.serv.sql;

public class QueryRepository {

    public enum AuthenticationQueries {
        USUARIO_ES_ACTIVO_QUERY("""
            SELECT u FROM UsuarioEntity u WHERE u.usuario = :user AND u.fechaInactivo IS NULL
                    """);

        private final String sql;

        AuthenticationQueries(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }
    }

    public enum CustodioQueries {
        CUSTODIOS_POR_EMPRESA_QUERY("""
            SELECT e FROM EmpresaEntity e LEFT JOIN FETCH e.custodios WHERE e.id = :empresaId
                   """),
        CUENTAS_POR_CUSTODIO_EMPRESA_QUERY("""
            SELECT c.cuenta 
            FROM CuentaEntity c
            WHERE c.custodio.id = :custodioId 
              AND c.empresa.id = :empresaId
            ORDER BY c.cuenta ASC
                    """);

        private final String sql;

        CustodioQueries(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }
    }

    public enum ConfrontaQueries {

        CONFRONTA_SALDOS_QUERY("""
            WITH 
            UltimoSaldoMercado AS (
                SELECT 
                    s.empresa_id, s.custodio_id, s.instrumento_id, s.cuenta,
                    s.fecha, s.cantidad, s.monto_clp, s.precio,
                    ROW_NUMBER() OVER(PARTITION BY s.empresa_id, s.custodio_id, s.instrumento_id, s.cuenta ORDER BY s.fecha DESC, s.id DESC) as rn
                FROM saldos s
                WHERE s.fecha <= :fechaCorte
            ),
            TodosLosGrupos AS (
                SELECT empresa_id, custodio_id, instrumento_id, cuenta FROM saldos_kardex
                UNION
                SELECT empresa_id, custodio_id, instrumento_id, cuenta FROM UltimoSaldoMercado WHERE rn = 1
            )
            SELECT 
                g.empresa_id, g.custodio_id, g.instrumento_id,
                e.razonSocial AS empresa_nombre, c.custodio AS custodio_nombre, i.nemo AS instrumento_nemo, g.cuenta,
                sk.fecha_ultima_actualizacion AS ultima_fecha_kardex,
                COALESCE(sk.saldo_cantidad, 0) AS cantidad_kardex,
                COALESCE(sk.costo_total, 0) AS valor_kardex,
                sm.fecha AS ultima_fecha_saldos,
                COALESCE(sm.cantidad, 0) AS cantidad_mercado,
                COALESCE(sm.monto_clp, 0) AS valor_mercado,
                (COALESCE(sm.cantidad, 0) - COALESCE(sk.saldo_cantidad, 0)) AS diferencia_cantidad,
                sm.precio AS precio_mercado
            FROM TodosLosGrupos g
            LEFT JOIN saldos_kardex sk ON g.empresa_id = sk.empresa_id AND g.custodio_id = sk.custodio_id AND g.instrumento_id = sk.instrumento_id AND g.cuenta = sk.cuenta
            LEFT JOIN (SELECT * FROM UltimoSaldoMercado WHERE rn = 1) sm ON g.empresa_id = sm.empresa_id AND g.custodio_id = sm.custodio_id AND g.instrumento_id = sm.instrumento_id AND g.cuenta = sm.cuenta
            LEFT JOIN empresas e ON g.empresa_id = e.id
            LEFT JOIN custodios c ON g.custodio_id = c.id
            LEFT JOIN instrumentos i ON g.instrumento_id = i.id
            HAVING ABS(diferencia_cantidad) > 0.0001
            ORDER BY e.razonSocial, c.custodio, g.cuenta, i.nemo
                    """),
        ULTIMA_FECHA_SALDOS_QUERY("""
            SELECT MAX(s.fecha) FROM SaldoEntity s
                    """);

        private final String sql;

        ConfrontaQueries(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }
    }

    public enum EmpresaQueries {

        EMPRESAS_CON_TRANSACCIONES_QUERY("""
            SELECT DISTINCT t.empresa FROM TransaccionEntity t 
            ORDER BY t.empresa.razonSocial ASC
                    """);

        private final String sql;

        EmpresaQueries(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }
    }

    public enum FiltroServiceQueries {

        EMPRESAS_CON_TRANSACCIONES_QUERY("""
            SELECT DISTINCT t.empresa FROM TransaccionEntity t ORDER BY t.empresa.razonSocial ASC
                    """),
        CUSTODIOS_CON_TRANSACCIONES_QUERY("""
            SELECT DISTINCT t.custodio FROM TransaccionEntity t WHERE t.empresa.id = :empresaId ORDER BY t.custodio.nombreCustodio ASC
                    """),
        CUENTAS_CON_TRANSACCIONES("""
            SELECT DISTINCT t.cuenta FROM TransaccionEntity t 
            WHERE t.empresa.id = :empresaId 
            AND t.custodio.id = :custodioId 
            AND t.cuenta IS NOT NULL 
            ORDER BY t.cuenta ASC
                    """),
        INSTRUMENTOS_CON_TRANSACCIONES_QUERY("""
            SELECT DISTINCT t.instrumento FROM TransaccionEntity t
            WHERE t.empresa.id = :empresaId AND t.custodio.id = :custodioId AND t.cuenta = :cuenta
            ORDER BY t.instrumento.instrumentoNemo ASC
                    """);

        private final String sql;

        FiltroServiceQueries(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }
    }

    public enum IntrumentoQueries {
        INSTRUMENTO_TODOS("""
            SELECT i FROM InstrumentoEntity i JOIN FETCH i.producto ORDER BY i.instrumentoNemo ASC
                    """),
        INSTRUMENTO_POR_CUSTODIO_Y_EMPRESA_QUERY("""
            SELECT DISTINCT t.instrumento FROM TransaccionEntity t
            WHERE t.empresa.id = :empresaId
              AND t.custodio.id = :custodioId
              AND t.cuenta = :cuenta
            ORDER BY t.instrumento.instrumentoNemo ASC
                    """);

        private final String sql;

        IntrumentoQueries(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }

    }

    public enum OperacionesQueries {
        OPERACIONES_QUERY("""
            SELECT new com.app.dto.OperacionesTrxsDto(
                t.id, t.fecha, t.folio, tm.tipoMovimiento, mc.tipoContable,
                CASE WHEN mc.tipoContable = :tipoIngreso THEN t.cantidad ELSE null END,
                CASE WHEN mc.tipoContable = :tipoEgreso THEN t.cantidad ELSE null END,
                t.precio,
                t.total,
                t.costeado, t.paraRevision, t.ignorarEnCosteo
            )
            FROM TransaccionEntity t
            JOIN t.empresa e JOIN t.custodio c JOIN t.instrumento i
            JOIN t.tipoMovimiento tm JOIN tm.movimientoContable mc
            WHERE e.razonSocial = :empresa
              AND c.nombreCustodio = :custodio
              AND t.cuenta = :cuenta
              AND i.instrumentoNemo IN (:nemos)
            ORDER BY
                t.fecha ASC,
                CASE WHEN tm.esSaldoInicial = true THEN 0 ELSE 1 END ASC,
                CASE WHEN mc.tipoContable = :tipoIngreso THEN 2 ELSE 3 END ASC,
                t.id ASC
                    """);

        private final String sql;

        OperacionesQueries(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }
    }

    public enum ProblemasQueries {
        PROBLEMAS_QUERY("""
            SELECT new com.app.dto.ProblemasTrxsDto(
                t.fecha,
                t.folio,
                tm.tipoMovimiento,
                i.instrumentoNemo,
                CASE WHEN mc.tipoContable = :tipoIngreso THEN t.cantidad ELSE null END,
                CASE WHEN mc.tipoContable = :tipoEgreso THEN t.cantidad ELSE null END,
                t.precio,
                -t.total,
                t.costeado
            )
            FROM TransaccionEntity t
            JOIN t.empresa e
            JOIN t.custodio c
            JOIN t.instrumento i
            JOIN t.tipoMovimiento tm
            JOIN tm.movimientoContable mc
            WHERE t.paraRevision = true
              AND e.razonSocial = :empresa
              AND c.nombreCustodio = :custodio
            ORDER BY t.fecha ASC, t.id ASC
            """);

        private final String sql;

        ProblemasQueries(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }
    }

    public enum AggregatesQueries {
        AGGREGATES_QUERY("""
            SELECT
                SUM(CASE WHEN tm.tipo_movimiento LIKE '%Dividendo%' THEN t.monto_clp ELSE 0 END) as total_dividendos,
                SUM(COALESCE(t.gasto,0) + COALESCE(t.iva,0) + COALESCE(t.comision,0)) as total_gastos
            FROM transacciones t
            JOIN tipo_movimientos tm ON t.movimiento_id = tm.id
            WHERE t.empresa_id = :empresaId
              AND t.custodio_id = :custodioId
              AND t.cuenta = :cuenta
              AND t.instrumento_id = :instrumentoId
                    """);

        private final String sql;

        AggregatesQueries(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }
    }

    public enum PreciosQueries {
        ULTIMOS_PRECIOS_QUERY("""
        SELECT s.instrumento.id, s.precio FROM SaldoEntity s
        WHERE s.empresa.id = :empresaId AND s.custodio.id = :custodioId 
        AND s.fecha = (
            SELECT MAX(sub.fecha) FROM SaldoEntity sub
            WHERE sub.empresa.id = :empresaId AND sub.custodio.id = :custodioId 
            AND sub.fecha <= :fechaActual 
        )
        """);

        private final String sql;

        PreciosQueries(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }
    }

    public enum ResultadoInstrumentoQueries {
        OPERACIONES_QUERY("""
        WITH resultados AS (
            SELECT 
                v.id, 
                v.fecha_tran, 
                tm.tipo_movimiento, 
                t.cantidad,
                CASE WHEN v.tipo_oper = 'INGRESO' THEN COALESCE(t.cantidad, 0) ELSE 0 END AS cant_compras,
                CASE WHEN v.tipo_oper = 'EGRESO' THEN COALESCE(t.cantidad, 0) ELSE 0 END AS cant_ventas,
                SUM(v.monto_compra) AS compra, 
                SUM(v.cant_usada * v.precio_venta) AS venta,
                SUM(v.costo_oper) AS costo, 
                SUM(v.utilidad) AS utilidad
            FROM kardex_view v
            JOIN transacciones t ON v.id = t.id
            JOIN tipo_movimientos tm ON t.movimiento_id = tm.id
            WHERE v.empresa_id = :empresaId AND v.custodio_id = :custodioId AND v.cuenta = :cuenta
              AND v.nemo_id = :instrumentoId AND v.tipo_oper IN ('INGRESO', 'EGRESO')
            GROUP BY v.id, v.fecha_tran, tm.tipo_movimiento, t.cantidad
        )
        SELECT 
            id, 
            fecha_tran, 
            tipo_movimiento,
            cant_compras,
            cant_ventas,
            SUM(cant_compras - cant_ventas) OVER (ORDER BY fecha_tran, id) AS saldo,
            compra, 
            venta, 
            costo, 
            utilidad
        FROM resultados
        ORDER BY fecha_tran, id
                    """),
        DIVIDENDOS_QUERY("""
        SELECT t.id, t.fecha, tm.tipo_movimiento, t.monto
        FROM transacciones t 
        JOIN tipo_movimientos tm ON t.movimiento_id = tm.id
        WHERE t.empresa_id = :empresaId AND t.custodio_id = :custodioId 
          AND t.cuenta = :cuenta AND t.instrumento_id = :instrumentoId
          AND tm.tipo_movimiento LIKE '%Dividendo%'
                    """),
        GASTOS_QUERY("""
        SELECT t.id, COALESCE(t.gastos,0) + COALESCE(t.iva,0) + COALESCE(t.comisiones,0)
        FROM TransaccionEntity t 
        WHERE t.empresa.id = :empresaId AND t.custodio.id = :custodioId 
          AND t.cuenta = :cuenta AND t.instrumento.id = :instrumentoId
                    """);

        private final String sql;

        ResultadoInstrumentoQueries(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }
    }

    public enum ResumenHistoricoQueries {
        RESUMEN_HISTORICO_QUERY("""
            WITH 
            saldos_activos_calculados AS (
                SELECT
                    t.instrumento_id AS nemo_id
                FROM
                    kardex k
                    JOIN transacciones t ON k.transaccion_id = t.id
                    JOIN tipo_movimientos tm ON t.movimiento_id = tm.id
                    JOIN tipos_contables tc ON tm.movimiento_contable_id = tc.id
                WHERE
                    t.empresa_id = :empresaId
                    AND t.custodio_id = :custodioId
                    AND t.cuenta = :cuenta
                GROUP BY
                    t.instrumento_id
                HAVING
                    SUM(CASE WHEN tc.tipo_contable = 'INGRESO' THEN k.cantidad ELSE -k.cantidad END) > 0
            ),
            operaciones_base AS (
                SELECT
                    t.instrumento_id AS nemo_id,
                    i.nemo,
                    i.instrumento,
                    (ti.precio * dc.cantidad_usada) AS costo_fifo,
                    ((t.precio - ti.precio) * dc.cantidad_usada) AS utilidad,
                    (coalesce(t.gasto, 0) + coalesce(t.comision, 0) + coalesce(t.iva, 0)) AS gasto
                FROM
                    transacciones t
                    JOIN instrumentos i ON t.instrumento_id = i.id
                    JOIN tipo_movimientos tm ON t.movimiento_id = tm.id
                    JOIN detalle_costeos dc ON t.id = dc.egreso_id
                    JOIN transacciones ti ON dc.ingreso_id = ti.id
                WHERE
                    t.empresa_id = :empresaId
                    AND t.custodio_id = :custodioId
                    AND t.cuenta = :cuenta
                    AND tm.movimiento_contable_id = (SELECT id FROM tipos_contables WHERE tipo_contable = 'EGRESO')
            ),
            operaciones_cerradas AS (
                SELECT 
                    nemo_id, nemo, instrumento, costo_fifo, utilidad, gasto, NULL AS dividendo
                FROM operaciones_base op
                WHERE NOT EXISTS (
                    SELECT 1 FROM saldos_activos_calculados sac WHERE sac.nemo_id = op.nemo_id
                )
            ),
            dividendos_transacciones AS (
                SELECT
                    i.id AS nemo_id,
                    i.nemo,
                    i.instrumento,
                    NULL AS costo_fifo,
                    NULL AS utilidad,
                    NULL AS gasto,
                    t.monto_clp AS dividendo
                FROM
                    transacciones t
                    JOIN tipo_movimientos tm ON t.movimiento_id = tm.id
                    JOIN instrumentos i ON t.instrumento_id = i.id
                WHERE
                    t.empresa_id = :empresaId
                    AND t.custodio_id = :custodioId
                    AND t.cuenta = :cuenta
                    AND tm.tipo_movimiento LIKE '%Dividendo%'
            )
            SELECT
                nemo,
                instrumento,
                SUM(coalesce(costo_fifo, 0)) AS costo_fifo,
                SUM(coalesce(gasto, 0)) AS gasto,
                SUM(coalesce(dividendo, 0)) AS dividendo,
                SUM(coalesce(utilidad, 0)) AS utilidad,
                SUM(coalesce(utilidad, 0)) + SUM(coalesce(dividendo, 0)) - SUM(coalesce(gasto, 0)) AS total
            FROM (
                SELECT * FROM operaciones_cerradas
                UNION ALL
                SELECT * FROM dividendos_transacciones
            ) AS resultados_finales
            GROUP BY
                nemo,
                instrumento
            HAVING
                total <> 0 OR costo_fifo <> 0
            ORDER BY
                nemo
                    """);

        private final String sql;

        ResumenHistoricoQueries(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }
    }

    public enum ResumenSaldoQueries {
        RESUMEN_SALDO_QUERY("""
            SELECT
                e.razonsocial AS empresa,
                c.custodio AS custodio,
                s.cuenta AS cuenta,
                SUM(s.monto_clp) AS saldo_clp,
                SUM(s.monto_usd) AS saldo_usd
            FROM saldos s
            JOIN empresas e ON s.empresa_id = e.id
            JOIN custodios c ON s.custodio_id = c.id
            WHERE s.fecha = (SELECT MAX(fecha) FROM saldos)
            GROUP BY e.razonsocial, c.custodio, s.cuenta
            ORDER BY e.razonsocial, c.custodio
                    """);

        private final String sql;

        ResumenSaldoQueries(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }
    }

    public enum SaldoMensualQueries {
        BASE_QUERY_TEMPLATE_QUERY("""
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
        %s
        GROUP BY i.nemo, i.instrumento 
        ORDER BY i.nemo
                    """);

        private final String sql;

        SaldoMensualQueries(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }
    }

    public enum TipoTransaccionQueries {
        TRANSACCION_COMPLETA_QUERY("""
            SELECT t FROM TransaccionEntity t
            LEFT JOIN FETCH t.instrumento
            LEFT JOIN FETCH t.tipoMovimiento
            LEFT JOIN FETCH t.empresa
            LEFT JOIN FETCH t.custodio
            WHERE t.id = :id
                    """);

        private final String sql;

        TipoTransaccionQueries(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }
    }

    public enum UsuarioQueries {
        USUARIOS_EXISTE_QUERY("""
            SELECT COUNT(u) FROM UsuarioEntity u WHERE LOWER(u.usuario) = LOWER(:username)
                    """),
        HAY_USUARIO_QUERY("""
            SELECT COUNT(u.id) FROM UsuarioEntity u
                    """),
        USUARIO_POR_PERFIL("""
            SELECT u FROM UsuarioEntity u WHERE :perfil MEMBER OF u.perfiles AND u.fechaInactivo IS NULL
                    """);

        private final String sql;

        UsuarioQueries(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }
    }

    // --- Métodos de acceso ---
    public static String getAuthenticationQuery(AuthenticationQueries query) {
        return query.getSql();
    }

    public static String getCustodioQuery(CustodioQueries query) {
        return query.getSql();
    }

    public static String getConfrontaQuery(ConfrontaQueries query) {
        return query.getSql();
    }

    public static String getEmpresaQuery(EmpresaQueries query) { // Corregido
        return query.getSql();
    }

    public static String getFiltroServiceQuery(FiltroServiceQueries query) {
        return query.getSql();
    }

    public static String getInstrumentoQuery(IntrumentoQueries query) {
        return query.getSql();
    }

    public static String getOperacionesQuery(OperacionesQueries query) {
        return query.getSql();
    }

    public static String getAggregatesQuery(AggregatesQueries query) {
        return query.getSql();
    }

    public static String getPreciosQuery(PreciosQueries query) { // Corregido
        return query.getSql();
    }

    public static String getResultadoInstrumentoQuery(ResultadoInstrumentoQueries query) {
        return query.getSql();
    }

    public static String getResumenHistoricoQuery(ResumenHistoricoQueries query) {
        return query.getSql();
    }

    public static String getResumenSaldoQuery(ResumenSaldoQueries query) {
        return query.getSql();
    }

    public static String getSaldoMensualQuery(SaldoMensualQueries query) {
        return query.getSql();
    }

    public static String getTransaccionQuery(TipoTransaccionQueries query) { // Renombrado para claridad
        return query.getSql();
    }

    public static String getUsuarioQuery(UsuarioQueries query) {
        return query.getSql();
    }

    public static String getProblemasQuery(ProblemasQueries query) {
        return query.getSql();
    }

}
