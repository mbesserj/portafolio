import os
import win32com.client

# Ruta donde guardar los archivos adjuntos
ruta_destino = os.path.expanduser(r"C:\AdjuntosFYNSA")
os.makedirs(ruta_destino, exist_ok=True)

# Obtener lista de archivos existentes en la carpeta
archivos_en_destino = set(os.listdir(ruta_destino))

# Conexión con Outlook
outlook = win32com.client.Dispatch("Outlook.Application").GetNamespace("MAPI")

# Acceder directamente a la cuenta y carpeta "FYNSA"
cuenta = outlook.Folders.Item("max.besser@pfalimentos.cl")
carpeta_fynsa = cuenta.Folders.Item("FYNSA")

# Obtener todos los correos y ordenarlos por fecha (más recientes primero)
mensajes = carpeta_fynsa.Items
mensajes.Sort("[ReceivedTime]", True)

# Filtrar por no leídos
mensajes = mensajes.Restrict("[Unread] = true")

# Remitente deseado
remitente_deseado = "contactoclientes@fynsa.cl"

# Recorrer correos filtrados
for mensaje in mensajes:
    try:
        if mensaje.Class == 43 and mensaje.SenderEmailAddress.lower() == remitente_deseado:
            archivos_guardados = False
            for adjunto in mensaje.Attachments:
                nombre = adjunto.FileName
                nombre_lower = nombre.lower()

                if "4039" in nombre_lower and nombre_lower.endswith((".xls", ".xlsx", ".xlsm")):
                    nombre_sin_extension, extension = os.path.splitext(nombre)
                    nombre_procesado = f"{nombre_sin_extension}_procesado{extension}"

                    if nombre not in archivos_en_destino and nombre_procesado not in archivos_en_destino:
                        ruta_archivo = os.path.join(ruta_destino, nombre)
                        adjunto.SaveAsFile(ruta_archivo)
                        archivos_en_destino.add(nombre)
                        archivos_guardados = True

            if archivos_guardados:
                mensaje.Unread = False
                mensaje.Save()
    except Exception as e:
        print(f"Error procesando un mensaje: {e}")