import pandas as pd
import pymysql
from datetime import date
import os

# Configuración de la conexión MySQL
db_config = {
    'host': 'localhost',
    'user': 'root',
    'password': 'Mb113017$',  # Reemplaza con tu contraseña real
    'database': 'fynsa',
    'charset': 'utf8mb4',
}

# Ruta del archivo CSV
csv_path = r'C:\AdjuntosFYNSA\movimientos.csv'

# Verificar si el archivo existe
if not os.path.exists(csv_path):
    raise FileNotFoundError(f"❌ No se encontró el archivo en la ruta: {csv_path}")

# Leer el archivo CSV
mov_df = pd.read_csv(csv_path)

# Conectar a MySQL y crear la tabla, luego insertar datos
connection = pymysql.connect(**db_config)
try:
    with connection.cursor() as cursor:
        # Eliminar tabla si existe
        cursor.execute("DROP TABLE IF EXISTS movimientos")

        # Crear tabla 'movimientos' con abono_cargo como INTEGER
        cursor.execute("""
            CREATE TABLE movimientos (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                movimiento VARCHAR(255) NOT NULL,
                abono_cargo INTEGER NOT NULL DEFAULT 1,
                estaActivo BOOLEAN DEFAULT TRUE,
                fecha_creado DATE NOT NULL DEFAULT (CURRENT_DATE),
                fecha_inactivo DATE DEFAULT NULL
            )
        """)

        # Insertar datos
        insert_query = """
            INSERT INTO movimientos (movimiento, abono_cargo, estaActivo, fecha_creado, fecha_inactivo)
            VALUES (%s, %s, %s, %s, %s)
        """
        for _, row in mov_df.iterrows():
            cursor.execute(insert_query, (
                row['MOVIMIENTO'],
                1,            # abono_cargo como entero
                True,         # estaActivo por defecto
                date.today(), # fecha_creado con la fecha actual
                None          # fecha_inactivo como NULL
            ))
    connection.commit()
    print("✅ Datos insertados correctamente en la tabla 'movimientos'.")
finally:
    connection.close()
