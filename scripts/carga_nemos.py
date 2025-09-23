import pandas as pd
import pymysql
from datetime import date

# Configuración de la conexión MySQL
db_config = {
    'host': 'localhost',
    'user': 'root',
    'password': 'Mb113017$',  # Reemplaza con tu contraseña real
    'database': 'fynsa',
    'charset': 'utf8mb4',
}

# Leer el archivo CSV
nemos_df = pd.read_csv(r'C:\AdjuntosFYNSA\nemos.csv')  # Ajusta la ruta si es necesario

# Conectar a MySQL y crear la tabla, luego insertar datos
connection = pymysql.connect(**db_config)
try:
    with connection.cursor() as cursor:
        # Eliminar tabla si existe
        cursor.execute("DROP TABLE IF EXISTS nemos")

        # Crear tabla 'nemos'
        cursor.execute("""
            CREATE TABLE nemos (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                NEMO VARCHAR(255),
                DESCRIPCION VARCHAR(255),
                estaActivo BOOLEAN DEFAULT TRUE,
                fecha_creado DATE DEFAULT NULL,
                fecha_inactivo DATE DEFAULT NULL
            )
        """)

        # Insertar datos
        insert_query = """
            INSERT INTO nemos (NEMO, DESCRIPCION, estaActivo, fecha_creado, fecha_inactivo)
            VALUES (%s, %s, %s, %s, %s)
        """
        for _, row in nemos_df.iterrows():
            cursor.execute(insert_query, (
                row['NEMO'],
                row['DESCRIPCION'] if pd.notna(row['DESCRIPCION']) else None,
                True,
                date.today(),
                None
            ))
    connection.commit()
    print("✅ Datos insertados correctamente en la tabla 'nemos'.")
finally:
    connection.close()
