package com.app.utiles;

import com.app.entities.*;
import com.app.enums.TipoEnumsCosteo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

public class DataInitializer {

    private DataInitializer() {
    }

    public static void inicializarDatosBase() {
        EntityManager em = LibraryInitializer.getEntityManager();
        try {
            if (datosYaExisten(em)) {
                System.out.println("Los datos base ya existen. No se requiere inicialización.");
                return;
            }
            
            if (!verificarVistaExiste(em, "kardex_view")) {
                System.out.println("La vista 'kardex_view' no existe. Creándola...");
                crearVistaKardex(em);
            } else {
                System.out.println("La vista 'kardex_view' ya existe");
            }
            
            System.out.println("Inicializando datos base en la base de datos...");
            if (!verificarVistaExiste(em, "saldos_view")) {
                System.out.println("La vista 'saldos_view' no existe. Creándola...");
                crearVistaSaldos(em);
            } else {
                System.out.println("La vista 'saldos_view' ya existe.");
            }

            em.getTransaction().begin();

            // --- Creación de Movimientos Contables (Ajustado a la entidad real) ---
            MovimientoContableEntity ingreso = crearMovimientoContable(em, TipoEnumsCosteo.INGRESO, "Movimiento de Ingreso General");
            MovimientoContableEntity egreso = crearMovimientoContable(em, TipoEnumsCosteo.EGRESO, "Movimiento de Egreso General");
            MovimientoContableEntity cargo = crearMovimientoContable(em, TipoEnumsCosteo.CARGO, "Movimiento de Cargo de caja");
            MovimientoContableEntity dividendo = crearMovimientoContable(em, TipoEnumsCosteo.DIVIDENDO, "Movimiento de dividendo");
            MovimientoContableEntity retorno = crearMovimientoContable(em, TipoEnumsCosteo.RETORNO, "Movimiento de Devolución capital");
            crearMovimientoContable(em, TipoEnumsCosteo.NO_COSTEAR, "Movimiento que no afecta costeo");

            // --- Creación de Tipos de Movimiento ---
            crearTipoMovimiento(em, "COMPRA", "Compra de activos", ingreso);
            crearTipoMovimiento(em, "VENTA", "Venta de activos", egreso);
            crearTipoMovimiento(em, "AJUSTE INGRESO", "Ajuste manual por faltante de inventario", ingreso);
            crearTipoMovimiento(em, "AJUSTE EGRESO", "Ajuste manual por sobrante de inventario", egreso);
            crearTipoMovimiento(em, "AJUSTE_AUTO_TOLERANCIA", "Ajuste automático por diferencia dentro de la tolerancia", ingreso);
            crearTipoMovimiento(em, "SALDO INICIAL", "Saldo inicial", ingreso);
            
            //crearEmpresa(em, "MAX ALEJANDRO BESSER JIRKAL", "87139166");
            //crearCustodio(em, "BanChile");
            //crearCustodio(em, "Fynsa");

            em.getTransaction().commit();
            System.out.println("¡Datos base creados exitosamente!");
        } catch (Exception e) {
            System.err.println("Error durante la inicialización de datos. Se revertirán los cambios.");
            e.printStackTrace();
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    private static boolean datosYaExisten(EntityManager em) {
        try {
            em.createQuery("SELECT tm FROM TipoMovimientoEntity tm WHERE tm.tipoMovimiento = :nombre", TipoMovimientoEntity.class)
              .setParameter("nombre", "COMPRA")
              .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private static MovimientoContableEntity crearMovimientoContable(EntityManager em, TipoEnumsCosteo tipo, String descripcion) {
        MovimientoContableEntity mc = new MovimientoContableEntity();
        mc.setTipoContable(tipo);
        mc.setDescripcionContable(descripcion);
        em.persist(mc);
        return mc;
    }

    private static TipoMovimientoEntity crearTipoMovimiento(EntityManager em, String nombre, String descripcion, MovimientoContableEntity mc) {
        TipoMovimientoEntity tm = new TipoMovimientoEntity();
        tm.setTipoMovimiento(nombre);
        tm.setDescripcion(descripcion);
        tm.setMovimientoContable(mc);
        em.persist(tm);
        return tm;
    }

    private static EmpresaEntity crearEmpresa(EntityManager em, String razonSocial, String rut) {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setRazonSocial(razonSocial);
        empresa.setRut(rut);
        em.persist(empresa);
        return empresa;
    }

    private static CustodioEntity crearCustodio(EntityManager em, String nombre) {
        CustodioEntity custodio = new CustodioEntity();
        custodio.setNombreCustodio(nombre);
        em.persist(custodio);
        return custodio;
    }

    private static boolean verificarVistaExiste(EntityManager em, String vistaNombre) {
        String sql = "SELECT COUNT(*) FROM information_schema.VIEWS WHERE TABLE_NAME = :vistaNombre";
        Object result = em.createNativeQuery(sql)
                          .setParameter("vistaNombre", vistaNombre)
                          .getSingleResult();
        return ((Number) result).intValue() > 0;
    }

    private static void crearVistaSaldos(EntityManager em) {
        String sql = """
            CREATE OR REPLACE VIEW saldos_view AS
            SELECT 
                `k`.`nemo_id` AS `nemo_id`,
                `k`.`empresa_id` AS `empresa_id`,
                `k`.`custodio_id` AS `custodio_id`,
                SUM(`k`.`saldo_cantidad`) AS `saldo_cantidad`,
                SUM(`k`.`saldo_valor`) AS `costo_total`,
                (CASE
                    WHEN (SUM(`k`.`saldo_cantidad`) > 0) THEN (SUM(`k`.`saldo_valor`) / SUM(`k`.`saldo_cantidad`))
                    ELSE 0
                END) AS `costo_unitario`,
                SUM(`s`.`cantidad`) AS `cantidad_mercado`,
                SUM(`s`.`saldo_valor`) AS `valor_mercado`
            FROM
                ((SELECT 
                    `kardex_view`.`empresa_id` AS `empresa_id`,
                        `kardex_view`.`custodio_id` AS `custodio_id`,
                        `kardex_view`.`nemo_id` AS `nemo_id`,
                        `kardex_view`.`saldo_cantidad` AS `saldo_cantidad`,
                        `kardex_view`.`saldo_valor` AS `saldo_valor`
                FROM
                    `kardex_view`
                WHERE
                    (`kardex_view`.`fecha_tran` = (SELECT 
                            MAX(`kv`.`fecha_tran`)
                        FROM
                            `kardex_view` `kv`
                        WHERE
                            ((`kv`.`empresa_id` = `kardex_view`.`empresa_id`)
                                AND (`kv`.`custodio_id` = `kardex_view`.`custodio_id`)
                                AND (`kv`.`nemo_id` = `kardex_view`.`nemo_id`))))) `k`
                LEFT JOIN (SELECT 
                    `saldos`.`empresa_id` AS `empresa_id`,
                        `saldos`.`custodio_id` AS `custodio_id`,
                        `saldos`.`instrumento_id` AS `nemo_id`,
                        SUM(`saldos`.`cantidad`) AS `cantidad`,
                        SUM(`saldos`.`monto_clp`) AS `saldo_valor`
                FROM
                    `saldos`
                WHERE
                    (`saldos`.`fecha` = (SELECT 
                            MAX(`s2`.`fecha`)
                        FROM
                            `saldos` `s2`
                        WHERE
                            ((`s2`.`empresa_id` = `saldos`.`empresa_id`)
                                AND (`s2`.`custodio_id` = `saldos`.`custodio_id`)
                                AND (`s2`.`instrumento_id` = `saldos`.`instrumento_id`))))
                GROUP BY `saldos`.`empresa_id` , `saldos`.`custodio_id` , `saldos`.`instrumento_id`) `s` ON (((`k`.`empresa_id` = `s`.`empresa_id`)
                    AND (`k`.`custodio_id` = `s`.`custodio_id`)
                    AND (`k`.`nemo_id` = `s`.`nemo_id`))))
            GROUP BY `k`.`nemo_id` , `k`.`empresa_id` , `k`.`custodio_id`
            HAVING (`saldo_cantidad` > 0)
        """;

        em.getTransaction().begin();
        em.createNativeQuery(sql).executeUpdate();
        em.getTransaction().commit();
    }
    
    private static void crearVistaKardex(EntityManager em) {
        String sql = """
            CREATE 
                ALGORITHM = UNDEFINED 
                DEFINER = `root`@`localhost` 
                SQL SECURITY DEFINER
            VIEW `kardex_view` AS
                SELECT 
                    `t`.`id` AS `id`,
                    `t`.`empresa_id` AS `empresa_id`,
                    `t`.`custodio_id` AS `custodio_id`,
                    `t`.`cuenta` AS `cuenta`,
                    `i`.`nemo` AS `nemo`,
                    `i`.`id` AS `nemo_id`,
                    `k`.`fecha_transaccion` AS `fecha_tran`,
                    `tc`.`tipo_contable` AS `tipo_oper`,
                    (CASE
                        WHEN (`tc`.`tipo_contable` = 'INGRESO') THEN `k`.`cantidad`
                        ELSE NULL
                    END) AS `cant_compra`,
                    (CASE
                        WHEN (`tc`.`tipo_contable` = 'INGRESO') THEN `t`.`precio`
                        ELSE NULL
                    END) AS `precio_compra`,
                    (CASE
                        WHEN (`tc`.`tipo_contable` = 'INGRESO') THEN (`t`.`precio` * `k`.`cantidad`)
                        ELSE NULL
                    END) AS `monto_compra`,
                    (CASE
                        WHEN (`tc`.`tipo_contable` = 'EGRESO') THEN `k`.`cantidad`
                        ELSE NULL
                    END) AS `total_fact`,
                    (CASE
                        WHEN (`tc`.`tipo_contable` = 'EGRESO') THEN `dc`.`cantidad_usada`
                        ELSE NULL
                    END) AS `cant_usada`,
                    (CASE
                        WHEN (`tc`.`tipo_contable` = 'EGRESO') THEN `ti`.`fecha`
                        ELSE NULL
                    END) AS `fecha_compra`,
                    (CASE
                        WHEN (`tc`.`tipo_contable` = 'EGRESO') THEN `ti`.`precio`
                        ELSE NULL
                    END) AS `costo_fifo`,
                    (CASE
                        WHEN (`tc`.`tipo_contable` = 'EGRESO') THEN `t`.`precio`
                        ELSE NULL
                    END) AS `precio_venta`,
                    (CASE
                        WHEN (`tc`.`tipo_contable` = 'EGRESO') THEN (`dc`.`cantidad_usada` * `ti`.`precio`)
                        ELSE NULL
                    END) AS `costo_oper`,
                    (CASE
                        WHEN (`tc`.`tipo_contable` = 'EGRESO') THEN (`t`.`precio` - `ti`.`precio`)
                        ELSE NULL
                    END) AS `margen`,
                    (CASE
                        WHEN (`tc`.`tipo_contable` = 'EGRESO') THEN ((`t`.`precio` - `ti`.`precio`) * `dc`.`cantidad_usada`)
                        ELSE NULL
                    END) AS `utilidad`,
                    `k`.`saldo_cantidad` AS `saldo_cantidad`,
                    `k`.`saldo_valor` AS `saldo_valor`
                FROM
                    ((((((`kardex` `k`
                    JOIN `transacciones` `t` ON ((`k`.`transaccion_id` = `t`.`id`)))
                    JOIN `instrumentos` `i` ON ((`t`.`instrumento_id` = `i`.`id`)))
                    JOIN `tipo_movimientos` `tm` ON ((`t`.`movimiento_id` = `tm`.`id`)))
                    JOIN `tipos_contables` `tc` ON ((`tm`.`movimiento_contable_id` = `tc`.`id`)))
                    LEFT JOIN `detalle_costeos` `dc` ON ((`t`.`id` = `dc`.`egreso_id`)))
                    LEFT JOIN `transacciones` `ti` ON ((`dc`.`ingreso_id` = `ti`.`id`)))
                ORDER BY `t`.`empresa_id` , `t`.`custodio_id` , `t`.`cuenta` , `i`.`id` , `k`.`fecha_transaccion` , `dc`.`ingreso_id`
            """;

        em.getTransaction().begin();
        em.createNativeQuery(sql).executeUpdate();
        em.getTransaction().commit();
    }
}
