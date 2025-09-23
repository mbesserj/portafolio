import os
import re
import pandas as pd
import pymysql
from datetime import datetime

# Configuración de la conexión MySQL
db_config = {
    'host': 'localhost',
    'user': 'root',
    'password': 'Mb113017$',  # Reemplaza con tu contraseña real
    'database': 'fynsa',
    'charset': 'utf8mb4',
    'cursorclass': pymysql.cursors.DictCursor
}

excel_dir = r"C:\AdjuntosFYNSA"

columnas_caja = [
    "FECHA", "NOMBRE", "RUT", "CUENTA", "CUSTODIO", "MOVIMIENTO",
    "FOLIO", "NEMO", "DESCRIPCION", "CANTIDAD", "PRECIO", "MONTO", "MONEDA"
]

columnas_movimientos = [
    "FECHA", "NOMBRE", "RUT", "CUENTA", "CUSTODIO", "MOVIMIENTO",
    "FOLIO", "NEMO", "DESCRIPCION", "CANTIDAD", "PRECIO", "MONTO",
    "COMISIONES", "GASTOS", "TOTAL", "MONEDA"
]

def limpiar_valor(valor, tipo='texto'):
    if pd.isna(valor) or (isinstance(valor, str) and valor.strip() == ''):
        return 0 if tipo == 'numero' else None
    if tipo == 'numero':
        try:
            return float(str(valor).replace('.', '').replace(',', '.').strip())
        except:
            return 0
    if tipo == 'fecha':
        try:
            return pd.to_datetime(valor).date()
        except:
            return None
    return str(valor).strip() if isinstance(valor, str) else valor

# Extraer fecha desde nombre de archivo
def extraer_fecha_archivo(nombre):
    match = re.search(r'mvtos_\d+_(\d{8})_\d+', nombre)
    if match:
        return datetime.strptime(match.group(1), "%Y%m%d")
    return None

# Seleccionar el último archivo por mes
archivos_por_mes = {}
for archivo in os.listdir(excel_dir):
    if archivo.lower().startswith("mvtos_") and archivo.lower().endswith(('.xlsx', '.xls', '.xlsm')):
        fecha = extraer_fecha_archivo(archivo)
        if fecha:
            clave_mes = fecha.strftime("%Y-%m")
            if clave_mes not in archivos_por_mes or fecha > archivos_por_mes[clave_mes][1]:
                archivos_por_mes[clave_mes] = (archivo, fecha)

# Conexión a la base de datos
conexion = pymysql.connect(**db_config)
try:
    with conexion.cursor() as cursor:
        # Crear tablas si no existen
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS carga_trxs_caja (
                FECHA DATE,
                NOMBRE VARCHAR(255),
                RUT VARCHAR(255),
                CUENTA VARCHAR(255),
                CUSTODIO VARCHAR(255),
                MOVIMIENTO VARCHAR(255),
                FOLIO VARCHAR(255),
                NEMO VARCHAR(255),
                DESCRIPCION VARCHAR(255),
                CANTIDAD DOUBLE,
                PRECIO DOUBLE,
                MONTO DOUBLE,
                MONEDA VARCHAR(255)
            )
        """)
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS carga_trxs_movimientos (
                FECHA DATE,
                NOMBRE VARCHAR(255),
                RUT VARCHAR(255),
                CUENTA VARCHAR(255),
                CUSTODIO VARCHAR(255),
                MOVIMIENTO VARCHAR(255),
                FOLIO VARCHAR(255),
                NEMO VARCHAR(255),
                DESCRIPCION VARCHAR(255),
                CANTIDAD DOUBLE,
                PRECIO DOUBLE,
                MONTO DOUBLE,
                COMISIONES DOUBLE,
                GASTOS DOUBLE,
                TOTAL DOUBLE,
                MONEDA VARCHAR(255)
            )
        """)

        for clave_mes, (archivo, fecha) in archivos_por_mes.items():
            ruta_archivo = os.path.join(excel_dir, archivo)
            print(f"Procesando archivo del mes {clave_mes}: {archivo}")
            try:
                xls = pd.read_excel(ruta_archivo, sheet_name=None, engine='openpyxl')
            except Exception as e:
                print(f"No se pudo leer el archivo {archivo}: {e}")
                continue

            # Hoja 1: carga_trxs_caja
            try:
                df_caja = list(xls.values())[0]
                if 'FECHA' not in df_caja.columns:
                    print(f"⚠️ Hoja 1 sin columna 'FECHA' en {archivo}")
                else:
                    df_caja = df_caja[[col for col in columnas_caja if col in df_caja.columns]]
                    df_caja['FECHA'] = pd.to_datetime(df_caja['FECHA'], errors='coerce')
                    df_caja = df_caja[df_caja['FECHA'].notna()]

                    sql_caja = """
                        INSERT INTO carga_trxs_caja (FECHA, NOMBRE, RUT, CUENTA, CUSTODIO,
                            MOVIMIENTO, FOLIO, NEMO, DESCRIPCION, CANTIDAD, PRECIO, MONTO, MONEDA)
                        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    """
                    for _, row in df_caja.iterrows():
                        valores = [
                            limpiar_valor(row.get(col), tipo='fecha' if col == 'FECHA' else 'numero' if col in ["CANTIDAD", "PRECIO", "MONTO"] else 'texto')
                            for col in columnas_caja
                        ]
                        cursor.execute(sql_caja, tuple(valores))
            except Exception as e:
                print(f"Error procesando hoja 1 de {archivo}: {e}")

            # Hoja 2: carga_trxs_movimientos
            try:
                df_mov = list(xls.values())[1]
                if 'FECHA' not in df_mov.columns:
                    print(f"⚠️ Hoja 2 sin columna 'FECHA' en {archivo}")
                else:
                    df_mov = df_mov[[col for col in columnas_movimientos if col in df_mov.columns]]
                    df_mov['FECHA'] = pd.to_datetime(df_mov['FECHA'], errors='coerce')
                    df_mov = df_mov[df_mov['FECHA'].notna()]

                    sql_mov = """
                        INSERT INTO carga_trxs_movimientos (FECHA, NOMBRE, RUT, CUENTA, CUSTODIO,
                            MOVIMIENTO, FOLIO, NEMO, DESCRIPCION, CANTIDAD, PRECIO, MONTO,
                            COMISIONES, GASTOS, TOTAL, MONEDA)
                        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    """
                    for _, row in df_mov.iterrows():
                        valores = [
                            limpiar_valor(row.get(col), tipo='fecha' if col == 'FECHA' else 'numero' if col in ["CANTIDAD", "PRECIO", "MONTO", "COMISIONES", "GASTOS", "TOTAL"] else 'texto')
                            for col in columnas_movimientos
                        ]
                        cursor.execute(sql_mov, tuple(valores))
            except Exception as e:
                print(f"Error procesando hoja 2 de {archivo}: {e}")

    conexion.commit()
    print("✅ Todos los datos válidos fueron insertados correctamente.")
finally:
    conexion.close()
