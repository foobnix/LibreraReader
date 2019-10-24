---
layout: main
---

# Cómo agregar una nueva sección de preguntas frecuentes

Si desea agregar una nueva sección de preguntas frecuentes, cree una carpeta **[example-faq-title]** con solo el archivo inglés **index.md** aquí.
[https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faqfont&gt;(https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faq)

Luego, el script actualiza automáticamente la tabla de contenido de preguntas frecuentes y agrega traducciones para todos los idiomas

Formato del encabezado del archivo: **index.md**

```
---
layout: main
---

# Example of Title
```

Todas las imágenes relacionadas para esta sección deben estar en la carpeta

```
* Image description 1
* Image description 2
* Image description 3

||||
|-|-|-|
|![](1.jpg)|![](2.jpg)|![](3.jpg)|
```
