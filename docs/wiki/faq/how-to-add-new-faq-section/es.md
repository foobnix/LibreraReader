---
layout: main
version: 5
---
[<](/wiki/faq)

# Cómo agregar una nueva sección de preguntas frecuentes

Si desea agregar una nueva sección de Preguntas frecuentes, cree una carpeta **[Título-muestra-Preguntas frecuentes]** con solo el archivo inglés **index.md** aquí.
[https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faqfont&gt;(https://github.com/foobnix/LibreraReader/tree/master/docs/wiki/faq)

Luego, el script actualiza automáticamente la tabla de contenido de preguntas frecuentes y agrega traducciones para todos los idiomas

Formato del encabezado del archivo: **index.md**

```
---
layout: main
version: 1
---
[<](/wiki/faq)
---

# Title sample FAQ

```

Todas las imágenes relacionadas para esta sección deben estar en la carpeta
```

* Desctiption 1
...
* Desctiption n

||||
|-|-|-|
|![](1.jpg)|![](2.jpg)|![](3.jpg)|

```

Si actualiza la sección de preguntas frecuentes después de la generatoína, debe aumentar +1 la versión {3}:** en el encabezado
```
---
layout: main
version: 2
---
```
