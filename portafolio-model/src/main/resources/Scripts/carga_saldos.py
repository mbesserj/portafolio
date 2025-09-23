import os
import pandas as pd
import pymysql

# Configuración de la conexión MySQL
db_config = {
    'host': 'localhost',
    'user': 'root',
    'password': 'Mb113017$',
    'database': 'fynsa',
    'charset': 'utf8mb4',
    'cursorclass': pymysql.cursors.DictCursor
}

# Ruta de la carpeta con archivos Excel
excel_dir = r"C:\AdjuntosFYNSA"

# Columnas a incluir
columnas_incluir = [
    "FECHA", "NOMBRE", "RUT", "CUENTA", "CUENTA_PSH", "CUSTODIO",
    "NEMO", "DESCRIPCION", "CANTIDAD", "PRECIO", "MONTO_CLP", "MONTO_USD", "MONEDA"
]

# Leer archivos Excel que comienzan con "stock"
datos_totales = []
for archivo in os.listdir(excel_dir):
    if archivo.lower().startswith("stock") and archivo.endswith(('.xlsx', '.xls', '.xlsm')):
        ruta_archivo = os.path.join(excel_dir, archivo)
        df = pd.read_excel(ruta_archivo, engine='openpyxl')
        if 'FECHA_INVERSION' in df.columns:
            df = df.drop(columns=['FECHA_INVERSION'])
        df = df[[col for col in columnas_incluir if col in df.columns]]
        datos_totales.append(df)

# Combinar todos los datos
df_final = pd.concat(datos_totales, ignore_index=True) if datos_totales else pd.DataFrame(columns=columnas_incluir)

# Insertar en la base de datos
conexion = pymysql.connect(**db_config)
try:
    with conexion.cursor() as cursor:
        # Verificar si la tabla existe
        cursor.execute("""
            SELECT COUNT(*) AS existe
            FROM information_schema.tables
            WHERE table_schema = %s AND table_name = 'carga_saldos'
        """, (db_config['database'],))
        resultado = cursor.fetchone()

        # Crear tabla si no existe
        if resultado['existe'] == 0:
            cursor.execute("""
                CREATE TABLE carga_saldos (
                    FECHA DATE,
                    NOMBRE VARCHAR(255),
                    RUT VARCHAR(255),
                    CUENTA VARCHAR(255),
                    CUENTA_PSH VARCHAR(255),
                    CUSTODIO VARCHAR(255),
                    NEMO VARCHAR(255),
                    DESCRIPCION VARCHAR(255),
                    CANTIDAD DOUBLE,
                    PRECIO DOUBLE,
                    MONTO_CLP DOUBLE,
                    MONTO_USD DOUBLE,
                    MONEDA VARCHAR(255)
                )
            """)
            print("Tabla 'carga_saldos' creada.")

        # Insertar datos con limpieza
        sql = """
            INSERT INTO carga_saldos (FECHA, NOMBRE, RUT, CUENTA, CUENTA_PSH,
                CUSTODIO, NEMO, DESCRIPCION, CANTIDAD, PRECIO, MONTO_CLP, MONTO_USD, MONEDA)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """
        for _, row in df_final.iterrows():
            valores = []
            for col in columnas_incluir:
                valor = row.get(col)
                if pd.isna(valor):
                    valores.append(None)
                elif col in ["CANTIDAD", "PRECIO", "MONTO_CLP", "MONTO_USD"]:
                    try:
                        valores.append(float(str(valor).replace(",", "").strip()))
                    except:
                        valores.append(None)
                else:
                    valores.append(str(valor).strip() if isinstance(valor, str) else valor)
            cursor.execute(sql, tuple(valores))

    conexion.commit()
    print("Datos insertados correctamente.")
finally:
    conexion.close()
