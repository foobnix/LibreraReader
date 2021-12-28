---
layout: main
---

# Copia de seguridad y migración de datos

> Se necesita una copia de seguridad de los datos si tiene la intención de transferir libros a un nuevo dispositivo, nueva carpeta o tarjeta SD

# Exportar (copia de seguridad)

Toque _Exportar_ para guardar todas las configuraciones de la aplicación en un archivo .zip. Elija la carpeta para guardar su archivo .zip y cambie el nombre del archivo, si lo desea.

Así ahorrarás:

* Configuración de la aplicación
* Marcadores
* Progreso de lectura
* Etiquetas de usuario
 
# Importar

Toque _Importar_ y busque el archivo .zip con sus datos de respaldo. Toque el archivo y luego toque _SELECT_

# Migrar

La migración solo reemplazará las rutas de archivo en los archivos de configuración de la aplicación.

La ruta completa se almacena en Configuración. Por ejemplo, si la ruta a su libro (example.pdf) es la siguiente:

/storage/Books/example.pdf

y desea moverlo a la carpeta **MyBooks**, debe cambiar la ubicación en el archivo de configuración de la aplicación a:

/storage/MyBooks/example.pdf

Ejecute _Migrate_ y reemplace:

Camino antiguo: **/ Libros /**
Nueva ruta: **/ MyBooks /**

Toca _ INICIAR MIGRACIÓN_

Si está moviendo su libro a una **tarjeta SD externa**, puede hacerlo fácilmente reemplazando el destino:

_Migrate_:/storage/AAAA-AAAA/Books to/storage/BBBB-BBBB/Libros:

Ruta anterior: **/ storage/AAAA-AAAA /**
Nueva ruta: **/ storage/BBBB-BBBB /**

> **Recordatorio**: no olvide hacer _Export_ primero para tener una copia de seguridad.

|1|2|3|
|-|-|-|
|![](1.png)|![](2.png)|![](3.png)|
