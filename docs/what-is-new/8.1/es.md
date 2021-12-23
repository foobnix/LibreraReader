---
layout: main
---

# 8.1

Al usar esta aplicación, usted acepta los términos de la [Política de privacidad](/PrivacyPolicy/es)

**Nuevas características y mejoras**

* TTS: grabación de libros completos o rangos de páginas en archivos MP3 (una página por archivo)
* Sincronización en dispositivos Android a través de Google Drive (progreso de lectura, marcadores, etc.)
* Perfiles múltiples
* TTS: reemplazos de caracteres, marcas de tensión manuales, reglas RegEx
* Configuraciones adicionales y navegación en modo Músico
* Funciones adicionales accesibles a través de la interfaz de usuario
* Mejoras diversas para la interfaz de usuario y la funcionalidad

# Sincronización a través de Google Drive

La sincronización está destinada a ser utilizada en múltiples dispositivos Android conectados a Google Drive. Mientras lee un libro, podrá recoger su tableta por la noche justo donde la dejó en su teléfono durante el día. La sincronización es compatible con **todas** aplicaciones de la familia Librera. Y es GRATIS.

Puede sincronizar los siguientes parámetros:

* Progreso de lectura para todos los libros que estás leyendo actualmente (sincronizados o no). Mantenga los nombres de archivo iguales en todos sus dispositivos, y sus libros se sincronizarán automáticamente
* Marcadores
* Lista reciente
* Favoritos y etiquetas

Ejemplos de sincronización

* Habilite la sincronización a través de Google Drive (necesita tener a mano sus credenciales de Google)
* Para sincronizar un libro, invoque su menú y elija _Sync_
* Todos tus libros en la pestaña Favoritos se sincronizarán

||||
|-|-|-|
|![](1.png)|![](3.png)|![](2.png)|
 
 
# Perfiles

Usar perfiles es como tener instancias múltiples de Librera instaladas en su dispositivo, cada una con sus configuraciones, listas de libros, avances de lectura y marcadores por separado. Se le permite crear nuevos perfiles y eliminar los antiguos. Todas las aplicaciones de la familia Librera tienen esta característica.

Los perfiles se almacenan en la memoria interna del dispositivo en /sdcard/Librera/profile.[NAME]. Sus configuraciones, marcadores, progresos de lectura se almacenan en archivos JSON, que se pueden ver con cualquier visor JSON (¡sin embargo, evite modificarlos!).

Una **pulsación larga** sobre el nombre de un perfil abrirá una ventana de alerta para que pueda restaurar su configuración predeterminada (inicial) (sus marcadores y progreso de lectura permanecerán intactos).

||||
|-|-|-|
|![](4.png)|![](5.png)|![](6.png)|

# Reemplazos y diccionarios de TTS

* Toque &quot;Reemplazos&quot; en **Configuración de TTS** y agregue una nueva regla de lectura.
* Reemplace un conjunto de caracteres (entre paréntesis) con un solo carácter para leer o ignorar
* Reemplazar una palabra por otra, por ejemplo, &quot;lib&quot; -&gt; &quot;Librera&quot;
* Agregue marcas de estrés manualmente (si su motor TTS lo admite): &quot;Librera&quot; -&gt; &quot;Libréra&quot;
* Toque &quot;Mostrar&quot; para ver los resultados de los reemplazos
* Agregue un diccionario externo: Librera admite archivos de reemplazo @Voice Aloud y .txt RegEx

||||
|-|-|-|
|![](7.png)|![](8.png)|![](9.png)|

# Configuraciones adicionales en el modo Músico

* Mostrar \ ocultar zonas de toque (delineado con líneas de puntos)
* Indique la última página con franjas rojas (anchas y anchas)
* Resalte los separadores de página para mejorar la legibilidad
* Navegue desde la primera a la última página tocando Zona de página anterior
* Navegue desde la última a la primera página tocando Zona de la página siguiente

||||
|-|-|-|
|![](10.png)|![](11.png)|![](12.png)|

# Funciones y mejoras adicionales a la interfaz de usuario

* Compartir \ copiar páginas como imágenes \ texto (mantenga presionada una miniatura de la página en la ventana _Ir a la página_)
* Soporte para pantallas con muescas
* Nuevos parámetros de clasificación: ordenar por **Editor** y **Fecha de publicación**

||||
|-|-|-|
|![](13.png)|![](14.png)|![](15.png)|

# Misceláneo

* Soporte para formatos MathML y SVG. Habilítelo en Opciones avanzadas (puede ralentizar la carga inicial del libro)
* Opción para ignorar los metadatos en las vistas de Biblioteca y Carpeta (solo se mostrarán los nombres de archivo)
* Marcador móvil, con un indicador flotante (marca automáticamente su posición de lectura actual y le permite volver a él, a través del flotador en la esquina inferior derecha, después de visitas temporales a otras partes y secciones del libro). Para iniciar (agregar) un marcador móvil, marque la casilla **Flotante**
* Permitir el uso de huellas digitales en lugar de contraseña (Android 9+)
* Permita que el tiempo de espera del sistema se use para apagar la pantalla


