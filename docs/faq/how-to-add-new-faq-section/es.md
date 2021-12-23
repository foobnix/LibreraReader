---
layout: main
---

# Cómo agregar un nuevo tema de preguntas frecuentes

Si desea agregar un nuevo tema de preguntas frecuentes, cree una carpeta **[cualquiera que sea el nombre separado por guiones]** con un solo archivo en-us **index.md** en [aquí](https : //github.com/foobnix/LibreraReader/tree/master/docs/faq).

El script actualizará automáticamente la tabla de contenido de preguntas frecuentes y agregará archivos de configuración regional para todos los idiomas admitidos.

Formato de encabezado de archivo para **index.md**:

```
---
layout: main
---

# Topic Name from Here Goes to the FAQ Page
```

Puede ilustrar su discusión con imágenes (JPEG). Todos los archivos de imagen relacionados con este tema deben colocarse en la carpeta, junto con **index.md**

```
* Image description 1
* Image description 2
* Image description 3

||||
|-|-|-|
|![](1.jpg)|![](2.jpg)|![](3.jpg)|
```
