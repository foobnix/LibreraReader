---
layout: main
---

# Codificación CSS personalizada

> Para la representación de libros, **Librera** generalmente toma los estilos del archivo .css del libro y también aplica su configuración desde la ventana **Preferencias**. También puede hacer uso de uno u otro por separado. Pero a veces no es suficiente. Algunos libros tienen un código CSS tan peculiar que no tiene otra opción que editar sus archivos .css para mejorar la legibilidad. **Librera**, sin embargo, le ofrece otra opción: agregar temporalmente un código CSS personalizado fácilmente removible una vez que haya terminado con el libro cuestionado.

Se admiten tres modos **Estilos**:

1. Documento + Definido por el usuario (toma lo bueno de los dos mundos)
2. Documento (usa solo la configuración .css del libro)
3. Definido por el usuario (usa solo la configuración del usuario especificada en las pestañas de la ventana **Preferencias**)

* El usuario puede cambiar entre los modos a través de una lista desplegable invocada al tocar el enlace junto a _Styles_.
* Toque el icono al lado de la lista _Styles_ para abrir la ventana **Código CSS personalizado** y vaya a ella.

|1|2|3|
|-|-|-|
|![](1.png)|![](2.png)|![](3.png)|

Documento + modo definido por el usuario está habilitado de forma predeterminada

El ejemplo de la Fig. 3 está tomado de la vida real.

{white-space: pre-line;}
Las secuencias de espacio en blanco se colapsan. Las líneas se rompen en los caracteres de nueva línea, en <br> , y según sea necesario para llenar cuadros de línea.

{white-space: pre;}
Se conservan las secuencias de espacios en blanco. Las líneas solo se dividen en los caracteres de nueva línea en la fuente y en <br> elementos.

span {display: block}
p&gt; span {display: en línea}
Elimina líneas vacías muy molestas entre páginas (rectificando defectos de muPDF).
